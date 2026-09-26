package com.banking.discoveryservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

@SpringBootApplication
@EnableEurekaServer
public class DiscoveryServiCeApplication {

    public static void main(String[] args) {
        SpringApplication.run(DiscoveryServiCeApplication.class, args);
    }
}