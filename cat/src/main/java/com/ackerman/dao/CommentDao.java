package com.ackerman.dao;

import com.ackerman.model.Comment;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * @Author: Ackerman
 * @Description:
 * @Date: Created in 下午9:59 18-6-23
 */
@Mapper
public interface CommentDao {
    public static final String TABLE = "comment";
    public static final String INSERT_FIELDS = "user_id, create_date, content, news_id";
    public static final String SELECT_FIELDS = "id, " + INSERT_FIELDS;

    @Select({"SELECT ", SELECT_FIELDS, " FROM ", TABLE, " WHERE news_id = #{newsId} order by create_date desc"})
    public List<Comment> getCommentsByNewsId(int newsId);


    @Insert({"INSERT INTO ", TABLE, "(", INSERT_FIELDS, ")",
            "VALUES (#{userId}, #{createDate}, #{content}, #{newsId} )"})
    public int addComment(Comment comment);
}