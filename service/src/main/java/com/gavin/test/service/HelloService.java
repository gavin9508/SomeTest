package com.gavin.test.service;

import com.gavin.test.entity.User;

import java.util.List;

/**
 * @author:gavin
 * @date:2018/9/20
 **/
public interface HelloService {

    String sayHello();

    List<User> createToken(String key);

}
