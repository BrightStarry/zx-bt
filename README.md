### BT 

- 尝试一个磁力搜索系统，基于ELK进行存储搜索。
- [BitTorrent官网](http://bittorrent.org)

#### 奇淫巧技
- ISO_8859_1 编码可表示0x00 - 0xff 范围(单字节)的所有字符.而不会发生UTF-8/ASCII等编码中的无法识别字符.导致byte[]转为String后,再转回byte[]时
发生变化.

- jdk8给Collection新增的removeIf十分好用.例如
> queue.removeIf(item -> item.getInfoHash().equals(infoHash));

- linux后台运行nohup.不产生.out文件的命令(不加2>&1会额外输出一句ignoring input and redirecting stderr to stdout)
> nohup java -jar /xxx/xxx/xxx.jar >/dev/null 2>&1 &

#### bug
- !!!!! Netty中发送byte[]消息时,需要 writeAndFlush(Unpooled.copiedBuffer(sendBytes)) .这样发送.而不是 writeAndFlush(sendBytes)
否则可能导致,收到回复时,执行了handler的channelReadComplete(),跳过了channelRead()方法(也有说该bug是由于粘包拆包问题导致的).
- 想尝试用类加载器或类自己的getResourceAsStream()方法获取文件时,如果一直为null,可能是因为编译文件未更新(而编译文件不自动更新,可能是因为未将项目加入IDEA maven窗口)
- maven分模块时,如果在父模块写了 < modules >  标签,寻找子模块会有bug.其优先级会变成 相对路径 - 本地仓库 - 远程仓库
- JDBC The last packet sent successfully to the server was 0 milliseconds ago. 异常. 原因是当mysql空闲连接超过一定数量后,  
mysql自动回收该连接,而hibernate还不知道,在连接url后加上&autoReconnect=true&failOverReadOnly=false&maxReconnects=10即可.
#### 注意点
- peer的联系信息编码为6字节长的字符串，也称作”Compact IP-address/ports info”。其中前4个字节是网络字节序（大端序(高字节存于内存低地址，低字节存于内存高地址)）的IP地址，后2个字节是网络字节序的端口号。
  
- node的联系信息编码为26字节长的字符串，也称作”Compact node info”。其中前20字节是网络字节序的node ID，后面6个字节是peer的”Compact IP-address/ports info”。

- byte[]转int等. 是将byte[0] 左位移最高位数,例如将2个byte转为int,是( bytes[1] & 0xFF) | (bytes[0] & 0xFF) << 8 而不是 ( bytes[0] & 0xFF) | (bytes[1] & 0xFF) << 8.  
其原因很简单,按照从左到右的四位,2个byte 00011100, 11100011. 显然是要变为 0001110011100011,将第一个byte放到第二个byte前面,那么也就是让第一个byte左位移8位即可

- java中的byte范围为-128 - 127,如果为负数,可通过 & 0xff转为int,其值为256 + 该负数, 例如(byte)-1 & 0xff = 255

 
#### 简述
- BitTorrent是分发文件的协议。它通过URL标识内容，旨在与网络无缝集成。
它优于简单的HTTP的优点是，当同时发生同一文件的多个下载时，下载器会相互上传，从而使文件源可以支持非常大量的下载程序，并且只有轻微的负载增加。

- 节点(node):只要实现它的DHT(Distributed Hash Table)协议就可以将自己作为一个节点注册到该P2P网络中去.每个节点的大致功能是(此处暂不考虑下载功能):
    - 可发送find_node请求,向其他节点(第一次查找时通常为该P2P网络上著名的节点)查找指定ID(SHA1压缩,160bit组成的随机值)的节点(目标节点).
        - 如果对方节点保存了目标node信息,即返回你该node的id/ip/port等.
        - 如果没有,它则返回你,和目标节点ID最相似(根据ID进行异或计算出相似度)的Top8节点的信息,然后你需要再去访问这些节点,继续发送find_node请求查找目标节点.
        - 当然,他也可以返回你空,表示他还没有存储任何节点
        - 我们可将查找到的每个节点保存到自己的存储中(routing table)
        - 通常,对方也会将我们保存到他们的路由表中
    - 当自己收到其他节点发送的find_node请求时,可按照常规流程查找出最相似的Top8个节点返回.不过也可以无论如何都将自己伪装成其中一个相似节点返回回去,以让自己更容易被别人找到.
        - 或者 返回一个空,表示自己没有存储任何节点
    - 可使用get_peers请求,根据资源ID查找某个资源,返回信息中包含有对应资源的ip:ports. 
    - 当收到别人的get_peers请求时,请求中的infohash就是我们需要的种子.
    - ping: 检测某个节点状态,已更新自己的路由表
    - 当某个节点有了某个资源时,会向之前向它请求过的所有节点,发送通知(应该就是在他自己的路由表中广播消息),BT嗅探器也正是基于这点进行操作.
    
- 综上所述,我们只需要从一个目标节点开始,不停地查找其他其他节点,并当其他节点请求我们时,让回复的nodes中包含自己,以扩大社交面积, 然后等待我们认识的nodes通知我们它拥有哪些资源,从而我们向它请求这些资源即可.


#### 根据info_hash获取到种子的metadata
- 参照BEP_10协议. 该协议仅用于获取.torrent文件的info-字典字段(该部分也就是metadata).对于该项目,也只需要该信息即可.



#### 函数式编程应用
- 在编写Bencode过程中，将一个byte[]进行解码，需要循环调用若干个方法，当某个方法没有抛出异常，成功返回，即为解码成功。
- 在其他函数式编程语言中，就可直接将函数组成数组，循环调用，达到效果。
- 而在java中，JDK8之前，都只能将方法一个一个写出来，依次调用，十分不优雅。（此处是指这些方法都在同一个类中，不使用更为冗杂的策略模式的情况下）
- 所以，我使用了JDK8中提供的BiFunction类进行优化。该类可传入一个函数实现，将两个任意类型的输入参数，转为一个任意类型的输出参数。
- 创建一个该对象数组，在函数实现中调用自己原本要调用的方法即可。

#### 路由表分段锁实现思路
- 给该树的每一个TrieNode节点分配一个lockId.该id允许少量重复.
- 创建一个锁数组.路由表初始化时就创建若干数目的锁.
- 给某个节点加解锁时,使用 lockId % 锁数量 = 数组下标. 用该数组下标从锁数组中获取锁.进行加解锁.
- 注意,同一代码块中,不能同时加若干把锁. 否则可能出现死锁问题.   

#### get_peers请求任务实现思路
- 当收到他人get_peers请求后,将信息中他人正在查找的info_hash加入查找队列.
- 从队列中取出新的info_hash,去重,向路由表所有节点发送get_peers请求.这若干个请求全部使用同一个消息id.并将该消息id和该info_hash缓存
- 收到回复后,从缓存中查找其对应的是哪一个info_hash.如果没找到,说明任务过期.
    - 如果没找到,继续向回复回来的更近的8个节点发送请求(也使用同一个消息id)
    - 找到了.清除该消息id的缓存.停止该info_hash的查找.
- 超过一定时间都未找到.缓存将被清除.

#### 责任链模式
- Netty的Handler收到消息后.先进行解析.解析出具体的消息类型.消息状态等.再交由处理类管理器执行处理.
- 将每个方法的每个状态(例如find_node的请求/ping的回复等)都定义为一个处理器.
- 将每个处理器加入到处理类管理器类中注册.并串联起来.
- 每一个处理器判断该消息是否由自己处理. 是,执行处理,返回结果; 否,交由下一处理器处理;
- 处理器的处理顺序可通过Spring的@Order修改.
- 处理器抽象类中定义了模版方法.包括异常捕获,处理判断等.
