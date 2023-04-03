package com.cursochat.websocket.handler;

import lombok.Data;

@Data
public class MessagePayload {

    private String to;
    private String text;

}