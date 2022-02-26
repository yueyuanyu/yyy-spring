package com.yyy.service;

import com.spring.BeanPostProcessor;

public class AopPostProcessor implements BeanPostProcessor {

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) {
        return bean;
    }
}
