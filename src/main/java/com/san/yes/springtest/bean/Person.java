package com.san.yes.springtest.bean;

import lombok.Data;

@Data
public class Person {
    private String name;
    private int age;

    public String run(){
        System.out.println("i am running");
        return "i am run";
    }
    public String say(){
        System.out.println("i am saying");
        return "i am say";
    }
}
