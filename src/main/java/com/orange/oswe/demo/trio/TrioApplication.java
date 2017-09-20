package com.orange.oswe.demo.trio;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.web.session.HttpSessionEventPublisher;

import javax.servlet.http.HttpSessionListener;

@SpringBootApplication
public class TrioApplication {

	public static void main(String[] args) {
		SpringApplication.run(TrioApplication.class, args);
	}

	/**
	 * Needed to receive {@link org.springframework.context.ApplicationEvent}
	 */
	@Bean
	public HttpSessionListener sessionEventPublisher() {
		return new HttpSessionEventPublisher();
	}

}
