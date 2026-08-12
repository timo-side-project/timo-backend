package com.dnd5.timoapi.domain.group.domain.repository;

import com.dnd5.timoapi.domain.group.domain.entity.GroupMemberReflectionCommentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GroupMemberReflectionCommentRepository extends JpaRepository<GroupMemberReflectionCommentEntity, Long> {

    long countByGroupIdAndReflectionIdAndDeletedAtIsNull(Long groupId, Long reflectionId);
}
