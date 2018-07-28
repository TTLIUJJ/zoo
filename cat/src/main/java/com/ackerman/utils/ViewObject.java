package com.ackerman.utils;

import java.util.HashMap;
import java.util.Map;

/**
 * @Author: Ackerman
 * @Description:
 * @Date: Created in 上午8:38 18-6-17
 */
public class ViewObject {
    private Map<String, Object> map;

    public ViewObject(){
        map = new HashMap<>();
    }

    public void set(String key, Object val){
        map.put(key, val);
    }

    public Object get(String key){
        return map.get(key);
    }
}