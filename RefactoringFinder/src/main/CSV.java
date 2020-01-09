package main;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.eclipse.jgit.revwalk.RevCommit;

import refdiff.core.rm2.model.refactoring.SDRefactoring;

public class CSV {
	
	private FileWriter writer;
	
	public CSV(String projectName) throws IOException {
		File csvFile = new File(FileUtils.getOutputFolder(), projectName + ".csv");
		
		writer = new FileWriter(csvFile);
		writer.write("Number;Commit;Parent;Refatoring;EntityBefore;EntityAfter;FullDescription");
		writer.flush();
	}
	
	public void addCSV(int number,RevCommit commit, SDRefactoring refactoring) throws IOException{
		writer.write("\n"+number);
		writer.write(";"+commit.getName());
		writer.write(";"+commit.getParent(0).getName());
		writer.write(";"+refactoring.getName());
		writer.write(";"+refactoring.getEntityBefore().fullName().replace(" ", "").replace("#", "."));
		writer.write(";"+refactoring.getEntityAfter().fullName().replace(" ", "").replace("#", "."));
		writer.write(";"+refactoring);
		writer.flush();
	}
	
	public void close() throws IOException {
		writer.close();
	}
}
