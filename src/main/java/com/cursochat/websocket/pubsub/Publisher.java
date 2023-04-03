package com.cursochat.websocket.pubsub;

import com.cursochat.websocket.config.RedisConfig;
import com.cursochat.websocket.data.User;
import com.cursochat.websocket.data.UserRepository;
import com.cursochat.websocket.dtos.ChatMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.logging.Logger;

@Component
public class Publisher {

    private final static Logger LOGGER = Logger.getLogger(Publisher.class.getName());

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ReactiveStringRedisTemplate redisTemplate;

    public void publishChatMessage(String userIdFrom, String userTo, String text) throws JsonProcessingException {
        User from = userRepository.findById(userIdFrom).orElseThrow();
        User to = userRepository.findById(userTo).orElseThrow();

        ChatMessage chatMessage = new ChatMessage(from, to, text);
        String chatMessageSerialized = new ObjectMapper().writeValueAsString(chatMessage);

        redisTemplate.convertAndSend(RedisConfig.CHAT_MESSAGES_CHANNEL, chatMessageSerialized)
                .subscribe();

        LOGGER.info("chat message was published");
    }
}
