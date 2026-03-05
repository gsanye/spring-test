package com.san.yes.springtest.aop.advice;

import lombok.extern.slf4j.Slf4j;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.Arrays;

@Slf4j
public class MyMethodInteceptor implements MethodInterceptor {
    @Override
    public @Nullable Object invoke(MethodInvocation invocation) throws Throwable {
        Method method = invocation.getMethod();
        @Nullable Object[] args = invocation.getArguments();
        log.info("你被拦截了：方法名为：" + method.getName() + " 参数为--" + Arrays.asList(args));
        return invocation.proceed();
    }
}
