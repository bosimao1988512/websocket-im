package com.taoge.websocketchat.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;

/**
 * Created by 滔哥 on 2020/5/28
 */
@Data
@Slf4j
@AllArgsConstructor
public class User {
    private String uid;
    private String nickname;

    @Override
    public boolean equals(Object o){
        if (this==o){
            return true;
        }
        if (null==o || getClass() != o.getClass()){
            return false;
        }
        User user=(User)o;
        return uid.equals(user.getUid());
    }

    @Override
    public int hashCode(){
        return Objects.hash(uid);
    }
}
