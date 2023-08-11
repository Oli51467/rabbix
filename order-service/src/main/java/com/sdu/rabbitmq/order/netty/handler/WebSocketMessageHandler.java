package com.sdu.rabbitmq.order.netty.handler;

import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.spring.SpringUtil;
import cn.hutool.json.JSONUtil;
import com.sdu.rabbitmq.order.entity.dto.WebSocketBaseReq;
import com.sdu.rabbitmq.order.entity.vo.WebSocketAuthorization;
import com.sdu.rabbitmq.order.enums.WebSocketEventTypeEnum;
import com.sdu.rabbitmq.order.service.WebSocketService;
import com.sdu.rabbitmq.order.utils.NettyUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WebSocketMessageHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {

    private WebSocketService webSocketService;

    /*
    handler被添加到channel的pipeline
     */
    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
        this.webSocketService = getService();
    }

    /*
    channel准备就绪
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) {

        System.out.println("Active");
    }

    /*
    channel被关闭
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        log.warn("channel inactive: {}", ctx.channel().id());
        userOffLine(ctx);
    }

    /*
    需要添加握手认证后才能进行后续连接
     */
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent idleStateEvent = (IdleStateEvent) evt;
            // 读空闲
            if (idleStateEvent.state() == IdleState.READER_IDLE) {
                userOffLine(ctx);
            }
        } else if (evt instanceof WebSocketServerProtocolHandler.HandshakeComplete) {
            String token = NettyUtil.getAttr(ctx.channel(), NettyUtil.TOKEN);
            if (StrUtil.isNotBlank(token)) {
                webSocketService.authorize(ctx.channel(), new WebSocketAuthorization(token));
            }
        }
        super.userEventTriggered(ctx, evt);
    }

    /**
     * 用户下线
     * 先进行Websocket逻辑移除 最后关闭连接
     */
    private void userOffLine(ChannelHandlerContext ctx) {
        this.webSocketService.offline(ctx.channel());
        ctx.channel().close();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.warn("连接发生异常，异常消息:{}", cause.getMessage());
        ctx.channel().close();
    }

    /*
        channel中有可读的数据
         */
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame msg) {
        String message = msg.text();
        log.info("收到数据：{}", message);
        if (!JSONUtil.isJson(msg.text())) {
            return;
        }
        WebSocketBaseReq request = JSONUtil.toBean(message, WebSocketBaseReq.class);
        WebSocketEventTypeEnum requestType = WebSocketEventTypeEnum.of(request.getType());
        switch (requestType) {
            case LOGIN:
                break;
            case AUTHORIZE:
                webSocketService.authorize(ctx.channel(), JSONUtil.toBean(request.getData(), WebSocketAuthorization.class));
                break;
            default:
                log.info("未知类型请求");
        }
    }

    private WebSocketService getService() {
        return SpringUtil.getBean(WebSocketService.class);
    }
}
