package main;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class Refactorings {
	
	private Map<String, String> changedClassSignatures;
	private Map<String, String> extractedMethods;
	private Map<String, String> inlinedMethods;
	
	private Map<String, String> movedAttributesLeftToRight;
	private Map<String, String> movedMethodsLeftToRight;
	private Map<String, String> movedAttributesRightToLeft;
	private Map<String, String> movedMethodsRightToLeft;
	
	private Map<String, String> renamedMethods;
	private List<String> addedClasses;
	private String parent;
	
	public Refactorings(File csvFile,String commit) throws FileNotFoundException {
		init(csvFile, commit);
	}
	
	private void init(File csvFile,String commit) throws FileNotFoundException {
		
		this.changedClassSignatures = new HashMap<String,String>();
		this.extractedMethods = new HashMap<String,String>();
		this.inlinedMethods = new HashMap<String,String>();
		this.renamedMethods = new HashMap<String,String>();
		
		this.movedMethodsLeftToRight = new HashMap<String,String>();
		this.movedAttributesLeftToRight = new HashMap<String,String>();
		this.movedMethodsRightToLeft = new HashMap<String,String>();
		this.movedAttributesRightToLeft = new HashMap<String,String>();
		
		this.addedClasses = new ArrayList<String>();
		
		
		Scanner in = new Scanner(csvFile).useDelimiter(";");
		
		boolean flag=false;  //responsible for stopping the loop when there is no more entries of the selected commit
		String key,value;
		in.nextLine();
		while(in.hasNext()) {
			in.next();
			if(in.next().equals(commit)) {
				flag=true;
				this.parent=in.next();
				switch(in.next()) {
					case "Extract Method":
						key=in.next();
						value=in.next();
						this.extractedMethods.put(key, value);
						break;
					case "Inline Method":
						value=in.next();
						key=in.next();
						this.inlinedMethods.put(key, value);
						break;
					case "Rename Method"://when the method is also moved, it is saved as moved method. When is not, is saved as renamedMethod.
						key=in.next();
						value=in.next();
						String before=key.substring(0, key.lastIndexOf("."));
						String after=value.substring(0, value.lastIndexOf("."));
						if(!before.equals(after)) {
							this.movedMethodsLeftToRight.put(key, value);
							this.movedMethodsRightToLeft.put(value, key);
						}else
							this.renamedMethods.put(key, value);
						break;
					case "Pull Up Method":
					case "Push Down Method":
					case "Move Method":
						key=in.next();
						value=in.next();
						this.movedMethodsLeftToRight.put(key, value);
						this.movedMethodsRightToLeft.put(value, key);
						break;
					case "Pull Up Attribute":
					case "Push Down Attribute":
					case "Move Attribute":
						key=in.next();
						value=in.next();
						this.movedAttributesLeftToRight.put(key, value);
						this.movedAttributesRightToLeft.put(value, key);
						break;
					case "Rename Class":
					case "Move Class":
					case "Move And Rename Class":
						key=in.next();
						value=in.next();
						changedClassSignatures.put(key, value);
						break;
					case "Extract Superclass":
						in.next();
						this.addedClasses.add(in.next());
						break;
					case "Extract Interface":
						in.next();
						this.addedClasses.add(in.next());
						break;
					default:
					
				}
				
			}else 
				if(flag)break;
			
			in.nextLine();
		}
		in.close();
	}
	
	public boolean isMovedMethod(String signature) {
		return (this.movedMethodsLeftToRight.containsKey(signature) || this.movedMethodsRightToLeft.containsKey(signature));
	}
	
	public boolean isMovedAttribute(String signature) {
		return (this.movedAttributesLeftToRight.containsKey(signature) || this.movedAttributesRightToLeft.containsKey(signature));
	}
	
	public boolean isExtractedMethod(String signature) {
		return (this.extractedMethods.containsKey(signature) || this.extractedMethods.containsValue(signature));
	}
	
	public boolean isInlinedMethod(String signature) {
		return (this.inlinedMethods.containsKey(signature) || this.inlinedMethods.containsValue(signature));
	}
	
	public boolean isRefactoredClass(String signature) {
		return (this.changedClassSignatures.containsKey(signature) || 
				this.changedClassSignatures.containsValue(signature) ||
				this.addedClasses.contains(signature));
	}

	public Map<String, String> getChangedClassSignatures() {
		return changedClassSignatures;
	}

	public Map<String, String> getMovedAttributesLeftToRight() {
		return movedAttributesLeftToRight;
	}

	public Map<String, String> getMovedMethodsLeftToRight() {
		return movedMethodsLeftToRight;
	}
	
	public Map<String, String> getMovedAttributesRightToLeft() {
		return movedAttributesRightToLeft;
	}

	public Map<String, String> getMovedMethodsRightToLeft() {
		return movedMethodsRightToLeft;
	}

	public List<String> getAddedClasses() {
		return addedClasses;
	}

	public Map<String, String> getExtractedMethods() {
		return extractedMethods;
	}

	public Map<String, String> getInlinedMethods() {
		return inlinedMethods;
	}

	public String getExtractedMethodSignature(String signature) {
		return extractedMethods.get(signature);
	}

	public String getInlinedMethodSignature(String signature) {
		return inlinedMethods.get(signature);
	}

	public String getNewMovedAttributeSignature(String oldSignature) {
		return movedAttributesLeftToRight.get(oldSignature);
	}

	public String getNewMovedMethodSignature(String oldSignature) {
		return movedMethodsLeftToRight.get(oldSignature);
	}
	
	public String getOldMovedAttributeSignature(String newSignature) {
		return movedAttributesRightToLeft.get(newSignature);
	}

	public String getOldMovedMethodSignature(String newSignature) {
		return movedMethodsRightToLeft.get(newSignature);
	}
	
	public String getNewClassSignature(String signature) {
		return changedClassSignatures.get(signature);
	}
	
	public Map<String,String> getRenamedMethods() {
		return renamedMethods;
	}

	public String getParent() {
		return parent;
	}
	
}
