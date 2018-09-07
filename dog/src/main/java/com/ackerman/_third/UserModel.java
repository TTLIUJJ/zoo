package com.ackerman._third;

import java.io.Serializable;

/**
 * @Author: Ackerman
 * @Description:
 * @Date: Created in 上午9:20 18-6-8
 */
public class UserModel implements Serializable{
    int id;
    String username;
    String password;
    String salt;
    String headImageUrl;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getSalt() {
        return salt;
    }

    public void setSalt(String salt) {
        this.salt = salt;
    }

    public String getHeadImageUrl() {
        return headImageUrl;
    }

    public void setHeadImageUrl(String headImageUrl) {
        this.headImageUrl = headImageUrl;
    }

    public String toString(){
        return "[id=" + id +
                ", username=" + username +
                ", password=" + password +
                ", salt=" + salt +
                ", headImageUrl=" + headImageUrl + "]";
    }

}