package com.example.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import javax.sql.DataSource;
import java.sql.Connection;

@SpringBootApplication
public class DemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }

    @Bean
    CommandLineRunner testConnection(DataSource dataSource) {
        return args -> {
            System.out.println("================================");
            System.out.println("Testing Database Connection...");
            System.out.println("================================");

            try (Connection connection = dataSource.getConnection()) {
                if (connection != null && !connection.isClosed()) {
                    System.out.println("Connection SUCCESSFUL!");
                    System.out.println("Database: " + connection.getCatalog());
                    System.out.println("URL: " + connection.getMetaData().getURL());
                    System.out.println("User: " + connection.getMetaData().getUserName());
                    System.out.println("================================");
                } else {
                    System.out.println("Connection FAILED!");
                }
            } catch (Exception e) {
                System.out.println("Connection ERROR!");
                System.out.println("Error: " + e.getMessage());
                e.printStackTrace();
            }
        };
    }
}