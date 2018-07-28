package com.ackerman.model;

import java.util.Date;

/**
 * @Author: Ackerman
 * @Description:
 * @Date: Created in 下午9:50 18-6-23
 */
public class Comment {
    private int id;
    private int userId;
    private int newsId;
    private String content;
    private Date createDate;

    public int getId() {
        return id;
    }

    public int getUserId() {
        return userId;
    }

    public int getNewsId() {
        return newsId;
    }

    public String getContent() {
        return content;
    }

    public Date getCreateDate() {
        return createDate;
    }

    public Comment setId(int id) {
        this.id = id;
        return this;
    }

    public Comment setUserId(int userId) {
        this.userId = userId;
        return this;
    }

    public Comment setNewsId(int newsId) {
        this.newsId = newsId;
        return this;
    }

    public Comment setContent(String content) {
        this.content = content;
        return this;
    }

    public Comment setCreateDate(Date createDate) {
        this.createDate = createDate;
        return this;
    }
}
