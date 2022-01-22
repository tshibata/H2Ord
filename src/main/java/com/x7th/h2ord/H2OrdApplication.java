package com.x7th.h2ord;

import java.io.*;
import java.nio.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import org.h2.tools.Server;

@SpringBootApplication
@EnableScheduling
public class H2OrdApplication {

	@Value("${app.backup.file:'eHH'}")
	String backup;

	static final File dir = new File("db");

	private static Server server;

	public static void main(String[] args) throws SQLException, IOException {
		dir.mkdirs();
		server = Server.createTcpServer("-ifNotExists", "-baseDir", dir.getPath()).start();
		SpringApplication.run(H2OrdApplication.class, args);
	}

	@Scheduled(cron = "${app.backup.cron:-}")
	void dbCron() throws SQLException, IOException {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern(backup);
		server.stop();
		try {
			String name = LocalDateTime.now().format(formatter) + ".zip";
			ZipOutputStream o = new ZipOutputStream(new FileOutputStream(name));
			try {
				for (File file: dir.listFiles()) {
					System.out.println(file);
					if (file.isFile()) {
						InputStream i = new FileInputStream(file);
						try {
							o.putNextEntry(new ZipEntry(file.getPath()));
							int b;
							while ((b = i.read()) != -1) {
								o.write(b);
							}
							o.closeEntry();
						} finally {
							i.close();
						}
					}
				}
			} finally {
				o.close();
			}
		} finally {
			server = Server.createTcpServer("-ifNotExists", "-baseDir", dir.getPath()).start();
		}
	}
}
