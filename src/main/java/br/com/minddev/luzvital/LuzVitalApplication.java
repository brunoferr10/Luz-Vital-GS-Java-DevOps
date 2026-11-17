package br.com.minddev.luzvital;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class LuzVitalApplication {
  public static void main(String[] args) {
    SpringApplication.run(LuzVitalApplication.class, args);
  }
}
