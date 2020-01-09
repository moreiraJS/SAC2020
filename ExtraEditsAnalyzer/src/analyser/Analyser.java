package analyser;

import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.builder.EqualsBuilder;

import com.google.inject.Guice;
import com.google.inject.Injector;

import analyser.callerAnalyser.CallerAnalyser;
import ch.uzh.ifi.seal.changedistiller.JavaChangeDistillerModule;
import ch.uzh.ifi.seal.changedistiller.ast.java.JavaSourceCodeChangeClassifier;
import ch.uzh.ifi.seal.changedistiller.distilling.Distiller;
import ch.uzh.ifi.seal.changedistiller.distilling.DistillerFactory;
import ch.uzh.ifi.seal.changedistiller.model.classifiers.ChangeType;
import ch.uzh.ifi.seal.changedistiller.model.classifiers.EntityType;
import ch.uzh.ifi.seal.changedistiller.model.classifiers.java.JavaEntityType;
import ch.uzh.ifi.seal.changedistiller.model.entities.Delete;
import ch.uzh.ifi.seal.changedistiller.model.entities.Insert;
import ch.uzh.ifi.seal.changedistiller.model.entities.Move;
import ch.uzh.ifi.seal.changedistiller.model.entities.SourceCodeChange;
import ch.uzh.ifi.seal.changedistiller.model.entities.SourceCodeEntity;
import ch.uzh.ifi.seal.changedistiller.model.entities.StructureEntityVersion;
import ch.uzh.ifi.seal.changedistiller.model.entities.Update;
import ch.uzh.ifi.seal.changedistiller.treedifferencing.Node;
import ch.uzh.ifi.seal.changedistiller.treedifferencing.matching.measure.NGramsCalculator;
import ch.uzh.ifi.seal.changedistiller.treedifferencing.matching.measure.StringSimilarityCalculator;
import main.ModificationHistory;
import main.Refactorings;

public class Analyser {
	
	private Refactorings refactorings;
	private ModificationHistory modificationHistory;
	private List<SourceCodeChange> verifiedSourceCodeChanges;
	private	StringAnalyser strAnalyser;
	private double lTh;
	private JavaSourceCodeChangeClassifier classifier;
	private CallerAnalyser callerAnalyser;
	
	public Analyser(Refactorings refactorings, ModificationHistory modificationHistory) {
		this.refactorings = refactorings;
		this.modificationHistory = modificationHistory;
		this.verifiedSourceCodeChanges = new LinkedList<SourceCodeChange>();
		this.strAnalyser = new StringAnalyser();
		this.classifier = new JavaSourceCodeChangeClassifier();
		this.callerAnalyser = new CallerAnalyser();
		this.lTh= 0.6;
	}
	
	public List<SourceCodeChange> getVerifiedSourceCodeChanges(){
		return this.verifiedSourceCodeChanges;
	}
	
	public void analyse() {
		callerAnalyser.extractShortNames(refactorings);
//		callerAnalyser.markCallers(modificationHistory); 
		
		for(SourceCodeChange scc: modificationHistory.getChangeHistory().keySet()) {
			if(!modificationHistory.isChecked(scc)) {
				if(refactorings.isExtractedMethod(scc.getRootEntity().getUniqueName())) {
					analiseExtractedMethod(scc.getRootEntity());
				}else if (refactorings.isInlinedMethod(scc.getRootEntity().getUniqueName())) {
					analiseInlinedMethod(scc.getRootEntity());
				}else if(scc.getChangedEntity().getType().isMethod()){
					String method = scc.getChangedEntity().getUniqueName();
					if (refactorings.isMovedMethod(method))  
						analiseMovedMethod(scc, method);
					else if(scc.getChangeType()!=ChangeType.METHOD_RENAMING
							&& !refactorings.isExtractedMethod(scc.getChangedEntity().getUniqueName())
							&& !refactorings.isInlinedMethod(scc.getChangedEntity().getUniqueName()))
						addChange(scc);
				}else if(scc.getChangedEntity().getType().isField()){
					String field = scc.getChangedEntity().getUniqueName();
					int index = field.indexOf(" : ");
					field = field.substring(0, index);
					if (refactorings.isMovedAttribute(field))  
						analiseMovedAttribute(scc, field);
					else
						addChange(scc);
				}else if(scc.getChangedEntity().getType().isClass()){
					String clazz = scc.getChangedEntity().getUniqueName();
					if(!refactorings.isRefactoredClass(clazz))
						addChange(scc);
				}else if(!callerAnalyser.isCaller(scc))
					addChange(scc);
			}	
		}
		
		for(SourceCodeChange scc: modificationHistory.getCreatedClasses().values()) {
			String clazz = scc.getChangedEntity().getUniqueName();
			if(!refactorings.isRefactoredClass(clazz))
				addChange(scc);
		}
		
		for(SourceCodeChange scc: modificationHistory.getDeletedClasses().values()) {
			String clazz = scc.getChangedEntity().getUniqueName();
			if(!refactorings.isRefactoredClass(clazz))
				addChange(scc);
		}
	}
	
	
	
	
	private void analiseExtractedMethod(StructureEntityVersion root) {
		String createdMethod = this.refactorings.getExtractedMethodSignature(root.getUniqueName());
		SourceCodeChange method=modificationHistory.getCreatedMethod(createdMethod);
		boolean flag = true;
		if(method==null) {//new method not found, new class.
			method = modificationHistory.getDisposableCreatedMethod(createdMethod);
			if(method==null) //new method not found, signature incompatibility
				return;
			flag = false;
		}
		Enumeration<Node> body = method.getBodyStructure().preorderEnumeration();
		List<SourceCodeChange> changes= new LinkedList<SourceCodeChange>(root.getSourceCodeChanges());
		List<MatchedPair> matches= new LinkedList<MatchedPair>();
		List<Node> returnStatements = new LinkedList<Node>();
		
		
		try {
			body.nextElement();
			while(body.hasMoreElements()) {
				Node node=body.nextElement();
				node.disableMatched();
				if(node.getEntity().getType() == JavaEntityType.RETURN_STATEMENT) {
					node.enableMatched();
					if(node.getEntity().getUniqueName().replaceAll("\\.", "").replaceAll(";", "").matches(".*\\W.*"))
						returnStatements.add(node);
				}else
					for(SourceCodeChange scc: changes) {
						if(isSameEntity(node,scc)
								&& isSameEntityType(( (Node)node.getParent() ).getEntity(),scc.getParentEntity())) {
							matches.add(new MatchedPair(node,scc));
							modificationHistory.setCheckedChange(scc);
							changes.remove(scc);
							node.enableMatched();
							break;
						}
					}
			}
		
			body = method.getBodyStructure().preorderEnumeration();
			body.nextElement();
			while(body.hasMoreElements()) {
				Node node=body.nextElement();
				if(!node.isMatched()) {
					for(SourceCodeChange scc: changes) {
						if(similarity(node,scc) >= this.lTh) {
							matches.add(new MatchedPair(node,scc));
							modificationHistory.setCheckedChange(scc);
							changes.remove(scc);
							node.enableMatched();
							if(!isSameEntity(node, scc)) {
								Node parent=(Node) node.getParent();
								SourceCodeChange scc1 = classifier.classify(
										new Update(method.getStructureEntityVersion(),
												scc.getChangedEntity(),
												node.getEntity(),
												parent.getEntity()));
								if ((scc1 != null 
										&& !verifiedSourceCodeChanges.contains(scc1)
										&& !this.callerAnalyser.isCaller(scc1))) {
									addRefactoringRelatedChange(scc1);
								}
							}
							break;
						}
					}
				}
			}
			
			for(Node node: returnStatements) {
				node.disableMatched();
				String returnExpression = node.getEntity().getUniqueName();
				for(SourceCodeChange scc: changes) {
					if(isSameEntityType(( (Node)node.getParent() ).getEntity(),scc.getParentEntity())
							&& scc.getChangedEntity().getUniqueName().equals(returnExpression)) {
						modificationHistory.setCheckedChange(scc);
						matches.add(new MatchedPair(node,scc));
						changes.remove(scc);
						node.enableMatched();
						break;
					}
				}
			}
			
			for(Node node: returnStatements) {
				if(node.isMatched())
					continue;
				String returnExpression = node.getEntity().getUniqueName();
				for(SourceCodeChange scc: changes) {
					if(this.strAnalyser.similarity(scc.getChangedEntity().getUniqueName(),returnExpression) >= this.lTh) {
						matches.add(new MatchedPair(node,scc));
						modificationHistory.setCheckedChange(scc);
						changes.remove(scc);
						if(!scc.getChangedEntity().getUniqueName().equals(returnExpression)) {
							Node parent=(Node) node.getParent();
							SourceCodeChange scc1 = classifier.classify(
									new Update(method.getStructureEntityVersion(),
											node.getEntity(),
											scc.getChangedEntity(),
											parent.getEntity()));
							((Update) scc1).setChangedEntity(scc.getChangedEntity());
							((Update) scc1).setNewEntity(node.getEntity());
							if ((scc1 != null 
									&& !verifiedSourceCodeChanges.contains(scc1)
									&& !this.callerAnalyser.isCaller(scc1)))
								addRefactoringRelatedChange(scc1);
						}
						break;
					}
				}
			}
			
			
			
			ParentAnalyser pa = new ParentAnalyser(matches,'l');
			
			for(MatchedPair match: matches) {
				if(pa.isParentChange(match)) {
					SourceCodeChange scc1 = new Move(ChangeType.STATEMENT_PARENT_CHANGE,
							method.getStructureEntityVersion(),
							match.getSourceCodeChangeEntity(),
							match.getNodeEntity(),
							match.getSourceCodeChangeParent(),
							match.getNodeParent());
					if ((scc1 != null) && !verifiedSourceCodeChanges.contains(scc1)) {
						addRefactoringRelatedChange(scc1);
					}
				}
			}	
			
			body = method.getBodyStructure().preorderEnumeration();
			body.nextElement();
			while(body.hasMoreElements()) {
				Node node=body.nextElement();
				if(!node.isMatched()) {
					Node parent=(Node) node.getParent();
					SourceCodeChange scc = classifier.classify(
							new Insert(method.getStructureEntityVersion(),node.getEntity(),parent.getEntity()));
					if ((scc != null) && !verifiedSourceCodeChanges.contains(scc)) {
						addRefactoringRelatedChange(scc);
					}	
				}
			}
			
			for(SourceCodeChange scc: changes) {
				if (!verifiedSourceCodeChanges.contains(scc)
						&& !this.callerAnalyser.isCaller(scc)
						&& !this.callerAnalyser.isCaller(scc, method))
					addRefactoringRelatedChange(scc);
				modificationHistory.setCheckedChange(scc);
			}
			if(flag)
	    		modificationHistory.setCheckedChange(method);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
	private void analiseInlinedMethod(StructureEntityVersion root) {
		String deletedMethod = this.refactorings.getInlinedMethodSignature(root.getUniqueName());
		SourceCodeChange method=modificationHistory.getDeletedMethod(deletedMethod);
		boolean flag = true;
		if(method==null) {//new method not found, new class.
			method = modificationHistory.getDisposableDeletedMethod(deletedMethod);
			if(method==null) //new method not found, signature incompatibility
				return;
			flag = false;
		}
		Enumeration<Node> body = method.getBodyStructure().preorderEnumeration();
		List<SourceCodeChange> changes = new LinkedList<SourceCodeChange>(root.getSourceCodeChanges());
		List<MatchedPair> matches = new LinkedList<MatchedPair>();
		List<Node> returnStatements = new LinkedList<Node>();

		try {
			body.nextElement();
			while(body.hasMoreElements()) {
				Node node=body.nextElement();
				node.disableMatched();
				if(node.getEntity().getType() == JavaEntityType.RETURN_STATEMENT) {
					node.enableMatched();
					if(node.getEntity().getUniqueName().replaceAll("\\.", "").matches(".*\\W.*"))
						returnStatements.add(node);
				}else
					for(SourceCodeChange scc: changes) {
						if(isSameEntity(node,scc)
								&& isSameEntityType(( (Node)node.getParent() ).getEntity(),scc.getParentEntity())) {	
							matches.add(new MatchedPair(node,scc));
							modificationHistory.setCheckedChange(scc);
							changes.remove(scc);
							node.enableMatched();
							break;
						}
					}
			}
		
			body = method.getBodyStructure().preorderEnumeration();
			body.nextElement();
			while(body.hasMoreElements()) {
				Node node=body.nextElement();
				if(!node.isMatched()) {
					for(SourceCodeChange scc: changes) {
						if(similarity(node,scc) >= this.lTh) {
							matches.add(new MatchedPair(node,scc));
							modificationHistory.setCheckedChange(scc);
							changes.remove(scc);
							node.enableMatched();
							if(!isSameEntity(node, scc)) {
								Node parent=(Node) node.getParent();
								SourceCodeChange scc1 = classifier.classify(
										new Update(method.getStructureEntityVersion(),
												node.getEntity(),
												scc.getChangedEntity(),
												parent.getEntity()));
								if ((scc1 != null) 
										&& !verifiedSourceCodeChanges.contains(scc1)
										&& !this.callerAnalyser.isCaller(scc1)) {
									addRefactoringRelatedChange(scc1);
									
								}
							}
							break;
						}
					}
					
				}
			}
			
			for(Node node: returnStatements) {
				node.disableMatched();
				String returnExpression = node.getEntity().getUniqueName();
				for(SourceCodeChange scc: changes) {
					if(isSameEntityType(( (Node)node.getParent() ).getEntity(),scc.getParentEntity())
							&& scc.getChangedEntity().getUniqueName().equals(returnExpression)) {
						modificationHistory.setCheckedChange(scc);
						matches.add(new MatchedPair(node,scc));
						changes.remove(scc);
						node.enableMatched();
						break;
					}
				}
			}
			
			for(Node node: returnStatements) {
				if(node.isMatched())
					continue;
				String returnExpression = node.getEntity().getUniqueName();
				for(SourceCodeChange scc: changes) {
					if(this.strAnalyser.similarity(scc.getChangedEntity().getUniqueName(),returnExpression) >= this.lTh) {
						matches.add(new MatchedPair(node,scc));
						modificationHistory.setCheckedChange(scc);
						changes.remove(scc);
						if(!scc.getChangedEntity().getUniqueName().equals(returnExpression)) {
							Node parent=(Node) node.getParent();
							SourceCodeChange scc1 = classifier.classify(
									new Update(method.getStructureEntityVersion(),
											node.getEntity(),
											scc.getChangedEntity(),
											parent.getEntity()));
							if ((scc1 != null 
									&& !verifiedSourceCodeChanges.contains(scc1)
									&& !this.callerAnalyser.isCaller(scc1))) 
								addRefactoringRelatedChange(scc1);
						}
						break;
					}
				}
			}
			
			ParentAnalyser pa = new ParentAnalyser(matches,'r');
			
			for(MatchedPair match: matches) {
				if(pa.isParentChange(match)) {
					SourceCodeChange scc1 = new Move(ChangeType.STATEMENT_PARENT_CHANGE,
							method.getStructureEntityVersion(),
							match.getNodeEntity(),
							match.getSourceCodeChangeEntity(),
							match.getNodeParent(),
							match.getSourceCodeChangeParent());
									
					if ((scc1 != null) && !verifiedSourceCodeChanges.contains(scc1)) {
						addRefactoringRelatedChange(scc1);
					}
				}
			}
			
			
			
			body = method.getBodyStructure().preorderEnumeration();
			body.nextElement();
			while(body.hasMoreElements()) {
				Node node=body.nextElement();
				if(!node.isMatched()) {
					Node parent=(Node) node.getParent();
					SourceCodeChange scc = classifier.classify(
							new Delete(method.getStructureEntityVersion(),node.getEntity(),parent.getEntity()));
					if (!verifiedSourceCodeChanges.contains(scc)) {
						addRefactoringRelatedChange(scc);
					}	
				}
			}
			
			for(SourceCodeChange scc: changes) {
				if ((scc != null) 
						&& !verifiedSourceCodeChanges.contains(scc)
						&& !this.callerAnalyser.isCaller(scc, method))
					addRefactoringRelatedChange(scc);
				modificationHistory.setCheckedChange(scc);
			}	
			
			if(flag)
	    		modificationHistory.setCheckedChange(method);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
	
	
	private void analiseMovedAttribute(SourceCodeChange field, String signature) {
		SourceCodeChange field2;
		List<SourceCodeChange> changes;
		boolean flag = true;
		if(field instanceof Insert) {
			signature = this.refactorings.getOldMovedAttributeSignature(signature);
			field2 = this.modificationHistory.getDeletedField(signature);
			if(field2==null) { //Field not found, check in deleted classes;
				field2 = this.modificationHistory.getDisposableDeletedField(signature);
				if(field2==null) 
					return;
				flag = false;
			}
			StructureEntityVersion rootEntity = field.getStructureEntityVersion();
			changes = extractChanges(field2.getDeclarationStructure(), field.getDeclarationStructure(), rootEntity);
		}else {
			signature = this.refactorings.getNewMovedAttributeSignature(signature);
			field2 = this.modificationHistory.getCreatedField(signature);
			if(field2==null) {//Field not found, check in deleted classes;
				field2 = this.modificationHistory.getDisposableCreatedField(signature);
				if(field2==null) 
					return;
				flag = false;
			}
			StructureEntityVersion rootEntity = field2.getStructureEntityVersion();
			changes = extractChanges(field.getDeclarationStructure(), field2.getDeclarationStructure(), rootEntity);
		}
		
		 
	    for(SourceCodeChange scc: changes)
	    	addRefactoringRelatedChange(scc);
	    try {
	    	modificationHistory.setCheckedChange(field);
	    	if(flag)
	    		modificationHistory.setCheckedChange(field2);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void analiseMovedMethod(SourceCodeChange method, String signature) {
		SourceCodeChange method2;
		List<SourceCodeChange> changes;
		boolean flag = true;
		if(method instanceof Insert) {
			signature = this.refactorings.getOldMovedMethodSignature(signature);
			method2 = this.modificationHistory.getDeletedMethod(signature);
			if(method2==null) { //Field not found, check in deleted classes;
				method2 = this.modificationHistory.getDisposableDeletedMethod(signature);
				if(method2==null) 
					return;
				flag = false;
			}
			StructureEntityVersion rootEntity = method.getStructureEntityVersion();
			changes = extractChanges(method2.getDeclarationStructure(), method.getDeclarationStructure(), rootEntity);
			changes.addAll(extractChanges(method2.getBodyStructure(), method.getBodyStructure(), rootEntity));
		}else {
			signature = this.refactorings.getNewMovedMethodSignature(signature);
			method2 = this.modificationHistory.getCreatedMethod(signature);
			if(method2==null) {//Field not found, check in deleted classes;
				method2 = this.modificationHistory.getDisposableCreatedMethod(signature);
				if(method2==null) 
					return;
				flag = false;
			}
			StructureEntityVersion rootEntity = method2.getStructureEntityVersion();
			changes = extractChanges(method.getDeclarationStructure(), method2.getDeclarationStructure(), rootEntity);
			changes.addAll(extractChanges(method.getBodyStructure(), method2.getBodyStructure(), rootEntity));
		}
		
		
	    for(SourceCodeChange scc: changes) {
	    	if(scc.getChangeType()!=ChangeType.METHOD_RENAMING
	    			&& !callerAnalyser.isCaller(scc))
		    	addRefactoringRelatedChange(scc);
	    }
	    try {
	    	modificationHistory.setCheckedChange(method);
	    	if(flag)
	    		modificationHistory.setCheckedChange(method2);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private boolean isLeafUpdate(Node node, SourceCodeChange scc) {
		if(node.isLeaf() && isSameEntityType(node.getEntity(),scc.getChangedEntity())) {
			return similarity(node,scc) >= this.lTh;
		}
		return false;
	}
	
	private double similarity(Node node, SourceCodeChange scc) {
		SourceCodeEntity sce1= node.getEntity();
		SourceCodeEntity sce2= scc.getChangedEntity();
		if(!isSameEntityType(sce1,sce2))
			return 0;
		else
			return this.strAnalyser.similarity(sce1.getUniqueName(), sce2.getUniqueName());
		
	}
	
	private boolean isSameEntityType(SourceCodeEntity entity1, SourceCodeEntity entity2) {
		EntityType et1 = entity1.getType();
		EntityType et2 = entity2.getType();
		return et1.equals(et2);
	}
	
	private boolean isSameEntity(Node node, SourceCodeChange scc) {
		SourceCodeEntity e1 = node.getEntity();
		SourceCodeEntity e2 = scc.getChangedEntity();
		return new EqualsBuilder().append(e1.getUniqueName(), e2.getUniqueName()).append(e1.getType(), e2.getType())
                .append(e1.getModifiers(), e2.getModifiers()).isEquals();
	}
	
	private List<SourceCodeChange> extractChanges(Node left, Node right, StructureEntityVersion rootEntity) {
		Injector injector = Guice.createInjector(new JavaChangeDistillerModule());
	    DistillerFactory df = injector.getInstance(DistillerFactory.class);
        Distiller distiller = df.create(rootEntity);
        disableMatchedNodes(left);
        disableMatchedNodes(right);
        distiller.extractClassifiedSourceCodeChanges(left, right);
        return distiller.getSourceCodeChanges();
    }
	
	private void disableMatchedNodes(Node n) {
		if(n == null)
			return;
		for (Enumeration<Node> nodes = n.postorderEnumeration(); nodes.hasMoreElements();)
            nodes.nextElement().disableMatched();
	}
	
	private void addChange(SourceCodeChange scc) {
		if(scc!=null && !verifiedSourceCodeChanges.contains(scc)){
			this.verifiedSourceCodeChanges.add(scc);
		}
	}
	
	private void addRefactoringRelatedChange(SourceCodeChange scc) {
		if(scc!=null && !verifiedSourceCodeChanges.contains(scc)){
			scc.setRefactoringRelated(true);
			this.verifiedSourceCodeChanges.add(scc);
		}
	}
}
