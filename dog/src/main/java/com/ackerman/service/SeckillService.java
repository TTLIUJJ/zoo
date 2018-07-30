package com.ackerman.service;

import com.ackerman.dao.MysqlLockDao;
import com.ackerman.utils.MysqlLock;
import com.ackerman.utils.RedisLock;
import com.ackerman.utils.RedisUtil;
import com.ackerman.utils.ZookeeperLock2;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.omg.PortableInterceptor.SYSTEM_EXCEPTION;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class SeckillService {
    @Autowired
    private MysqlLockDao mysqlLockDao;

    @Autowired
    private RedisUtil redisUtil;


    private static Logger logger = LoggerFactory.getLogger(SeckillService.class);
    private Random random = new Random(47);

    public String testMysql(final String goods, final int goodsNum, int attendance, final int limit) {
        String header = "商品名称：" + goods + ", 商品总量: " + goodsNum + ", 抢购人数：" + attendance + ", 限购数量：" + limit;
        final StringBuffer sb = new StringBuffer(header + "<br/>");
        final CountDownLatch countDownLatch = new CountDownLatch(attendance);
        final AtomicInteger luckys = new AtomicInteger(0);
        final MysqlLock mysqlLock = new MysqlLock(goods, goodsNum);

        try {
            for (int i = 0; i < attendance; ++i) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            int buys = random.nextInt(limit) + 1;
                            String msg = Thread.currentThread().getName() + " 尝试购买数量:" + String.format("%2d", buys) + "...  结果：";
                            if (mysqlLock.tryLockAndBuy(goods, buys)) {
                                msg += "[成功]";
                                luckys.incrementAndGet();
                            } else {
                                msg += " 失败 ";
                            }
                            sb.append(msg + "<br/>");
                        } catch (Exception e) {
                            e.printStackTrace();
                        } finally {
                            countDownLatch.countDown();
                        }
                    }
                }, "Thread: " + String.format("%4d", i + 1)).start();
            }
            countDownLatch.await();
            String footer = "抢购结束, 商品剩余：" + mysqlLockDao.queryRemain(goods) + ", 成功抢购人数：" + luckys;
            sb.append(footer);

            return sb.toString();
        } catch (Exception e) {
            logger.error("测试Mysql锁异常" + e.getMessage());
            e.printStackTrace();
        }

        return "测试异常，请查看是否输入不合理的数据";
    }


    public String testRedis(final String goods, final int goodsNum, int attendance, final int limit) {
        String header = "商品名称：" + goods + ", 商品总量: " + goodsNum + ", 抢购人数：" + attendance + ", 限购数量：" + limit;
        final StringBuffer sb = new StringBuffer(header + "<br/>");
        final CountDownLatch countDownLatch = new CountDownLatch(attendance);
        final AtomicInteger luckys = new AtomicInteger(0);

        final String mutex = "mutex-" + goods;
        final RedisLock redisLock = new RedisLock(mutex);
        redisUtil._set(goods, String.valueOf(goodsNum));

        try{
            for(int i = 0; i < attendance; ++i){
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try{
                            redisLock.tryLock();
                            int buys = random.nextInt(limit) + 1;
                            long remain = Long.valueOf(redisUtil._get(goods));
                            String msg = Thread.currentThread().getName() + " 尝试购买数量:" + String.format("%2d", buys) + "...  结果：";
                            if(remain - buys >= 0){
                                redisUtil._set(goods, String.valueOf(remain-buys));
                                msg += "[成功]";
                                luckys.incrementAndGet();
                            }
                            else{
                                msg += "失败";
                            }
                            sb.append(msg + "<br/>");
                        }catch (Exception e){
                            e.printStackTrace();
                        }finally {
                            redisLock.unLock();
                            countDownLatch.countDown();
                        }
                    }
                }).start();
            }
            countDownLatch.await();
            String footer = "抢购结束, 商品剩余：" + redisUtil._get(goods) + ", 成功抢购人数：" + luckys;
            sb.append(footer);

            return sb.toString();

        }catch (Exception e){
            logger.error("测试Redis锁异常" + e.getMessage());
            e.printStackTrace();
        }finally {
            redisUtil._del(mutex);
            redisUtil._del(goods);
        }
        return "测试异常，请查看是否输入不合理的数据";
    }

    public String testZookeeper(final String goods, final int goodsNum, int attendance, final int limit) {
        String header = "商品名称：" + goods + ", 商品总量: " + goodsNum + ", 抢购人数：" + attendance + ", 限购数量：" + limit;
        final StringBuffer sb = new StringBuffer(header + "<br/>");
        final CountDownLatch countDownLatch = new CountDownLatch(attendance);
        final AtomicInteger luckys = new AtomicInteger(0);
        final ZookeeperLock2 zookeeperLock = new ZookeeperLock2( goods);
        redisUtil._set(goods, String.valueOf(goodsNum));

        try {
            for(int i = 0; i < attendance; ++i){
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try{
                            zookeeperLock.lock();
                            int buys = random.nextInt(limit) + 1;
                            long remain = Long.valueOf(redisUtil._get(goods));
                            String msg = Thread.currentThread().getName() + " 尝试购买数量:" + String.format("%2d", buys) + "...  结果：";
                            if(remain - buys >= 0){
                                redisUtil._set(goods, String.valueOf(remain-buys));
                                msg += "[成功]";
                                luckys.incrementAndGet();
                            }
                            else{
                                msg += "失败";
                            }
                            sb.append(msg + "<br/>");
                        }catch (Exception e){
                            e.printStackTrace();
                        }finally {
                            zookeeperLock.unlock();
//                             System.out.println("---------------?????--------------");
                            countDownLatch.countDown();
                        }
                    }
                }, "Thread: " + String.format("%4d", i + 1)).start();
            }

            countDownLatch.await();
            Thread.sleep(3000);
            String footer = "抢购结束, 商品剩余：" + redisUtil._get(goods) + ", 成功抢购人数：" + luckys;
            sb.append(footer);

            return sb.toString();
        }catch (Exception e){
            logger.error("测试Zookeeper锁异常" + e.getMessage());
            e.printStackTrace();
        }finally {
            redisUtil._del(goods);
            zookeeperLock.close();
        }


        return "测试异常，请查看是否输入不合理的数据";
    }


//    public static void main(String []args) throws Exception{
//        RetryPolicy retryPolicy = new ExponentialBackoffRetry(5000, 5);
//        CuratorFramework client = CuratorFrameworkFactory.newClient(
//                "127.0.0.1:2181", 60000, 20000, retryPolicy
//        );
//        client.start();
//        try{
//            ZookeeperLock zookeeperLock = new ZookeeperLock("127.0.0.1:2181", "apple");
//            zookeeperLock.lock(new Runnable() {
//                @Override
//                public void run() {
//                    System.out.println("hello!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
//                }
//            });
//
////        zookeeperLock.unlock();
//
//            Thread.sleep(3000);
//            zookeeperLock.unlock();
//
//            zookeeperLock.close();
//        }catch (Exception e){
//            e.printStackTrace();
//        }
//
//    }

}