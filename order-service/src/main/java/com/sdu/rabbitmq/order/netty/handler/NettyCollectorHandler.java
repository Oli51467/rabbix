package com.sdu.rabbitmq.order.netty.handler;

import com.sdu.rabbitmq.order.entity.dto.RequestInfo;
import com.sdu.rabbitmq.order.utils.NettyUtil;
import com.sdu.rabbitmq.order.utils.UserContextHolder;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

import java.util.UUID;

@Slf4j
public class NettyCollectorHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        String tid = UUID.randomUUID().toString();
        MDC.put("tid", tid);
        RequestInfo info = new RequestInfo();
        info.setUid(NettyUtil.getAttr(ctx.channel(), NettyUtil.UID));
        info.setIp(NettyUtil.getAttr(ctx.channel(), NettyUtil.IP));
        UserContextHolder.set(info);

        ctx.fireChannelRead(msg);
    }
}
