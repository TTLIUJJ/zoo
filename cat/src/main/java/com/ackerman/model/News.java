package com.ackerman.model;

import java.util.Date;

/**
 * @Author: Ackerman
 * @Description:　新闻的相关信息
 * @Date: Created in 下午5:00 18-6-4
 */
public class News {
    private int id;             //新闻的序号
    private int type;           //1原创  2转载
    private int userId;         //创建人
    private int likeCount;      //点赞数
    private int commentCount;   //评论数
    private String title;       //新闻标题
    private String content;        //原创为内容, 转载为新闻转接链接
    private String imageLink;   //图片链接,
    private Date createDate;    //发表日期

    private long score;          //计算热门新闻的分数, 排序所需

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getLikeCount() {
        return likeCount;
    }

    public void setLikeCount(int likeCount) {
        this.likeCount = likeCount;
    }

    public int getCommentCount() {
        return commentCount;
    }

    public void setCommentCount(int commentCount) {
        this.commentCount = commentCount;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getImageLink() {
        return imageLink;
    }

    public void setImageLink(String imageLink) {
        this.imageLink = imageLink;
    }

    public Date getCreateDate() {
        return createDate;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

    public long getScore() {
        return score;
    }

    public void setScore(long score) {
        this.score = score;
    }

    public String toString(){
        return "[id=" + id + ", " +
                "type=" + type + ", " +
                "userId=" + userId + ", " +
                "likeCount=" + likeCount + ", " +
                "commentCount=" + commentCount + ", " +
                "title=" + title + ", " +
                "content=" + content + ", " +
                "imageLink=" + imageLink + ", " +
                "createDate=" + createDate.toString() +
                "score=" + score + "]";
    }
}