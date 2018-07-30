package com.ackerman.dao;

import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;

@Mapper
public interface MysqlLockDao {
    public static final String TABLE = "seckill_goods";
    public static final String INSERT_FIELDS = "mutex, remain";
    public static final String SELECT_FIELDS = "id, " + INSERT_FIELDS;


    @Select({"INSERT INTO ", TABLE, " (mutex, remain) VALUES( #{mutex}, #{remain})"})
    public void insertLock(@Param("mutex") String mutex, @Param("remain") int remain);

    @Select({"SELECT remain FROM ", TABLE, " WHERE mutex = #{mutex}"})
    public Object checkLockIfExist(String mutex);

    @Select({"SELECT remain FROM ", TABLE, " WHERE mutex = #{mutex}"})
    public int queryRemain(String mutex);


    @Update({"UPDATE ", TABLE, " SET remain = #{remain} WHERE mutex = #{mutex}"})
    public int resetLock(@Param("mutex") String mutex, @Param("remain") int remain);

    @Update({"UPDATE ", TABLE, "SET remain = remain - #{buy} WHERE remain >= #{buy} and mutex = #{mutex}"})
    public int tryUpdate(@Param("mutex") String mutex, @Param("buy") int buy);

}
