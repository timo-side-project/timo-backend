package com.dnd5.timoapi.domain.group.domain.repository;

import com.dnd5.timoapi.domain.group.domain.entity.GroupMemberReflectionLikeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GroupMemberReflectionLikeRepository extends JpaRepository<GroupMemberReflectionLikeEntity, Long> {

    long countByGroupIdAndReflectionId(Long groupId, Long reflectionId);
}
