# 前言
由于目前项目通信服务端采用的Netty实现的TCPServer，为了保证数据的实时推送以及避免频繁查询数据库，所以采用websocket在接收到消息后立即推送到html页面
代码已经上传至GitHub  链接: [nettyAndWebsocket](https://github.com/noTryNoHard/nettyAndWebsocket)
#  maven依赖
```xml 
<dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- SpringBoot集成websocket -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-websocket</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
            <exclusions>
                <exclusion>
                    <groupId>org.junit.vintage</groupId>
                    <artifactId>junit-vintage-engine</artifactId>
                </exclusion>
            </exclusions>
        </dependency>

        <!-- https://mvnrepository.com/artifact/io.netty/netty-all -->
        <dependency>
            <groupId>io.netty</groupId>
            <artifactId>netty-all</artifactId>
            <version>4.1.51.Final</version>
        </dependency>

        <!-- SpringBoot集成thymeleaf模板 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-thymeleaf</artifactId>
        </dependency>

		<!-- hutool工具包 -->
        <dependency>
            <groupId>cn.hutool</groupId>
            <artifactId>hutool-all</artifactId>
            <version>5.3.9</version>
        </dependency>

        <!-- Lombok插件 -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

    </dependencies>
```
# SpringBootApplication启动类
```java 

@SpringBootApplication
public class NettyAndWebsocketApplication {

    public static void main(String[] args) throws Exception {
        SpringApplication.run(NettyAndWebsocketApplication.class, args);
        //启动服务端
        NettyServer nettyServer = new NettyServer();
        nettyServer.run();
    }
}

```
# Netty相关代码
## NettyServer
Netty启动类及相关配置
```java 
@Slf4j
@Component
@Configuration
public class NettyServer {

    private Integer port = 8773;

    final EventLoopGroup bossGroup = new NioEventLoopGroup();
    final EventLoopGroup workerGroup = new NioEventLoopGroup();

    public void run() throws Exception {
        //创建BossGroup 和 WorkerGroup
        //说明
        //1. 创建两个线程组 bossGroup 和 workerGroup
        //2. bossGroup 只是处理连接请求 , 真正的和客户端业务处理，会交给 workerGroup完成
        //3. 两个都是无限循环
        //4. bossGroup 和 workerGroup 含有的子线程(NioEventLoop)的个数
        //默认实际 cpu核数 * 2
        try {
            //创建服务器端的启动对象，配置参数
            ServerBootstrap bootstrap = new ServerBootstrap();
            //使用链式编程来进行设置
            //设置两个线程组
            bootstrap.group(bossGroup, workerGroup)
                    //使用NioSocketChannel 作为服务器的通道实现
                    .channel(NioServerSocketChannel.class)
                    // 设置线程队列得到连接个数(也可以说是并发数)
                    .option(ChannelOption.SO_BACKLOG, 2048)
                    //设置保持活动连接状态
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    //.handler(null) // 该 handler对应 bossGroup , childHandler 对应 workerGroup
                    //.childOption(ChannelOption.TCP_NODELAY,true)//socketchannel的设置,关闭延迟发送
                    .childHandler(new NettyInitializer());

            log.info(".....服务器 is ready.....");
            //绑定一个端口并且同步, 生成了一个 ChannelFuture 对象
            //启动服务器(并绑定端口)
            ChannelFuture cf = bootstrap.bind(port).sync();
            //给cf 注册监听器，监控我们关心的事件
            cf.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    if (cf.isSuccess()) {
                        log.info("监听端口[{}]成功!", port);
                    } else {
                        log.error("监听端口[{}]失败!", port);
                    }
                }
            });
            //对关闭通道进行监听
            cf.channel().closeFuture().sync();
        } catch (Exception e) {
            log.error(" netty服务启动异常 " + e.getMessage());
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
```
## NettyServerHandler
用于处理各种Netty连接、端口、接入等
```java 
@Slf4j
@Component
@ChannelHandler.Sharable
public class NettyServerHandler extends ChannelInboundHandlerAdapter {

    private static Lock lock_1 = new ReentrantLock();

    private static Lock lock_2 = new ReentrantLock();

    private static Lock lock_3 = new ReentrantLock();

    private static Lock lock_4 = new ReentrantLock();

    /**
     * 管理一个全局map，保存连接进服务端的通道数量
     */
    private static final ConcurrentHashMap<ChannelId, ChannelHandlerContext> CHANNEL_MAP = new ConcurrentHashMap<>();

    @Override //数据读取完毕
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        //writeAndFlush 是 write + flush
        //将数据写入到缓存，并刷新
        //一般讲，我们对这个发送的数据进行编码
        //ctx.writeAndFlush(Unpooled.copiedBuffer("hello, 客户端~(>^ω^<)喵1", CharsetUtil.UTF_8));
    }

    /**
     * 处理异常, 一般是需要关闭通道
     *
     * @param ctx
     * @param cause
     *
     * @throws Exception
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
        log.info("服务端异常关闭" + ctx.channel());
    }


    /**
     * @param ctx
     *
     * @DESCRIPTION: 有客户端连接服务器会触发此函数
     * @return: void
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        lock_1.lock();
        try {
            //获取连接通道唯一标识
            ChannelId channelId = ctx.channel().id();
            //如果map中不包含此连接，就保存连接
            if (CHANNEL_MAP.containsKey(channelId)) {
                log.info("客户端【" + channelId + "】是连接状态，连接通道数量: " + CHANNEL_MAP.size());
            } else {
                //保存连接
                CHANNEL_MAP.put(channelId, ctx);

                log.info("客户端【" + channelId + "】连接netty服务器");
                log.info("连接通道数量: " + CHANNEL_MAP.size());
            }
        } finally {
            lock_1.unlock();
        }
    }

    /**
     * @param ctx
     *
     * @DESCRIPTION: 有客户端终止连接服务器会触发此函数
     * @return: void
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        lock_2.lock();
        try {
            ChannelId channelId = ctx.channel().id();
            //包含此客户端才去删除
            if (CHANNEL_MAP.containsKey(channelId)) {
                //删除连接
                CHANNEL_MAP.remove(channelId);
                System.out.println();
                log.info("客户端【" + channelId + "】退出netty服务器");
                log.info("连接通道数量: " + CHANNEL_MAP.size());
            }
        } finally {
            lock_2.unlock();
        }
    }


    /**
     * 1. ChannelHandlerContext ctx:上下文对象, 含有 管道pipeline , 通道channel, 地址
     * 2. Object msg: 就是客户端发送的数据 默认Object
     * <p>
     * 读取数据实际(这里我们可以读取客户端发送的消息)
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        lock_3.lock();
        try {
            log.info("服务器读取线程 " + Thread.currentThread().getName() + " channle = " + ctx.channel());
            Channel channel = ctx.channel();
            //将 msg 转成一个 ByteBuf
            //ByteBuf 是 Netty 提供的，不是 NIO 的 ByteBuffer.
            ByteBuf buf = (ByteBuf) msg;
            //得到此时客户端的数据长度
            int bytes_length = buf.readableBytes();
            //组件新的字节数组
            byte[] buffer = new byte[bytes_length];
            buf.readBytes(buffer);
            final String allData = NettyByteAndStringUtils.byteToHex(buffer);

            log.info("进入服务端数据：" + allData);
            ctx.executor().execute(new NettySendThread(ctx, allData));

        } finally {
            lock_3.unlock();
        }
    }


    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        lock_4.lock();
        try {
            String socketString = ctx.channel().remoteAddress().toString();
            if (evt instanceof IdleStateEvent) {
                IdleStateEvent event = (IdleStateEvent) evt;
                if (event.state() == IdleState.READER_IDLE) {
                    log.info("Client: " + socketString + " READER_IDLE 读超时");
                    ctx.disconnect();
                } else if (event.state() == IdleState.WRITER_IDLE) {
                    log.info("Client: " + socketString + " WRITER_IDLE 写超时");
                    ctx.disconnect();
                } else if (event.state() == IdleState.ALL_IDLE) {
                    log.info("Client: " + socketString + " ALL_IDLE 总超时");
                    ctx.disconnect();
                }
            }
        } finally {
            lock_4.unlock();
        }
    }
}
```
## NettyDataSvervice 
用于处理接收的数据，比如处理黏包、拆包、数据过滤等，以及数据解析后续操作
```java 
@Slf4j
public class NettyDataSvervice {

    private static final ReentrantLock Lock = new ReentrantLock();

    /**
     * 将传送过来的数据进行解析，包括异或运算 （第一次服务器端给客户端发）
     *
     * @param ReceiveData
     *
     * @return
     */
    public static String sendData(String ReceiveData) {
        final ReentrantLock putLock = Lock;
        log.info("接收数据" + ReceiveData);
        putLock.lock();
        try {
            //此处需要读数据进行校验以及分包黏包处理，本文主要提供思路所以省略
            /*
            *   处理分包黏包、拆分、解析等
            */
            //进入数据解析
            parseData(ReceiveData);
            try {
                //数据帧WebSocket推送
                WebSocketServer.BroadCastInfo(ReceiveData);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return ReceiveData;
        } finally {
            putLock.unlock();
        }
    }

    /**
     * 数据解析入库处理
     */
    public static void parseData(String ReceiveData) {
    	//此处省略真实入库操作
        System.out.println("执行入库操作");
    }
}
```
# websocket相关代码
## WebSocketConfig 开启WebSocket支持
```java
@Configuration
public class WebSocketConfig {
    @Bean
    public ServerEndpointExporter serverEndpointExporter() {
        return new ServerEndpointExporter();
    }
}
```

## WebSocketServer
websocket 入口以及方法调用
```java
@ServerEndpoint(value = "/ws/asset")
@Component
public class WebSocketServer {

    @PostConstruct
    public void init() {
        System.out.println("websocket 加载");
    }

    private static Logger log = LoggerFactory.getLogger(WebSocketServer.class);
    private static final AtomicInteger OnlineCount = new AtomicInteger(0);
    // concurrent包的线程安全Set，用来存放每个客户端对应的Session对象。
    private static CopyOnWriteArraySet<Session> SessionSet = new CopyOnWriteArraySet<Session>();


    /**
     * 连接建立成功调用的方法
     */
    @OnOpen
    public void onOpen(Session session) {
        SessionSet.add(session);
        // 在线数加1
        int cnt = OnlineCount.incrementAndGet();
        log.info(String.valueOf(session.getRequestURI()));
        log.info("有连接加入，当前连接数为：{},sessionId={}", cnt, session.getId());
        SendMessage(session, "连接成功");
    }

    /**
     * 连接关闭调用的方法
     */
    @OnClose
    public void onClose(Session session) {
        SessionSet.remove(session);
        int cnt = OnlineCount.decrementAndGet();
        log.info("有连接关闭，当前连接数为：{}", cnt);
    }

    /**
     * 收到客户端消息后调用的方法
     *
     * @param message 客户端发送过来的消息
     */
    @OnMessage
    public void onMessage(String message, Session session) {
        log.info("来自客户端的消息：{}", message);
        SendMessage(session, "收到消息，消息内容：" + message);
    }

    /**
     * 出现错误
     *
     * @param session
     * @param error
     */
    @OnError
    public void onError(Session session, Throwable error) {
        log.error("发生错误：{}，Session ID： {}", error.getMessage(), session.getId());
        error.printStackTrace();
    }

    /**
     * 发送消息，实践表明，每次浏览器刷新，session会发生变化。
     *
     * @param session
     * @param message
     */
    public static void SendMessage(Session session, String message) {
        try {
            session.getBasicRemote().sendText(message);
        } catch (IOException e) {
            log.error("发送消息出错：{}", e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 群发消息
     *
     * @param message
     *
     * @throws IOException
     */
    public static void BroadCastInfo(String message) throws IOException {
        for (Session session : SessionSet) {
            if (session.isOpen()) {
                SendMessage(session, message);
            }
        }
    }

    /**
     * 指定Session发送消息
     *
     * @param sessionId
     * @param message
     *
     * @throws IOException
     */
    public static void SendMessage(String message, String sessionId) throws IOException {
        Session session = null;
        for (Session s : SessionSet) {
            if (s.getId().equals(sessionId)) {
                session = s;
                break;
            }
        }
        if (session != null) {
            SendMessage(session, message);
        } else {
            log.warn("没有找到你指定ID的会话：{}", sessionId);
        }
    }
}

```

## websocket.html
```html 
<!DOCTYPE html>
<html lang="zh">
<head>
    <meta charset="utf-8">
    <title>websocket通讯</title>
</head>
<script src="https://cdn.bootcss.com/jquery/3.3.1/jquery.js"></script>
<body>
<div style="margin-left: 25%;margin-top: 10%">
    <h3>WebSocket测试，客户端接收到的消息如下：</h3>
    <textarea id="messageId" readonly="readonly" cols="150" rows="20"></textarea>
    <input type="button" value="清空内容" onclick="document.getElementById('messageId').value=''">
</div>
<script type="text/javascript">
    var socket;
    if (typeof (WebSocket) == "undefined") {
        console.log("遗憾：您的浏览器不支持WebSocket");
    } else {
        console.log("恭喜：您的浏览器支持WebSocket");
        //实现化WebSocket对象
        //指定要连接的服务器地址与端口建立连接
        //注意ws、wss使用不同的端口。我使用自签名的证书测试，
        //无法使用wss，浏览器打开WebSocket时报错
        //ws对应http、wss对应https。
        socket = new WebSocket("ws://localhost:8080/ws/asset");
        //连接打开事件
        socket.onopen = function () {
            console.log("Socket 已打开");
            // socket.send("消息发送测试(From Client)");
        };
        //收到消息事件
        socket.onmessage = function (msg) {
            $("#messageId").append(msg.data + "\n");
            console.log(msg.data);
        };
        //连接关闭事件
        socket.onclose = function () {
            console.log("Socket已关闭");
        };
        //发生了错误事件
        socket.onerror = function () {
            console.log("Socket发生了错误");
        };
        //窗口关闭时，关闭连接
        window.unload = function () {
            socket.close();
        };
    }
</script>
</body>
</html>
```
# 项目启动
![在这里插入图片描述](https://img-blog.csdnimg.cn/20200904162656835.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80OTYwMjUxMw==,size_16,color_FFFFFF,t_70#pic_center)
浏览器地址栏输入 http://localhost:8080/webSocket 如下图所示
![在这里插入图片描述](https://img-blog.csdnimg.cn/20200904162931344.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80OTYwMjUxMw==,size_16,color_FFFFFF,t_70#pic_center)
# 模拟TCP发送数据
使用串口调试助手模拟客户端连接Netty服务端并发送数据
![在这里插入图片描述](https://img-blog.csdnimg.cn/20200904163900830.gif#pic_center)
这是我们同步打开网页查看数据是否通过websocket推送成功到前台![在这里插入图片描述](https://img-blog.csdnimg.cn/20200904164742981.gif#pic_center)
至此我们就完成了Netty接收数据并处理，Websocket推送数据到前台。
全部代码都已上传到[GitHub仓库](https://www.csdn.net/)

本人使用的数据发送是hex16进制，具体数据格式以自身业务需求位置，比如Json格式等。