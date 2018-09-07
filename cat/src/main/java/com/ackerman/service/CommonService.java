package com.ackerman.service;

import com.ackerman._third.Common;
import com.ackerman._third.Shit;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.stereotype.Service;

@Service
public class CommonService implements InitializingBean {
    private Common common;



    @Override
    public void afterPropertiesSet() throws Exception {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("consumer.xml");
        context.start();

        Shit bean =  context.getBean(Shit.class);

        System.out.println(bean.say());
    }
}
