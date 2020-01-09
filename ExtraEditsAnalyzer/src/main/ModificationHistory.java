package main;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import analyser.StringAnalyser;
import ch.uzh.ifi.seal.changedistiller.model.classifiers.ChangeType;
import ch.uzh.ifi.seal.changedistiller.model.classifiers.java.JavaEntityType;
import ch.uzh.ifi.seal.changedistiller.model.entities.Delete;
import ch.uzh.ifi.seal.changedistiller.model.entities.Insert;
import ch.uzh.ifi.seal.changedistiller.model.entities.SourceCodeChange;
import ch.uzh.ifi.seal.changedistiller.model.entities.SourceCodeEntity;

public class ModificationHistory {
	
	private Map<SourceCodeChange,Boolean> changeHistory;
	private Map<String,SourceCodeChange> createdMethods;
	private Map<String,SourceCodeChange> deletedMethods;
	private Map<String,SourceCodeChange> createdFields;
	private Map<String,SourceCodeChange> deletedFields;
	private Map<String,SourceCodeChange> createdClasses;
	private Map<String,SourceCodeChange> deletedClasses;
	
	private Map<String,SourceCodeChange> disposableCreatedMethods;
	private Map<String,SourceCodeChange> disposableDeletedMethods;
	private Map<String,SourceCodeChange> disposableCreatedFields;
	private Map<String,SourceCodeChange> disposableDeletedFields;
	

	public ModificationHistory() {
		changeHistory = new HashMap<SourceCodeChange,Boolean>();
		createdMethods = new HashMap<String,SourceCodeChange>();
		deletedMethods = new HashMap<String,SourceCodeChange>();
		createdFields = new HashMap<String,SourceCodeChange>();
		deletedFields = new HashMap<String,SourceCodeChange>();
		createdClasses = new HashMap<String,SourceCodeChange>();
		deletedClasses = new HashMap<String,SourceCodeChange>();
		
		disposableCreatedMethods = new HashMap<String,SourceCodeChange>();
		disposableDeletedMethods = new HashMap<String,SourceCodeChange>();
		disposableCreatedFields = new HashMap<String,SourceCodeChange>();
		disposableDeletedFields = new HashMap<String,SourceCodeChange>();
	}
	
	public void addAllChanges(List<SourceCodeChange> changes) {
		for(SourceCodeChange scc: changes)
			addChange(scc);
	}
	
	public void addChange(SourceCodeChange sc) {
		StringAnalyser sa = new StringAnalyser();
		String signature;
		switch(sc.getChangeType()) {
		case ADDITIONAL_FUNCTIONALITY:
			signature = sc.getChangedEntity().getUniqueName();
			signature = sa.removeSubString(signature, '<', '>', false).replaceAll("\\s","");
			createdMethods.put(signature, sc);
			break;
		case REMOVED_FUNCTIONALITY:
			signature = sc.getChangedEntity().getUniqueName();
			signature = sa.removeSubString(signature, '<', '>', false).replaceAll("\\s","");
			deletedMethods.put(signature, sc);
			break;
		case ADDITIONAL_OBJECT_STATE:
			signature = sc.getChangedEntity().getUniqueName();
			signature=signature.substring(0, signature.indexOf(" : "));
			signature = sa.removeSubString(signature, '<', '>', false).replaceAll("\\s","");
			createdFields.put(signature, sc);
			break;
		case REMOVED_OBJECT_STATE:
			signature = sc.getChangedEntity().getUniqueName();
			signature=signature.substring(0, signature.indexOf(" : "));
			signature = sa.removeSubString(signature, '<', '>', false).replaceAll("\\s","");
			deletedFields.put(signature, sc);
			break;
		default:
			break;
		}
		this.changeHistory.put(sc,false);
	}
	
	public void setCheckedChange(SourceCodeChange sc) throws Exception{
		if(this.changeHistory.containsKey(sc))
			this.changeHistory.put(sc,true);
		else
			throw new Exception("Source Code Change not Found: "+sc+". Root Entity: "+sc.getRootEntity());
		
	}
	
	public void setUncheckedChange(SourceCodeChange sc) throws Exception {
		if(this.changeHistory.containsKey(sc))
			this.changeHistory.put(sc,false);
		else
			throw new Exception("Source Code Change not Found");
		
	}
	
	public boolean isChecked(SourceCodeChange sc) {
		return this.changeHistory.containsKey(sc) && this.changeHistory.get(sc);
	}
	
	public boolean containsChange(SourceCodeChange sc) {
		return this.changeHistory.containsKey(sc);
	}

	public Map<SourceCodeChange, Boolean> getChangeHistory() {
		return changeHistory;
	}

	public SourceCodeChange getCreatedMethod(String signature) {
		return createdMethods.get(signature);
	}

	public SourceCodeChange getDeletedMethod(String signature) {
		return deletedMethods.get(signature);
	}

	public SourceCodeChange getCreatedField(String signature) {
		return createdFields.get(signature);
	}

	public SourceCodeChange getDeletedField(String signature) {
		return deletedFields.get(signature);
	}
	
	public void addCreatedClass(String signature, File file) {
		SourceCodeEntity sce = new SourceCodeEntity(signature,JavaEntityType.CLASS,null);
		Insert ins = new Insert(ChangeType.ADDITIONAL_CLASS,null,sce,null);
		StringAnalyser sa = new StringAnalyser();
		signature = sa.removeSubString(signature, '<', '>', false).replaceAll("\\s","");
		this.createdClasses.put(signature, ins);
		
		MissingFileHandler mfh = new MissingFileHandler();
		List<SourceCodeChange> changes =  mfh.getChangesFromCreatedClass(signature, file);
		addAllDisposableChanges(changes);
	}
	
	public void addDeletedClass(String signature, File file) {
		SourceCodeEntity sce = new SourceCodeEntity(signature,JavaEntityType.CLASS,null);
		Delete del = new Delete(ChangeType.REMOVED_CLASS,null,sce,null);
		StringAnalyser sa = new StringAnalyser();
		signature = sa.removeSubString(signature, '<', '>', false).replaceAll("\\s","");
		this.deletedClasses.put(signature, del);
		
		MissingFileHandler mfh = new MissingFileHandler();
		List<SourceCodeChange> changes =  mfh.getChangesFromDeletedClass(signature, file);
		addAllDisposableChanges(changes);
	}

	public Map<String, SourceCodeChange> getCreatedClasses() {
		return createdClasses;
	}

	public Map<String, SourceCodeChange> getDeletedClasses() {
		return deletedClasses;
	}
	
	
	private void addAllDisposableChanges(List<SourceCodeChange> changes) {
		for(SourceCodeChange scc: changes)
			addDisposableChange(scc);
	}
	
	private void addDisposableChange(SourceCodeChange sc) {
		StringAnalyser sa = new StringAnalyser();
		String signature;
		switch(sc.getChangeType()) {
		case ADDITIONAL_FUNCTIONALITY:
			signature = sc.getChangedEntity().getUniqueName();
			signature = sa.removeSubString(signature, '<', '>', false).replaceAll("\\s","");
			disposableCreatedMethods.put(signature, sc);
			break;
		case REMOVED_FUNCTIONALITY:
			signature = sc.getChangedEntity().getUniqueName();
			signature = sa.removeSubString(signature, '<', '>', false).replaceAll("\\s","");
			disposableDeletedMethods.put(signature, sc);
			break;
		case ADDITIONAL_OBJECT_STATE:
			signature = sc.getChangedEntity().getUniqueName();
			signature=signature.substring(0, signature.indexOf(" : "));
			signature = sa.removeSubString(signature, '<', '>', false).replaceAll("\\s","");
			disposableCreatedFields.put(signature, sc);
			break;
		case REMOVED_OBJECT_STATE:
			signature=sc.getChangedEntity().getUniqueName();
			signature=signature.substring(0, signature.indexOf(" : "));
			signature = sa.removeSubString(signature, '<', '>', false).replaceAll("\\s","");
			disposableDeletedFields.put(signature, sc);
			break;
		default:
			break;
		}
	}
	
	public SourceCodeChange getDisposableCreatedMethod(String signature) {
		return disposableCreatedMethods.get(signature);
	}

	public SourceCodeChange getDisposableDeletedMethod(String signature) {
		return disposableDeletedMethods.get(signature);
	}

	public SourceCodeChange getDisposableCreatedField(String signature) {
		return disposableCreatedFields.get(signature);
	}

	public SourceCodeChange getDisposableDeletedField(String signature) {
		return disposableDeletedFields.get(signature);
	}
	
	
}
