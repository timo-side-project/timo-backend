package com.dnd5.timoapi.global.infrastructure.nlp;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.ko.KoreanAnalyzer;
import org.apache.lucene.analysis.ko.KoreanTokenizer;
import org.apache.lucene.analysis.ko.POS;
import org.apache.lucene.analysis.ko.tokenattributes.PartOfSpeechAttribute;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class KoreanNounExtractor implements DisposableBean {

    private static final int MIN_WORD_LENGTH = 2;

    private final KoreanAnalyzer koreanAnalyzer = new KoreanAnalyzer(
            null,
            KoreanTokenizer.DecompoundMode.DISCARD,
            Collections.emptySet(),
            false
    );

    public List<String> extractNouns(String text) {
        if (!StringUtils.hasText(text)) {
            return List.of();
        }

        List<String> nouns = new ArrayList<>();
        try (TokenStream tokenStream = koreanAnalyzer.tokenStream("text", new StringReader(text))) {
            CharTermAttribute termAttribute = tokenStream.addAttribute(CharTermAttribute.class);
            PartOfSpeechAttribute posAttribute = tokenStream.addAttribute(PartOfSpeechAttribute.class);
            tokenStream.reset();
            while (tokenStream.incrementToken()) {
                POS.Tag tag = posAttribute.getLeftPOS();
                String term = termAttribute.toString();
                if (isNoun(tag) && term.length() >= MIN_WORD_LENGTH) {
                    nouns.add(term);
                }
            }
            tokenStream.end();
        } catch (IOException e) {
            return List.of();
        }
        return nouns;
    }

    private boolean isNoun(POS.Tag tag) {
        return tag == POS.Tag.NNG || tag == POS.Tag.NNP;
    }

    @Override
    public void destroy() {
        koreanAnalyzer.close();
    }
}
