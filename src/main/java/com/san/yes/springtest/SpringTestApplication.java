package com.san.yes.springtest;

import com.san.yes.springtest.bean.Color;
import com.san.yes.springtest.bean.Color2;
import com.san.yes.springtest.bean.Person;
import com.san.yes.springtest.dto.User;
import com.san.yes.springtest.service.UserService;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScans;

@SpringBootApplication(scanBasePackages = {"com.san.yes.springtest"})
public class SpringTestApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(SpringTestApplication.class, args);
//        testCreateBean(context);
        destroyContext(context);
    }

    private static void destroyContext(ConfigurableApplicationContext context) {
        context.close();
    }

    private static void testCreateBean(ConfigurableApplicationContext context) {
        System.out.println(context.getBeansOfType(Color.class));
        System.out.println(context.getBeansOfType(Color2.class));
        // get factoryBean
        System.out.println(context.getBean("&myFactoryBean"));
        // get factoryBean created bean
        System.out.println(context.getBean("myFactoryBean"));
    }

    private static void testAddUser(ConfigurableApplicationContext context) {
        UserService userService = context.getBean(UserService.class);
        User user = new User();
        user.setName("zhangsan");
        user.setAge(18);
        userService.addUser(user);
    }

}
