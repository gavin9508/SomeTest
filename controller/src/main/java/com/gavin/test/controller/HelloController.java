package com.gavin.test.controller;

import com.gavin.test.entity.User;
import com.gavin.test.service.HelloService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author:gavin
 * @date:2018/9/20
 **/
@RestController
public class HelloController {

    @Autowired
    private HelloService helloService;

    @GetMapping("/hello")
    public String hello() {
        return helloService.sayHello();
    }

    @GetMapping("/getUser")
    public List<User> getUser() {
        return helloService.createToken("");
    }

}
