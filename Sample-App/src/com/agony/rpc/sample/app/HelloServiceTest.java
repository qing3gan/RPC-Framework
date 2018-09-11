package com.agony.rpc.sample.app;

import com.agony.rpc.client.RpcProxy;
import com.agony.rpc.sample.client.HelloService;
import com.agony.rpc.sample.client.Person;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:spring.xml")
public class HelloServiceTest {

    @Autowired
    private RpcProxy rpcProxy;

    @Test
    public void helloTest1() {
        HelloService helloService = rpcProxy.create(HelloService.class);
        System.out.println(helloService.hello("test1"));
    }

    @Test
    public void helloTest2() {
        HelloService helloService = rpcProxy.create(HelloService.class);
        System.out.println(helloService.hello(new Person("test2", "test2")));
    }
}
