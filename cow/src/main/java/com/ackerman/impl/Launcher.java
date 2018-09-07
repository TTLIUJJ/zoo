package com.ackerman.impl;

import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.io.IOException;

public class Launcher {

    public static void main(String []args) throws IOException {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("common_provider.xml");
        context.start();

        System.out.println("启动用户中心系统");
        System.in.read();
    }
}
