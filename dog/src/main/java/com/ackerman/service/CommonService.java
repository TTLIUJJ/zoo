package com.ackerman.service;

import com.ackerman._third.Common;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.stereotype.Service;

@Service
public class CommonService {
    private static Common common;

    public static void main(String []args){
        try {
            ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("common-consumer.xml");
            context.start();

            common = context.getBean(Common.class);

            System.out.println(common.createToken(111));
        }catch (Exception e){
            e.printStackTrace();
        }
    }

}
