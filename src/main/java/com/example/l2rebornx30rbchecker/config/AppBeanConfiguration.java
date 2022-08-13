package com.example.l2rebornx30rbchecker.config;

import org.modelmapper.ModelMapper;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxBinary;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class AppBeanConfiguration {

    @Bean
    public WebDriver driver() {
        System.setProperty("webdriver.gecko.driver", "/app/vendor/geckodriver/geckodriver");
        FirefoxBinary firefoxBinary = new FirefoxBinary();
        FirefoxOptions options = new FirefoxOptions();
        options.setBinary(firefoxBinary);
        options.setHeadless(true);
        return new FirefoxDriver(options);
    }

    @Bean
    public ModelMapper modelMapper() {
        return new ModelMapper();
    }
}
