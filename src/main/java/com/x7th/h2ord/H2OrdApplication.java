package com.x7th.h2ord;

import java.io.*;
import java.nio.*;
import java.util.*;
import java.nio.file.*;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import org.h2.tools.Server;

@SpringBootApplication
@EnableScheduling
public class H2OrdApplication {

	private static Server server;

	public static void main(String[] args) throws SQLException, IOException {
		server = Server.createTcpServer("-ifNotExists").start();
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
