package com.san.yes.springtest.aop;

import com.san.yes.springtest.bean.Person;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.Advisor;
import org.springframework.aop.MethodBeforeAdvice;
import org.springframework.aop.SpringProxy;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.interceptor.CustomizableTraceInterceptor;
import org.springframework.aop.support.AopUtils;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.core.DecoratingProxy;
import org.springframework.util.ClassUtils;

import java.lang.reflect.Proxy;
import java.util.Arrays;

@Slf4j
public class AopTest {
    public static void main(String[] args) {
        new AopTest().cglibProxyFiledNull();
    }

    private void cglibProxyFiledNull() {
        ProxyFactory proxyFactory = new ProxyFactory();
        proxyFactory.setTarget(new Demo());
        // CGLIB 代理可以转为实现类
        Demo proxy = (Demo) proxyFactory.getProxy();
        // proxy 底层调用target的方法，设置到target对象中，proxy本身的age为null
        proxy.setAge(18);
        // 18 ：proxy 底层委托target对象的get方法
        System.out.println(proxy.getAge());
        // null ：proxy自身的age并没有赋值
        System.out.println(proxy.age);
        // null ：proxy无法代理final方法，因此无法委托，自身的age为null
        System.out.println(proxy.finalAge());
    }

    private void jdkProxyCast2Impl() {
        ProxyFactory proxyFactory = new ProxyFactory(new Demo());
        proxyFactory.setInterfaces(DemoInterface.class);
        proxyFactory.addAdvice((MethodBeforeAdvice) (method, args1, target) -> {
                    System.out.println("你被拦截了：方法名为：" + method.getName() + " 参数为--" + Arrays.asList(args1));
                }
        );
        Object proxy = proxyFactory.getProxy();
        // 使用jdk动态代理，生成代理类为接口代理，非实现类子类，此处会报错
        // Exception in thread "main" java.lang.ClassCastException: class com.san.yes.springtest.aop.$Proxy2 cannot be cast to class com.san.yes.springtest.aop.AopTest$Demo (com.san.yes.springtest.aop.$Proxy2 and com.san.yes.springtest.aop.AopTest$Demo are in unnamed module of loader 'app')
        // Demo demo = (Demo) proxy;
        // 可以转换为接口
        DemoInterface demoInterface = (DemoInterface) proxy;
    }

    private void cglibAopProxyTest() {
        // target
        Demo targetObject = new Demo();
        // 创建代理工厂
        ProxyFactory proxyFactory = new ProxyFactory();
        // 不指定代理的接口,则factory默认使用CglibAopProxy
//        proxyFactory.setInterfaces(DemoInterface.class);
//        proxyFactory.setInterfaces(ClassUtils.getAllInterfacesForClass(targetObject.getClass()));

        // 设置目标对象
        proxyFactory.setTarget(targetObject);
        // 添加通知
        proxyFactory.addAdvice((MethodBeforeAdvice) (method, args, target) -> {
            System.out.println("你被拦截了：方法名为：" + method.getName() + " 参数为--" + Arrays.asList(args));
        });
        //创建代理对象,直接转换为实现类
        Demo proxy = (Demo) proxyFactory.getProxy();
        // 调用接口方法
        // 你被拦截了：方法名为：hello 参数为--[]
        // hello world!
        proxy.hello();
        // class com.san.yes.springtest.aop.AopTest$Demo$$SpringCGLIB$$0
        System.out.println(proxy.getClass());
    }

    private void jdkAopProxyTest() {
        // target
        Demo targetObject = new Demo();
        // 创建代理工厂
        ProxyFactory proxyFactory = new ProxyFactory();
        // 指定需要代理的接口
//        proxyFactory.setInterfaces(DemoInterface.class);
        proxyFactory.setInterfaces(ClassUtils.getAllInterfacesForClass(targetObject.getClass()));

        // 设置目标对象
        proxyFactory.setTarget(targetObject);
        // 添加通知
        proxyFactory.addAdvice((MethodBeforeAdvice) (method, args, target) -> {
            System.out.println("你被拦截了：方法名为：" + method.getName() + " 参数为--" + Arrays.asList(args));
        });
        //创建代理对象
        DemoInterface proxy = (DemoInterface) proxyFactory.getProxy();
        // 调用接口方法
        // 你被拦截了：方法名为：hello 参数为--[]
        // hello world!
        proxy.hello();
        // ===== 工厂类型 =====
        // class com.san.yes.springtest.aop.AopTest$Demo
        System.out.println(proxyFactory.getTargetClass());
        // SingletonTargetSource for target object [com.san.yes.springtest.aop.AopTest$Demo@4386f16]
        System.out.println(proxyFactory.getTargetSource());
        // [interface com.san.yes.springtest.aop.AopTest$DemoInterface]
        System.out.println(Arrays.asList(proxyFactory.getProxiedInterfaces()));
        // [org.springframework.aop.support.DefaultPointcutAdvisor: pointcut [Pointcut.TRUE]; advice [com.san.yes.springtest.aop.AopTest$$Lambda$57/0x000001fdba080e78@363ee3a2]]
        System.out.println(Arrays.asList(proxyFactory.getAdvisors()));

        // ===== 代理对象 =====
        // true JDK 动态代理继承JDK Proxy类
        System.out.println(proxy instanceof Proxy);
        // true Spring生成代理实现标记接口：SpringProxy
        System.out.println(proxy instanceof SpringProxy);
        // class com.san.yes.springtest.aop.$Proxy0
        System.out.println(proxy.getClass());
        // true Proxy类静态方法
        System.out.println(Proxy.isProxyClass(proxy.getClass()));
        // true AopUtils 工具方法
        System.out.println(AopUtils.isAopProxy(proxy));
        // false AopUtils 工具方法
        System.out.println(AopUtils.isCglibProxy(proxy));
        // ===== 代理对象实现Advised =====
        Advised advised = (Advised) proxy;
        // [org.springframework.aop.support.DefaultPointcutAdvisor: pointcut [Pointcut.TRUE]; advice [com.san.yes.springtest.aop.AopTest$$Lambda$18/0x000001e48101cce8@23faf8f2]]
        System.out.println(Arrays.asList(advised.getAdvisors()));
        // [interface com.san.yes.springtest.aop.AopTest$DemoInterface]
        System.out.println(Arrays.asList(advised.getProxiedInterfaces()));
        // false
        System.out.println(advised.isExposeProxy());
        // false
        System.out.println(advised.isFrozen());
        // ===== 代理对象实现DecoratingProxy =====
        DecoratingProxy decoratingProxy = (DecoratingProxy) proxy;
        // class com.san.yes.springtest.aop.AopTest$Demo
        System.out.println(decoratingProxy.getDecoratedClass());

        // ===== 代理对象的Object方法 =====
        // 1718317206
        System.out.println(proxy.hashCode());
        // false
        System.out.println(proxy.equals(new Object()));
        // class com.san.yes.springtest.aop.$Proxy0
        System.out.println(proxy.getClass());
        // toString方法被拦截
        // 你被拦截了：方法名为：toString 参数为--[]
        // com.san.yes.springtest.aop.AopTest$Demo@27a5f880
        System.out.println(proxy.toString());
    }

    private static void customizableTraceInterceptorTest() {
        log.info("customizableTraceInterceptorTest");
        ProxyFactory proxyFactory = new ProxyFactory(new Person());
        // 强制使用CGLIB，确保Person方法能正常调用
        proxyFactory.setProxyTargetClass(true);
        // 通知
        CustomizableTraceInterceptor advice = new CustomizableTraceInterceptor();
//        advice.setUseDynamicLogger(false);
        // 切面：切点+通知
        Advisor advisor = new DefaultPointcutAdvisor(advice);
        // 工厂设置切面
        proxyFactory.addAdvisor(advisor);
        // 获取代理对象
        Person person = (Person) proxyFactory.getProxy();
        person.run();
        person.say();
    }

    interface DemoInterface {
        void hello();
    }

    class Demo implements DemoInterface {
        public Integer age;

        // 此处用final修饰了  CGLIB也不会代理此方法了
        public final Integer finalAge() {
            return age;
        }

        public Integer getAge() {
            return age;
        }

        public void setAge(Integer age) {
            this.age = age;
        }

        @Override
        public void hello() {
            System.out.println("hello world!");
        }
    }
}
