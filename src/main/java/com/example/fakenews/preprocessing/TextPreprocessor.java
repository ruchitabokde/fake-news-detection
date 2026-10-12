package com.example.fakenews.preprocessing;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Component
public class TextPreprocessor {
    private static final Pattern TOKEN_PATTERN = Pattern.compile("[\\p{L}]+");
    private static final Set<String> STOP_WORDS = Set.of(
            "a", "about", "above", "after", "again", "against", "all", "am", "an", "and",
            "any", "are", "as", "at", "be", "because", "been", "before", "being", "below",
            "between", "both", "but", "by", "can", "did", "do", "does", "doing", "down",
            "during", "each", "few", "for", "from", "further", "had", "has", "have", "having",
            "he", "her", "here", "hers", "herself", "him", "himself", "his", "how", "i", "if",
            "in", "into", "is", "it", "its", "itself", "just", "me", "more", "most", "my",
            "myself", "no", "nor", "not", "now", "of", "off", "on", "once", "only", "or",
            "other", "our", "ours", "ourselves", "out", "over", "own", "same", "she", "should",
            "so", "some", "such", "than", "that", "the", "their", "theirs", "them", "themselves",
            "then", "there", "these", "they", "this", "those", "through", "to", "too", "under",
            "until", "up", "very", "was", "we", "were", "what", "when", "where", "which",
            "while", "who", "whom", "why", "will", "with", "you", "your", "yours", "yourself");

    public List<String> process(String text) {
        var matcher = TOKEN_PATTERN.matcher(text.toLowerCase(Locale.ROOT));
        List<String> tokens = new ArrayList<>();
        while (matcher.find()) {
            String token = matcher.group();
            if (!STOP_WORDS.contains(token)) {
                tokens.add(lemmatize(token));
            }
        }
        return tokens;
    }

    private String lemmatize(String word) {
        if (word.length() > 4 && word.endsWith("ies")) {
            return word.substring(0, word.length() - 3) + "y";
        }
        if (word.length() > 5 && word.endsWith("sses")) {
            return word.substring(0, word.length() - 2);
        }
        if (word.length() > 5 && word.endsWith("ing")) {
            String stem = removeRepeatedEnding(word.substring(0, word.length() - 3));
            return stem.endsWith("c") ? stem + "e" : stem;
        }
        if (word.length() > 4 && word.endsWith("ed")) {
            return removeRepeatedEnding(word.substring(0, word.length() - 2));
        }
        if (word.length() > 3 && word.endsWith("s") && !word.endsWith("ss")) {
            return word.substring(0, word.length() - 1);
        }
        return word;
    }

    private String removeRepeatedEnding(String word) {
        int length = word.length();
        if (length > 2 && word.charAt(length - 1) == word.charAt(length - 2)) {
            return word.substring(0, length - 1);
        }
        return word;
    }
}
