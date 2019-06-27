package com.gavin.test.entity;

/**
 * @author:gavin
 * @date:2018/9/20
 **/
public class User extends BaseEntity {

    private int id;
    private String name;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
