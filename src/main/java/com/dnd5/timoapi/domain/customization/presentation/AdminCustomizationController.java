package com.dnd5.timoapi.domain.customization.presentation;

import com.dnd5.timoapi.domain.customization.application.service.CustomizationItemService;
import com.dnd5.timoapi.domain.customization.presentation.request.CustomizationItemCreateRequest;
import com.dnd5.timoapi.domain.customization.presentation.request.CustomizationItemUpdateRequest;
import com.dnd5.timoapi.domain.customization.presentation.response.AdminCustomizationItemDetailResponse;
import com.dnd5.timoapi.domain.customization.presentation.response.AdminCustomizationItemResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/customizations")
@RequiredArgsConstructor
@Validated
public class AdminCustomizationController {

    private final CustomizationItemService customizationItemService;

    @Operation(summary = "커스터마이징 아이템 목록 조회 (어드민)")
    @GetMapping
    public List<AdminCustomizationItemResponse> getCustomizations() {
        return customizationItemService.findAllForAdmin();
    }

    @Operation(summary = "커스터마이징 아이템 상세 조회 (어드민)")
    @GetMapping("/{customizationItemId}")
    public AdminCustomizationItemDetailResponse getCustomization(@Positive @PathVariable Long customizationItemId) {
        return customizationItemService.findByIdForAdmin(customizationItemId);
    }

    @Operation(summary = "커스터마이징 아이템 생성 (어드민)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void create(@Valid @RequestBody CustomizationItemCreateRequest request) {
        customizationItemService.create(request);
    }

    @Operation(summary = "커스터마이징 아이템 수정 (어드민)")
    @PatchMapping("/{customizationItemId}")
    @ResponseStatus(HttpStatus.OK)
    public void update(
            @Positive @PathVariable Long customizationItemId,
            @Valid @RequestBody CustomizationItemUpdateRequest request
    ) {
        customizationItemService.update(customizationItemId, request);
    }

    @Operation(summary = "커스터마이징 아이템 삭제 (어드민)")
    @DeleteMapping("/{customizationItemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@Positive @PathVariable Long customizationItemId) {
        customizationItemService.delete(customizationItemId);
    }
}
