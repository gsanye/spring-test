package com.san.yes.springtest.bean.factory;

import com.san.yes.springtest.bean.Color;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.FactoryBean;

public class MyFactoryBean implements FactoryBean<Color> {
    public MyFactoryBean() {
        System.out.println("MyFactoryBean constructor called");
    }

    @Override
    public @Nullable Color getObject() throws Exception {
        System.out.println("MyFactoryBean getObject called");
        Color color = new Color();
        color.setColor("create by MyFactoryBean");
        return color;
    }

    @Override
    public @Nullable Class<?> getObjectType() {
        return Color.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }
}
