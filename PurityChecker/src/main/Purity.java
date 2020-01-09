package main;

import java.io.File;
import java.io.IOException;

import utils.GithubDownloader;
import utils.MavenHandler;
import utils.ZipExtractor;

public class Purity {
	
	private String repositoryUrl;
	private double timeLimit;
	
	public Purity(String urlRepository, double timeLimit){
		this.repositoryUrl=urlRepository;
		this.timeLimit = timeLimit;
	}
	
	public void setUrlRepository(String urlRepository){
		this.repositoryUrl=urlRepository;
	}
	
	public String getUrlRepository(){
		return this.repositoryUrl;	
	}
	
	public double getTimeLimit() {
		return timeLimit;
	}

	public void setTimeLimit(double timeLimit) {
		this.timeLimit = timeLimit;
	}

	public void analyse(File refactoringFile, boolean shouldLog) throws IOException  {
		
		String projectName= getProjectNameFromUrl(repositoryUrl);
		
		CSV csv = new CSV(projectName);
		Logger logger = new Logger(projectName, shouldLog);
		RefactoringIterator ri = new RefactoringIterator(refactoringFile);
		
		String[] commitPair = ri.getNextCommitPair();
		while(commitPair != null) {
			String commit = commitPair[0];
			String parent = commitPair[1];
			try {
				boolean sameBehaviour=check(commit, parent, timeLimit);
				csv.addResult(commit, sameBehaviour);
				System.out.println("Same Behaviour [Commit]: "+sameBehaviour);
			} catch (Exception e) {
				csv.addErrorResult(commit);
				logger.logError(commit, e);
				System.out.println("Same Behaviour: Error");
				e.printStackTrace();
			}
			commitPair = ri.getNextCommitPair();
		}
		ri.close();	
		csv.close();
	}

	public boolean check(String commit, String parent, double timeLimit) throws Exception {
		
		GithubDownloader git=new GithubDownloader(repositoryUrl);
		git.setLocation(new File(git.getLocation(), commit));
		
		File sourceFolder=new File(git.getLocation(), parent);
		File targetFolder=new File(git.getLocation(), commit);
		
		try {
			if(!sourceFolder.exists()) {
				File sourceFile=git.downloadCommit(parent);
				sourceFolder=ZipExtractor.extract(sourceFile, sourceFolder);
				
				File targetFile=git.downloadCommit(commit);
				targetFolder=ZipExtractor.extract(targetFile, targetFolder);
				
				System.out.println(sourceFolder.getAbsolutePath());
				
				MavenHandler mavenHandler = new MavenHandler();
				mavenHandler.addGoal("compile");
				mavenHandler.buildProject(sourceFolder);
				mavenHandler.buildProject(targetFolder);
			}
		
		
			Test test=new Test(sourceFolder,targetFolder, timeLimit);
			boolean hasSameBehaviour=test.hasSameBehaviour();
			deleteDirectory(git.getLocation());
			return hasSameBehaviour;
		} catch (Exception e) {
			deleteDirectory(git.getLocation());
			throw e;	
		}
	}
	
	
	private void deleteDirectory(File dir){
		File[] contents=dir.listFiles();
		for(File f: contents){
			if(f.isDirectory())
				deleteDirectory(f);
			else
				f.delete();
		}
		dir.delete();
	}
	
	private String getProjectNameFromUrl(String url) {
		int index = url.lastIndexOf("/");
		return url.substring(index+1);	
	}
	
}
