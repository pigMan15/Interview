package com.pigman;

import com.pigman.service.UserInterface;
import com.pigman.service.UserService;
import com.spring.PigmanApplicationContext;

public class Test {

    public static void main(String[] args) {
        PigmanApplicationContext applicationContext = new PigmanApplicationContext(AppConfig.class);
        UserInterface userService = (UserInterface) applicationContext.getBean("userService");
        UserInterface userService2 = (UserInterface) applicationContext.getBean("userService");
        UserInterface userService3 = (UserInterface) applicationContext.getBean("userService");

        userService.test();
        userService2.test();
        userService3.test();
    }

}
