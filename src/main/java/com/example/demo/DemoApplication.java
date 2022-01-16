package com.example.demo;

import java.io.*;
import java.nio.*;
import java.util.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import org.h2.tools.Server;

@SpringBootApplication
@EnableScheduling
public class DemoApplication {

	private static Server server;

	private static String standby;

	private static ConfigurableApplicationContext context;

	public static void main(String[] args) throws SQLException, IOException {
		if (args.length == 1) {
			try {
				standby = args[0];
				if (Files.exists(Paths.get(standby))) {
					Files.copy(Paths.get(standby), Paths.get("h2.mv.db"), StandardCopyOption.REPLACE_EXISTING);
				}
			} catch (IOException ex) {
				// should retry?
			}
		}
		server = Server.createTcpServer("-ifNotExists").start();
		context = SpringApplication.run(DemoApplication.class, args);
	}

	public static void shutdown() {
		context.close();
		server.stop();
		if (standby != null) {
			try {
				Files.copy(Paths.get("h2.mv.db"), Paths.get(standby), StandardCopyOption.REPLACE_EXISTING);
			} catch (IOException ex) {
				// should retry?
			}
		}
		System.exit(0);
	}

	@Value("${app.backup.file:'backup.mv.db'}")
	String backup;

	@Scheduled(cron = "${app.backup.cron:-}")
	void dbCron() throws SQLException, IOException {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern(backup);
		server.stop();
		String name = LocalDateTime.now().format(formatter);
		Files.copy(Paths.get("h2.mv.db"), Paths.get(name), StandardCopyOption.REPLACE_EXISTING);
		server = Server.createTcpServer().start();
	}
}
