package com.dnd5.timoapi.domain.group.presentation.response;

import java.time.LocalDateTime;

public record GroupReflectionCommentResponse(
        Long id,
        Long commenterId,
        String commenterNickname,
        String content,
        LocalDateTime createdAt
) {
}
