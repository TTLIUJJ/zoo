package com.ackerman.utils;

import com.ackerman._thrid.UserModel;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * @Author: Ackerman
 * @Description:
 * @Date: Created in 上午10:54 18-6-8
 */
@Component
public class LocalInfo {
    private ThreadLocal<Map<String, Object>> threadLocal;

    private Map<String, Object> info;

    public LocalInfo(){
        threadLocal = new ThreadLocal<>();
        info = new HashMap<>();
        threadLocal.set(info);
    }

    public void setUser(UserModel user){
        info.put("user", user);
    }

    public UserModel getUser(){
        return (UserModel) info.get("user");
    }

    public void setGlobalTicket(String ticket){
        info.put("global", ticket);
    }

    public String getGlobalTicket(){
        return (String) info.get("global");
    }

    public void removeUser(){
        info.remove("user");
    }

    public void removeGlobalTicket(){
        info.remove("global");
    }

    public void removeAll(){
        info.clear();
        threadLocal.remove();
    }
}