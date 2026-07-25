package com.dnd5.timoapi.domain.customization.presentation.response;

import com.dnd5.timoapi.domain.customization.domain.model.CustomizationItem;
import com.dnd5.timoapi.domain.customization.domain.model.enums.CustomizationItemType;
import com.dnd5.timoapi.domain.customization.domain.model.enums.CustomizationUnlockConditionType;

import java.util.List;

public record AdminCustomizationItemDetailResponse(
        Long id,
        String name,
        CustomizationItemType type,
        String description,
        CustomizationUnlockConditionType unlockConditionType,
        Integer unlockConditionCount,
        boolean usesCharacterImage,
        List<CustomizationItemImageResponse> images
) {
    public static AdminCustomizationItemDetailResponse from(CustomizationItem model, List<CustomizationItemImageResponse> images) {
        return new AdminCustomizationItemDetailResponse(
                model.id(),
                model.name(),
                model.type(),
                model.description(),
                model.unlockConditionType(),
                model.unlockConditionCount(),
                model.usesCharacterImage(),
                images
        );
    }
}
