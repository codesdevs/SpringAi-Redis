package com.liyuxiang.controller;

import com.liyuxiang.memory.SpringAiRedisChatMemory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;


/**
 * @Author: liyuxiang
 * @Date: 2025/8/3 14:02
 */
@RestController
@RequestMapping("/ai")
public class ChatController {
    @Autowired
    ChatClient chatClient;
    @Autowired
    SpringAiRedisChatMemory springAiRedisChatMemory;

    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ChatResponse> chatStream(String question, String sessionId) {
        return this.chatClient.prompt()
                              .advisors(advisor -> advisor
                                      .param(AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY, sessionId))
                              .user(question)
                              .stream()
                              .chatResponse();
    }

    @GetMapping("/list/{sessionId}")
    public List<Message> list(@PathVariable(value = "sessionId") String sessionId) {
        return springAiRedisChatMemory.get(sessionId, -1);
    }

    @DeleteMapping("/clear/{sessionId}")
    public void clear(@PathVariable(value = "sessionId") String sessionId) {
        springAiRedisChatMemory.clear(sessionId);
    }

    @DeleteMapping("/delete/{sessionId}/{messageId}")
    public void deleteByMessageId(@PathVariable(value = "sessionId") String sessionId, @PathVariable(value = "messageId") String messageId) {
        springAiRedisChatMemory.deleteByMessageId(sessionId, messageId);
    }
}
