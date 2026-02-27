package com.san.yes.springtest.service;

import com.san.yes.springtest.dto.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Transactional
    @Override
    public int addUser(User user) {
        // 插入记录
        String sql = "insert into t_user(name,age) values(?,?)";
        int update = jdbcTemplate.update(sql, user.getName(), user.getAge());
        // do something else
        System.out.println(1 / 0);
        return update;
    }

}
