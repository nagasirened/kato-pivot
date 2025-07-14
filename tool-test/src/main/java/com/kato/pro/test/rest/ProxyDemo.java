package com.kato.pro.test.rest;

import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;

import java.lang.reflect.Method;

public class ProxyDemo {

    public interface IUser {
        void sayHello();

        String getName();

        void setName(String name);
    }

    public static class User implements IUser {

        private String name;

        @Override
        public void sayHello() {
            System.out.println("hello " + getName());
        }

        @Override
        public String getName() {
            return this.name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }


    public static void main(String[] args) {
        // User user = new User();
        // InvocationHandler invocationHandler = (proxy, method, args1) -> {
        //     System.out.println("代理类开始执行了哟");
        //     Object invoke = method.invoke(user, args1);
        //     System.out.println("代理类执行结束了哟");
        //     System.out.println("========");
        //     return invoke;
        // };
        // IUser userProxy = (IUser) Proxy.newProxyInstance(
        //         User.class.getClassLoader(),
        //         User.class.getInterfaces(),
        //         invocationHandler
        // );
        // userProxy.setName("kato");
        // userProxy.sayHello();

        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(User.class);
        enhancer.setCallback(new MethodInterceptor() {
            @Override
            public Object intercept(Object o, Method method, Object[] objects, MethodProxy methodProxy) throws Throwable {
                System.out.println("代理类开始执行了哟" + method.getName());
                Object res = methodProxy.invokeSuper(o, objects);
                System.out.println("代理类执行结束了哟" + method.getName());
                return res;
            }
        });
        User userProxy = (User)enhancer.create();
        userProxy.sayHello();
    }

}
