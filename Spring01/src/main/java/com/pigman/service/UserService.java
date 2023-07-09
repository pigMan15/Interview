package com.pigman.service;

import com.spring.*;

@Component
public class UserService implements UserInterface, BeanNameAware {

    @Autowired
    private OrderService orderService;

    @PigmanValue(value = "测试字段属性")
    private String testName;

    private String beanName;

    public void test(){
        System.out.println(beanName);
    }

    @Override
    public void setBeanName(String name) {
        this.beanName = name;
    }
}
