package com.example.sales.dto.websocket;

import com.example.sales.constant.WebSocketMessageType;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class WebSocketMessage<T> {
    private WebSocketMessageType type;
    private T data;
}
