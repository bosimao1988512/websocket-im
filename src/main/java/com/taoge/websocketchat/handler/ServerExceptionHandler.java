package com.taoge.websocketchat.handler;

import com.taoge.websocketchat.processer.ChatProcesser;
import com.taoge.websocketchat.session.ServerSession;
import com.taoge.websocketchat.session.SessionMap;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by 滔哥 on 2020/5/29
 */
@Slf4j
@ChannelHandler.Sharable
public class ServerExceptionHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ServerSession session = ServerSession.getSession(ctx);
        SessionMap.inst.remove(session);
        session.processError(cause);
    }

    /**
     * 通道 Read 读取 Complete 完成
     * 做刷新操作 ctx.flush()
     */
    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        ServerSession session = ServerSession.getSession(ctx);
        SessionMap.inst.remove(session);
        //处理错误，得到处理结果
        String echo = ChatProcesser.inst().onClose(session);
        //发送处理结果到全部的用户
        SessionMap.inst.sendToAll(echo, session);
        SessionMap.inst.closeSession(session);
    }
}
