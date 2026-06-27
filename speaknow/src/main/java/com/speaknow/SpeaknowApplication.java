package com.speaknow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SpeaknowApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpeaknowApplication.class, args);
        System.out.println("🚀 SpeakNow AI is running on http://localhost:8080");
    }

}