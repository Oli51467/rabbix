package com.sdu.rabbix.order.netty;

import com.sdu.rabbix.order.netty.handler.HeartbeatHandler;
import com.sdu.rabbix.order.netty.handler.HttpHeadersHandler;
import com.sdu.rabbix.order.netty.handler.NettyCollectorHandler;
import com.sdu.rabbix.order.netty.handler.WebSocketMessageHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.NettyRuntime;
import io.netty.util.concurrent.Future;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

@Slf4j
@Configuration
public class WebSocketServer {

    public static final int WEB_SOCKET_PORT = 19000;
    private final EventLoopGroup masterGroup = new NioEventLoopGroup(1);
    private final EventLoopGroup workerGroup = new NioEventLoopGroup(NettyRuntime.availableProcessors());

    @PostConstruct
    public void start() {
        try {
            run();
        } catch (Exception e) {
            System.exit(500);
        }
    }

    @PreDestroy
    public void destroy() {
        Future<?> future = masterGroup.shutdownGracefully();
        Future<?> future1 = workerGroup.shutdownGracefully();
        future.syncUninterruptibly();
        future1.syncUninterruptibly();
        log.info("WebSocketServer Shutdown gracefully");
    }

    public void run() throws InterruptedException {
        ServerBootstrap server = new ServerBootstrap();     // 服务端主程序
        server.group(masterGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, 10240) // 服务端可连接队列大小
                .option(ChannelOption.SO_REUSEADDR, true) // 参数表示允许重复使用本地地址和端口
                .handler(new LoggingHandler(LogLevel.INFO)) // 为 bossGroup 添加 日志处理器
                .childOption(ChannelOption.TCP_NODELAY, true) // 是否禁用Nagle算法 简单点说是否批量发送数据 true关闭 false开启。 开启的话可以减少一定的网络开销，但影响消息实时性
                .childOption(ChannelOption.SO_KEEPALIVE, true) // 保活开关2h没有数据服务端会发送心跳包
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ChannelPipeline pipeline = ch.pipeline();
                        // 心跳检测 事件会通过责任链模式传递
                        pipeline.addLast(new IdleStateHandler(120, 0, 120));
                        pipeline.addLast(new HeartbeatHandler());
                        // websocket基于http协议，http编解码器
                        pipeline.addLast(new HttpServerCodec());
                        // 对写大数据流的支持
                        pipeline.addLast(new ChunkedWriteHandler());
                        // tcp数据在传输过程中是分段的，HttpObjectAggregator可以把多个段聚合起来
                        pipeline.addLast(new HttpObjectAggregator(8192));
                        // 保存用户ip
                        pipeline.addLast(new HttpHeadersHandler());
                        pipeline.addLast(new NettyCollectorHandler());
                        /*
                         * websocket 服务器处理的协议，用于指定给客户端连接访问的路由 : /ws
                         * 本handler会帮你处理一些繁重的复杂的事
                         * 会帮你处理握手动作： handshaking（close, ping, pong） ping + pong = 心跳
                         * 对于websocket来讲，都是以frames进行传输的，不同的数据类型对应的frames也不同
                         * WebSocketServerProtocolHandler 核心功能是把 http协议升级为 ws 协议，保持长连接；
                         */
                        pipeline.addLast(new WebSocketServerProtocolHandler("/ws"));
                        pipeline.addLast(new WebSocketMessageHandler());
                    }
                });
        server.bind(WEB_SOCKET_PORT).sync();
        log.info("WebSocketServer Start");
    }
}
