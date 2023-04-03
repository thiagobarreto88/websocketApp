package com.cursochat.websocket.services;

import com.cursochat.websocket.data.User;
import com.cursochat.websocket.data.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    public List<User> findChatUsers(){
        return userRepository.findAll();
    }

}
