package com.san.yes.springtest;

import com.san.yes.springtest.service.UserServiceImpl;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;

@SpringBootTest(classes = {SpringTestApplication.class, JdbcConfig.class})
public class SpringTestApplicationTests {

    @Test
    public void contextLoads() {
    }


    @Autowired
    private DataSource dataSource;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    @Autowired
    private UserServiceImpl userServiceImpl;

    @Test
    public void testAddUser() {
        System.out.println(dataSource);
        System.out.println(jdbcTemplate);
        System.out.println(namedParameterJdbcTemplate);
        System.out.println(userServiceImpl);

    }

}
