package com.example.fakenews.data;

public record NewsArticle(String title, String text, boolean fake) {
    public String content() {
        return (title + " " + text).trim();
    }
}
