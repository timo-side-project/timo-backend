package com.dnd5.timoapi.domain.group.presentation;

import com.dnd5.timoapi.domain.group.application.service.GroupReflectionPrivacyService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/groups/{groupId}/reflections/{reflectionId}/private")
@RequiredArgsConstructor
@Validated
public class GroupReflectionPrivacyController {

    private final GroupReflectionPrivacyService groupReflectionPrivacyService;

    @Operation(summary = "내 회고 비공개 설정")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void setPrivate(@Positive @PathVariable Long groupId, @Positive @PathVariable Long reflectionId) {
        groupReflectionPrivacyService.setPrivate(groupId, reflectionId);
    }

    @Operation(summary = "내 회고 공개 전환")
    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void setPublic(@Positive @PathVariable Long groupId, @Positive @PathVariable Long reflectionId) {
        groupReflectionPrivacyService.setPublic(groupId, reflectionId);
    }
}
