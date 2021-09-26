package com.ariescat.seckill.mapper;

import com.ariescat.seckill.bean.User;
import org.apache.ibatis.annotations.*;

@Mapper
public interface UserMapper {

    @Select("select * from sk_user where id = #{id}")
    User getById(@Param("id") long id);

    @Update("update sk_user set password = #{password} where id = #{id}")
    void updatePassword(User user);

    @Insert("insert into sk_user(id, name) values(#{id}, #{name})")
    int insert(User user);
}
