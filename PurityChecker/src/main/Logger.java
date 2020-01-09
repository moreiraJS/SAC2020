package main;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import utils.FileUtils;

public class Logger {
	
	private FileWriter writer;
	private boolean log;
	
	public Logger(String projectName, boolean shouldLog) throws IOException {
		if(log) {
			File errorLogFile = new File(FileUtils.getOutputFolder(),projectName + " - Log.txt");
			
			writer = new FileWriter(errorLogFile);
			writer.write("Project Name: " + projectName + "\n");
			writer.flush();
		}
	}
	
	public boolean shouldLog() {
		return log;
	}

	public void logURL(String url) throws IOException {
		if(log) {
			writer.write("Repositry's URL: " + url + "\n");
			writer.flush();
		}
	}
	
	public void logError(String commitHash, Exception e) throws IOException {
		if(log) {
			writer.write("Error in commit: " + commitHash);
			writer.write("\r\nErro: "+e.toString());
			writer.flush();
		}
	}
	
	public void close() throws IOException {
		if(log)
			writer.close();
	}
}
