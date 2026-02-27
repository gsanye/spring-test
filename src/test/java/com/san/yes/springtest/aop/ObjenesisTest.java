package com.san.yes.springtest.aop;

import lombok.Data;
import org.springframework.cglib.core.DefaultGeneratorStrategy;
import org.springframework.cglib.core.SpringNamingPolicy;
import org.springframework.cglib.proxy.*;
import org.springframework.objenesis.Objenesis;
import org.springframework.objenesis.ObjenesisStd;
import org.springframework.objenesis.SpringObjenesis;
import org.springframework.objenesis.instantiator.ObjectInstantiator;

import java.lang.reflect.Method;

public class ObjenesisTest {
    private Boolean worthTrying;

    public static void main(String[] args) throws Exception {
        enhanceAndObjenesis();
    }

    private static void enhanceAndObjenesis() {
        MethodInterceptor methodInterceptor = (o, method, args, methodProxy) -> {
            System.out.println(method.getName() + "---方法拦截前");
            Object result = methodProxy.invokeSuper(o, args);
            System.out.println(method.getName() + "---方法拦截后");
            return result;
        };
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(AopObjectDemo.class);
        // 使用createClass方式不能直接设置callback
        // Exception in thread "main" java.lang.IllegalStateException: createClass does not accept callbacks
        // enhancer.setCallback(methodInterceptor);
        // 设置回调类型
        enhancer.setCallbackTypes(new Class[]{MethodInterceptor.class});
        enhancer.setStrategy(new DefaultGeneratorStrategy());
        enhancer.setNamingPolicy(SpringNamingPolicy.INSTANCE);
        // 设置filter
        enhancer.setCallbackFilter(new CallbackFilter() {
            @Override
            public int accept(Method method) {
                return 0;
            }
        });
        // 使用enhancer创建代理类class
        Class proxyClass = enhancer.createClass();
        // 使用Objenesis创建对象实例，跳过构造函数
        Objenesis objenesis = new SpringObjenesis();
        AopObjectDemo proxyInstance = (AopObjectDemo) objenesis.newInstance(proxyClass);
        // 设置方法代理 和 enhancer的注册静态回调二选一
//        ((Factory) proxyInstance).setCallbacks(new Callback[]{methodInterceptor});
        //关键：注册静态 callbacks（必须在 newInstance 前！）
        Enhancer.registerStaticCallbacks(proxyClass, new Callback[]{methodInterceptor});

        //class com.san.yes.springtest.aop.ObjenesisTest$AopObjectDemo$$SpringCGLIB$$0
        System.out.println(proxyClass);
        System.out.println(proxyInstance.getClass());
        // ObjenesisTest.AopObjectDemo(code=null)
        System.out.println(proxyInstance);
        // null
        System.out.println(proxyInstance.getCode());

    }

    private static void enhanceTest() {
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(AopObjectDemo.class);
        enhancer.setCallback((MethodInterceptor) (o, method, args, methodProxy) -> {
            System.out.println(method.getName() + "---方法拦截前");
            // 此处不能使用method直接invoke，否则将死循环
//            method.invoke(o,args);
            // 应该使用methodProxy#invokeSuper
            Object result = methodProxy.invokeSuper(o, args);
            System.out.println(method.getName() + "---方法拦截后");
            return result;
        });
        // 直接创建实例需要空参构造函数
        // Exception in thread "main" java.lang.IllegalArgumentException: Superclass has no null constructors but no arguments were given
        // AopObjectDemo instance = (AopObjectDemo) enhancer.create();
        // 指定构造函数和参数
        AopObjectDemo instance = (AopObjectDemo) enhancer.create(new Class[]{String.class}, new Object[]{"cglib enhancer create"});
        System.out.println(instance);
        System.out.println(instance.getCode());
    }

    private static void springObjenesisTest() {
        // 只需要替换为spring实现即可
        SpringObjenesis objenesis = new SpringObjenesis();
        // 存在方法：是否值得尝试，默认：true
        System.out.println(objenesis.isWorthTrying());
    }

    /**
     * 是否需要尝试：也就是说，它是否还没有被使用过，或者已知是否有效。方法返回true，表示值得尝试
     * 如果配置的Objenesis Instantiator策略被确定为不处理当前JVM。或者系统属性"spring.objenesis.ignore"值设置为true，表示不尝试了
     * 这个在ObjenesisCglibAopProxy创建代理实例的时候用到了。若不尝试使用Objenesis，那就还是用老的方式用空构造函数吧
     */
    public boolean isWorthTrying() {
        return (this.worthTrying != Boolean.FALSE);
    }

    private static void ObjenesisInstantiatorTest() {
        Objenesis objenesis = new ObjenesisStd();
        // 相当于生成了一个实例创建的工厂，方便创建实例
        // 如果需要创建多个实例，建议使用工厂
        ObjectInstantiator<AopObjectDemo> instantiator = objenesis.getInstantiatorOf(AopObjectDemo.class);
        AopObjectDemo instance1 = instantiator.newInstance();
        AopObjectDemo instance2 = instantiator.newInstance();
        System.out.println(instance1);
        System.out.println(instance2);
    }

    private static void objenesisNewInstatnceTest() throws InstantiationException, IllegalAccessException {
        ObjenesisStd objenesis = new ObjenesisStd();
        // 没有打印任何语句，说明没有通过构造函数以及实例代码块逻辑（即使存在空参构造函数）
        AopObjectDemo instance = objenesis.newInstance(AopObjectDemo.class);
        // ObjenesisTest.ObjenesisObjectDemo(code=null)
        System.out.println(instance);
        // null -- 没有执行构造函数以及初始化动作
        System.out.println(instance.getCode());

        // 不存在空参构造函数的情况下，直接newInstance会抛出异常
        // NoSuchMethodException: com.san.yes.springtest.aop.ObjenesisTest$ObjenesisObjectDemo.<init>()
        AopObjectDemo.class.newInstance();
    }

    @Data
    public static class AopObjectDemo {
        private String code = "default field init";

        {
            System.out.println("objenesisObjectDemo instance struct");
        }

        public AopObjectDemo(String message) {
            System.out.println("objenesisObjectDemo construct " + message);
            this.code = message;
        }

//        public ObjenesisObjectDemo() {
//            System.out.println("objenesisObjectDemo default constructor");
//            this.code = "empty construct";
//        }
    }
}
