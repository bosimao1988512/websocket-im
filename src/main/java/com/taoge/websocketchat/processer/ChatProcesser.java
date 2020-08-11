package com.taoge.websocketchat.processer;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.taoge.websocketchat.model.User;
import com.taoge.websocketchat.session.ServerSession;
import com.taoge.websocketchat.utils.JsonUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by 滔哥 on 2020/5/29
 */
@Slf4j
public class ChatProcesser {

    public static ChatProcesser inst() {
        return SingleInstance.chatProcesser;
    }

    private static class SingleInstance {
        private static ChatProcesser chatProcesser = new ChatProcesser();
    }

    /**
     * 连接建立成功调用的方法
     *
     * @param s 会话
     */
    public String onOpen(ServerSession s) throws IOException {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("type", "bing");
        jsonObject.addProperty("sendUser", "系统消息");
        jsonObject.addProperty("id", s.getSessionId());
        return jsonObject.toString();

        /*Map<String, String> result = new HashMap<>();
        result.put("type", "bing");
        result.put("sendUser", "系统消息");
        result.put("id", s.getSessionId());
        return JsonUtil.pojoToJson(result);*/
    }

    /**
     * 连接关闭调用的方法
     */
    public String onClose(ServerSession s) {
        User user = s.getUser();
        if (null != user) {
            Map<String, String> result = new HashMap<>();
            result.put("type", "init");
            result.put("msg", user.getNickname() + "离开房间");
            result.put("sendUser", "系统消息");
            return JsonUtil.pojoToJson(result);
        }
        return null;
    }

    /**
     * 收到客户端消息后调用的方法
     *
     * @param message 消息内容
     * @param session 会哈
     */
    public Map<String, String> onMessage(String message, ServerSession session) {
        TypeToken typeToken = new TypeToken<HashMap<String, String>>() {
        };
        Map<String, String> map = JsonUtil.jsonToPojo(message, typeToken);
        Map<String, String> result = new HashMap<>();
        switch (map.get("type")) {
            case "msg":
                result.put("type", "msg");
                result.put("msg", map.get("msg"));
                result.put("sendUser", session.getUser().getNickname());
                break;
            case "init":
                session.setGroup(map.get("room"));
                String nick = map.get("nick");
                session.setUser(new User(session.getSessionId(), nick));
                result.put("type", "init");
                result.put("msg", nick + "成功加入房间");
                result.put("sendUser", "系统消息");
                break;
            case "ping":
                break;
            default:
                break;
        }

        return result;
    }

    /**
     * 连接发生错误时的调用方法
     *
     * @param session 会话
     * @param error   异常
     */
    public String onError(ServerSession session, Throwable error) {

        //捕捉异常信息
        if (null != error) {
            log.error(error.getMessage());
        }

        User user = session.getUser();
        if (user == null) {
            return null;
        }
        String nick = user.getNickname();

        Map<String, String> result = new HashMap<>();
        result.put("type", "init");
        result.put("msg", nick + "离开房间");
        result.put("sendUser", "系统消息");

        String json = JsonUtil.pojoToJson(result);
        return json;
    }
}
