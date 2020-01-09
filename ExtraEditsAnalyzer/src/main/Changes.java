package main;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import analyser.Analyser;
import ch.uzh.ifi.seal.changedistiller.ChangeDistiller;
import ch.uzh.ifi.seal.changedistiller.ChangeDistiller.Language;
import ch.uzh.ifi.seal.changedistiller.distilling.FileDistiller;
import ch.uzh.ifi.seal.changedistiller.model.entities.SourceCodeChange;
import utils.CSV;
import utils.FileUtils;
import utils.GithubDownloader;
import utils.Logger;
import utils.ZipExtractor;

public class Changes {
	private String repositoryUrl;
	private File refactoringsCSVFile;
	private CSV resultCSV;
	
	public Changes(String repositoryUrl, File refactoringsCSVFile) throws IOException {
		String projectName= getProjectNameFromUrl(repositoryUrl);
		this.repositoryUrl = repositoryUrl;
		this.refactoringsCSVFile = refactoringsCSVFile;
		this.resultCSV = new CSV(projectName);
	}
	
	public void analyse(File isRefactoringFile, boolean shouldLog) throws IOException  {
		String projectName= getProjectNameFromUrl(repositoryUrl);
		Logger logger = new Logger(projectName, shouldLog);
		RefactoringIterator ri = new RefactoringIterator(isRefactoringFile);
		
		String commit = ri.getNextCommit();
		while(commit != null) {
			try {
				extractChanges(commit);
			} catch (Exception e) {
				System.out.println("Get Changes: Error");
				e.printStackTrace();
				logger.logError(commit, e);
			}
			
		}
		ri.close();
		logger.close();
		resultCSV.close();
	}
	
	public void extractChanges(String commit) throws Exception {
		//initialize
		Refactorings refactorings = new Refactorings(this.refactoringsCSVFile,commit);
		String parent=refactorings.getParent();
		GithubDownloader git = new GithubDownloader(this.repositoryUrl);
		ModificationHistory modificationHistory = new ModificationHistory();
		
		//download and extract projects
		File targetFile=git.downloadCommit(commit);
		File targetFolder=ZipExtractor.extract(targetFile, new File(git.getLocation(),commit));
		File sourceFile=git.downloadCommit(parent);
		File sourceFolder=ZipExtractor.extract(sourceFile, new File(git.getLocation(),parent));
		
		//list Java files
		Map<String,File> sourceFiles=FileUtils.getClasses(sourceFolder);
		Map<String,File> targetFiles=FileUtils.getClasses(targetFolder);
		

		for(String clazz:sourceFiles.keySet()) {
			if(refactorings.getChangedClassSignatures().containsKey(clazz)) {
				String newSignature=refactorings.getChangedClassSignatures().get(clazz);
				File left=sourceFiles.get(clazz);
				File right=targetFiles.get(newSignature);
				modificationHistory.addAllChanges(listChangesOfClass(left,right));
				targetFiles.remove(newSignature);
			}else if (targetFiles.containsKey(clazz)){
				File left=sourceFiles.get(clazz);
				File right=targetFiles.get(clazz);
				modificationHistory.addAllChanges(listChangesOfClass(left,right));
				targetFiles.remove(clazz);
			}else {
				System.out.println("File Removed: "+clazz);
				modificationHistory.addDeletedClass(clazz,sourceFiles.get(clazz));
			}
		}
		
		
		for(String signature: targetFiles.keySet()) {
			System.out.println("Created File: "+signature);
			modificationHistory.addCreatedClass(signature,targetFiles.get(signature));
		}
		
		//Analyse changes
		Analyser analyser= new Analyser(refactorings, modificationHistory);
		analyser.analyse();
		List<SourceCodeChange> verifiedSourceCodeChanges = analyser.getVerifiedSourceCodeChanges();
		
		//SaveChanges
		resultCSV.addAll(verifiedSourceCodeChanges,commit);
		
		deleteDirectory(git.getLocation());
		
		System.out.println("\n\nFim commit: "+commit+"\n");
	}
	
	
	public List<SourceCodeChange> listChangesOfClass(File left, File right) {
		FileDistiller distiller = ChangeDistiller.createFileDistiller(Language.JAVA);
		try {
		    distiller.extractClassifiedSourceCodeChanges(left, right);
		} catch(Exception e) {
		    System.err.println("Warning: error while change distilling. " + e.getMessage());
		}
		List<SourceCodeChange> changes = distiller.getSourceCodeChanges();
		return changes;
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
