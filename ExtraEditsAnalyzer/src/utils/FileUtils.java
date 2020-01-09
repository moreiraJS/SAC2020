package utils;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public abstract class FileUtils {
	
	private static final File TMPDIR = new File(System.getProperty("java.io.tmpdir"));
	private static final File USERDIR = new File(System.getProperty("java.io.user.dir"));
	private static File downloadFolder = new File(TMPDIR, "Repositories");
	private static File outputFolder = new File(USERDIR, "Output");
	
	public static File getDownloadFolder() {
		return downloadFolder;
	}
	public static void setDownloadFolder(String downloadFolder) {
		FileUtils.downloadFolder = new File(downloadFolder);
	}
	public static File getOutputFolder() {
		return outputFolder;
	}
	public static void setOutputFolder(String outputFolder) {
		FileUtils.outputFolder = new File(outputFolder);
	}

	public static Map<String,File> getClasses(File projectFolder) throws Exception {
		List<File> modules=XMLUtils.getModules(new File(projectFolder,"pom.xml"));
		Map<String,File> classes=new HashMap<String,File>();
		for(File module:modules) {
			classes.putAll(FileUtils.listClasses(new File(module,"src/main/java")));
		}
		return classes;
	}
	
	public static Map<String,File> listClasses(File srcFolder){
		Map<String,File> result= new HashMap<String,File>();
		if(!srcFolder.exists())
			return result;
		
		
		File[] files=srcFolder.listFiles();
		
		for(File file:files) {
			if(file.isDirectory())
				result.putAll(listClasses(file,file.getName()));
			else
				if(file.getName().toLowerCase().endsWith(".java") && 
						!file.getName().toLowerCase().equals("package-info.java"))
					result.put(file.getName().substring(0, file.getName().lastIndexOf(".")),file);
				
		}
		
		return result;
		
	}
	
	public static Map<String,File> listClasses(File srcFolder,String name){
		Map<String,File> result= new HashMap<String,File>();
		
		File[] files=srcFolder.listFiles();
		
		
		for(File file:files) {
			if(file.isDirectory())
				result.putAll(listClasses(file,name+"."+file.getName()));
			else
				if(file.getName().toLowerCase().endsWith(".java") && 
						!file.getName().toLowerCase().equals("package-info.java"))
					result.put(name+"."+file.getName().substring(0, file.getName().lastIndexOf(".")),file);
				
		}
		
		return result;
		
	}

}
