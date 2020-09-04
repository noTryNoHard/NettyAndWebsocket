package com.june.study.websocket.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * @author June
 * @description
 */
@Controller
public class WebsocketTest {

    @RequestMapping("/webSocket")
    public String webSocket() {
        return "webSocket/webSocket";
    }
}
