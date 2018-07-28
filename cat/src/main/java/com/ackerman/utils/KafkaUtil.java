package com.ackerman.utils;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * @Author: Ackerman
 * @Description:
 * @Date: Created in 上午10:45 18-6-15
 */
@Component
public class KafkaUtil {
    public static final String KAFKA_TOPIC_REGISTER = "celtics_register";
    public static final String KAFKA_TOPIC_OTHER = "celtics_another";

    private static KafkaProducer<String, String> kafkaProducer;
    private static KafkaConsumer<String, String> kafkaConsumer;

    static {
        /*
            kafka生产者可以定义的属性：
                1. kafka服务的地址, 不需要将所有的broker指定上
                2. 消息被接收的确认信号
                        - 0表示不确认
                        - -1表示所有的follower都返回ack才确认
                        - 1表示接受到leader的ack返回确认
                3. 消息发送失败的重复次数
                4. 当多个producer向同一个partition发送消息, 要求消息以16384的大小发送, 从而减少交互次数
                5. 消息储存于发送缓冲区的时间
                6. 发送缓冲区最大值, server.properties默认值=102400
                7. partition对应的key的序列化类
                8. partition储存数据value的序列化类
         */

        Properties properties = new Properties();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "127.0.0.1:9091");
        properties.put(ProducerConfig.ACKS_CONFIG, "-1");
        properties.put(ProducerConfig.RETRIES_CONFIG, 1);
        properties.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
        properties.put(ProducerConfig.LINGER_MS_CONFIG, 10);
        properties.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 102400);
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");

        kafkaProducer = new KafkaProducer<String, String>(properties);
    }

    static {
        /*
            kafka消费者可以定义的属性（自动确认offset方案）：
                1. kafka服务的地址, 不需要将所有的broker指定上
                2. 消费者组
                3. 是否自动确认offset
                4. 自动确认offset的时间间隔
                5. 会话超时时间（是不是指的是和zooKeeper的会话?） [6, 30]s
                6. partition对应的key的序列化类
                7. partition储存数据value的序列化类
         */

        Properties properties = new Properties();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "127.0.0.1:9092");
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, "112233");
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);
        properties.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, 1000);
        properties.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 30000);
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");

        kafkaConsumer = new KafkaConsumer<String, String>(properties);
    }


    public void send(String topic, String msg){
        try{
            ProducerRecord<String, String> record = new ProducerRecord<String, String>(topic, msg);
            kafkaProducer.send(record);

        }catch (Exception e){
            e.printStackTrace();
        }
    }


    public void subscribe(Set<String> topics){
        try{
            kafkaConsumer.subscribe(topics);
        }catch (Exception e){
            e.printStackTrace();
        }
    }


    public void consume(long timeout){
        try{
            System.out.println("------------------------------------");
            ConsumerRecords<String, String> records = kafkaConsumer.poll(timeout);
            for(ConsumerRecord<String, String> record : records){
                System.out.println(record.topic() + ", " + record.value());
            }
            System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
        }catch (Exception e){
            e.printStackTrace();
        }
    }


}