package cn.edu.teamtoolbox;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class TeamToolboxApplication {

    public static void main(String[] args) {
        SpringApplication.run(TeamToolboxApplication.class, args);
    }
}
