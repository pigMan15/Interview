package com.pigman.service;

import com.spring.BeanPostProcessor;
import com.spring.Component;
import com.spring.PigmanValue;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

@Component
public class PigmanValueBeanPostProcessor implements BeanPostProcessor {

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) {
        Class<?> clazz = bean.getClass();
        for(Field field: clazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(PigmanValue.class)) {
                field.setAccessible(true);
                String value = field.getAnnotation(PigmanValue.class).value();
                try {
                    field.set(bean, value);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }

        }

        return bean;
    }
}
