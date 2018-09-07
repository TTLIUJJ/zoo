package com.ackerman.util;

import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class RedisUtil {
    private volatile static RedisUtil redisUtil;
    private JedisPool jedisPool;

    private RedisUtil(){
        jedisPool = new JedisPool(new JedisPoolConfig(), "127.0.0.1", 6379);
    }

    public static RedisUtil getRedisUtilInstance(){
        if(redisUtil == null){
            synchronized (RedisUtil.class){
                if(redisUtil == null){
                    redisUtil = new RedisUtil();
                }
            }
        }
        return redisUtil;
    }



}
