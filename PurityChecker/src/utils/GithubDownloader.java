package utils;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;


public class GithubDownloader {
	
	private File location;
	private String repositoryUrl;
	
	public GithubDownloader(String repositoryUrl){
		this.repositoryUrl=repositoryUrl;
		this.location = new File(FileUtils.getDownloadFolder(), getProjectNameFromUrl(repositoryUrl));
		
		if(!this.location.exists())
			this.location.mkdirs();
	}
	
	public boolean setLocation(File location){
		this.location = location;
		if(!this.location.isDirectory())
			return this.location.mkdirs();
		else
			return true;
	}
	
	public File getLocation(){
		return this.location;
	}
	
	public File downloadCommit(String commit) throws Exception {
		URL website = new URL(repositoryUrl+"/archive/"+commit+".zip");
		System.out.println("Downloading "+website);
		ReadableByteChannel rbc;
		while(true) {
			try {
				rbc = Channels.newChannel(website.openStream());
				break;
			} catch (IOException e) {
				System.out.println("Error downloading "+commit+".zip");
				System.out.println("Trying again in 3 seconds");
				Thread.sleep(3000);
			}
		}
		File arquivo=new File(location,commit+".zip");
		FileOutputStream fos = new FileOutputStream(arquivo);
		fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
		fos.close();
		return arquivo;
	}
	
	private String getProjectNameFromUrl(String url) {
		int index = url.lastIndexOf("/");
		return url.substring(index+1);	
	}
}
