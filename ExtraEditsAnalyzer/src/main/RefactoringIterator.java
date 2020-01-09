package main;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

public class RefactoringIterator {
	
	private Scanner scanner;
	
	
	public RefactoringIterator(File isRefactoringFile) throws FileNotFoundException {
		scanner = new Scanner(isRefactoringFile).useDelimiter(";");
		scanner.nextLine();
	}
	
	public String getNextCommit() {
		scanner.nextLine();
		while(scanner.hasNext()) {
			String commit=scanner.next();
			if(scanner.nextLine().equals(";0"))
				return commit;
		}
		return null;
	}
	
	public void close() {
		scanner.close();
	}

}
