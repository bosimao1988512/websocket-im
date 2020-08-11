package com.taoge.websocketchat.handler;

import com.taoge.websocketchat.processer.ChatProcesser;
import com.taoge.websocketchat.session.ServerSession;
import com.taoge.websocketchat.session.SessionMap;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;

import java.util.Map;

/**
 * Created by 滔哥 on 2020/5/29
 */
@ChannelHandler.Sharable
public class TextWebSocketFrameHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {
    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, TextWebSocketFrame textWebSocketFrame) throws Exception {
        //增加消息的引用计数（保留消息），并将他写到 ChannelGroup 中所有已经连接的客户端
        ServerSession session = ServerSession.getSession(channelHandlerContext);
        System.err.println(textWebSocketFrame.text());
        Map<String, String> result = ChatProcesser.inst().onMessage(textWebSocketFrame.text(), session);
        if (result != null && null != result.get("type")) {
            switch (result.get("type")) {
                case "msg":
                    SessionMap.inst.sendToOthers(result, session);
                    break;
                case "init":
                    SessionMap.inst.addSession(result, session);
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        //是否握手成功，升级为 Websocket 协议
        //if (evt == WebSocketServerProtocolHandler.ServerHandshakeStateEvent.HANDSHAKE_COMPLETE)
        if (evt instanceof WebSocketServerProtocolHandler.HandshakeComplete) {
            // 握手成功，移除 HttpRequestHandler，因此将不会接收到任何HTTP消息 参考《Netty实战十二之WebSocket》
            // 并把握手成功的 Channel 加入到 ChannelGroup 中
            ServerSession session = new ServerSession(ctx.channel());
            String echo = ChatProcesser.inst().onOpen(session);
            SessionMap.inst.sendMsg(ctx, echo);
        } else if (evt instanceof IdleStateEvent) {
            IdleStateEvent stateEvent = (IdleStateEvent) evt;
            if (stateEvent.state() == IdleState.READER_IDLE) {
                ServerSession session = ServerSession.getSession(ctx);
                SessionMap.inst.remove(session);
                session.processError(null);
            }
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }
}
