### BT 

- 尝试一个磁力搜索系统，基于ELK进行存储搜索。
- [BitTorrent官网](http://bittorrent.org)


#### 重构
- 将其重构为如下模块,以便将爬虫和网站分开部署
    - top: pom项目,直接继承自SpringBoot依赖.并被parent项目引入.以达到增加<properties>直接修改spring boot定义的版本号的目的.(详见下面的bug记录)
    - parent: pom项目,依赖版本定义.
    - common: 通用的一些工具类/实体(例如metadata)/枚举类等.
    - spider: 爬虫模块,DHT爬虫的实现.只负责收集有效的metadata信息,存入es.
    - web: 磁力网站web项目. 



### 前提
很久之前,在其他磁力网站搜索羞羞的东西的时候,我就想过一个问题:这些网站的数据从何而来,总不可能在和搜索引擎一样在广域网中胡乱搜索把.   
过年时在家无聊,便开始研究起磁力链接来. 
       
刚开始时看[BitTorrent协议官网](http://bittorrent.org),看得一头雾水.然后开始搜索相关博客,大多数都只是炫耀自己写的爬虫,并没有详细说明其实现.     

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


### 简介
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

      
#### 总结
- 目前只实现了基本功能.接下去准备性能调优,并将数据库换为elasticsearch,以达到更好的性能.
- 目前如果所有任务都开启,在阿里云1核2G服务器上运行,将线程数都尽量调低也只能勉强运行(可能是我udp端口开启太多).主要是cpu占用99%.
- 在2核4G服务器上测试了下,最快的一次能保持在每分钟近30个有效metadata信息的入库(cpu占用也为99%). 
- 目前从其他磁力网站获取已有磁力信息,我只写了两个网站的解析方法,可再增加若干网站.并且,可将这些方法抽离成策略类,并定义统一接口,直接使用Spring统一注入List<该接口>,以更好的扩展.
- 初始化任务中的节点,当前也是从配置文件中导入,可以从自己的node表中,获取topN个节点.
- 可以给各任务增加单独的暂停功能,例如当从其他网站获取metadata任务队列满了,就暂停findNode任务等.
- 然后,完成前端页面,实现一个完整的磁力搜索网站.

#### 统计接口/stat
- 可查看各队列元素数目.

#### 线程属性配置
- 需要尽可能保证fetchMetadataOtherWebTask队列是稳定的,不为0,也不能很快就满.根据这个需求,设置findNodeTask线程数和fetchMetadataOtherWebTask线程数.

#### 奇淫巧技
- IDEA在pom中,选择依赖的version中的版本值. 再 C + A + v.可自动抽离.

- ISO_8859_1 编码可表示0x00 - 0xff 范围(单字节)的所有字符.而不会发生UTF-8/ASCII等编码中的无法识别字符.导致byte[]转为String后,再转回byte[]时
发生变化.

- jdk8给Collection新增的removeIf十分好用.例如
> queue.removeIf(item -> item.getInfoHash().equals(infoHash));

- linux后台运行nohup.不产生.out文件的命令(不加2>&1会额外输出一句ignoring input and redirecting stderr to stdout)
> nohup java -jar /xxx/xxx/xxx.jar >/dev/null 2>&1 &

- 在Application配置@ComponentScan("com.zx.bt")可以扫描到jar中的对应包下的bean(例如注解了@Compoent的)

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


#### websocket
- 准备增加弹幕聊天功能
- [Spring Web Socket 文档](https://docs.spring.io/spring/docs/4.3.15.BUILD-SNAPSHOT/spring-framework-reference/htmlsingle/#websocket)
