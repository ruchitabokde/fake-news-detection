package com.example.fakenews.preprocessing;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TextPreprocessorTest {
    private final TextPreprocessor preprocessor = new TextPreprocessor();

    @Test
    void tokenizesRemovesStopwordsAndLemmatizes() {
        assertEquals(java.util.List.of("government", "announce", "policy"),
                preprocessor.process("The governments are announcing policies."));
    }
}
