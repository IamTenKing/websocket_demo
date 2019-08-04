package com.example.websoketdemo.flash;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

/**
 * @author jt
 * @date 2019-8-3
 */
@Component
public class MyApplicationListener implements ApplicationListener<ContextRefreshedEvent> {

    @Autowired
    private FlashPolicyServer flashPolicyServer;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        System.out.println("flashPolicyServer start....");
        flashPolicyServer.start();

    }
}
