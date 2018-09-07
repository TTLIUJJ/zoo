package com.ackerman.controller;

import com.ackerman._third.*;
import com.ackerman.service.MailService;
import com.ackerman.service.SSOService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @Author: Ackerman
 * @Description:
 * @Date: Created in 下午10:26 18-6-4
 */
@Controller
public class UserController {
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private SSOService ssoService;

    @Autowired
    private MailService mailService;


    @RequestMapping(path = "/doRegister", method = {RequestMethod.POST})
    public String doRegister(@RequestParam("username") String username,
                             @RequestParam("password") String password,
                             @RequestParam(value = "remember", defaultValue = "0") int remember,
                             HttpServletRequest request,
                             HttpServletResponse response){
        //remember = 1代表记住登录
        try {
            UserModel user = ssoService.register(request, response, username, password);

            if(user != null){
                //注册成功, 发送邮件, 异步操作, 往kafka消息队列增加消息
                mailService.sendRegisterMessage(user);

                if (remember == 1) {
                    ssoService.autoLogin(response, user.getId());
                }
            }

        }catch (Exception e){
            logger.error("注册失败", e);
        }

        return "forward:/index";
    }

    @RequestMapping(path = "/doLogin", method = {RequestMethod.POST})
    public String doLogin(@RequestParam("username") String username,
                          @RequestParam("password") String password,
                          @RequestParam(value = "remember", defaultValue = "0") int remember,
                          HttpServletRequest request,
                          HttpServletResponse response){

        try {
            UserModel user = ssoService.loginViaPassword(request, response, username, password);
            if (remember == 1) {
                ssoService.autoLogin(response, user.getId());
            }
        }catch (Exception e){
            logger.error("账号密码登录异常", e);
        }

        return "forward:/index";
    }

    @RequestMapping(path = "/logout", method = RequestMethod.GET)
    public String logout(HttpServletRequest request){

        ssoService.logoffTicket(request);

        return "redirect:/index";
    }


    @RequestMapping(path = "/register_verify", method = RequestMethod.GET)
    public String registerVerify(@RequestParam(value = "ticket", defaultValue = "0") String ticket,
                                 Model model){

        //TODO 获取注册验证码, 并且进行验证
        String key = "msg";

        if(ssoService.verifyOneTimeTicket(ticket)) {
            model.addAttribute(key, "认证成功");
        } else{
            model.addAttribute(key, "认证失败（验证码失效）");
        }
        return "verified";

    }
}