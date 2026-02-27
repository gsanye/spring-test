package com.san.yes.springtest.service;

import com.san.yes.springtest.JdbcConfig;
import com.san.yes.springtest.SpringTestApplication;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.sql.DataSource;

//@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = {SpringTestApplication.class, JdbcConfig.class})
@ContextConfiguration(classes = {SpringTestApplication.class})
public class UserServiceImplTest {

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