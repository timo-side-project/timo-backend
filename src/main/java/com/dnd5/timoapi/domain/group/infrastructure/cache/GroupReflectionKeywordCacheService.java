package com.dnd5.timoapi.domain.group.infrastructure.cache;

import com.dnd5.timoapi.domain.group.presentation.response.KeywordCount;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GroupReflectionKeywordCacheService {

    private static final String KEY_PREFIX = "group:reflection:keyword:";

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    public List<KeywordCount> get(Long groupId) {
        String value = redisTemplate.opsForValue().get(KEY_PREFIX + groupId);
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.readValue(value, new TypeReference<List<KeywordCount>>() {
            });
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    public void put(Long groupId, List<KeywordCount> keywords) {
        try {
            String value = objectMapper.writeValueAsString(keywords);
            redisTemplate.opsForValue().set(KEY_PREFIX + groupId, value, untilMidnight());
        } catch (JsonProcessingException ignored) {
        }
    }

    public void evict(Long groupId) {
        redisTemplate.delete(KEY_PREFIX + groupId);
    }

    private Duration untilMidnight() {
        return Duration.between(
                LocalDateTime.now(),
                LocalDate.now().plusDays(1).atTime(LocalTime.MIDNIGHT)
        );
    }
}
