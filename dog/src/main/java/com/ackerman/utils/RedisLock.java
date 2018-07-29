package com.ackerman.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Random;

public class RedisLock {
    private static Logger logger = LoggerFactory.getLogger(RedisLock.class);

    @Autowired
    private RedisUtil redisUtil;

    private Random random = new Random(47);
    private long interval;  //分布式锁最多 占有时间

    public RedisLock(long interval){
        this.interval = interval;
    }

    /**
     * @param mutex
     * @param timeout 尝试获取锁的时间
     * @return 是否获得分布式锁
     */
    public boolean tryLock(String mutex, long timeout){
        Long current = System.currentTimeMillis();
        timeout *= 1000;
        try{
            while (true){
                if((System.currentTimeMillis() - current) > timeout){
                    break;
                }
                else{
                    if(innerTryLock(mutex)){
                        return true;
                    }
                    else{
                        Thread.sleep(200 + random.nextInt(40));
                    }
                }
            }
        }catch (Exception e){
            logger.error("尝试获取Redis分布式锁异常：" + e.getMessage());
        }

        return false;
    }

    private boolean innerTryLock(String mutex){
        try{
            long now = System.currentTimeMillis();
            String expireTime = String.valueOf(now + interval);
            if(redisUtil._setnx(mutex, expireTime) == 1){
                return true;
            }
            else{
                if(checkIfLockTimeout(mutex, now)){
                    String expect = redisUtil._get(mutex);
                    String preExpireTime = redisUtil._getSet(mutex, expireTime);
                    if(preExpireTime != null && preExpireTime.equals(expect)){
                        return true;
                    }
                }
                return false;
            }
        }catch (Exception e){
            e.printStackTrace();
        }

        return false;
    }

    public void releaseLock(String mutex){
        redisUtil._del(mutex);
    }


    /**
     * @param mutex 分布式锁的名称
     * @param timeStamp 当前时间戳
     * @return 检查分布式锁是否过期
     */
    private boolean checkIfLockTimeout(String mutex, long timeStamp){
        try{
            return  timeStamp > Long.valueOf(redisUtil._get(mutex));
        }catch (Exception e){
            logger.error("检查过期时间异常, mutex=" + mutex + ", " + e.getMessage());
        }
        return true;
    }
}
