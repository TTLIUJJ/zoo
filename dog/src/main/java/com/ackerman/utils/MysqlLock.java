package com.ackerman.utils;

import com.ackerman.dao.MysqlLockDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class MysqlLock {
    private static Logger logger = LoggerFactory.getLogger(MysqlLock.class);

    private final MysqlLockDao mysqlLockDao;
    private final String mutex;
    private final int remain;

    public MysqlLock(String mutex, int goodsNum){
        this.mysqlLockDao = SpringContextContainer.getBean(MysqlLockDao.class);
        this.mutex = mutex;
        this.remain = goodsNum;

        try{
            if(mysqlLockDao.checkLockIfExist(mutex) != null){
                mysqlLockDao.resetLock(mutex, remain);
            }
            else{
                mysqlLockDao.insertLock(mutex, remain);
            }
        }catch (Exception e){
            logger.error("创建Mysql异常");
            e.printStackTrace();
        }
    }

    /**
     * @param mutex 用户购买的商品
     * @param buys 购买的数量
     * @return 购买成功返回true
     * @desp 其实这里并没有加锁操作，纯粹是利用数据库行锁的特性
     *
     */
    public boolean tryLockAndBuy(String mutex, int buys){
        try{
            if(mysqlLockDao.tryUpdate(mutex, buys) == 0){
                return false;
            }
            return true;
        }catch (Exception e){
            logger.error("购买商品异常：" + mutex + ", " + e.getMessage());
        }
        return false;
    }

}
