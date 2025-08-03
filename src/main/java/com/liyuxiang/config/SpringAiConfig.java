package com.liyuxiang.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.liyuxiang.memory.SpringAiRedisChatMemory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * @Author: liyuxiang
 * @Date: 2025/8/3 14:02
 */
@Configuration
public class SpringAiConfig {
    @Autowired
    StringRedisTemplate redisTemplate;
    @Autowired
    ObjectMapper objectMapper;

    /**
     * 创建并返回一个ChatClient的Spring Bean实例。
     *
     * @param builder 用于构建ChatClient实例的构建者对象
     * @return 构建好的ChatClient实例
     */
    @Bean
    public ChatClient chatClient(ChatClient.Builder builder
    ) {
        return builder
                .defaultAdvisors(new SimpleLoggerAdvisor(), new MessageChatMemoryAdvisor(chatMemory())) // 设置默认的Advisor
                .build();
    }


    @Bean
    public ChatMemory chatMemory(){
        return new SpringAiRedisChatMemory(redisTemplate,objectMapper);
    }
}
