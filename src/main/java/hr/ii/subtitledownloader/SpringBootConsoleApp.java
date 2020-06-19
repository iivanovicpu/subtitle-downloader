package hr.ii.subtitledownloader;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;

public class SpringBootConsoleApp implements CommandLineRunner {

    public static void main(String[] args) {
        SpringApplication.run(SpringBootConsoleApp.class, args);
    }

    @Override
    public void run(String... args) {
        Main.main(args);
    }
}
