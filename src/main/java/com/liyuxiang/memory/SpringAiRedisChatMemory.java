package com.liyuxiang.memory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.liyuxiang.entity.ChatMsg;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * @Author: liyuxiang
 * @Date: 2025/8/3 14:28
 * 基于Redis实现的聊天记忆存储
 * 实现了ChatMemory接口，用于存储和检索聊天消息历史
 */
@Component
public class SpringAiRedisChatMemory implements ChatMemory {

    // Redis操作模板
    StringRedisTemplate redisTemplate;
    // JSON序列化工具
    ObjectMapper objectMapper;

    public SpringAiRedisChatMemory(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }


    /**
     * 添加消息到聊天记忆
     *
     * @param conversationId: 会话ID，用于区分不同对话
     * @param messages:       要添加的消息列表
     */
    @Override
    public void add(String conversationId, List<Message> messages) {
        if (messages == null || messages.isEmpty()) {
            return;
        }

        List<String> list = messages.stream()
                                    .map(msg -> {
                                        try {
                                            //由于SpringAI的UserMessage没有id，所以我们在这里生成一个唯一的ID
                                            msg.getMetadata().put("id", UUID.randomUUID().toString().replaceAll("-", ""));
                                            return objectMapper.writeValueAsString(msg);
                                        } catch (JsonProcessingException e) {
                                            throw new RuntimeException("Failed to serialize message", e);
                                        }
                                    })
                                    .toList();
        redisTemplate.opsForList().leftPushAll(conversationId, list);
    }

    /**
     * 从聊天记忆中获取指定数量的最近消息
     *
     * @param conversationId: 会话ID
     * @param lastN:          要获取消息的结束位置（从0开始计数）
     *                        例如，lastN=5表示获取最近5条消息
     *                        注意：这里的lastN是一个结束位置，而不是数量
     * @return 返回一个包含最近消息的列表
     */
    @Override
    public List<Message> get(String conversationId, int lastN) {
        // 从Redis获取指定范围的聊天记录（从0到lastN-1）
        List<String> list = redisTemplate.opsForList().range(conversationId, 0, lastN);
        // 如果结果为空，返回空列表
        if (list == null || list.isEmpty()) {
            return List.of();
        }
        // 将JSON字符串反序列化为Message对象列表
        return list.stream()
                   .map(json -> {
                       try {
                           return objectMapper.readValue(json, ChatMsg.class);
                       } catch (JsonProcessingException e) {
                           throw new RuntimeException("反序列化消息失败", e);
                       }
                   })
                   //再将ChatMsg转换为Message对象
                   .map(ChatMsg::toMessage)
                   .sorted((a, b) -> -1)
                   .collect(Collectors.toList());
    }

    /**
     * 清除指定会话的聊天记忆
     *
     * @param conversationId: 要清除的会话ID
     */
    @Override
    public void clear(String conversationId) {
        redisTemplate.delete(conversationId);
    }

    /**
     * 移除指定消息ID的聊天记忆
     *
     * @param conversationId: 会话ID
     * @param messageId:      消息ID
     */
    public void deleteByMessageId(String conversationId, String messageId) {
        // 获取会话的所有消息
        List<String> messages = redisTemplate.opsForList().range(conversationId, 0, -1);
        if (messages != null && !messages.isEmpty()) {
            // 按照指定的消息id查询出消息
            messages.forEach(message -> {
                ChatMsg chatMsg;
                try {
                    chatMsg = objectMapper.readValue(message, ChatMsg.class);
                    if (chatMsg != null && chatMsg.getMetadata().get("id").equals(messageId)) {
                        redisTemplate.opsForList().remove(conversationId, 1, message);
                    }
                } catch (JsonProcessingException e) {
                    throw new RuntimeException("反序列化消息失败", e);
                }
            });
        }
    }
}
