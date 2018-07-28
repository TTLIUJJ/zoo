package com.ackerman.dao;

import com.ackerman.model.News;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * @Author: Ackerman
 * @Description:
 * @Date: Created in 下午10:49 18-6-4
 */
@Mapper
public interface NewsDao {
    public static final String TABLE = "news";
    public static final String INSERT_FIELDS = "type, user_id, like_count, comment_count, title, content, image_link, create_date";
    public static final String SELECT_FIELDS = "id, " + INSERT_FIELDS;

    @Insert({"INSERT INTO ", TABLE, "(" , INSERT_FIELDS, ")",
            "VALUES(#{type}, #{userId}, #{likeCount}, #{commentCount}, #{title}, #{content}, #{imageLink}, #{createDate})"})
    public int addNews(News news);

    @Select({"SELECT ", SELECT_FIELDS, " FROM ", TABLE, " WHERE id = #{id}"})
    public News getNewsById(int id);

    @Select({"SELECT ", SELECT_FIELDS, " FROM ", TABLE, " LIMIT #{offset}, #{limit}"})
    public List<News> getNewsByOffsetAndLimit(@Param("offset") int offset,
                                              @Param("limit") int limit);

    @Select({"SELECT ", SELECT_FIELDS, " FROM ", TABLE, " order by id desc limit #{limitNum}"})
    public List<News> getLastNews(int limitNum);

    @Select({"SELECT comment_count from ", TABLE, " WHERE id = #{newsId}"})
    public int getCommentCount(int newsId);

    @Update({"UPDATE ", TABLE, " SET comment_count = comment_count + 1 WHERE id = #{newsId}"})
    public int incrCommentCount(int newsId);



}