package com.ackerman.utils;

import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * @Author: Ackerman
 * @Description:
 * @Date: Created in 下午2:51 18-6-17
 */
@Component
public class JsonUtil {

    public String getJsonString(int code){
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("code", code);
        return jsonObject.toJSONString();
    }

    public String getJsonString(int code, String msg){
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("code", code);
        jsonObject.put("msg", msg);
        return jsonObject.toJSONString();
    }

    public String getJsonString(int code, Map<String, Object> map){
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("code", code);
        for(Map.Entry<String, Object> entry: map.entrySet()){
            jsonObject.put(entry.getKey(), entry.getValue());
        }
        return jsonObject.toJSONString();
    }

    public String getJsonString(String key,Object val){
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(key, val);
        return jsonObject.toJSONString();
    }

    public <T> T parseJsonString(String key, Class<T> clazz){
        if(key == null)
            return null;

        return JSONObject.parseObject(key, clazz);
    }
}