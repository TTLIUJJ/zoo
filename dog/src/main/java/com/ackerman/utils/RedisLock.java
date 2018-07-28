package com.ackerman.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class RedisLock {
    private static Logger logger = LoggerFactory.getLogger(RedisLock.class);

    @Autowired
    private RedisUtil redisUtil;

    public boolean tryLock(String mutex, long timeout){
        Long current = System.currentTimeMillis();
        timeout *= 1000;
        try{
            while (true){
                if((System.currentTimeMillis() - current) > timeout){
                    break;
                }
                else{

                }
            }
        }
    }

    private boolean innerTryLock(String mutex){
        try{

        }catch (Exception e){

        }

        return false;
    }

    public void releaseLock(String mutex){
        redisUtil._del(mutex);
    }

    private boolean checkIfLockTimeout(String mutex, long timeStamp){
        try{
            return  timeStamp > Long.valueOf(redisUtil._get(mutex));
        }catch (Exception e){
            logger.error("检查过期时间异常, mutex=" + mutex);
        }
        return true;
    }
}
