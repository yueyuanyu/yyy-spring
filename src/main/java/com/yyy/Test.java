package com.yyy;

import com.spring.ApplicationContext;
import com.yyy.service.UserService;

public class Test {

    public static void main(String[] args) throws  Throwable{

        ApplicationContext applicationContext = new ApplicationContext(AppConfig.class);

        UserService userService = (UserService) applicationContext.getBean("userService");
        userService.test();
    }
}
