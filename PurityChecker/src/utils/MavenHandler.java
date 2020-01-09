package utils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.MavenInvocationException;

public class MavenHandler {

	private Invoker invoker;
	private List<String> goals;
	
	public MavenHandler(String mavenHome) {
		invoker = new DefaultInvoker();
		goals = new ArrayList<>();
		setMavenHome(mavenHome);
	}
	
	public MavenHandler() {
		invoker = new DefaultInvoker();
		goals = new ArrayList<>();
		String mavenHome = System.getenv("MAVEN_HOME");
		setMavenHome(mavenHome);	
	}
	
	public String getMavenHome() {
		return invoker.getMavenHome().getAbsolutePath();
	}

	public void setMavenHome(String mavenHome) {
		invoker.setMavenHome(new File(mavenHome));
	}
	
	public List<String> getGoals() {
		return goals;
	}

	public void addGoal(String goal) {
		goals.add(goal);
	}
	
	public boolean removeGoal(String goal) {
		return goals.remove(goal);
	}

	public File copyDependencies(File projectFolder) throws MavenInvocationException {
		File pomFile = new File(projectFolder, "pom.xml"); 
		execute(pomFile, Arrays.asList( "dependency:copy-dependencies"));
		File dependenciesFolder = new File(projectFolder, "target" + File.separator + "dependency");
		if(dependenciesFolder.exists())
			return dependenciesFolder;
		else
			return null;
	}
	
	public void buildProject(File projectFolder) throws Exception{
		File pomFile = new File(projectFolder, "pom.xml"); 
		execute(pomFile, goals);
	}
	
	public void execute(File pomFile, List<String> goals) throws MavenInvocationException {
		InvocationRequest request = new DefaultInvocationRequest();
		request.setPomFile( pomFile );
		request.setGoals( goals );
		invoker.execute( request );
	}
	
}
