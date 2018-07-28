package com.ackerman.utils;

import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.log4j.Logger;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.data.Stat;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @Author: Ackerman
 * @Description:
 * @Date: Created in 上午9:11 18-6-16
 */
@Component
public class MasterUtil {
    private static Logger logger = Logger.getLogger(MasterUtil.class);
    private String sdfStamp = "yyyy-MM-dd HH:mm";
    private String ROOT_PATH = "/master_election/hot_news";

    private  CuratorFramework client;
    {
        String connectString = "127.0.0.1:2181,127.0.0.1:2182,127.0.0.1:2183";
        int sessionTimeout = 5000;
        int connectionTimeout = 3000;
        RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 3);
        client = CuratorFrameworkFactory.newClient(connectString,
                sessionTimeout,
                connectionTimeout,
                retryPolicy);

    }

    public void start(){
        try{
            client.start();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     * @Description: 计算距离下一个整10分, 需要的睡眠时间
     * @Date: 上午9:30 18-6-16
     */
    public long waitForMillseconds(){
        Calendar now = Calendar.getInstance();

        int minute = now.get(Calendar.MINUTE);
        int second = now.get(Calendar.SECOND);
        int millis = now.get(Calendar.MILLISECOND);

        long beyond = (minute % 10) * 60 * 1000 + second * 1000 + millis;
        long remain = 10 * 60 * 1000 - beyond;

        return remain;
    }


    public boolean setData(String path, byte []data){
        try{
            client.setData().forPath(path, data);
            return true;
        }catch (Exception e){
            logger.error("Master往节点写入数据异常", e);
        }
        return false;
    }


    public byte[] fetchData(String nodePath){
        try{
            return client.getData().forPath(nodePath);
        }catch (Exception e){
            logger.error("往Master节点中获取节点异常", e);
        }
        return null;
    }

    /**
     * @Description: 竞争称为master, 如果创建节点的时候发生异常, 有如下两种情况:
     *                          1. 系统内部异常
     *                          2. 节点已被其他服务器创建
     * @Date: 下午2:50 18-6-16
     */
    public boolean createNextMasterNode(String path){
        try{
            client.create()
                    .creatingParentsIfNeeded()
                    .withMode(CreateMode.PERSISTENT)
                    .forPath(path);

            return true;
        }catch (Exception e){
            try {
                Stat stat = client.checkExists().forPath(path);
                if(stat != null)
                    return false;
                else
                    logger.error("节点:" + path + ", 异常分析: 没有被创建并且不能被创建");
            }catch (Exception e1){
                e1.printStackTrace();
            }
        }

        return false;
    }

    //实际上, 如果所有服务区的时间戳 想要一致的时间戳, 必须来自同一个系统--->dubbo

    /**
     * @Description: 删除上一个十分的master节点
     * @Date: 下午3:01 18-6-16
     */
    public void deleteLastMasterNode(String path){
        try{
            client.delete().guaranteed().forPath(path);
        }catch (Exception e){
            //删除不存在的节点会抛出异常, 直接忽略
        }
    }

    /**
     * @Description: 生成全局唯一临时节点的　<名称>, 格式:yyyy-MM-dd HH:mm, 并且保证是下一个 十分 的时间戳
     * @Date: 上午9:55 18-6-16
     */
    public String createNextMasterNodePath(){
        long guaranteeNextMinute = System.currentTimeMillis() + 60*5000; //多了0.5分钟
        Date date = new Date(guaranteeNextMinute);
        SimpleDateFormat sdf = new SimpleDateFormat(sdfStamp);
        return ROOT_PATH + "/" + sdf.format(date);
    }

    /**
     * @Description: 获取上个小时的节点 <名称>, 准备将其删除
     * @Date: 下午2:39 18-6-16
     */
    public String getLastMasterNodePath(){
        long guaranteeLastMinute = System.currentTimeMillis() - 60*500; //少了0.5分钟
        Date date = new Date(guaranteeLastMinute);
        SimpleDateFormat sdf = new SimpleDateFormat(sdfStamp);
        return ROOT_PATH + "/" + sdf.format(date);
    }





    public static void main(String []args){
        MasterUtil masterUtil = new MasterUtil();

        //测试, 两份代码

//        Random random = new Random(47);
//        masterUtil.start();
//
//        while (true){
//            long sleepMills = masterUtil.waitForMillseconds();
//            System.out.println("睡眠时间: " + sleepMills);
//            try {
//                TimeUnit.MILLISECONDS.sleep(sleepMills);
//
//            }catch (Exception e){
//                e.printStackTrace();
//            }
//            String nextPath = masterUtil.createNextMasterNodePath();
//            if(masterUtil.createNextMasterNode(nextPath)){
//                String lastPath = masterUtil.getLastMasterNodePath();
//                masterUtil.deleteLastMasterNode(lastPath);
//
//                StringBuilder sb = new StringBuilder();
//                for(int i = 0; i < 10; ++i){
//                    sb.append(random.nextInt(100) + ",");
//                }
//
//                masterUtil.setData(nextPath, sb.toString().getBytes());
//                System.out.println("----------------------------------------------------");
//                System.out.println("设置节点数据成功:" + sb.toString());
//                System.out.println("----------------------------------------------------");
//            }
//            else{
//                try{
//                    //等待master设置完数据
//                    TimeUnit.SECONDS.sleep(10);
//                }catch (Exception e){}
//
//                byte []data = masterUtil.fetchData(nextPath);
//                System.out.println("----------------------------------------------------");
//                System.out.println("从节点中获取数据:" + new String(data));
//                System.out.println("----------------------------------------------------");
//            }
//
//        }

    }
}