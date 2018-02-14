package com.zx.bt;

import com.dampcake.bencode.Bencode;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class BtApplication{

	public static void main(String[] args) {
		SpringApplication.run(BtApplication.class, args);
	}

	/**
	 * Bencode编解码工具类
	 */
	@Bean
	public Bencode bencode() {
		return new Bencode();
	}


}
