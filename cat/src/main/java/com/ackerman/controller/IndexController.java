package com.ackerman.controller;

import com.ackerman.utils.LocalInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;



/**
 * @Author: Ackerman
 * @Description:
 * @Date: Created in 下午2:36 18-6-4
 */
@Controller
public class IndexController {

    @Autowired
    private LocalInfo localInfo;
    /**
     单点登录失败的原因:
     session的会话机制, 重定向访问127.0.0.0:1234, 那么原来的8080服务器, 原服务器8080失去global_ticket,
     而直接使用html的网站跳转到seckill, 不属于从8080服务器跳转
     */
//    @RequestMapping(path = "/seckill", method = RequestMethod.GET)
//    public String seckill(HttpServletRequest request){
//        String ticket = "";
//        try{
//            ticket = localInfo.getGlobalTicket();
//        }catch (Exception e){
//            e.printStackTrace();
//        }
//        return "redirect:http://127.0.0.1:1234/ssoIndex?global=" + ticket;
//    }


    @RequestMapping(path = "/login", method = {RequestMethod.GET, RequestMethod.POST})
    public String login(){
        return "login";
    }

    @RequestMapping(path = "/register", method = RequestMethod.GET)
    public String register(){
        return "register";
    }

}