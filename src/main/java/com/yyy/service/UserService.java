package com.yyy.service;

import com.spring.AutoWired;
import com.spring.Component;

@Component
public class UserService {
    @AutoWired
    private OrderService orderService;

    public void test() {
        orderService.printJoke();
    }
}
