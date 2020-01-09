package main;

import java.io.File;
import java.util.List;

import org.apache.maven.shared.invoker.MavenInvocationException;

import saferefactor.core.Parameters;
import saferefactor.core.SafeRefactor;
import saferefactor.core.SafeRefactorImp;
import saferefactor.core.util.Project;
import utils.MavenHandler;
import utils.XMLUtils;

public class Test {
	private File sourceProjectFolder;
	private File targetProjectFolder;
	private double timeLimit;
 

	public Test(File sourceProjectFolder, File targetProjectFolder, double timeLimit) throws Exception {
		this.sourceProjectFolder = sourceProjectFolder;
		this.targetProjectFolder = targetProjectFolder;
		this.timeLimit = timeLimit;
	}
	
	public File getSourceProjectFolder() {
		return sourceProjectFolder;
	}

	public void setSourceProjectFolder(File sourceProjectFolder) {
		this.sourceProjectFolder = sourceProjectFolder;
	}

	public File getTargetProjectFolder() {
		return targetProjectFolder;
	}

	public void setTargetProjectFolder(File targetProjectFolder) {
		this.targetProjectFolder = targetProjectFolder;
	}
	
	public double getTimeLimit() {
		return timeLimit;
	}

	public void setTimeLimit(double timeLimit) {
		this.timeLimit = timeLimit;
	}

	public boolean hasSameBehaviour() throws Exception {
		List<File> sourceModules = XMLUtils.getModules(new File(sourceProjectFolder,"pom.xml"));
		List<File> targetModules = XMLUtils.getModules(new File(targetProjectFolder,"pom.xml"));
		
		int moduleCount = 0;
		int errorCount = 0;
		for(File sourceFolder: sourceModules) {
			String path = sourceFolder.getAbsolutePath();
			File targetFolder = new File(path.replace(sourceProjectFolder.getName(), targetProjectFolder.getName()));
			if(!targetModules.contains(targetFolder)) {
				System.out.print("Module not found in target: ");
				System.out.println(path.split(sourceProjectFolder.getName())[1]);
				continue;
			}
			moduleCount++;
			try {
				Project source = createProject(sourceFolder);
				Project target = createProject(targetFolder);
				boolean isRefactoring = execute(source,target);
				System.out.println("Same Behavior [Module]: " + isRefactoring);
				if(!isRefactoring) 
					return false;
			} catch (Exception e) {
				errorCount++;
				System.out.println("Error verifing module: " );
				System.out.println(sourceFolder);
				System.out.println(targetFolder);
				e.printStackTrace();
			}
		}
		if(moduleCount == errorCount)
			throw new Exception("No module verified!");
		return true;
	}
	
	private Project createProject(File projectFolder) throws MavenInvocationException {
		Project project = new Project();
		project.setProjectFolder(sourceProjectFolder);
		project.setBuildFolder(new File(projectFolder, "target" + File.separator + "classes"));
		project.setSrcFolder(new File(projectFolder, "src" + File.separator + "main" + File.separator + "java"));
		File libFolder = new MavenHandler().copyDependencies(projectFolder);
		if (libFolder != null)
			project.setLibFolder(libFolder);
		return project;
	}
	
	private boolean execute(Project source, Project target) throws Exception {
		Parameters parameters = new Parameters();
		parameters.setTimeLimit(timeLimit);
		
		boolean sourceBin = true;
		if(!source.getBuildFolder().exists()) {
			source.getBuildFolder().mkdirs();
			sourceBin = false;
		}
		boolean targetBin = true;
		if(!target.getBuildFolder().exists()) {
			source.getBuildFolder().mkdirs();
			targetBin = false;
		}
		parameters.setCompileProjects(!(sourceBin && targetBin));
		
		SafeRefactor sr = new SafeRefactorImp(source, target,parameters);
		sr.checkTransformation();
		return sr.getReport().isRefactoring();
	}
	
	
	

}
