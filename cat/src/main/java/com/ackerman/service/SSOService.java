package com.ackerman.service;

import com.ackerman._thrid.*;
import com.ackerman.utils.LocalInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.stereotype.Service;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

/**
 * @Author: Ackerman
 * @Description:
 * @Date: Created in 下午11:22 18-6-6
 */
@Service
public class SSOService {
    private static Logger logger = LoggerFactory.getLogger(SSOService.class);

    @Autowired
    private LocalInfo localInfo;

    private ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("sso.xml");
    private Common common = (Common) context.getBean("impl");


    /**
     * @Description: 验证ticket是否有效, 如果有效, 更新过期时间
     * @Param（tryp: ticket的类型
     * @Param（ticket: ticket的值
     * @Return: 是否有效
     */
    public boolean verifyTicketOrUpdate(String type, String ticket){
        return common.verifyTicketOrUpdate(type, ticket);
    }

    public boolean verifyToken(String type, String token){
        return common.verifyToken(type, token);
    }

    /**
     * @Description: 通过帐号密码登陆, 需要进入几点操作：
     *                                  1. 验证帐号密码的正确性, 如果正确, 返回wUserModel
     *                                  2. 创建全局会话
     *                                  3. 创建局部会话
     *                              注意: 需要将user保存在localUser中
     * @Date: 上午11:38 18-6-8
     */
    public UserModel loginViaPassword(HttpServletRequest request, HttpServletResponse response, String username, String password){
        UserModel user = common.loginViaPassword(username, password);
        if(user == null || user.getId() <= 0) {
            logger.warn("user is null or userId <= zero");
        }
        else {
            String global = createGlobalSession(request, response, user);
            String local = createLocalSession(request, response, Common.LOCAL_TICKET_NEWS, user);
            addTicketSet(global, Common.LOCAL_TICKET_NEWS, local);
        }

        return user;
    }

    public String createLocalSession(HttpServletRequest request, HttpServletResponse response, String type, UserModel user){
        String ticket = null;
        try{
            ticket = common.creteTicket(type, user.getId());


//            HttpSession session = request.getSession();
//            session.setAttribute(type, ticket);
//            session.setMaxInactiveInterval(Common.TICKET_TIMEOUT_SECONDS);
            localInfo.setUser(user);
        }catch (Exception e){
            logger.error("创建全局会话异常", e);
        }
        return ticket;
    }

    public String createGlobalSession(HttpServletRequest request, HttpServletResponse response, UserModel user){
        String ticket = null;
        try{
            ticket = common.creteTicket(Common.GLOBAL_TICKET, user.getId());
            Cookie cookie = new Cookie(Common.GLOBAL_TICKET, ticket);
            response.addCookie(cookie);

//            HttpSession session = request.getSession();
//            session.setAttribute(Common.GLOBAL_TICKET, ticket);
//            session.setMaxInactiveInterval(Common.TICKET_TIMEOUT_SECONDS);
            localInfo.setGlobalTicket(ticket);
        }catch (Exception e){
            logger.error("创建全局会话异常", e);
        }
        return ticket;
    }

    /**
     * @Description: 在验证会话中的ticket有效之后,
     * @Date: 下午12:05 18-6-8
     */
    public UserModel getUserByTicket(String type, String ticket){
        UserModel user = null;
        try{
            user = common.getUserModelByTicket(type, ticket);
        }catch (Exception e){

        }
        return user;
    }

    public UserModel getUserByToken(String type, String token){
        return getUserByTicket(type, token);
    }


    /**
     * @Description: 注册完毕, 相当于登录一边了
     * @Date: 下午4:37 18-6-17
     */
    public UserModel register(HttpServletRequest request, HttpServletResponse response, String username, String password){
        UserModel user = null;
        try{
            int res = common.register(username, password);
            if(res == 0)
                return null;

            user = loginViaPassword(request, response, username, password);
        }catch (Exception e){
            e.printStackTrace();
        }
        return user;
    }


    /**
     * @Description: 免登录函数
     * @Date: 下午9:17 18-6-17
     */
    public void autoLogin(HttpServletResponse response, int id){
        try {
            String token = common.createToken(id);
            Cookie cookie = new Cookie(Common.CELTICS_TOKEN, token);
            cookie.setPath("/");
//            cookie.setMaxAge(Common.TOKEN_TIMEOUT_SECONDS);   //单位:s
            response.addCookie(cookie);
        }catch (Exception e){
            logger.error("免登录出错", e);
        }
    }

    public void addTicketSet(String global, String localType, String local){
        try{
            common.addTicketSet(global, localType, local);
        }catch (Exception e){
            logger.error("添加ticket到记录集合失败", e);
        }
    }

    /**
     * @Description: 注销令牌, 没有点击退出登陆, 所有的ticket在一段时间后会被删除
     *                  但是点击了退出, 全局和局部ticket都会被删除,
     *                  当然, token也必须一并删除
     * @Date: 下午9:58 18-6-17
     */
    public void logoffTicket(HttpServletRequest request){
        try{
            String global = null;
            String token = null;

            Cookie []cookies = request.getCookies();

            for(Cookie cookie : cookies){
                String name = cookie.getName();
                if(name.equals(Common.CELTICS_TOKEN)) {
                    token = cookie.getValue();
                    cookie.setMaxAge(0);
                }
                else if(name.equals(Common.GLOBAL_TICKET)){
                    global = cookie.getValue();
                    cookie.setMaxAge(0);
                }

            }

            Map<String, String> ticketMap = common.getLocalTickets(global);
            for(Cookie cookie : cookies){
                String name = cookie.getName();
                if(ticketMap.containsKey(name)){
                    cookie.setMaxAge(0);
                }
            }

            common.logoffTicket(global, token);
            localInfo.removeAll();
        }catch (Exception e){
            logger.error("注销登录失败", e);
        }
    }


    /**
     * @Description: 一次性ticket的验证
     * @Date: 下午3:21 18-6-21
     */

    public boolean verifyOneTimeTicket(String ticket){
        try{
            return common.verifyOneTimeTicket(Common.RGISTER_TICKET, ticket);
        }catch (Exception e){
            logger.error("邮箱验证失败", e);
        }
        return false;
    }

    public UserModel getUserById(int id){
        return common.getUserById(id);
    }
}