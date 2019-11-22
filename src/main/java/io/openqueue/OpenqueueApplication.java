package io.openqueue;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * @author chenjing
 */
@SpringBootApplication
@EnableScheduling
public class OpenqueueApplication {

	public static void main(String[] args) {
		SpringApplication.run(OpenqueueApplication.class, args);
	}

}
