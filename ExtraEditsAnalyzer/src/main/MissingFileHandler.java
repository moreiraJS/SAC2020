package main;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import ch.uzh.ifi.seal.changedistiller.ChangeDistiller;
import ch.uzh.ifi.seal.changedistiller.ChangeDistiller.Language;
import ch.uzh.ifi.seal.changedistiller.distilling.FileDistiller;
import ch.uzh.ifi.seal.changedistiller.model.entities.SourceCodeChange;

public class MissingFileHandler {
	
	
	
	public List<SourceCodeChange> getChangesFromDeletedClass(String signature, File file){
		FileDistiller distiller = ChangeDistiller.createFileDistiller(Language.JAVA);
		try {
			File auxFile = createClassFile(signature);
			distiller.extractClassifiedSourceCodeChanges(file, auxFile);
			auxFile.delete();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return distiller.getSourceCodeChanges();
	}
	
	
	public List<SourceCodeChange> getChangesFromCreatedClass(String signature, File file){
		FileDistiller distiller = ChangeDistiller.createFileDistiller(Language.JAVA);
		try {
			File auxFile = createClassFile(signature);
			distiller.extractClassifiedSourceCodeChanges(auxFile,file);
			auxFile.delete();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return distiller.getSourceCodeChanges();
	}
	
	
	private File createClassFile(String signature) throws IOException {
		int index = signature.lastIndexOf(".");
		String pack = signature.substring(0, index);
		String name = signature.substring(index+1, signature.length());
		File clazz = new File(name+".java");
		FileWriter writer = new FileWriter(clazz);
		
		writer.write("package "+pack+";\n\n");
		writer.write("public class ");
		writer.write(name+" {}");
		writer.flush();
		writer.close();
		return clazz;
	}

}
