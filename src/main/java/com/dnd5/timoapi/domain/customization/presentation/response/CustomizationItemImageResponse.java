package com.dnd5.timoapi.domain.customization.presentation.response;

import com.dnd5.timoapi.domain.customization.domain.model.CustomizationItemImage;
import com.dnd5.timoapi.domain.test.domain.model.enums.ZtpiCategory;

public record CustomizationItemImageResponse(
        ZtpiCategory category,
        String image,
        String imageWithoutBackground
) {
    public static CustomizationItemImageResponse from(CustomizationItemImage model) {
        return new CustomizationItemImageResponse(model.category(), model.image(), model.imageWithoutBackground());
    }
}
