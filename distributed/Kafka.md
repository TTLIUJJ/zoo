# Kafka

- 特点
- 架构
- 工作原理
- 高性能
- feature



## 特点

Kafka是一个pub-sub的消息系统，无法是发布还是订阅，都必须指定topic。

## 架构

## 工作原理


## 高性能之道

- partition并行
- mmap
- sendfile
- isr队列
- 批量传输
- 高效利用磁盘


#### partition 并行

kafka的生产者和消费者分别根据topic进行消息的发布和拉取，而topic只是逻辑上的存储消息的概念，真正存储数据的单位是partition。一个topic可以包含一个或多个partition，如果让topic的partition分配到多个broker中，那么消费者可以拉对应borker中topic的partition存储的消息。

如果consumer同时消费的个数多于partition的个数，那么部分consumer无法及时消费到技术，即partition的个数决定了并行的上限。



