package com.ackerman.controller;

import com.ackerman.dao.MysqlLockDao;
import com.ackerman.service.SeckillService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class IndexController {
    @Autowired
    private SeckillService seckillService;

    @Autowired
    private MysqlLockDao mysqlLockDao;

    private static Logger logger = LoggerFactory.getLogger(IndexController.class);

    @RequestMapping(path = "/", method = RequestMethod.GET)
    public String index(){
        return "index";
    }

    @ResponseBody
    @RequestMapping(path = "/mysql", method = RequestMethod.GET)
    public String mysqlLockTest( @RequestParam(value = "goods") String goods,
                      @RequestParam(value = "goodsNum", defaultValue = "100") int goodsNum,
                      @RequestParam(value = "attendance", defaultValue = "500") int attendance,
                      @RequestParam(value = "limit", defaultValue = "1") int limit){

        try{
            return seckillService.testMysql(goods, goodsNum, attendance, limit);
        }catch (Exception e){
            logger.error("测试mysql锁异常：" + e.getMessage());
        }
        return "系统内部错误";
    }

    @ResponseBody
    @RequestMapping(path = "/redis", method = RequestMethod.GET)
    public String redisLockTest( @RequestParam(value = "goods") String goods,
                                 @RequestParam(value = "goodsNum", defaultValue = "100") int goodsNum,
                                 @RequestParam(value = "attendance", defaultValue = "500") int attendance,
                                 @RequestParam(value = "limit", defaultValue = "1") int limit){

        try{
            return seckillService.testRedis(goods, goodsNum, attendance, limit);
        }catch (Exception e){
            logger.error("测试mysql锁异常：" + e.getMessage());
        }
        return "系统内部错误";
    }


    @ResponseBody
    @RequestMapping(path = "/zookeeper", method = RequestMethod.GET)
    public String ZookeeperLockTest(@RequestParam(value = "goods") String goods,
                                 @RequestParam(value = "goodsNum", defaultValue = "100") int goodsNum,
                                 @RequestParam(value = "attendance", defaultValue = "500") int attendance,
                                 @RequestParam(value = "limit", defaultValue = "1") int limit){

        try{
            return seckillService.testZookeeper(goods, goodsNum, attendance, limit);
        }catch (Exception e){
            logger.error("测试mysql锁异常：" + e.getMessage());
        }
        return "系统内部错误";
    }


}
