package main;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

public class RefactoringIterator {
	
	private Scanner scanner;
	private String previousCommit;
	
	
	public RefactoringIterator(File refactoringFile) throws FileNotFoundException {
		scanner = new Scanner(refactoringFile).useDelimiter(";");
		previousCommit = "";
		scanner.nextLine();
	}
	
	public String[] getNextCommitPair() {
		while(scanner.hasNext()) {
			String[] result = new String[2];
			scanner.next();
			result[0] = scanner.next(); 
			if(!previousCommit.equals(result[0])) {
				previousCommit = result[0];
				result[1]=scanner.next();
				scanner.nextLine();
				return result;
			}
			scanner.nextLine();
		}
		return null;
	}
	
	public void close() {
		scanner.close();
	}

}
