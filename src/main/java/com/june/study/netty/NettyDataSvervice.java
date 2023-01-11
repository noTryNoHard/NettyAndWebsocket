package com.june.study.netty;

import com.june.study.websocket.controller.WebSocketServer;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author June
 */
@Slf4j
public class NettyDataSvervice {

    private static final ReentrantLock Lock = new ReentrantLock();

    /**
     * 将传送过来的数据进行解析，包括异或运算 （第一次服务器端给客户端发）
     *
     * @param ReceiveData 接收数据
     *
     * @return String
     */
    public static String sendData(String ReceiveData) {
        final ReentrantLock putLock = Lock;
        log.info("接收数据" + ReceiveData);
        putLock.lock();
        try {
            //此处需要读数据进行校验以及分包黏包处理，本文主要提供思路所以省略
            /*
            *   处理分包黏包、拆分、解析等
            */
            //数据解析
            parseData(ReceiveData);
            try {
                //数据帧WebSocket推送
                WebSocketServer.BroadCastInfo(ReceiveData);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return ReceiveData;
        } finally {
            putLock.unlock();
        }
    }

    /**
     * 数据入库处理
     */
    public static void parseData(String ReceiveData) {
        System.out.println("执行入库操作");
    }
}
