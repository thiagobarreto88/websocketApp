package com.cursochat.websocket.handler;

import com.cursochat.websocket.data.User;
import com.cursochat.websocket.dtos.ChatMessage;
import com.cursochat.websocket.events.Event;
import com.cursochat.websocket.events.EventType;
import com.cursochat.websocket.pubsub.Publisher;
import com.cursochat.websocket.services.TicketService;
import com.cursochat.websocket.services.UserService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

@Component
public class WebSocketHandler extends TextWebSocketHandler  {

    private final static Logger LOGGER = Logger.getLogger(WebSocketHandler.class.getName());

    private final TicketService ticketService;

    private final UserService userService;

    private final Publisher publisher;

    private final Map<String, WebSocketSession> sessions;
    private final Map<String, String> userIds;

    public WebSocketHandler(TicketService ticketService, UserService userService, Publisher publisher){
        this.ticketService = ticketService;
        this.userService = userService;
        this.publisher = publisher;
        sessions = new ConcurrentHashMap<>();
        userIds = new ConcurrentHashMap<>();
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        LOGGER.info("[afterConnectionEstablished] session id: " + session.getId());

        Optional<String> ticket = ticketOf(session);
        if(ticket.isEmpty() || ticket.get().isBlank()){
            LOGGER.warning("session " + session.getId() + " without ticket");
            close(session, CloseStatus.POLICY_VIOLATION);
            return;
        }

        Optional<String> userId = ticketService.getUserIdByTicket(ticket.get());

        if(userId.isEmpty()){
            LOGGER.warning("session " + session.getId() + " with invalid ticket");
            close(session, CloseStatus.POLICY_VIOLATION);
            return;
        }

        sessions.put(userId.get(), session);
        userIds.put(session.getId(), userId.get());
        LOGGER.info("session " + session.getId() + " was bind to user " + userId.get());

        sendChatUsers(session);
    }

    private void sendChatUsers(WebSocketSession session) {
        List<User> chatUsers = userService.findChatUsers();
        Event<List<User>> event = new Event<>(EventType.CHAT_USERS_WERE_UPDATED, chatUsers);
        sendEvent(session, event);
    }

    private void sendEvent(WebSocketSession session, Event<?> event) {
        try {
            String eventJson = new ObjectMapper().writeValueAsString(event);
            session.sendMessage(new TextMessage(eventJson));

        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private void close(WebSocketSession session, CloseStatus closeStatus){

        try {
            session.close(closeStatus);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    private Optional<String> ticketOf(WebSocketSession session){
        return Optional
                .ofNullable(session.getUri())
                .map(UriComponentsBuilder::fromUri)
                .map(UriComponentsBuilder::build)
                .map(UriComponents::getQueryParams)
                .map(it -> it.get("ticket"))
                .flatMap(it -> it.stream().findFirst())
                .map(String::trim);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
        LOGGER.info("[handleTextMessage] message: " + message.getPayload());
        if(message.getPayload().equals("ping")) {
            session.sendMessage(new TextMessage("pong"));
        }
        MessagePayload payload = new ObjectMapper().readValue(message.getPayload(), MessagePayload.class);

        String userIdFrom = userIds.get(session.getId());

        publisher.publishChatMessage(userIdFrom, payload.getTo(), payload.getText());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        LOGGER.info("[afterConnectionClosed] session id: " + session.getId());
        String userId = userIds.get(session.getId());
        sessions.remove(userId);
        userIds.remove(session.getId());
    }


    public void notifyUser(ChatMessage chatMessage){

        Event<ChatMessage> event = new Event<>(EventType.CHAT_MESSAGE_WAS_CREATED, chatMessage);
        WebSocketSession userToSession = sessions.get(chatMessage.getTo().getId());
        if(Objects.nonNull(userToSession)){
            sendEvent(userToSession, event);
        }

    }

}
