package com.cursochat.websocket.events;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Event<T> {

    private EventType type;
    private T payload;

}