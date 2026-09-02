package com.dnd5.timoapi.domain.group.presentation.response;

import java.util.List;

public record GroupReflectionKeywordResponse(List<KeywordCount> keywords) {

    public static GroupReflectionKeywordResponse from(List<KeywordCount> keywords) {
        return new GroupReflectionKeywordResponse(keywords);
    }
}
