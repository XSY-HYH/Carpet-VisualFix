package com.xiaoqi;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DebugLogger {
	private static final boolean ENABLED = false;
	private static final Path LOG_PATH = Paths.get("logs/Carpet-VisualFix/debug.log");
	private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
	
	static {
		if (ENABLED) {
			try {
				Files.createDirectories(LOG_PATH.getParent());
				if (Files.exists(LOG_PATH)) {
					Files.delete(LOG_PATH);
				}
				Files.createFile(LOG_PATH);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public static void log(String message) {
		if (!ENABLED) {
			return;
		}
		try {
			String timestamp = LocalDateTime.now().format(TIME_FORMAT);
			String logLine = "[" + timestamp + "] " + message + "\n";
			Files.write(LOG_PATH, logLine.getBytes(), java.nio.file.StandardOpenOption.APPEND);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
