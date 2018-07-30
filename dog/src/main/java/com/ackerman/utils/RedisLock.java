package com.ackerman.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Random;

public class RedisLock {
    private static Logger logger = LoggerFactory.getLogger(RedisLock.class);

    private static RedisUtil redisUtil = SpringContextContainer.getBean(RedisUtil.class);

    private Random random = new Random(47);
    private final String mutex;
    private long interval;  //ms 锁 最多被占有的时间, 如果持有锁的线程死掉，其他线程就可以抢占
    private long timeout;   //ms 锁 最多等待获取的最长时间

    public RedisLock(String mutex,long interval, long timeout){
        this.mutex = mutex;
        this.interval = interval;
        this.timeout = timeout;
        init();
    }

    public RedisLock(String mutex){
        this(mutex, 5000, 1000);
    }


    private void init(){
        try{
            if(redisUtil._get(mutex) != null){
                redisUtil._del(mutex);
            }
        }catch (Exception e){
            logger.error("Redis锁初始化出错");
            e.printStackTrace();
        }
    }

    /**
     * @param timeout ms 尝试获取锁的时间
     * @return 是否获得分布式锁
     */
    public boolean tryLock(long timeout){
        Long current = System.currentTimeMillis();
        try{
            while (true){
                if((System.currentTimeMillis() - current) > timeout){
                    break;
                }
                else{
                    if(innerTryLock()){
                        return true;
                    }
                    else{
                        Thread.sleep(200 + this.random.nextInt(40));
                    }
                }
            }
        }catch (Exception e){
            logger.error("尝试获取Redis分布式锁异常：" + e.getMessage());
        }

        return false;
    }

    public boolean tryLock(){
        return tryLock(this.timeout);
    }


    private boolean innerTryLock(){
        try{
            long now = System.currentTimeMillis();
            String expireTime = String.valueOf(now + this.interval);
            if(redisUtil._setnx(this.mutex, expireTime) == 1){
                return true;
            }
            else{
                if(checkIfLockTimeout(now)){
                    String expect = redisUtil._get(this.mutex);
                    String preExpireTime = redisUtil._getSet(this.mutex, expireTime);
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

    public void unLock(){
        redisUtil._del(this.mutex);
    }


    /**
     * @param timeStamp 当前时间戳
     * @return 检查分布式锁是否过期
     *
     * 在高并发的情况下，会经常抛出异常
     * 原因是其他线程_del锁，而本线程调用了_get
     */
    private boolean checkIfLockTimeout(long timeStamp){
        try{
            return  timeStamp > Long.valueOf(redisUtil._get(this.mutex));
        }catch (Exception e){
//            logger.error("检查过期时间异常, mutex=" + this.mutex);
//            e.printStackTrace();
        }
        return true;
    }
}
