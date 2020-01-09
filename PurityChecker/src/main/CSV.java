package main;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import utils.FileUtils;

public class CSV {
	
	private FileWriter writer;
	
	public CSV(String projectName) throws IOException {
		File csvFile = new File(FileUtils.getOutputFolder(), projectName + ".csv");
		
		writer = new FileWriter(csvFile);
		writer.write("Commit;isRefactoring");
		writer.flush();
	}
	
	public void addResult(String commit, boolean result) throws IOException{
		writer.write("\n"+commit);
		writer.write("\n"+ (result? 1:0));
		writer.flush();
	}
	
	public void addErrorResult(String commit) throws IOException{
		writer.write("\n"+commit);
		writer.write("\n"+ -1);
		writer.flush();
	}
	
	public void close() throws IOException {
		writer.close();
	}
}
