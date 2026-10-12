package com.example.fakenews.data;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
public class CsvDatasetLoader {
    public List<NewsArticle> load(Path csvPath) throws IOException {
        if (!Files.isRegularFile(csvPath)) {
            throw new IOException("Dataset CSV was not found: " + csvPath.toAbsolutePath());
        }

        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreEmptyLines(true)
                .get();
        List<NewsArticle> articles = new ArrayList<>();
        try (var reader = Files.newBufferedReader(csvPath, StandardCharsets.UTF_8);
             CSVParser parser = format.parse(reader)) {
            var headers = parser.getHeaderMap();
            for (String required : List.of("title", "text", "label")) {
                if (!headers.containsKey(required)) {
                    throw new IOException("CSV must contain a '" + required + "' column.");
                }
            }
            for (CSVRecord row : parser) {
                String title = row.get("title").trim();
                String text = row.get("text").trim();
                String label = row.get("label").trim().toLowerCase(Locale.ROOT);
                if (!title.isEmpty() || !text.isEmpty()) {
                    articles.add(new NewsArticle(title, text, parseFakeLabel(label, row.getRecordNumber())));
                }
            }
        }
        if (articles.isEmpty()) {
            throw new IOException("The dataset contains no usable news rows.");
        }
        return articles;
    }

    private boolean parseFakeLabel(String label, long rowNumber) throws IOException {
        return switch (label) {
            case "fake", "false", "1", "1.0", "yes" -> true;
            case "real", "true", "0", "0.0", "no" -> false;
            default -> throw new IOException(
                    "Unsupported label '" + label + "' at CSV record " + rowNumber
                            + "; expected fake/real or 1/0.");
        };
    }
}
