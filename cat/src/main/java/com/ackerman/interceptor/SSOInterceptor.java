package com.ackerman.interceptor;

import com.ackerman._thrid.*;
import com.ackerman.service.SSOService;
import com.ackerman.utils.LocalInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @Author: Ackerman
 * @Description:
 * @Date: Created in 下午7:41 18-6-7
 */
@Component
public class SSOInterceptor implements HandlerInterceptor{
    private static Logger logger = LoggerFactory.getLogger(SSOInterceptor.class);

    @Autowired
    private SSOService ssoService;

    @Autowired
    private LocalInfo localInfo;


    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object o) throws Exception {
        try {
            Cookie []cookies = request.getCookies();
            String local  = null;
            String global = null;
            String token = null;

            if(cookies != null){
                for(Cookie cookie : cookies){
                    String name = cookie.getName();

                    if(name.equals(Common.LOCAL_TICKET_NEWS))
                        local = cookie.getValue();
                    if(name.equals(Common.GLOBAL_TICKET))
                        global = cookie.getValue();
                    if(name.equals(Common.CELTICS_TOKEN))
                        token = cookie.getValue();
                }
            }
//            HttpSession session = request.getSession();

//            local = (String) session.getAttribute(Common.LOCAL_TICKET_NEWS);
//            global = (String) session.getAttribute(Common.GLOBAL_TICKET);

            if(local != null){
                logger.info("local:" + local);
            }

            if(global != null){
                logger.info("global:" + global);
            }

            //1.　判断是否有局部会话

//            logger.info("local: " + local);
            if(local != null && ssoService.verifyTicketOrUpdate(Common.LOCAL_TICKET_NEWS, local)) {
                UserModel user = ssoService.getUserByTicket(Common.LOCAL_TICKET_NEWS, local);
                localInfo.setUser(user);
                return true;
            }

            //登陆的时候, 一定会有本系统的局部会话 --> 也会有本系统的全局会话
            //2. 判断是否有全局会话, 如果存在, 创建局部会话

//            logger.info("global: " + global);
            if(global != null && ssoService.verifyTicketOrUpdate(Common.GLOBAL_TICKET, global)){
                UserModel user = ssoService.getUserByTicket(Common.GLOBAL_TICKET, global);
                local = ssoService.createLocalSession(request, response, Common.LOCAL_TICKET_NEWS, user);
                ssoService.addTicketSet(global, Common.LOCAL_TICKET_NEWS, local);
                return true;
            }

            //3. 判断是否有免登陆系统
            if(token != null && ssoService.verifyToken(Common.CELTICS_TOKEN, token)){
                UserModel user = ssoService.getUserByToken(Common.CELTICS_TOKEN, token);
                global = ssoService.createGlobalSession(request, response, user);
                local = ssoService.createLocalSession(request, response, Common.LOCAL_TICKET_NEWS, user);
                ssoService.addTicketSet(global, Common.LOCAL_TICKET_NEWS, local);
                return true;
            }
        }catch (Exception e){
            e.printStackTrace();
        }

        return true;
    }

    @Override
    public void postHandle(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Object o, ModelAndView modelAndView) throws Exception {
        try {
            UserModel user = localInfo.getUser();
            String global = localInfo.getGlobalTicket();
            if (user != null)
                modelAndView.addObject("user", user);

            if (global != null) {
                modelAndView.addObject("global", global);
            }
        }catch (Exception e){

        }
    }

    @Override
    public void afterCompletion(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Object o, Exception e) throws Exception {
        localInfo.removeUser();
    }
}