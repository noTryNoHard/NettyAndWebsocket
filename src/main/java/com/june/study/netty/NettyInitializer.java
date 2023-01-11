package com.june.study.netty;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * @author June
 * @description
 */
@Slf4j
@Component
@ChannelHandler.Sharable
public class NettyInitializer extends ChannelInitializer {

    @Override
    protected void initChannel(Channel channel) {
        //创建一个通道初始化对象(匿名对象)
        //给pipeline 设置处理器
        //可以使用一个集合管理 SocketChannel,再推送消息时,可以将业务加入到各个channel对应的NIOEventLoop的taskQueue
        // 或者 scheduleTaskQueue
        log.info("客户socketChannel hashcode=" + channel.hashCode());
        //入站解码
        //channel.pipeline().addLast(new MyByteToLongDecoder2());
        //出站编码
        channel.pipeline().addLast(new MyByteEncoder());
        channel.pipeline().addLast(new NettyServerHandler());
    }
}
