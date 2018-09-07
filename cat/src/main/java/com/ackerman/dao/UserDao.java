package com.ackerman.dao;

import com.ackerman._third.UserModel;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * @Author: Ackerman
 * @Description:
 * @Date: Created in 下午5:39 18-6-4
 */
@Mapper
public interface UserDao {
    public static final String TABLE = "user";
    public static final String INSERT_FIELDS = " username, password, salt, head_image_url ";
    public static final String SELECT_FIELDS = " id, " + INSERT_FIELDS;

    @Insert({"INSERT INTO ", TABLE, "(" + INSERT_FIELDS + ")",
            "VALUES(#{username}, #{password}, #{salt}, #{headImageUrl})"})
    public int addUser(UserModel user);

    @Select({"SELECT id FROM ", TABLE, " WHERE username = #{username}"})
    public int getIdByUsername(String username);

    @Select({"SELECT ", SELECT_FIELDS, " FROM ", TABLE, " WHERE id = #{id}"})
    public UserModel getUserById(int id);

    @Select({"SELECT", SELECT_FIELDS, " FROM ", TABLE, " WHERE username = #{username} AND password = #{password}"})
    public UserModel getUserViaLogin(@Param("username") String username,
                                     @Param("password") String password);
}