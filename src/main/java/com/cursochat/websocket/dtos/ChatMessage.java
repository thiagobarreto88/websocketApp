package com.cursochat.websocket.dtos;

import com.cursochat.websocket.data.User;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ChatMessage {

    private User from;
    private User to;
    private String text;

}
