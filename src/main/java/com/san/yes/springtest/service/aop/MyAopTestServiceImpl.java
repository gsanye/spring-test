package com.san.yes.springtest.service.aop;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class MyAopTestServiceImpl {
    public void hello() {
        log.info("hello:{}", doHello());
    }

    private String doHello() {
        return "private method doGet";
    }
}
