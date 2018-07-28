package com.ackerman._thrid;

import java.util.Map;

/**
 * @Author: Ackerman
 * @Description:
 * @Date: Created in 上午9:37 18-6-7
 */
public interface Common {
    public static final String CELTICS_TOKEN = "celtics_token";
    public static final String GLOBAL_TICKET = "celtics_ticket_global";
    public static final String LOCAL_TICKET_NEWS = "celtics_ticket_news";
    public static final String LOCAL_TICKET_SECKILL = "celtics_ticket_seckill";
    public static final String RGISTER_TICKET = "celtics_ticket_register";

    public static int TICKET_TIMEOUT_SECONDS = 60 * 60 * 2;         // 2小时
    public static int TOKEN_TIMEOUT_SECONDS  = 60 * 60 * 24 * 7;    // 7天


    public boolean verifyTicketOrUpdate(String type, String ticket);
    public boolean verifyToken(String type, String token);
    public UserModel loginViaPassword(String username, String password);
    public String creteTicket(String type, int id);
    public UserModel getUserModelByTicket(String type, String ticket);

    public int register(String username, String password);
    public String createToken(int id);
    public void addTicketSet(String global, String localType, String local);
    public void logoffTicket(String global, String token);

    public Map<String, String> getLocalTickets(String global);

    public boolean verifyOneTimeTicket(String type, String ticket);

    public UserModel getUserById(int id);
}