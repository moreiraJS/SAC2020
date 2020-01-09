package main;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import utils.FileUtils;

public class Main {
	
	public static void main(String[] args) throws Exception {
		
		/*
		 * Set the output and download folder or comment these lines.
		 * The default output folder is this directory.
		 * The default download folder is the TEMP directory*/
		FileUtils.setOutputFolder("outputFolder");
		FileUtils.setDownloadFolder("downloadFolder");
		
		/*
		 * Set the repository URL, the refactoring file path, the time limit and whether it should create a log file. 
		 */
		String repositoryUrl = "Enter the repository URL here";
		String refactoringFile = "Enter the path of the refactoring file";
		double timeLimit = 120;
		boolean shouldLog = false;
		
		Purity purity = new Purity(repositoryUrl, timeLimit);
		purity.analyse(new File(refactoringFile), shouldLog);
	}
}
