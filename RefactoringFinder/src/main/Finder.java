package main;

import java.util.List;

import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;

import refdiff.core.RefDiff;
import refdiff.core.api.GitService;
import refdiff.core.rm2.model.refactoring.SDRefactoring;
import refdiff.core.util.GitServiceImpl;

public class Finder {
	
	private Repository repository;
	private String cloneURL;
	
	public Finder(String cloneURL) throws Exception {
		String downloadFolder = FileUtils.getDownloadFolder().getAbsolutePath();
		GitService gitService = new GitServiceImpl(); 
		
		this.cloneURL=cloneURL;
		this.repository = gitService.cloneIfNotExists(downloadFolder, cloneURL);
	}
	
	public int findRefactorings(String initialCommit, boolean shouldLog){
		RefDiff refDiff = new RefDiff();
		try {
			String projectName=getProjectNameFromUrl(cloneURL);
			CSV csv = new CSV(projectName);
			RevWalk revWalk = new RevWalk(repository);
			ObjectId id=repository.resolve(initialCommit);
			
			int numberOfCommits = getNumberOfCommits(initialCommit);
			
			Logger logger = new Logger(projectName, shouldLog);
			logger.logURL(cloneURL);
			logger.logTotalCommits(numberOfCommits);
			
			int errorCount = 0;
			for(int i=0;i<numberOfCommits;i++){
				RevCommit commit=revWalk.parseCommit(id);
				try {
					List<SDRefactoring> refactorings = refDiff.detectAtCommit(repository, commit.getName());
					for (SDRefactoring refactoring : refactorings) {
						csv.addCSV(i,commit,refactoring);
					}
				}catch(Exception e) {
					errorCount++;
					logger.logError(i, commit.getName(), e);
					e.printStackTrace();
				}
				System.out.println(i+"# "+commit);
				id=commit.getParent(0);
			}
			System.out.println("END!!!");
			
			revWalk.close();
			logger.logTotalErrors(errorCount);
			logger.close();
			csv.close();
			return errorCount;
		} catch (Exception e) {
			e.printStackTrace();
			return -1;
		}
		
	}
	
	public int getNumberOfCommits(String commitInicial) {
		int total=0;
		RevWalk revWalk = new RevWalk(repository);
		try {
			ObjectId id=repository.resolve(commitInicial);
			RevCommit commit=revWalk.parseCommit(id);
			for(total=0;;total++){
				commit=revWalk.parseCommit(id);
				id=commit.getParent(0);
			}
		} catch (ArrayIndexOutOfBoundsException e) {
			revWalk.close();
			return total;
		}catch (Exception e) {
			e.printStackTrace();
			revWalk.close();
			return -1;
		}
	}
	
	private String getProjectNameFromUrl(String url) {
		int index = url.lastIndexOf("/");
		return url.substring(index+1);	
	}
}
