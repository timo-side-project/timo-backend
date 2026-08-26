package com.dnd5.timoapi.domain.group.application.service;

import com.dnd5.timoapi.domain.group.domain.entity.GroupMemberEntity;
import com.dnd5.timoapi.domain.group.domain.entity.GroupMemberReflectionPrivateEntity;
import com.dnd5.timoapi.domain.group.domain.repository.GroupMemberReflectionPrivateRepository;
import com.dnd5.timoapi.domain.group.domain.repository.GroupMemberRepository;
import com.dnd5.timoapi.domain.group.exception.GroupErrorCode;
import com.dnd5.timoapi.domain.group.infrastructure.cache.GroupReflectionKeywordCacheService;
import com.dnd5.timoapi.domain.group.presentation.response.GroupReflectionKeywordResponse;
import com.dnd5.timoapi.domain.group.presentation.response.KeywordCount;
import com.dnd5.timoapi.domain.reflection.domain.entity.ReflectionEntity;
import com.dnd5.timoapi.domain.reflection.domain.repository.ReflectionRepository;
import com.dnd5.timoapi.global.exception.BusinessException;
import com.dnd5.timoapi.global.infrastructure.nlp.KoreanNounExtractor;
import com.dnd5.timoapi.global.security.context.SecurityUtil;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GroupReflectionKeywordService {

    private static final int TOP_KEYWORD_COUNT = 3;

    private final GroupMemberRepository groupMemberRepository;
    private final ReflectionRepository reflectionRepository;
    private final GroupMemberReflectionPrivateRepository groupMemberReflectionPrivateRepository;
    private final GroupReflectionKeywordCacheService cacheService;
    private final KoreanNounExtractor koreanNounExtractor;

    public GroupReflectionKeywordResponse getKeywords(Long groupId) {
        Long userId = SecurityUtil.getCurrentUserId();
        if (!groupMemberRepository.existsByGroupIdAndUserIdAndDeletedAtIsNull(groupId, userId)) {
            throw new BusinessException(GroupErrorCode.GROUP_ACCESS_DENIED);
        }

        List<KeywordCount> cached = cacheService.get(groupId);
        if (cached != null) {
            return GroupReflectionKeywordResponse.from(cached);
        }

        List<KeywordCount> keywords = computeKeywords(groupId);
        cacheService.put(groupId, keywords);
        return GroupReflectionKeywordResponse.from(keywords);
    }

    private List<KeywordCount> computeKeywords(Long groupId) {
        List<Long> memberUserIds = groupMemberRepository.findAllByGroupIdAndDeletedAtIsNull(groupId).stream()
                .map(GroupMemberEntity::getUserId)
                .toList();
        if (memberUserIds.isEmpty()) {
            return List.of();
        }

        YearMonth thisMonth = YearMonth.now();
        LocalDate start = thisMonth.atDay(1);
        LocalDate end = thisMonth.atEndOfMonth();

        List<ReflectionEntity> reflections = reflectionRepository
                .findAllByDateBetweenAndUserIdIn(start, end, memberUserIds);
        if (reflections.isEmpty()) {
            return List.of();
        }

        Set<Long> privateReflectionIds = groupMemberReflectionPrivateRepository
                .findAllByGroupIdAndReflectionIdIn(
                        groupId,
                        reflections.stream().map(ReflectionEntity::getId).toList())
                .stream()
                .map(GroupMemberReflectionPrivateEntity::getReflectionId)
                .collect(Collectors.toSet());

        List<String> nouns = reflections.stream()
                .filter(reflection -> !privateReflectionIds.contains(reflection.getId()))
                .map(ReflectionEntity::getAnswerText)
                .flatMap(answerText -> koreanNounExtractor.extractNouns(answerText).stream())
                .toList();

        Map<String, Long> wordCounts = nouns.stream()
                .collect(Collectors.groupingBy(word -> word, Collectors.counting()));

        return wordCounts.entrySet().stream()
                .sorted(Comparator.<Map.Entry<String, Long>>comparingLong(Map.Entry::getValue).reversed()
                        .thenComparing(Map.Entry::getKey))
                .limit(TOP_KEYWORD_COUNT)
                .map(entry -> new KeywordCount(entry.getKey(), entry.getValue()))
                .toList();
    }
}
