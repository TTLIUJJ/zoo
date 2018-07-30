package com.ackerman.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.util.HashSet;

@Component
public class RedisUtil {
    private static Logger logger = LoggerFactory.getLogger(RedisUtil.class);
    private static JedisPool jedisPool = new JedisPool(
            new JedisPoolConfig(), "127.0.0.1", 6379
    );

    private static JedisCluster jedisCluster;
    static {
        HashSet<HostAndPort> set = new HashSet<HostAndPort>();
        set.add(new HostAndPort("127.0.0.1", 7000));
        set.add(new HostAndPort("127.0.0.1", 8000));
        set.add(new HostAndPort("127.0.0.1", 9000));
        jedisCluster = new JedisCluster(set);
    }


    /*
        ------------------ 单机操作 ---------------------------
     */


    /*
        ------------------ 集群操作 ---------------------------
     */

    public String _get(String key){
        try{
            return jedisCluster.get(key);
        }catch (Exception e){
            logger.error("Redis集群get()异常， key:" + key);
        }
        return null;
    }

    public String _set(String key, String val){
        try{
            return jedisCluster.set(key, val);
        }catch (Exception e){
            logger.error("Redis集群set()异常， key:" + key);
        }
        return null;
    }

    public Long _del(String key){
        try{
            return jedisCluster.del(key);
        }catch (Exception e){
            logger.error("Redis集群del()异常， key:" + key);
        }
        return -1L;
    }

    public long _setnx(String key, String val){
        try{
            return jedisCluster.setnx(key, val);
        }catch (Exception e){
            logger.error("Redis集群setnx()异常， key:" + key);
        }
        return -1L;
    }

    public String _getSet(String key, String val){
        try{
            return jedisCluster.getSet(key, val);
        }catch (Exception e){
            logger.error("Redis集群getSet()异常， key:" + key);
        }
        return null;
    }
}
