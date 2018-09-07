package com.ackerman.impl;

import com.ackerman._third.Common;
import com.ackerman._third.UserModel;

import java.io.Serializable;
import java.util.Map;

public class CommonImpl implements Common, Serializable {

    public boolean verifyTicketOrUpdate(String type, String ticket) {
        return false;
    }

    public boolean verifyToken(String type, String token) {
        return false;
    }

    public UserModel loginViaPassword(String username, String password) {
        return null;
    }

    public String creteTicket(String type, int id) {
        return null;
    }

    public UserModel getUserModelByTicket(String type, String ticket) {
        return null;
    }

    public int register(String username, String password) {
        return 0;
    }

    public String createToken(int id) {
        return "shit" + id;
    }

    public void addTicketSet(String global, String localType, String local) {

    }

    public void logoffTicket(String global, String token) {

    }

    public Map<String, String> getLocalTickets(String global) {
        return null;
    }

    public boolean verifyOneTimeTicket(String type, String ticket) {
        return false;
    }

    public UserModel getUserById(int id) {
        return null;
    }
}
