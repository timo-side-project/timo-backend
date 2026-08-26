package com.dnd5.timoapi.global.infrastructure.nlp;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class KoreanNounExtractorTest {

    private final KoreanNounExtractor koreanNounExtractor = new KoreanNounExtractor();

    @Test
    void extractNouns_명사만_추출되고_조사_어미_동사_활용형은_제외된다() {
        List<String> nouns = koreanNounExtractor.extractNouns("오늘 회고를 작성하면서 많이 배웠다");

        assertThat(nouns).containsExactly("회고", "작성");
    }

    @Test
    void extractNouns_null_입력시_빈_리스트() {
        List<String> nouns = koreanNounExtractor.extractNouns(null);

        assertThat(nouns).isEmpty();
    }

    @Test
    void extractNouns_빈_문자열_입력시_빈_리스트() {
        List<String> nouns = koreanNounExtractor.extractNouns("   ");

        assertThat(nouns).isEmpty();
    }

    @Test
    void extractNouns_1글자_명사는_제외된다() {
        List<String> nouns = koreanNounExtractor.extractNouns("나는 오늘 꿈을 꾸었다");

        assertThat(nouns).isEmpty();
    }

    @Test
    void extractNouns_복합명사는_분해되어_집계된다() {
        List<String> nouns = koreanNounExtractor.extractNouns("이번 성장통을 겪으며 많이 배웠다");

        assertThat(nouns).containsExactly("성장");
        assertThat(nouns).doesNotContain("성장통");
    }
}
