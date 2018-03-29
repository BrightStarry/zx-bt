### BT 
一个磁力搜索系统，基于Elasticsearch进行存储搜索，基于Netty实现BT协议的node和peer间的通信。     
并基于Bootstrap实现了简单的响应式页面，基于WebSocket实现了弹幕功能。

#### 目前
- 网站当前已经部署:[福利球](https://www.fuliqiu.com)
- web模块和Elasticsearch单节点部署在阿里云1核2G1M的ECS上;spider模块部署在阿里云1核2G6M的ECS上(cpu占用80%左右,带宽基本全部占用);MySQL也是用的阿里云的RDS;
- 目前爬取速率保持在800个有效Metadata记录/5分钟.
- 目前数据在200W+，并以每天15W+的速度入库。


#### 实现过程
- 刚开始看[BitTorrent官网](http://bittorrent.org)，但一头雾水。
- [一步一步教你写BT种子嗅探器](https://www.jianshu.com/p/5c8e1ef0e0c3)这篇博文很详细地说明了种子嗅探器的原理和实现思路。
- [抓包分析BitTorrent协议，很全面的文章](http://www.aneasystone.com/archives/2015/05/analyze-magnet-protocol-using-wireshark.html)
这篇博文通过抓包，详细地展示了每个请求和响应的具体数据，并且详细说明了通过BEP-009协议，用info_hash获取Metadata的过程。
- [Go语言实现的DHT项目](https://github.com/shiyanhui/dht)参考了该项目的部分代码，因为该项目不是Java语言，而且之前也没接触过GO语言，所以看得还是比较累的。

#### 优化
- 单个服务器可同时开启多个端口，每个端口使用各自的nodeId和routingTable。
>   理论上端口越多，nodeId越分散，越可能和更多的node交互，获取更多的infoHash.  

- 将所有需要发送find_node请求的节点保存到缓冲队列，异步发送。
>   同步发送时，开启的节点稍微多些就容易阻塞，并且无法控制实时的并发。

- 自己实现了Becode编解码

- 除了BEP-009协议外，实现了若干解析类，解析数个其他磁力搜索网站已有的磁力资源，获取metadata（使用HttpClient+Jsoup）。

- 支持热更换解析其他磁力网站的解析器类，因为其他网站可能突然不稳定等。

- 将几个步骤分成若干个任务，各自维护各自的队列，可调控各个任务的线程数、队列长度等参数，自定义配置速率。

- 将原来的从数据库查询已有数据判断是否重复，优化为使用布隆过滤器在info_hash入队时统一去重，并每x小时重置布隆过滤器。

- 使用分段锁优化RoutingTable，保证并发下的线程安全。

- 提供了一个状态接口，可实时查看各个任务队列、各个端口的状态，以及5分钟内metadata入库数量。

- 将爬虫模块区分出主节点和从节点，实现集群部署。
>   主节点维护自己的布隆过滤器，并提供一个判断info_hash是否重复的接口；从节点内部不再维护过滤器，通过调用主节点的接口去重。

- 使用nginx实现wss和https。


#### 模块划分
- 将其重构为如下模块,以便将爬虫和网站分开部署
    - top: pom项目,直接继承自SpringBoot依赖.并被parent项目依赖.以达到增加<properties>直接修改spring boot定义的版本号的目的.(详见下面的bug记录)
    - parent: pom项目,依赖版本定义.
    - common: 通用的一些工具类/实体(例如metadata)/枚举类等.
    - spider: 爬虫模块,DHT爬虫的实现.只负责收集有效的metadata信息,存入es.
    - web: 磁力网站web项目,响应式页面，弹幕功能。

#### 爬虫模块
>
    com.zx.bt.spider:
        config: 配置类相关
            BeanConfig: 注入各类bean
            Config: 所有自定义配置相关，具体参考application.yml中的属性，有每个属性详细注解
        controller: 接口
            FilterController: 主节点才会启用的接口（使用@ConditionalOnProperty注解），提供布隆过滤器的put方法去重。
            MainController: 提供状态查看 和 热更换解析其他磁力网站的解析类接口
        convertor: 转换器相关，只有一个类，将一个Map转为Metadata对象
        dto: 数据传输实体类
            bt: BitTorrent协议的各个方法相关实体类    
        entity: 实体类，node和infoHash
        parser: 解析其他磁力网站的解析器相关类，使用模版方法模式。
        socket: netty实现TCP、UDP协议相关类
            processor: 和其他node间的udp连接的处理类，使用责任链模式，处理BT协议的各个方法
            Sender: 通用的发送器类，封装了发送BT协议各方法的函数，并保存了所有开启的端口的channel，用于发送请求
            UDPServer: udp协议连接建立类。
        store: 各类存储相关
            InfoHashFilter: 自己封装的布隆过滤器的接口，包含了主节点使用的布隆过滤器和从节点使用的布隆过滤器
            RoutingTable: 路由表          
        task: 自定义任务相关
        util: 
            BeanUtil: bean转map、获取bean及父类所有属性等方法
            Bencode: 自己实现的Bencode编解码
            BTUtil: BT协议相关的若干方法
            HtmlResolver: 简单封装了Jsoup的一些方法
            HttpClientUtil: httpClient连接类，维护了一个连接池，封装了各类请求方法，并封装了简易的header及cookie策略。
    其他还有一些类在common模块中            
>


### 详细思路
>
    刚开始时看[BitTorrent协议官网](http://bittorrent.org),看得一头雾水.  
    
    [一步一步教你写BT种子嗅探器](https://www.jianshu.com/p/5c8e1ef0e0c3)这篇博客才让我对其有了一知半解.但是它在更新到要如何通过info_hash获取torrent的metadata信息时,却断更了...   
    
    按照它的的思路,在我基本完成官网中的BEP-005协议.在本地运行时,却基本无法收到announce_peer请求.并且发送的并发稍微大些(即使是单线程发送)都会不停引发Network dropped connection on reset: no further information异常.  
    
    为了避免该异常,我开始给任务之间增加阻塞队列,然后开启自定义数量的线程进行相关任务. 但情况并没有任何改善.只能将其和无法收到announce_peer请求的原因,都归咎于内网问题.    
    
    因为,我将其发到云服务器上后,该异常不再发生,并且运行一段时间后,可接收到大量announce_peer请求(目前,大部分info_hash都是通过announce_peer获取到的).     
    
    然后我开始着手如何通过info_hash获取到torrent的metadata信息.网上的普遍说法是两种方式:     
    1. 从迅雷种子库(以及其他一些磁力网站的接口)获取.大多数的实现方式,都是拼接URL + infoHash + ".torrent".但是大多数能查到的接口都已经失效.   
    2. 通过bep-009协议获取.但是我看了官网的该协议,仍是一头雾水. 
    
    对于第二种方式,直到我找到了这篇[全面文章](http://www.aneasystone.com/archives/2015/05/analyze-magnet-protocol-using-wireshark.html),它对torrent的整个获取过程中的报文请求响应进行了很详细的解析.
    还忍着语言的隔阂看了[该Go语言实现的DHT项目](https://github.com/shiyanhui/dht)的代码,才成功实现.
    
    在我成功通过bep-009协议连接peer获取到metadata信息后,才开始研究如何通过其他网站获取已有的metadata信息.    
    我这才发现...说什么从迅雷种子库,以及各类网站的xxx.torrent接口获取的都不靠谱(因为已经全部失效(我猜想应该是目前大部分网站开始被监管,不再存储整个torrent文件的缘故)).  
      
    metadata信息其实可以直接爬取其他磁力搜索网站,解析其html,进行获取.因为目前的的磁力搜索网站某个磁力的详情页地址大多是xxxx.com/<40位16进制infoHash>.html的形式.
    而我们要获取的信息,都会显示在页面上,例如名字/长度/子文件目录/子文件长度等信息.
    
    在实现过程中,对于Bencode编解码,我本来是使用了github上的一个项目.后来自己实现了.     
    还自己实现了一个基于词典树的RoutingTable.而且为了支持并发操作..     
    我脑洞清奇地给它增加了分段锁(对每个节点生成递增的分段锁id,操作某个节点就使用该 锁id 取余 分段锁数量 作为下标从锁数组中获取锁. 我总觉得不太好...目前测试时高并发下仍有少许安全问题,但正常运行下没有任何问题)     
    
    此外,增加了布隆过滤器(guava包)对info_hash进行去重.考虑到info_hash的实时有效性(某个此时无效的infoHash指不定啥时候就有效了),定时清空过滤器(每次清空后都会导入所有有效的(获取到了metadata信息)的info_hash).
>


#### 简介
此处我尽量用直白的话简述下BitTorrent(更详细的自行参见上文提到的博客).   

首先,BitTorrent是一种各机器相互通讯的协议.就像Http协议,浏览器和服务器都遵守了相应的报文规则,才能交互解析,得以通讯.BitTorrent也是这样的基于UDP和TCP通讯的协议.   
 
该协议的目的是为了分发大体积文件(.羞羞的电影等).而下载某个文件,则需要连接到遵守该协议的某台服务器(一旦遵守该协议,并实现对应的协议部分,例如提供文件下载功能,该服务器就被称为peer),下载该文件的某个部分.     

也正是因此,当该文件被x个服务器(peer)拥有,就可以同时从这些peer下载文件的不同部分,然后将其拼接为整个文件(这样速度就会很快).   

Torrent(种子)就保存了一个文件的一些信息,名字/长度/子文件目录/子文件长度等信息,其中最重要是拥有该文件的peers服务器,也因此,可以通过种子,向这些peers发送下载请求.下载到文件.

但是在DHT(分布式哈希表)出现之前,所有节点都需要连接到Tracker服务器,以获取到拥有某个文件的peers(或者Torrent).     

DHT协议基于udp通讯,规定每个node(遵守BitTorrent协议,未实现提供下载功能的服务器)内部存储一个RoutingTable(路由表).该表存储了其他的node(或peer)节点.    

每个node的信息包括nodeId(随机的20个byte/ip/port(其中ip/port在udp通讯的包中都已经携带,所以,实际上最重要的就是nodeId))   

而且定义了几种方法进行node间的交互(此处设A节点为请求方,B节点为响应方, 这些方法就是发送对应规则的请求响应报文,例如Http的GET/POST/DELETE等)
```
    ping: A向B发送请求,测试对方节点是否存活. 如果B存活,需要响应对应报文
    find_node: A向B查询某个nodeId. B需要从自己的路由表中找到对应的nodeId返回,或者返回离该nodeId最近的8个node信息. 然后A节点可以再向B节点继续发送find_node请求
    get_peers: A向B查询某个infoHash(可以理解为一个Torrent的id,也是由20个字节组成.该20个字节并非随机,是由Torrent文件中的metadata字段(该字段包含了文件的主要信息,
    也就是上文提到的名字/长度/子文件目录/子文件长度等信息,实际上一个磁力搜索网站提供的也就是这些信息).进行SH1编码生成的). 如果B拥有该infoHash的信息,则返回该infoHash
    的peers(也就是可以从这些peers处下载到该种子和文件). 如果没有,则返回离该infoHash最近的8个node信息. 然后 A节点可继续向这些node发送请求.
    announce_peer: A通知B(以及其他若干节点)自己拥有某个infoHash的资源(也就是A成为该infoHash的peer,可提供文件或种子的下载),并给B发送下载的端口.
```
此外,node间的距离是通过nodeId进行异或计算的(也就是160个bit间进行异或)得出一个值,值越小,则距离越近.

由此可得出,越是高位的bit不相同(异或值为1),则值越大,距离越远(因为假设两个nodeId第1位就不同,其异或值必然大于2^160).   

DHT出现之后,假设一个新的节点想要加入该网络,只需要获取到已经在网络中的任何一个node信息,向其发送find_node请求即可.想要获取某个info_hash的peer,也可直接发送get_peers,而无需连接到Tracker服务器.  

如此,DHT可理解为一个去中心化的P2P网络.
         


#### 项目介绍
- 该项目的主要功能就是,将自己加入dht网络,不停发送find_node请求,并正确回应其他节点的其他请求,等待其他节点的get_peers请求或announce_peer请求,以获取到info_hash.  
然后通过info_hash获取到metadata信息,入库即可.    

- 待实现功能: 使用elasticsearch替换MySql存储metadata信息,并实现较好的全文检索,以实现一个磁力搜索网站.

- 项目基于Netty实现TCP/UDP通讯,基于Caffeine实现内部缓存,基于HttpClient+Jsoup实现Http请求和解析,基于Guava实现布隆过滤器.   
并基于java8函数式编程实现了Bencode编解码,基于TrieTree实现路由表,基于Elasticsearch实现全文检索

- 项目主要任务流程:
    - 初始化任务(InitTask): 开启若干个端口的udp服务. 初始化布隆过滤器,导入所有已经获取到metadata的info_hash. 
    导入已知的若干有效节点,并用所有端口的udp连接依次向其发送find_node请求. 以及启动其他所有任务.
    - UDP处理器: 处理DHT协议中的所有方法. 使用了责任链模式,解耦处理方法. 不同的dht方法交由对应的处理类处理.
    - 当收到announce_peer或get_peers请求后,获取到其携带的info_hash,将该info_hash交由FetchMetadataByOtherWebTask任务处理
    - 从其他网站获取metadata任务(FetchMetadataByOtherWebTask): 
        - 先将info_hash通过布隆过滤器去重,然后加入阻塞队列.
        - 若干个线程阻塞地从队列获取info_hash,通过httpClient访问若干个其他磁力搜索网站.
        - 如果这些网站中已经有保存该info_hash,则通过Jsoup解析html,获取到名字/长度/子文件目录/子文件长度等信息
        - 如果没有,则将该info_hash加入GetPeersTask任务阻塞队列
    - GetPeersTask任务:
        - 单个线程从队列中获取info_hash,使用每个端口的udp连接向路由表中离该info_hash最近的8个节点发送get_peers请求.
        并将请求的消息id加入缓存,也将该info_hash信息加入FetchMetadataByPeerTask任务.
        - 当有收到对应回复时,如果是peer信息,将其入库,并清除该get_peers任务,如果不是,继续向返回的更接近的x个node发送get_peers请求.
        - 当缓存过期时,如果还未找到,则对应get_peers任务自动失效(因为后面再发送过来的该任务的get_peers回复,已经无法从缓存中找到消息id了)
        - 当同时进行的get_peers任务超过若干数目,暂停开启新任务
    - 从peer获取metadata任务(FetchMetadataByPeerTask):
        - 从GetPeersTask任务加入的info_hash会被放到一个延时队列(DelayQueue). 其延期时间和GetPeersTask任务的缓存过期时间相同.
        - 有若干个线程从该队列获取到延期结束的info_hash,从数据库中查询是否有其对应的peer信息,如果没有,则结束.
        - 如果有,则取出所有peers.和所有peer建立tcp连接,根据bep-009协议,发送获取metadata的请求.
        - 如果获取到了,则入库; 否则,结束任务.

#### 奇淫巧技
- IDEA在pom中,选择依赖的version中的版本值. 再 C + A + v.可自动抽离.

- ISO_8859_1 编码可表示0x00 - 0xff 范围(单字节)的所有字符.而不会发生UTF-8/ASCII等编码中的无法识别字符.导致byte[]转为String后,再转回byte[]时
发生变化.

- jdk8给Collection新增的removeIf十分好用.例如
> queue.removeIf(item -> item.getInfoHash().equals(infoHash));

- linux后台运行nohup.不产生.out文件的命令(不加2>&1会额外输出一句ignoring input and redirecting stderr to stdout)
> nohup java -jar /xxx/xxx/xxx.jar >/dev/null 2>&1 &

- 在Application配置@ComponentScan("com.zx.bt")可以扫描到jar中的对应包下的bean(例如注解了@Compoent的)

- Lombok的@NonNull注解,之前我一直没怎么使用过.它相当于增加了一个if(xxx == null)的判断,并可抛出携带为空的变量的变量名的NPE.






#### 阻塞队列的实现
无意中看了下ArrayBlockingQueue的源码. 主要在于维护一把锁和几个Condition. 例如put()时,如果元素满了,就使用一个condition让该当前线程等待.  
而在删除方法中,在删除成功后,会使用同一个Condition唤醒某个线程(是signals而不是signalAll). 就酱.   

此外需要注意的是,等待前获取的锁是调用ReentrantLock的lockInterruptibly()方法获取的可中断锁  
(该锁,在已经中断的状态下,不能执行等待方法,需要处理InterruptedException; 普通锁如果在已经中断的状态下,仍可以进入等待状态);   

### Elasticsearch

#### 创建索引
PUT http://106.14.7.29:9200/indexName
JSON:
```json
    {
      "settings":{
        "number_of_replicas": 0
      },
      "mappings":{
        "metadata":{
          "dynamic": false,
          "properties":{
            "id": {
              "type": "long"
            },
            "infoHash":{
              "type": "keyword"
            },
            "infoString": {
              "type": "keyword"
            },
            "name": {
              "type": "text",
              "index": true,
              "analyzer": "ik_max_word",
              "search_analyzer": "ik_max_word"
            },
            "length":{
              "type": "long"
            },
            "type":{
              "type": "integer"
            },
            "hot":{
              "type": "long"
            },
            "createTime":{
              "type": "date",
              "format": "strict_date_optional_time||epoch_millis"
            },
            "updateTime":{
              "type": "date",
              "format": "strict_date_optional_time||epoch_millis"
            }
          }
        }
      }
    }
```

#### 测试分词
POST http://106.14.7.29:9200/indexName/_analyze?pretty=true
JSON:
```json
    {
    	"text": "fdfsdfsf",
    	"analyzer":"ik_max_word"
    }
```


#### 前端插件记录
- bootstrap-switch: bootstrap的开关样式.并包含了模态框
- bootstrap-select: 选择框插件
- jquery.barrager.js: 弹幕插件


#### websocket
- 准备增加弹幕聊天功能
- [Spring Web Socket 文档](https://docs.spring.io/spring/docs/4.3.15.BUILD-SNAPSHOT/spring-framework-reference/htmlsingle/#websocket)


####  Nginx + HTTPS + WebSocket
- 在Nginx + HTTPS的基础上,想要连接wss,需要在nginx.conf中的http的server配置中,增加如下:
```
location /websocket {
    proxy_pass http://proxy_fuliqiu;
    proxy_http_version 1.1;
    proxy_set_header Upgrade $http_upgrade;
    proxy_set_header Connection "upgrade";
}
```
其中,proxy_fuliqiu只的是先前配置的upstream的方向代理配置名字

- 整个nginx.conf
```
events {
    worker_connections  1024;
}

http {
    include       mime.types;
    default_type  application/octet-stream;

    #日志格式 main
    log_format  main  '$remote_addr - $remote_user [$time_local] "$request" '
                      '$status $body_bytes_sent "$http_referer" '
                      '"$http_user_agent" "$http_x_forwarded_for"';

    sendfile        on;

    keepalive_timeout  65;

    server {
        listen       80;
        server_name  fuliqiu.com;
        #让80请求,重定向到https
        return 301    https://$host$request_uri;

        #charset koi8-r;

        #access_log  logs/host.access.log  main;
        #日志位置
        access_log  /log/nginx/access.log  main;
    }

    upstream proxy_fuliqiu {
        #如果你要测试，把这里换成你自己要代理后端的ip
        server localhost:8081 weight=1;
	server localhost:8082 backup;
        #当负载两台以上用ip来hash解决session的问题，一台就别hash了。
        #ip_hash;
    }

    # HTTPS server
    server {
        listen       443;
        server_name  fuliqiu.com;
        ssl on;
        ssl_certificate      ssl/214415723560141.pem;
        ssl_certificate_key  ssl/214415723560141.key;
        ssl_session_timeout  5m;
        ssl_ciphers AESGCM:ALL:!DH:!EXPORT:!RC4:+HIGH:!MEDIUM:!LOW:!aNULL:!eNULL;
        ssl_protocols TLSv1 TLSv1.1 TLSv1.2;
        ssl_prefer_server_ciphers  on;
        location / {
            #这里proxy_test是上面的负载的名称，映射到代理服务器，可以是ip加端口,或url
            proxy_pass       http://proxy_fuliqiu;
            proxy_set_header Host      $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        }
        location /websocket {
            proxy_pass http://proxy_fuliqiu;
            proxy_http_version 1.1;
            proxy_set_header Upgrade $http_upgrade;
            proxy_set_header Connection "upgrade";
        }
        #日志位置
        access_log  /log/nginx/access.log  main;
    }
}
```


#### bug
- !!!!! Netty中发送byte[]消息时,需要 writeAndFlush(Unpooled.copiedBuffer(sendBytes)) .这样发送.而不是 writeAndFlush(sendBytes)
否则可能导致,收到回复时,执行了handler的channelReadComplete(),跳过了channelRead()方法(也有说该bug是由于粘包拆包问题导致的).
- 想尝试用类加载器或类自己的getResourceAsStream()方法获取文件时,如果一直为null,可能是因为编译文件未更新(而编译文件不自动更新,可能是因为未将项目加入IDEA maven窗口)
- maven分模块时,如果在父模块写了相对路径的 < modules >  标签,寻找子模块会有bug.其优先级会变成 相对路径 - 本地仓库 - 远程仓库
- JDBC The last packet sent successfully to the server was 0 milliseconds ago. 异常. 原因是当mysql空闲连接超过一定数量后,  
mysql自动回收该连接,而hibernate还不知道,在连接url后加上&autoReconnect=true&failOverReadOnly=false&maxReconnects=10即可.

- Thymeleaf模版的功能需要3.0+版本才能实现.如果项目直接继承自SpringBoot,可通过直接定义 <properties>修改版本号.
但如果继承了自己的父项目,则无法这样修改. 可另建一个项目(zx-bt-top),继承spring-boot,然后在自己的父项目中,依赖管理该项目.
```xml
<!--新建的用于继承SpringBoot的项目-->
<groupId>com.zx.bt</groupId>
<artifactId>zx-bt-top</artifactId>
<version>1.0</version>
<packaging>pom</packaging>
<name>zx-bt-top</name>
<description>该项目仅用于继承Spring Boot依赖,而不是自己管理其依赖, 以达到通过增加&lt;properties&gt;标签直接修改Spring Boot定义的依赖的版本号</description>
<parent>
	<groupId>org.springframework.boot</groupId>
	<artifactId>spring-boot-starter-parent</artifactId>
	<version>1.5.10.RELEASE</version>
	<relativePath/>
</parent>

<!--自己的父项目-->
 <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>com.zx.bt</groupId>
                <artifactId>zx-bt-top</artifactId>
                <version>${zx-bt.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
</dependencyManagement>
```
然后需要在新建的用于继承SpringBoot的项目,增加<properties>,以修改版本.

- !!! 有一个很坑的东西,那就是SpringMVC的URL传参,无法传逗号.其他常用符号都可以,但是如果值中间有逗号.就会被截断.

- Spring使用@ServerEndpoint注解实现WebSocket服务器,被注解的类必须包含空的构造函数,否则会提示init异常.

- webSocket中配置的消息解码器一直无法启用,导致onMessage方法中因直接使用解码后的对象作为参数,报类型不匹配异常.
    - 因为我以前做过类似的弹幕功能,所以代码直接照搬了过来,但是,我几乎检查了所有地方,都是一样,却仍是不行.
    - 之后,我试着验证了下该解码器的初始化方法(当收到第一个消息时初始化该解码器).然后我才看到他有个willDecode()方法,
    用于判断该解码器是否支持当前消息. 而这个方法我返回了false.所以总结如下两点:
        - 一些容易忽略的细节必须加以注释
        - 多看源码,看方法注解,防止一些愚蠢的bug(但这个解码器类的源码中没有任何注解,mmp)

- 如果es的字段长度超过32766字节,将提示过长,无法被存入的异常,可在建立索引的mappings的对应字段中增加"ignore_above": 256,
表示超过该长度仍然可以被存入.但被索引. 并且该字段要关闭分词.
- 索引修改,可参考[该博客](http://blog.csdn.net/napoay/article/details/52012249). 修改成功后可通过head插件查看.

- 一个愚蠢的bug, 该hql执行失败,提示没有id字段:
> 	@Query(value = "SELECT city FROM keyword_record GROUP BY ip  ORDER BY id LIMIT 0,?1",nativeQuery = true)
>   List<KeywordRecord> findDistinctIpTopX(int size);

看了好久才发现~~~hibernate的返回对象的所有属性需要和返回结果一一匹配,改为如下即可:
> List<String> findDistinctIpTopX(int size);

- 出现如下异常:
>  Result window is too large, from + size must be less than or equal to: [10000] but was [22440]. See the scroll api for a more efficient way to request large data sets. This limit can be set by changing the [index.max_result_window] index level setting.

表示分页页数过多.可通过如下请求修改
> PUT http://106.14.7.29:9200/metadata/_settings   主体: {"index":{"max_result_window":100000000}}


#### 注意点
- peer的联系信息编码为6字节长的字符串，也称作”Compact IP-address/ports info”。其中前4个字节是网络字节序（大端序(高字节存于内存低地址，低字节存于内存高地址)）的IP地址，后2个字节是网络字节序的端口号。
- node的联系信息编码为26字节长的字符串，也称作”Compact node info”。其中前20字节是网络字节序的node ID，后面6个字节是peer的”Compact IP-address/ports info”。
- byte[]转int等. 是将byte[0] 左位移最高位数,例如将2个byte转为int,是( bytes[1] & 0xFF) | (bytes[0] & 0xFF) << 8 而不是 ( bytes[0] & 0xFF) | (bytes[1] & 0xFF) << 8.  
其原因很简单,按照从左到右的四位,2个byte 00011100, 11100011. 显然是要变为 0001110011100011,将第一个byte放到第二个byte前面,那么也就是让第一个byte左位移8位即可
- java中的byte范围为-128 - 127,如果为负数,可通过 & 0xff转为int,其值为256 + 该负数, 例如(byte)-1 & 0xff = 255
- SpringBoot项目依赖自己的父项目时,打包可能不包含依赖. 需要添加如下:
- 在linux启动SpringBoot jar项目时,如果想使用外部配置文件,需要在该外部配置文件所在目录执行启动命令
```xml
<build>
		<plugins>
			<plugin>
				<groupId>org.springframework.boot</groupId>
				<artifactId>spring-boot-maven-plugin</artifactId>
				<configuration>
					<mainClass>com.zx.bt.web.WebApplication</mainClass>
				</configuration>
				<executions>
					<execution>
						<goals>
							<goal>repackage</goal>
						</goals>
					</execution>
				</executions>
			</plugin>
		</plugins>
		<finalName>zx-bt-web</finalName>
	</build>
```

- 使用Nginx + WebSocket的话,由于Nginx的proxy_read_timeout(Default: 60s;)属性设置,webSocket会自动断开连接.

- 之前我给find_node任务设置了20个线程,然后暂停x毫秒进行发送,但我忽然发觉这样简直是麻瓜.因为这样相当于自己给自己挖了个二十
个并发线程争夺锁的坑...于是改为10个线程,不暂停发送.