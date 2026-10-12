package com.example.fakenews;

import com.example.fakenews.training.ModelTrainer;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.CommandLineRunner;

import java.nio.file.Path;

@SpringBootApplication
public class FakeNewsApplication {
    public static void main(String[] args) {
        SpringApplication.run(FakeNewsApplication.class, args);
    }

    @Bean
    CommandLineRunner trainWhenRequested(
            ApplicationArguments arguments,
            ModelTrainer trainer,
            ConfigurableApplicationContext context) {
        return ignored -> {
            if (arguments.containsOption("train")) {
                String dataset = arguments.getOptionValues("data") == null
                        ? "data/fake_news.csv"
                        : arguments.getOptionValues("data").get(0);
                trainer.train(Path.of(dataset), Path.of("models/fake-news.model"));
                SpringApplication.exit(context, () -> 0);
            }
        };
    }
}
