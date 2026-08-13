package com.dnd5.timoapi.domain.group.application.service;

import com.dnd5.timoapi.domain.group.domain.entity.GroupMemberEntity;
import com.dnd5.timoapi.domain.group.domain.entity.GroupMemberReflectionPrivateEntity;
import com.dnd5.timoapi.domain.group.domain.repository.GroupMemberReflectionPrivateRepository;
import com.dnd5.timoapi.domain.group.domain.repository.GroupMemberRepository;
import com.dnd5.timoapi.domain.group.exception.GroupErrorCode;
import com.dnd5.timoapi.domain.reflection.domain.entity.ReflectionEntity;
import com.dnd5.timoapi.domain.reflection.domain.repository.ReflectionRepository;
import com.dnd5.timoapi.domain.reflection.exception.ReflectionErrorCode;
import com.dnd5.timoapi.global.exception.BusinessException;
import com.dnd5.timoapi.global.security.context.SecurityUtil;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GroupReflectionPrivacyServiceTest {

    @InjectMocks
    private GroupReflectionPrivacyService groupReflectionPrivacyService;

    @Mock
    private GroupMemberRepository groupMemberRepository;

    @Mock
    private ReflectionRepository reflectionRepository;

    @Mock
    private GroupMemberReflectionPrivateRepository groupMemberReflectionPrivateRepository;

    @Test
    void setPrivate_성공() {
        Long userId = 1L;
        Long groupId = 10L;
        Long reflectionId = 100L;

        GroupMemberEntity member = mock(GroupMemberEntity.class);
        when(member.getCreatedAt()).thenReturn(LocalDateTime.of(2026, 7, 1, 0, 0));
        when(groupMemberRepository.findByGroupIdAndUserIdAndDeletedAtIsNull(groupId, userId))
                .thenReturn(Optional.of(member));

        ReflectionEntity reflection = mock(ReflectionEntity.class);
        when(reflection.getUserId()).thenReturn(userId);
        when(reflection.getDate()).thenReturn(LocalDate.of(2026, 8, 1));
        when(reflectionRepository.findById(reflectionId)).thenReturn(Optional.of(reflection));

        when(groupMemberReflectionPrivateRepository.existsByGroupIdAndReflectionId(groupId, reflectionId)).thenReturn(false);

        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(userId);

            groupReflectionPrivacyService.setPrivate(groupId, reflectionId);

            verify(groupMemberReflectionPrivateRepository).save(any(GroupMemberReflectionPrivateEntity.class));
        }
    }

    @Test
    void setPrivate_그룹_멤버가_아니면_403() {
        Long userId = 1L;
        Long groupId = 10L;
        Long reflectionId = 100L;

        when(groupMemberRepository.findByGroupIdAndUserIdAndDeletedAtIsNull(groupId, userId))
                .thenReturn(Optional.empty());

        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(userId);

            assertThatThrownBy(() -> groupReflectionPrivacyService.setPrivate(groupId, reflectionId))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(GroupErrorCode.GROUP_ACCESS_DENIED));
        }
    }

    @Test
    void setPrivate_본인_소유가_아니면_403() {
        Long userId = 1L;
        Long otherUserId = 2L;
        Long groupId = 10L;
        Long reflectionId = 100L;

        GroupMemberEntity member = mock(GroupMemberEntity.class);
        when(groupMemberRepository.findByGroupIdAndUserIdAndDeletedAtIsNull(groupId, userId))
                .thenReturn(Optional.of(member));

        ReflectionEntity reflection = mock(ReflectionEntity.class);
        when(reflection.getId()).thenReturn(reflectionId);
        when(reflection.getUserId()).thenReturn(otherUserId);
        when(reflectionRepository.findById(reflectionId)).thenReturn(Optional.of(reflection));

        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(userId);

            assertThatThrownBy(() -> groupReflectionPrivacyService.setPrivate(groupId, reflectionId))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ReflectionErrorCode.REFLECTION_NOT_OWNER));
        }
    }

    @Test
    void setPrivate_가입일_이전_회고면_404() {
        Long userId = 1L;
        Long groupId = 10L;
        Long reflectionId = 100L;

        GroupMemberEntity member = mock(GroupMemberEntity.class);
        when(member.getCreatedAt()).thenReturn(LocalDateTime.of(2026, 7, 1, 0, 0));
        when(groupMemberRepository.findByGroupIdAndUserIdAndDeletedAtIsNull(groupId, userId))
                .thenReturn(Optional.of(member));

        ReflectionEntity reflection = mock(ReflectionEntity.class);
        when(reflection.getUserId()).thenReturn(userId);
        when(reflection.getDate()).thenReturn(LocalDate.of(2026, 6, 1));
        when(reflectionRepository.findById(reflectionId)).thenReturn(Optional.of(reflection));

        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(userId);

            assertThatThrownBy(() -> groupReflectionPrivacyService.setPrivate(groupId, reflectionId))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ReflectionErrorCode.REFLECTION_NOT_FOUND));
        }
    }

    @Test
    void setPrivate_이미_비공개면_409() {
        Long userId = 1L;
        Long groupId = 10L;
        Long reflectionId = 100L;

        GroupMemberEntity member = mock(GroupMemberEntity.class);
        when(member.getCreatedAt()).thenReturn(LocalDateTime.of(2026, 7, 1, 0, 0));
        when(groupMemberRepository.findByGroupIdAndUserIdAndDeletedAtIsNull(groupId, userId))
                .thenReturn(Optional.of(member));

        ReflectionEntity reflection = mock(ReflectionEntity.class);
        when(reflection.getUserId()).thenReturn(userId);
        when(reflection.getDate()).thenReturn(LocalDate.of(2026, 8, 1));
        when(reflectionRepository.findById(reflectionId)).thenReturn(Optional.of(reflection));

        when(groupMemberReflectionPrivateRepository.existsByGroupIdAndReflectionId(groupId, reflectionId)).thenReturn(true);

        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(userId);

            assertThatThrownBy(() -> groupReflectionPrivacyService.setPrivate(groupId, reflectionId))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(GroupErrorCode.GROUP_REFLECTION_PRIVATE_ALREADY_EXISTS));
        }
    }

    @Test
    void setPublic_성공() {
        Long userId = 1L;
        Long groupId = 10L;
        Long reflectionId = 100L;

        GroupMemberEntity member = mock(GroupMemberEntity.class);
        when(member.getCreatedAt()).thenReturn(LocalDateTime.of(2026, 7, 1, 0, 0));
        when(groupMemberRepository.findByGroupIdAndUserIdAndDeletedAtIsNull(groupId, userId))
                .thenReturn(Optional.of(member));

        ReflectionEntity reflection = mock(ReflectionEntity.class);
        when(reflection.getUserId()).thenReturn(userId);
        when(reflection.getDate()).thenReturn(LocalDate.of(2026, 8, 1));
        when(reflectionRepository.findById(reflectionId)).thenReturn(Optional.of(reflection));

        GroupMemberReflectionPrivateEntity privateEntity = mock(GroupMemberReflectionPrivateEntity.class);
        when(groupMemberReflectionPrivateRepository.findByGroupIdAndReflectionId(groupId, reflectionId))
                .thenReturn(Optional.of(privateEntity));

        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(userId);

            groupReflectionPrivacyService.setPublic(groupId, reflectionId);

            verify(groupMemberReflectionPrivateRepository).delete(privateEntity);
        }
    }

    @Test
    void setPublic_비공개가_아니면_404() {
        Long userId = 1L;
        Long groupId = 10L;
        Long reflectionId = 100L;

        GroupMemberEntity member = mock(GroupMemberEntity.class);
        when(member.getCreatedAt()).thenReturn(LocalDateTime.of(2026, 7, 1, 0, 0));
        when(groupMemberRepository.findByGroupIdAndUserIdAndDeletedAtIsNull(groupId, userId))
                .thenReturn(Optional.of(member));

        ReflectionEntity reflection = mock(ReflectionEntity.class);
        when(reflection.getUserId()).thenReturn(userId);
        when(reflection.getDate()).thenReturn(LocalDate.of(2026, 8, 1));
        when(reflectionRepository.findById(reflectionId)).thenReturn(Optional.of(reflection));

        when(groupMemberReflectionPrivateRepository.findByGroupIdAndReflectionId(groupId, reflectionId))
                .thenReturn(Optional.empty());

        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(userId);

            assertThatThrownBy(() -> groupReflectionPrivacyService.setPublic(groupId, reflectionId))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(GroupErrorCode.GROUP_REFLECTION_PRIVATE_NOT_FOUND));
        }
    }

    @Test
    void setPublic_그룹_멤버가_아니면_403() {
        Long userId = 1L;
        Long groupId = 10L;
        Long reflectionId = 100L;

        when(groupMemberRepository.findByGroupIdAndUserIdAndDeletedAtIsNull(groupId, userId))
                .thenReturn(Optional.empty());

        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentUserId).thenReturn(userId);

            assertThatThrownBy(() -> groupReflectionPrivacyService.setPublic(groupId, reflectionId))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(GroupErrorCode.GROUP_ACCESS_DENIED));
        }
    }
}
