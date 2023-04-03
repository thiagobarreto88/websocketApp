package com.cursochat.websocket.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collation = "user")
@Data
@AllArgsConstructor
public class User{

    private String id;
    private String name;
    private String picture;
}
