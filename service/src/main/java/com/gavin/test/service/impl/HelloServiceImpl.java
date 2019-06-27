package com.gavin.test.service.impl;

import com.gavin.test.dao.UserMapper;
import com.gavin.test.entity.User;
import com.gavin.test.service.HelloService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author:gavin
 * @date:2018/9/20
 **/
@Service
public class HelloServiceImpl implements HelloService {


    @Autowired
    private UserMapper userMapper;

    @Override
    public String sayHello() {
        return "Hello, SpringBoot";
    }

    @Override
    public List<User> createToken(String key) {
        List<User> data = userMapper.all();
        return data;
    }
}
