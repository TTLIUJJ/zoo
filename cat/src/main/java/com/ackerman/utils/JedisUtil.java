package com.ackerman.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import redis.clients.jedis.*;


/**
 * @Author: Ackerman
 * @Description: Redis‘s API
 * @Date: Created in 下午11:25 18-6-4
 */
@Component
public class JedisUtil {
    private static Logger logger = LoggerFactory.getLogger(JedisUtil.class);
    private static JedisPool jedisPool = new JedisPool(new JedisPoolConfig(), "127.0.0.1", 6379);

    public static final String HOT_NEWS_KEY = "hot-news";



    public void del(String key){
        Jedis jedis = null;
        try{
            jedis = jedisPool.getResource();
            jedis.del(key);
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            if(jedis != null)
                jedis.close();
        }
    }


    public void rpush(String key,String val){
        Jedis jedis = null;
        try{
            jedis = jedisPool.getResource();
            jedis.rpush(key, val);
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            if(jedis != null)
                jedis.close();
        }
    }

    public long llen(String key){
        Jedis jedis = null;
        try{
            jedis = jedisPool.getResource();
            return jedis.llen(key);
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            if(jedis != null)
                jedis.close();
        }
        return 0;
    }

    public String lindex(String key, long index){
        Jedis jedis = null;
        try{
            jedis = jedisPool.getResource();
            return jedis.lindex(key, index);
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            if(jedis != null)
                jedis.close();
        }
        return null;
    }

    public String get(String key){
        Jedis jedis = null;
        try{
            jedis = jedisPool.getResource();
            return jedis.get(key);
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            if(jedis != null)
                jedis.close();
        }
        return null;
    }

    public void setex(String key, int seconds, String value){
        Jedis jedis = null;
        try{
            jedis = jedisPool.getResource();
            jedis.setex(key, seconds, value);
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            if(jedis != null)
                jedis.close();
        }
    }
}