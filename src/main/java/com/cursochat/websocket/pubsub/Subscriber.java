package com.cursochat.websocket.pubsub;

import com.cursochat.websocket.config.RedisConfig;
import com.cursochat.websocket.dtos.ChatMessage;
import com.cursochat.websocket.handler.WebSocketHandler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.ReactiveSubscription;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.logging.Logger;

@Component
public class Subscriber {

    private static final Logger LOGGER = Logger.getLogger(Subscriber.class.getName());

    @Autowired
    private ReactiveStringRedisTemplate redisTemplate;

    @Autowired
    private WebSocketHandler webSocketHandler;

    @PostConstruct
    private void init(){
        this.redisTemplate.listenTo(ChannelTopic.of(RedisConfig.CHAT_MESSAGES_CHANNEL))
                .map(ReactiveSubscription.Message::getMessage)
                .subscribe(this::onChatMessage);
    }

    private void onChatMessage(final String chatMEssageSerialized) {

        LOGGER.info("chat mesage was received");

        try {
            ChatMessage chatMessage = new ObjectMapper().readValue(chatMEssageSerialized, ChatMessage.class);
            webSocketHandler.notifyUser(chatMessage);

        } catch (JsonProcessingException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

    }
}
