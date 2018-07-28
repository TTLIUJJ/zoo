package com.ackerman.service;

import com.ackerman.dao.CommentDao;
import com.ackerman.dao.NewsDao;
import com.ackerman.model.Comment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @Author: Ackerman
 * @Description:
 * @Date: Created in 下午10:02 18-6-23
 */
@Service
public class CommentService {

    @Autowired
    private CommentDao commentDao;

    @Autowired
    private NewsDao newsDao;

    public List<Comment> getCommentListByUserId(int userId){
        try{
            return commentDao.getCommentsByNewsId(userId);
        }catch (Exception e){
            e.printStackTrace();
        }
        return new ArrayList<>();
    }


    public int addComment(int userId,  int newsId, String content){
        try{
            Comment comment = new Comment();
            comment.setUserId(userId)
                    .setNewsId(newsId)
                    .setContent(content)
                    .setCreateDate(new Date());

            return commentDao.addComment(comment);
        }catch (Exception e){
            e.printStackTrace();
        }
        return 0;
    }

    public int incrAndGetCommentCount(int newsId){
        try{
            newsDao.incrCommentCount(newsId);
            return newsDao.getCommentCount(newsId);
        }catch (Exception e){
            e.printStackTrace();
        }
        return 0;
    }
}