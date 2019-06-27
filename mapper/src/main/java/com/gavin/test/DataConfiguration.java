package com.gavin.test;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * @author:gavin
 * @date:2018/9/20
 **/
@ComponentScan
@Configuration
@MapperScan("com.gavin.test.dao")
public class DataConfiguration {

}
