package com.june.study.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author June
 * @description
 */
@Slf4j
@Component
@ChannelHandler.Sharable
public class MyByteEncoder extends MessageToByteEncoder<byte[]> {
    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, byte[] bytes, ByteBuf out) {
        log.info("MyByteEncoder 被调用");
        //字节数组
        out.writeBytes(bytes);
    }
}
