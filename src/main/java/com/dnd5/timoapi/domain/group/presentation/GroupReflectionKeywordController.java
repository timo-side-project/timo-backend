package com.dnd5.timoapi.domain.group.presentation;

import com.dnd5.timoapi.domain.group.application.service.GroupReflectionKeywordService;
import com.dnd5.timoapi.domain.group.presentation.response.GroupReflectionKeywordResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/groups/{groupId}/reflections/keywords")
@RequiredArgsConstructor
@Validated
public class GroupReflectionKeywordController {

    private final GroupReflectionKeywordService groupReflectionKeywordService;

    @Operation(summary = "그룹 회고 키워드 TOP3 조회")
    @GetMapping
    public GroupReflectionKeywordResponse getKeywords(@Positive @PathVariable Long groupId) {
        return groupReflectionKeywordService.getKeywords(groupId);
    }
}
