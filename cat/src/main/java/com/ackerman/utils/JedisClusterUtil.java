package com.ackerman.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisCluster;

import java.util.HashSet;


/**
 * @Author: Ackerman
 * @Description:
 * @Date: Created in 下午3:22 18-6-11
 */
@Component
public class JedisClusterUtil {
    private static Logger logger = LoggerFactory.getLogger(JedisClusterUtil.class);

    private static JedisCluster jedisCluster;

    static{
        HashSet<HostAndPort> set = new HashSet<>();
        set.add(new HostAndPort("127.0.0.1", 7000));
        set.add(new HostAndPort("127.0.0.1", 8000));
        set.add(new HostAndPort("127.0.0.1", 9000));
        jedisCluster = new JedisCluster(set);
    }

    public boolean _sismember(String key, String val){
        try {
            return jedisCluster.sismember(key, val);
        }catch (Exception e){
            e.printStackTrace();
        }
        return false;
    }

    public long _srem(String key, String val){
        try{
            return jedisCluster.srem(key, val);
        }catch (Exception e){
            e.printStackTrace();
        }
        return 0;
    }

    public long _sadd(String key, String val){
        try{
            return jedisCluster.sadd(key, val);
        }catch (Exception e){
            e.printStackTrace();
        }
        return 0;
    }

    public long _scard(String key){
        try{
            return jedisCluster.scard(key);
        }catch (Exception e){
            e.printStackTrace();
        }
        return 0;
    }
}