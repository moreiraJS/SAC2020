package main;

public class Main {
	
	public static void main(String[] args) throws Exception {
		
		/*
		 * Set the output and download folder or comment these lines.
		 * The default output folder is this directory.
		 * The default download folder is the TEMP directory*/
		FileUtils.setOutputFolder("outputFolder");
		FileUtils.setDownloadFolder("downloadFolder");
		
		/*
		 * Set the repository URL, the initial commit and whether it should create a log file. 
		 */
		String repositoryUrl = "Enter the repository URL here";
		String initialCommit = "HEAD";
		boolean shouldLog = false;

		Finder finder = new Finder(repositoryUrl);
		finder.findRefactorings(initialCommit, shouldLog);
	}
}
