package com.ackerman.service;

import com.ackerman._thrid.UserModel;
import com.ackerman.dao.UserDao;
import com.ackerman.utils.JedisClusterUtil;
import com.ackerman.utils.JedisUtil;
import com.ackerman.utils.LocalInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.UUID;

/**
 * @Author: Ackerman
 * @Description:
 * @Date: Created in 下午10:28 18-6-4
 */
@Service
public class UserService {
    private static Logger logger = LoggerFactory.getLogger(UserService.class);
    private static final String CELTICS_TOKEN = "celtics_token";
    private static final int seconds = 60 * 60 * 24 * 7;

    @Autowired
    private UserDao userDao;

    @Autowired
    private JedisUtil jedisUtil;

    @Autowired
    private LocalInfo localInfo;

    @Autowired
    private SSOService ssoService;

    public int register(String username, String password){
        int id = -1;
        try{
            UserModel user = new UserModel();
            user.setUsername(username);
            user.setPassword(password);
            user.setSalt("aaaaa");
            user.setHeadImageUrl("www.google.com");
            userDao.addUser(user);

            //此时的user才有id
            user = userDao.getUserViaLogin(username, password);
            localInfo.setUser(user);

            return userDao.getIdByUsername(username);
        }catch (Exception e){
            logger.error("添加用户异常", e);
        }

        return id;
    }

    public int login(String username, String password){
        int id = -1;
        try{
            UserModel user = userDao.getUserViaLogin(username, password);
            if(user == null)
                return id;
            id = user.getId();
            localInfo.setUser(user);
        }catch (Exception e){
            logger.error("用户登陆异常", e);
        }
        return id;
    }


    public void logout(HttpServletRequest request){
        try{
            String token = null;
            for(Cookie cookie : request.getCookies()){
                if(cookie.getName().equals(CELTICS_TOKEN)){
                    token = cookie.getValue();
                    break;
                }
            }
            String key = CELTICS_TOKEN + ":" + token;
            jedisUtil.del(key);
        }catch (Exception e){
            logger.error("logout()", e);
        }
    }

    public void addCookie(HttpServletResponse response, int id){
        try{
            String token = createCelticsToken(id);
            Cookie cookie = new Cookie(CELTICS_TOKEN, token);
            cookie.setMaxAge(seconds);
            response.addCookie(cookie);
        }catch (Exception e){
            logger.error("addCookie()", e);
        }
    }

    public String createCelticsToken(int id){
        String token = null;
        try{
            token = UUID.randomUUID().toString().replace("-", "");
            String key = CELTICS_TOKEN + ":" + token;
            jedisUtil.setex(key, seconds, String.valueOf(id));
        }catch (Exception e){
            logger.error("createCelticsToken()", e);
        }
        return token;
    }

    public UserModel parseUserFromToken(String token){
        UserModel user = null;
        try{
            String key = CELTICS_TOKEN + ":" + token;
            String value = jedisUtil.get(key);
            if(value == null)
                return null;

            int id = Integer.parseInt(value);
            user = userDao.getUserById(id);

            return user;
        }catch (Exception e){
            logger.error("parseUserFromToken()", e);
        }
        return user;
    }

    public UserModel getUserFromId(int userId){
        return ssoService.getUserById(userId);
    }


}