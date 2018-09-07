package com.ackerman.service;

import com.ackerman._third.UserModel;
import com.ackerman.utils.KafkaUtil;
import com.alibaba.fastjson.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @Author: Ackerman
 * @Description: 邮件服务中心, 给用户发送各类邮件
 * @Date: Created in 上午9:13 18-6-21
 */
@Service
public class MailService {
    private static Logger logger = LoggerFactory.getLogger(MailService.class);

    @Autowired
    private KafkaUtil kafkaUtil;

    public void sendRegisterMessage(UserModel user){
        try{
            String message = JSON.toJSONString(user);
            kafkaUtil.send(KafkaUtil.KAFKA_TOPIC_REGISTER, message);
        }catch (Exception e){
            logger.error("发送消息到celtics_register失败", e);
        }
    }

}