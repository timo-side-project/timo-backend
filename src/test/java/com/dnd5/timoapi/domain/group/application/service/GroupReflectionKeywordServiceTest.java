package com.dnd5.timoapi.domain.group.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GroupReflectionKeywordServiceTest {

    @InjectMocks
    private GroupReflectionKeywordService groupReflectionKeywordService;

    @Mock
    private GroupMemberRepository groupMemberRepository;

    @Mock
    private ReflectionRepository reflectionRepository;

    @Mock
    private GroupMemberReflectionPrivateRepository groupMemberReflectionPrivateRepository;

    @Mock
    private GroupReflectionKeywordCacheService cacheService;

    @Mock
    private KoreanNounExtractor koreanNounExtractor;

    private static final Long VIEWER_ID = 1L;
    private static final Long GROUP_ID = 10L;

    @Test
    void getKeywords_비멤버_403() {
        when(groupMemberRepository.existsByGroupIdAndUserIdAndDeletedAtIsNull(GROUP_ID, VIEWER_ID)).thenReturn(false);

        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(VIEWER_ID);

            assertThatThrownBy(() -> groupReflectionKeywordService.getKeywords(GROUP_ID))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(GroupErrorCode.GROUP_ACCESS_DENIED));
        }
    }

    @Test
    void getKeywords_캐시_hit이면_계산없이_반환() {
        when(groupMemberRepository.existsByGroupIdAndUserIdAndDeletedAtIsNull(GROUP_ID, VIEWER_ID)).thenReturn(true);

        List<KeywordCount> cached = List.of(new KeywordCount("가나다", 5));
        when(cacheService.get(GROUP_ID)).thenReturn(cached);

        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(VIEWER_ID);

            GroupReflectionKeywordResponse response = groupReflectionKeywordService.getKeywords(GROUP_ID);

            assertThat(response.keywords()).isEqualTo(cached);
        }

        verify(groupMemberRepository, never()).findAllByGroupIdAndDeletedAtIsNull(any());
        verify(reflectionRepository, never()).findAllByDateBetweenAndUserIdIn(any(), any(), any());
        verify(koreanNounExtractor, never()).extractNouns(any());
        verify(cacheService, never()).put(any(), any());
    }

    @Test
    void getKeywords_그룹_멤버_없으면_빈_리스트() {
        when(groupMemberRepository.existsByGroupIdAndUserIdAndDeletedAtIsNull(GROUP_ID, VIEWER_ID)).thenReturn(true);
        when(cacheService.get(GROUP_ID)).thenReturn(null);
        when(groupMemberRepository.findAllByGroupIdAndDeletedAtIsNull(GROUP_ID)).thenReturn(List.of());

        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(VIEWER_ID);

            GroupReflectionKeywordResponse response = groupReflectionKeywordService.getKeywords(GROUP_ID);

            assertThat(response.keywords()).isEmpty();
        }

        verify(reflectionRepository, never()).findAllByDateBetweenAndUserIdIn(any(), any(), any());
    }

    @Test
    void getKeywords_이번달_회고_없으면_빈_리스트() {
        Long memberUserId = 2L;
        when(groupMemberRepository.existsByGroupIdAndUserIdAndDeletedAtIsNull(GROUP_ID, VIEWER_ID)).thenReturn(true);
        when(cacheService.get(GROUP_ID)).thenReturn(null);

        GroupMemberEntity member = mock(GroupMemberEntity.class);
        when(member.getUserId()).thenReturn(memberUserId);
        when(groupMemberRepository.findAllByGroupIdAndDeletedAtIsNull(GROUP_ID)).thenReturn(List.of(member));

        when(reflectionRepository.findAllByDateBetweenAndUserIdIn(any(), any(), eq(List.of(memberUserId))))
                .thenReturn(List.of());

        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(VIEWER_ID);

            GroupReflectionKeywordResponse response = groupReflectionKeywordService.getKeywords(GROUP_ID);

            assertThat(response.keywords()).isEmpty();
        }

        verify(koreanNounExtractor, never()).extractNouns(any());
    }

    @Test
    void getKeywords_전부_비공개면_빈_리스트() {
        Long memberUserId = 2L;
        Long reflectionId = 100L;
        when(groupMemberRepository.existsByGroupIdAndUserIdAndDeletedAtIsNull(GROUP_ID, VIEWER_ID)).thenReturn(true);
        when(cacheService.get(GROUP_ID)).thenReturn(null);

        GroupMemberEntity member = mock(GroupMemberEntity.class);
        when(member.getUserId()).thenReturn(memberUserId);
        when(groupMemberRepository.findAllByGroupIdAndDeletedAtIsNull(GROUP_ID)).thenReturn(List.of(member));

        ReflectionEntity reflection = mock(ReflectionEntity.class);
        when(reflection.getId()).thenReturn(reflectionId);
        when(reflectionRepository.findAllByDateBetweenAndUserIdIn(any(), any(), eq(List.of(memberUserId))))
                .thenReturn(List.of(reflection));

        GroupMemberReflectionPrivateEntity privateEntity = mock(GroupMemberReflectionPrivateEntity.class);
        when(privateEntity.getReflectionId()).thenReturn(reflectionId);
        when(groupMemberReflectionPrivateRepository.findAllByGroupIdAndReflectionIdIn(GROUP_ID, List.of(reflectionId)))
                .thenReturn(List.of(privateEntity));

        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(VIEWER_ID);

            GroupReflectionKeywordResponse response = groupReflectionKeywordService.getKeywords(GROUP_ID);

            assertThat(response.keywords()).isEmpty();
        }

        verify(koreanNounExtractor, never()).extractNouns(any());
    }

    @Test
    void getKeywords_상위3개만_빈도_내림차순으로_반환하고_캐시에_저장한다() {
        Long memberUserId = 2L;
        when(groupMemberRepository.existsByGroupIdAndUserIdAndDeletedAtIsNull(GROUP_ID, VIEWER_ID)).thenReturn(true);
        when(cacheService.get(GROUP_ID)).thenReturn(null);

        GroupMemberEntity member = mock(GroupMemberEntity.class);
        when(member.getUserId()).thenReturn(memberUserId);
        when(groupMemberRepository.findAllByGroupIdAndDeletedAtIsNull(GROUP_ID)).thenReturn(List.of(member));

        ReflectionEntity r1 = mock(ReflectionEntity.class);
        when(r1.getId()).thenReturn(1L);
        when(r1.getAnswerText()).thenReturn("t1");

        ReflectionEntity r2 = mock(ReflectionEntity.class);
        when(r2.getId()).thenReturn(2L);
        when(r2.getAnswerText()).thenReturn("t2");

        ReflectionEntity r3 = mock(ReflectionEntity.class);
        when(r3.getId()).thenReturn(3L);
        when(r3.getAnswerText()).thenReturn("t3");

        ReflectionEntity r4 = mock(ReflectionEntity.class);
        when(r4.getId()).thenReturn(4L);
        when(r4.getAnswerText()).thenReturn("t4");

        List<ReflectionEntity> reflections = List.of(r1, r2, r3, r4);
        when(reflectionRepository.findAllByDateBetweenAndUserIdIn(any(), any(), eq(List.of(memberUserId))))
                .thenReturn(reflections);

        when(groupMemberReflectionPrivateRepository.findAllByGroupIdAndReflectionIdIn(eq(GROUP_ID), any()))
                .thenReturn(List.of());

        when(koreanNounExtractor.extractNouns("t1")).thenReturn(List.of("가", "가", "가", "가"));
        when(koreanNounExtractor.extractNouns("t2")).thenReturn(List.of("나", "나", "나"));
        when(koreanNounExtractor.extractNouns("t3")).thenReturn(List.of("다", "다"));
        when(koreanNounExtractor.extractNouns("t4")).thenReturn(List.of("라"));

        List<KeywordCount> expected = List.of(
                new KeywordCount("가", 4),
                new KeywordCount("나", 3),
                new KeywordCount("다", 2)
        );

        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(VIEWER_ID);

            GroupReflectionKeywordResponse response = groupReflectionKeywordService.getKeywords(GROUP_ID);

            assertThat(response.keywords()).containsExactlyElementsOf(expected);
        }

        verify(cacheService).put(eq(GROUP_ID), eq(expected));
    }

    @Test
    void getKeywords_동점_단어는_가나다순으로_정렬된다() {
        Long memberUserId = 2L;
        when(groupMemberRepository.existsByGroupIdAndUserIdAndDeletedAtIsNull(GROUP_ID, VIEWER_ID)).thenReturn(true);
        when(cacheService.get(GROUP_ID)).thenReturn(null);

        GroupMemberEntity member = mock(GroupMemberEntity.class);
        when(member.getUserId()).thenReturn(memberUserId);
        when(groupMemberRepository.findAllByGroupIdAndDeletedAtIsNull(GROUP_ID)).thenReturn(List.of(member));

        ReflectionEntity r1 = mock(ReflectionEntity.class);
        when(r1.getId()).thenReturn(1L);
        when(r1.getAnswerText()).thenReturn("t1");

        ReflectionEntity r2 = mock(ReflectionEntity.class);
        when(r2.getId()).thenReturn(2L);
        when(r2.getAnswerText()).thenReturn("t2");

        ReflectionEntity r3 = mock(ReflectionEntity.class);
        when(r3.getId()).thenReturn(3L);
        when(r3.getAnswerText()).thenReturn("t3");

        List<ReflectionEntity> reflections = List.of(r1, r2, r3);
        when(reflectionRepository.findAllByDateBetweenAndUserIdIn(any(), any(), eq(List.of(memberUserId))))
                .thenReturn(reflections);

        when(groupMemberReflectionPrivateRepository.findAllByGroupIdAndReflectionIdIn(eq(GROUP_ID), any()))
                .thenReturn(List.of());

        when(koreanNounExtractor.extractNouns("t1")).thenReturn(List.of("가", "가", "가"));
        when(koreanNounExtractor.extractNouns("t2")).thenReturn(List.of("다", "다"));
        when(koreanNounExtractor.extractNouns("t3")).thenReturn(List.of("나", "나"));

        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(VIEWER_ID);

            GroupReflectionKeywordResponse response = groupReflectionKeywordService.getKeywords(GROUP_ID);

            assertThat(response.keywords()).containsExactly(
                    new KeywordCount("가", 3),
                    new KeywordCount("나", 2),
                    new KeywordCount("다", 2)
            );
        }
    }
}
