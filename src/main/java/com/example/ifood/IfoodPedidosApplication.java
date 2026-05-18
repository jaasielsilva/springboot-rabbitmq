package com.example.ifood;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class IfoodPedidosApplication {

	public static void main(String[] args) {
		SpringApplication.run(IfoodPedidosApplication.class, args);
	}

}
