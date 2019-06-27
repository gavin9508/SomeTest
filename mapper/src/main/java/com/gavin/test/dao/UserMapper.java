package com.gavin.test.dao;

import com.gavin.test.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author:gavin
 * @date:2018/9/20
 **/
@Mapper
@Repository
public interface UserMapper {

    List<User> all();

}
