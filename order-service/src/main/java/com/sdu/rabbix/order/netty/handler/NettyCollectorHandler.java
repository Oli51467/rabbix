package com.sdu.rabbix.order.netty.handler;

import com.sdu.rabbix.common.domain.dto.RequestInfo;
import com.sdu.rabbix.common.utils.UserContextHolder;
import com.sdu.rabbix.order.utils.NettyUtil;
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
