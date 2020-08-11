package com.taoge.websocketchat.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.nio.charset.StandardCharsets;

/**
 * Created by 滔哥 on 2020/5/29
 */
public class JsonUtil {
    private static final Gson gson;

    static {
        //不需要html escape
        gson = new GsonBuilder().disableHtmlEscaping().create();
    }

    /**
     * 使用谷歌 Gson 将 POJO 转成字符串
     */
    public static String pojoToJson(Object obj) {
        //String json = new Gson().toJson(obj);
        return gson.toJson(obj);
    }

    /**
     * Object对象转成字节数组
     */
    public static byte[] object2JsonBytes(Object obj) {
        return pojoToJson(obj).getBytes(StandardCharsets.UTF_8);
    }

    /**
     * 将字符串转成 POJO对象
     */
    public static <T> T jsonToPojo(String json, Class<T> clazz) {
        return gson.fromJson(json, clazz);
    }

    /**
     * 将节数组转化成对象
     */
    public static <T> T jsonBytes2Object(byte[] bytes, Class<T> clazz) {
        String json = new String(bytes, StandardCharsets.UTF_8);
        return jsonToPojo(json, clazz);
    }

    public static <T> T jsonToPojo(String json, TypeToken typeToken) {
        return gson.fromJson(json, typeToken.getType());
    }
}
