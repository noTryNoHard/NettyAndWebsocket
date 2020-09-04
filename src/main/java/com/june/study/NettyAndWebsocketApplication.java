package com.june.study;

import com.june.study.netty.NettyServer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author June
 */
@SpringBootApplication
public class NettyAndWebsocketApplication {

    public static void main(String[] args) throws Exception {
        SpringApplication.run(NettyAndWebsocketApplication.class, args);
        //启动服务端
        NettyServer nettyServer = new NettyServer();
        nettyServer.run();
    }
}
