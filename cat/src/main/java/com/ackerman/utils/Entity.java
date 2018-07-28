package com.ackerman.utils;

/**
 * @Author: Ackerman
 * @Description:
 * @Date: Created in 下午3:07 18-6-17
 */
public class Entity {
    public static final String DISLKE_KEY = "dislike";
    public static final String LKIE_KEY = "like";

    public static final String MAIL_TYPE_REGISTER = "register";
    public static final String MAIL_TYPE_LOGIN_EXCEPTION = "login_exception";

    public static String getNewsAttitudeKey(int newsId, String type){
        return "news-" + String.valueOf(newsId) + "-" + type;
    }
}