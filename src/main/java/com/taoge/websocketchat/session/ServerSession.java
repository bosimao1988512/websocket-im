package com.taoge.websocketchat.session;

import com.taoge.websocketchat.model.User;
import com.taoge.websocketchat.processer.ChatProcesser;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.AttributeKey;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 用户实现服务端会话管理的核心
 * Created by 滔哥 on 2020/5/29
 */
@Data
@Slf4j
public class ServerSession {
    public static final AttributeKey<ServerSession> SESSION_KEY = AttributeKey.valueOf("SESSION_KEY");

    private Channel channel;
    private User user;
    private final String sessionId;
    private String group;

    /**
     * 登录状态
     */
    private boolean isLogin = false;

    /**
     * session中存储的session 变量属性值
     */
    private Map<String, Object> map = new HashMap<String, Object>();

    public ServerSession(Channel channel) {
        this.channel = channel;
        this.sessionId = UUID.randomUUID().toString().replaceAll("-", "");
        log.info(" ServerSession 绑定会话 " + channel.remoteAddress());
        channel.attr(ServerSession.SESSION_KEY).set(this);
    }

    //反向导航
    public static ServerSession getSession(ChannelHandlerContext ctx) {
        return ctx.channel().attr(ServerSession.SESSION_KEY).get();
    }

    public static ServerSession getSession(Channel channel) {
        return channel.attr(ServerSession.SESSION_KEY).get();
    }

    public synchronized void set(String key, Object value) {
        map.put(key, value);
    }

    public synchronized <T> T get(String key) {
        return (T) map.get(key);
    }


    public boolean isValid() {
        return null != getUser();
    }

    //关闭连接channel
    public synchronized void close() {
        ChannelFuture future = channel.close();
        future.addListener((ChannelFutureListener) future1 -> {
            if (!future1.isSuccess()) {
                log.error("CHANNEL_CLOSED error ");
            }
        });
    }

    //关闭连接session
    public static void closeSession(ChannelHandlerContext ctx) {
        ServerSession session = ctx.channel().attr(ServerSession.SESSION_KEY).get();
        if (null != session && session.isValid()) {
            session.close();
            SessionMap.inst.remove(session);
        }
    }

    public void processError(Throwable error) {
        //处理错误，得到处理结果
        String result = ChatProcesser.inst().onError(this, error);
        //发送处理结果到其他的组内用户
        SessionMap.inst.sendToAll(result, this);
        String echo = ChatProcesser.inst().onClose(this);
        //关闭连接， 关闭前发送一条通知消息
        SessionMap.inst.closeSession(this, echo);
    }
}
