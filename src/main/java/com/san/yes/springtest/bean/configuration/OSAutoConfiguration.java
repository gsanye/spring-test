package com.san.yes.springtest.bean.configuration;

import com.san.yes.springtest.bean.Person;
import com.san.yes.springtest.bean.condition.LinuxOSCondition;
import com.san.yes.springtest.bean.condition.WindowsOSCondition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OSAutoConfiguration {
    @Bean
    @Conditional(WindowsOSCondition.class)
    public Person windowsCreator(){
        Person person = new Person();
        person.setName("Windows Creator");
        return person;
    }

    @Bean
    @Conditional(LinuxOSCondition.class)
    public Person linuxCreator(){
        Person person = new Person();
        person.setName("Linux Creator");
        return person;
    }

}
