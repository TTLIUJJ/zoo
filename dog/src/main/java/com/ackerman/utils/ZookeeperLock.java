package com.ackerman.utils;

import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.NodeCache;
import org.apache.curator.framework.recipes.cache.NodeCacheListener;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/*
    /zk-locks
             /milk
                  /number-1
                  /number-2
                  /number-3
                  .
                  .
                  .

            /apple
                  /number-1
                  /number-2
                  .
                  .
                  .
 */
public class ZookeeperLock {
    private static Logger logger = LoggerFactory.getLogger(ZookeeperLock.class);
    private static final String ROOT_PATH = "/zk-locks";
    private static final String NUMBER = "/number";
    private Map<Thread, String> threadMap;
    private String mutex;
    private CuratorFramework client;


    public ZookeeperLock(String connectString, String mutex){
        try{
            this.threadMap = new HashMap<Thread, String>();
            this.mutex = "/" + mutex;
            RetryPolicy retryPolicy = new ExponentialBackoffRetry(5000, 5);
            client = CuratorFrameworkFactory.newClient(connectString, 60000, 20000, retryPolicy);
            client.start();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public void close(){
        client.close();
    }

    public void lock(final Runnable work){
        try{
            String currentLock = client.create()
                    .creatingParentsIfNeeded()
                    .withMode(CreateMode.EPHEMERAL_SEQUENTIAL)
                    .forPath(ROOT_PATH + mutex + NUMBER);
            String index = currentLock.substring(currentLock.lastIndexOf("/"));
            List<String> brotherNodes = client.getChildren().forPath(ROOT_PATH + mutex);
            Collections.sort(brotherNodes);

            String prevNode = null;
            for(int i = brotherNodes.size()-1; i >= 0; --i){
                if(brotherNodes.get(i).compareTo(index) < 0){
                    prevNode = brotherNodes.get(i);
                    break;
                }
            }

            System.out.println("**********************************************************");
            System.out.println(Thread.currentThread().getName() + "-创建节点:" + index + ", 前一个节点是:" + prevNode);
            System.out.println("**********************************************************");

            threadMap.put(Thread.currentThread(), currentLock);
            if(prevNode == null){
                work.run(); //直接在调用lock的线程中进行工作
                return;
            }
            else{
                final String watchPath = ROOT_PATH + mutex + "/" + prevNode;
                final NodeCache cache = new NodeCache(client, watchPath, false);
                cache.start(true);
                cache.getListenable().addListener(new NodeCacheListener() {
                    @Override
                    public void nodeChanged() throws Exception {
//                        System.out.println("------------------------------------------------------------------");
                        work.run();
//                        System.out.println(Thread.currentThread().getName() + "-"+ threadMap.get(Thread.currentThread()) +", CacheNode被触发, 被删除的节点:" + cache.getPath());
//                        System.out.println("------------------------------------------------------------------");

                    }
                });
            }
        }catch (Exception e){
            logger.error("zookeeper分布式锁添加节点异常：" + work.getClass().getName());
        }
    }

    public void unlock(){
        try{
            String currentPath = threadMap.get(Thread.currentThread());
            if(currentPath != null && client.checkExists().forPath(currentPath) != null){
                client.delete().guaranteed().forPath(currentPath);
            }
        }catch (Exception e){
            logger.error("释放分布式锁出错：" + e.getMessage());
            e.printStackTrace();
        }
    }
}
