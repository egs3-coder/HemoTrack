package br.com.hemotrack;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class HemoTrackApplication {

    public static void main(String[] args) {
        SpringApplication.run(HemoTrackApplication.class, args);
    }
}
