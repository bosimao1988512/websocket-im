package com.taoge.websocketchat.session;

import com.taoge.websocketchat.model.User;
import com.taoge.websocketchat.utils.JsonUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.ChannelGroupFuture;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.concurrent.ImmediateEventExecutor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Created by 滔哥 on 2020/5/29
 */
@Data
@Slf4j
public class SessionMap {

    public static SessionMap inst = SingleInstance.singleInstance;
    /**
     * 会话集合
     */
    private ConcurrentHashMap<String, ServerSession> sessionMap = new ConcurrentHashMap<>();

    /**
     * 组的集合
     */
    private ConcurrentHashMap<String, ChannelGroup> groupMap = new ConcurrentHashMap<>();

    private static class SingleInstance {
        private static SessionMap singleInstance = new SessionMap();
    }

    /**
     * 增加session 的通道到组
     */
    private void addChannelGroup(ServerSession s) {
        String groupName = s.getGroup();
        if (StringUtils.isEmpty(groupName)) {
            return;
        }
        ChannelGroup group = groupMap.get(groupName);
        if (null == group) {
            group = new DefaultChannelGroup(ImmediateEventExecutor.INSTANCE);
            groupMap.put(groupName, group);
        }
        group.add(s.getChannel());
    }

    /**
     * 和 channel 通道实现双向绑定
     */
    private ServerSession add(ServerSession s) {
        sessionMap.put(s.getSessionId(), s);
        log.info("用户登录:id={},nick={},在线总数: {} ", s.getUser().getUid(), s.getUser().getNickname(), sessionMap.size());
        //增加session 的通道到组
        addChannelGroup(s);
        return s;
    }

    public void addSession(Map<String, String> result, ServerSession session) {
        add(session);
        String json = JsonUtil.pojoToJson(result);
        sendToAll(json, session);
    }

    /**
     * 解除绑定关系
     */

    public ServerSession remove(ServerSession s) {
        Channel channel = s.getChannel();
        SessionMap.inst.removeSession(s.getSessionId());

        //从组中移除通道
        ChannelGroup group = groupMap.get(s.getGroup());
        if (null != group) {
            group.remove(channel);
        }
        return s;
    }

    /**
     * 删除session
     */
    public void removeSession(String sessionId) {
        if (!sessionMap.containsKey(sessionId)) {
            return;
        }
        ServerSession s = sessionMap.get(sessionId);
        sessionMap.remove(sessionId);
        log.info("用户下线:id={}, 在线总数:{}", s.getUser().getUid(), sessionMap.size());
    }

    /**
     * 获取session对象
     */
    public ServerSession getSession(String sessionId) {
        return sessionMap.getOrDefault(sessionId, null);
    }

    /**
     * 根据用户id，获取session对象
     */
    public List<ServerSession> getSessionsBy(String userId) {
        List<ServerSession> list = sessionMap.values()
                .stream()
                .filter(s -> s.getUser().getUid().equals(userId))
                .collect(Collectors.toList());
        return list;
    }

    /**
     * 关闭组
     */
    public void shutdownGracefully() {
        for (ChannelGroup group : groupMap.values()) {
            group.close();
        }
    }

    /**
     * 向组内除开自己的其他成员发送消息
     */
    public void sendToOthers(Map<String, String> result, ServerSession s) {
        //获取组
        ChannelGroup group = groupMap.get(s.getGroup());
        if (null == group) {
            return;
        }

        String json = JsonUtil.pojoToJson(result);
        //自己发送的消息不返回给自己
        Channel channel = s.getChannel();
        //从组中移除通道
        group.remove(channel);
        ChannelGroupFuture future = group.writeAndFlush(new TextWebSocketFrame(json));
        future.addListener(f -> {
            log.debug("sendToOthers finished:{}", json);
            group.add(channel);
        });
    }

    /**
     * 向组内所有成员发送消息
     */
    public void sendToAll(String json, ServerSession s) {
        //获取组
        ChannelGroup group = groupMap.get(s.getGroup());
        if (null == group) {
            return;
        }
        ChannelGroupFuture future = group.writeAndFlush(new TextWebSocketFrame(json));
        future.addListener(f -> log.debug("sendToAll finished:{}", json));
    }

    /**
     * 获取组用户
     */
    public Set<User> getGroupUsers(String room) {
        //获取组
        ChannelGroup group = groupMap.get(room);
        if (null == group) {
            return null;
        }
        Set<User> userSet = new HashSet<>();
        for (Channel next : group) {
            userSet.add(ServerSession.getSession(next).getUser());
        }
        return userSet;
    }

    /**
     * 获取所有组名称
     *
     * @return
     */
    public List<String> getGroupNames() {
        return new ArrayList<>(groupMap.keySet());
    }

    /**
     * 发送消息
     *
     * @param ctx 上下文
     * @param msg 待发送的消息
     */
    public void sendMsg(ChannelHandlerContext ctx, String msg) {
        ChannelFuture sendFuture = ctx.writeAndFlush(new TextWebSocketFrame(msg));
        sendFuture.addListener(f ->log.debug("send finished:{}", msg));
    }

    /**
     * 关闭连接，关闭前发送一条通知消息
     */
    public void closeSession(ServerSession session, String echo) {
        ChannelFuture sendFuture = session.getChannel().writeAndFlush(new TextWebSocketFrame(echo));
        sendFuture.addListener((ChannelFutureListener) future -> {
            log.debug("last package send finished:{},channel will closed", echo);
            future.channel().close();
        });
    }

    /**
     * 关闭连接
     */
    public void closeSession(ServerSession session) {
        ChannelFuture sendFuture = session.getChannel().close();
        sendFuture.addListener((ChannelFutureListener) future -> log.debug("closeSession finished,Nickname:{}", session.getUser().getNickname()));
    }

}
