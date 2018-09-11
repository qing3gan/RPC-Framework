package com.agony.rpc.sample.server;

import com.agony.rpc.sample.client.HelloService;
import com.agony.rpc.sample.client.Person;
import com.agony.rpc.server.RpcService;

@RpcService(HelloService.class)
public class HelloServiceImpl implements HelloService {

    public String hello(String name) {
        System.out.println(String.format("RpcRequest success, RpcResponse is [Hello, %s!]", name));
        return String.format("[Hello, %s!]", name);
    }

    public String hello(Person person) {
        System.out.println(String.format("RpcRequest success, RpcResponse is [Hello, %s %s!]", person.getFirstName(), person.getLastName()));
        return String.format("[Hello, %s %s!]", person.getFirstName(), person.getLastName());
    }
}
