package com.modu.office;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@org.springframework.boot.context.properties.ConfigurationPropertiesScan
@org.springframework.cache.annotation.EnableCaching
public class ModuOfficeApplication {

    public static void main(String[] args) {
        SpringApplication.run(ModuOfficeApplication.class, args);
    }

}
