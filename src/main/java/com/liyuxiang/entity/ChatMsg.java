package com.liyuxiang.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.ai.chat.messages.*;

import java.util.List;
import java.util.Map;

/**
 * @Author: liyuxiang
 * @Date: 2025/8/3 15:54
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatMsg {
    //信息类型：是AI的，还是用户的，或者是系统的
    MessageType messageType;
    //对话的文本信息
    String text;
    //其他信息
    Map<String, Object> metadata;

    //将我们的ChatMsg转为SpringAI的Message
    public Message toMessage() {
        return switch (messageType) {
            case SYSTEM -> new SystemMessage(text);
            case USER -> new UserMessage(text, List.of(), metadata);
            case ASSISTANT -> new AssistantMessage(text, metadata, List.of(), List.of());
            default -> throw new IllegalArgumentException("Unsupported message type: " + messageType);
        };
    }
}
