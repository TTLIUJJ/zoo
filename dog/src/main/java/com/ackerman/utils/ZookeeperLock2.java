package com.ackerman.utils;

import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.NodeCache;
import org.apache.curator.framework.recipes.cache.NodeCacheListener;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class ZookeeperLock2 {
    private static Logger logger = LoggerFactory.getLogger(ZookeeperLock.class);
    private static final String connectString = "127.0.0.1:2181";
    private static final String ROOT_PATH = "/zk-locks";
    private static final String NUMBER = "/number-";
    private static int sessionTimeout = 40000;
    private static int connectTimeout = 20000;
    private static RetryPolicy retryPolicy = new ExponentialBackoffRetry(5000, 5);
    private final CuratorFramework client;
    private final String mutex;
    private final CountDownLatch countDownLatch = new CountDownLatch(1);
    private final ThreadLocal<String> localPath = new ThreadLocal<String>();
    private final String prefix;

    public ZookeeperLock2(String mutex){
        this.mutex = "/" + mutex;
        this.prefix = ROOT_PATH + "/" + mutex + "/";
        client = CuratorFrameworkFactory.newClient(connectString, sessionTimeout, connectTimeout, retryPolicy);
        client.start(); //阻塞函数

    }


    public void close(){
        try{
            client.close();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public void lock(){
        try{
            final String currentLock = client.create()
                    .creatingParentsIfNeeded()
                    .withMode(CreateMode.EPHEMERAL_SEQUENTIAL)
                    .forPath(ROOT_PATH + mutex + NUMBER);
            String index = currentLock.substring(currentLock.lastIndexOf("/")+1);
            List<String> brotherNodes = client.getChildren().forPath(ROOT_PATH + mutex);
            Collections.sort(brotherNodes);

            String prevNode = null;
            for(int i = brotherNodes.size()-1; i >= 0; --i){
                if(brotherNodes.get(i).compareTo(index) < 0){
                    prevNode = this.prefix + brotherNodes.get(i);
                    break;
                }
            }

//            System.out.println("**********************************************************");
//            System.out.println(Thread.currentThread().getName() + "-创建节点:" + index + ", 前一个节点是:" + prevNode);
//            System.out.println("**********************************************************");

            localPath.set(currentLock);
            if(prevNode == null){
                return;
            }
            else{
                final CountDownLatch latch = new CountDownLatch(1);
                final String watchPath = prevNode;
                final NodeCache cache = new NodeCache(client, watchPath, false);
                cache.start(true);
                cache.getListenable().addListener(new NodeCacheListener() {
                    @Override
                    public void nodeChanged() throws Exception {
                        latch.countDown();
                    }
                });

                //bug ... 添加prevNode节点为监听监听节点时，节点已被删除，此时的currentPath无法被唤醒，
                try {
                    Stat stat = client.checkExists().forPath(prevNode);
                    if (stat == null) {
                        localPath.set(currentLock);
                        latch.countDown();
                    }
                }catch (Exception e){
                    logger.error("防止prevNode被删除，currentLock永远监听的Bug," + e.getMessage());
                    e.printStackTrace();
                }
                latch.await();
            }

        }catch (Exception e){
            logger.error("获取zookeeper锁异常: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void unlock(){
        try{
            String currentPath = localPath.get();
            if(currentPath != null && client.checkExists().forPath(currentPath) != null){
                client.delete().guaranteed().forPath(currentPath);
            }
        }catch (Exception e){
            logger.error("释放zookeeper分布式锁出错：" + e.getMessage());
            e.printStackTrace();
        }
    }


}
