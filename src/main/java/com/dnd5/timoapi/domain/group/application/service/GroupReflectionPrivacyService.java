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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class GroupReflectionPrivacyService {

    private final GroupMemberRepository groupMemberRepository;
    private final ReflectionRepository reflectionRepository;
    private final GroupMemberReflectionPrivateRepository groupMemberReflectionPrivateRepository;

    public void setPrivate(Long groupId, Long reflectionId) {
        Long userId = SecurityUtil.getCurrentUserId();
        getOwnedGroupReflection(groupId, reflectionId, userId);

        if (groupMemberReflectionPrivateRepository.existsByGroupIdAndReflectionId(groupId, reflectionId)) {
            throw new BusinessException(GroupErrorCode.GROUP_REFLECTION_PRIVATE_ALREADY_EXISTS);
        }

        groupMemberReflectionPrivateRepository.save(
                new GroupMemberReflectionPrivateEntity(groupId, reflectionId));
    }

    public void setPublic(Long groupId, Long reflectionId) {
        Long userId = SecurityUtil.getCurrentUserId();
        getOwnedGroupReflection(groupId, reflectionId, userId);

        GroupMemberReflectionPrivateEntity privateEntity = groupMemberReflectionPrivateRepository
                .findByGroupIdAndReflectionId(groupId, reflectionId)
                .orElseThrow(() -> new BusinessException(GroupErrorCode.GROUP_REFLECTION_PRIVATE_NOT_FOUND));

        groupMemberReflectionPrivateRepository.delete(privateEntity);
    }

    private ReflectionEntity getOwnedGroupReflection(Long groupId, Long reflectionId, Long userId) {
        GroupMemberEntity member = groupMemberRepository
                .findByGroupIdAndUserIdAndDeletedAtIsNull(groupId, userId)
                .orElseThrow(() -> new BusinessException(GroupErrorCode.GROUP_ACCESS_DENIED));

        ReflectionEntity reflectionEntity = reflectionRepository.findById(reflectionId)
                .orElseThrow(() -> new BusinessException(ReflectionErrorCode.REFLECTION_NOT_FOUND));

        if (!reflectionEntity.getUserId().equals(userId)) {
            throw new BusinessException(
                    ReflectionErrorCode.REFLECTION_NOT_OWNER, reflectionEntity.getId(), reflectionEntity.getUserId(), userId);
        }

        if (reflectionEntity.getDate().isBefore(member.getCreatedAt().toLocalDate())) {
            throw new BusinessException(ReflectionErrorCode.REFLECTION_NOT_FOUND);
        }

        return reflectionEntity;
    }
}
