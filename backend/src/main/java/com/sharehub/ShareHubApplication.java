package com.sharehub;

import com.sharehub.files.FileStorageProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(FileStorageProperties.class)
public class ShareHubApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShareHubApplication.class, args);
    }
}
