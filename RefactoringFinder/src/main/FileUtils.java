package main;

import java.io.File;

public class FileUtils {
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
}
