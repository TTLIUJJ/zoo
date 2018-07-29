package com.ackerman.utils;

import com.ackerman.dao.MysqlLockDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class MysqlLock {
    @Autowired
    private MysqlLockDao mysqlLockDao;

    private static Logger logger = LoggerFactory.getLogger(MysqlLock.class);

    private String mutex;
    private int remain;

    public MysqlLock(String mutex, int goodsNum){
        this.mutex = mutex;
        this.remain = goodsNum;
        try{
            if(mysqlLockDao.checkLockIfExist(mutex) == 0){
                mysqlLockDao.resetLock(mutex, remain);
            }
            else{
                mysqlLockDao.insertLock(mutex, remain);
            }
        }catch (Exception e){
            logger.error("创建Mysql分布式锁异常:" + e.getMessage());
        }
    }

    /**
     * @param goods 用户购买的商品
     * @param buys 购买的数量
     * @return 购买成功返回true
     * @desp 其实这里并没有加锁操作，纯粹是利用数据库行锁的特性
     *
     */
    public boolean tryLockAndBuy(String goods, int buys){
        try{
            if(mysqlLockDao.tryUpdate(goods, buys) == 0){
                return false;
            }
            return true;
        }catch (Exception e){
            logger.error("购买商品异常：" + goods + ", " + e.getMessage());
        }
        return false;
    }
}
