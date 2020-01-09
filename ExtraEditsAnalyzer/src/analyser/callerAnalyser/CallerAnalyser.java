package analyser.callerAnalyser;

import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import analyser.StringAnalyser;
import analyser.callerAnalyser.CallerPattern.CallerType;
import ch.uzh.ifi.seal.changedistiller.model.classifiers.ChangeType;
import ch.uzh.ifi.seal.changedistiller.model.classifiers.java.JavaEntityType;
import ch.uzh.ifi.seal.changedistiller.model.entities.Insert;
import ch.uzh.ifi.seal.changedistiller.model.entities.SourceCodeChange;
import ch.uzh.ifi.seal.changedistiller.model.entities.SourceCodeEntity;
import ch.uzh.ifi.seal.changedistiller.model.entities.Update;
import ch.uzh.ifi.seal.changedistiller.treedifferencing.Node;
import main.ModificationHistory;
import main.Refactorings;

public class CallerAnalyser {
	
	private Set<CallerPattern> callerPatterns;
	
	public CallerAnalyser() {
		callerPatterns = new HashSet<CallerPattern>();
	}
	
	public void extractShortNames(Refactorings refactorings) {
		Collection<String> signatures = refactorings.getInlinedMethods().values();
		for(String signature: signatures)
			callerPatterns.add(new CallerPattern(signature));
		
		signatures = refactorings.getRenamedMethods().values();
		for(String signature: signatures)
			callerPatterns.add(new CallerPattern(signature));
		
		signatures = refactorings.getChangedClassSignatures().values();
		for(String signature: signatures) {
			CallerPattern cp = new CallerPattern(signature);
			cp.setType(CallerType.Class);
			callerPatterns.add(cp);
		}
		
		Set<String> signatures2 = refactorings.getMovedMethodsLeftToRight().keySet();
		for(String signature: signatures2)
			callerPatterns.add(new CallerPattern(signature));
		
		signatures2 = refactorings.getMovedMethodsRightToLeft().keySet();
		for(String signature: signatures2)
			callerPatterns.add(new CallerPattern(signature));
		
		signatures2 = refactorings.getMovedAttributesLeftToRight().keySet();
		for(String signature: signatures2)
			callerPatterns.add(new CallerPattern(signature));
		
		signatures2 = refactorings.getMovedAttributesRightToLeft().keySet();
		for(String signature: signatures2)
			callerPatterns.add(new CallerPattern(signature));
		
		signatures2 = refactorings.getRenamedMethods().keySet();
		for(String signature: signatures2)
			callerPatterns.add(new CallerPattern(signature));
		
		signatures2 = refactorings.getChangedClassSignatures().keySet();
		for(String signature: signatures) {
			CallerPattern cp = new CallerPattern(signature);
			cp.setType(CallerType.Class);
			callerPatterns.add(cp);
		}
	}
	
	public void markCallers(ModificationHistory mh) {
		try {
			for(SourceCodeChange scc: mh.getChangeHistory().keySet()) { 
				if(isCaller(scc)) {
					mh.setCheckedChange(scc);
					break;
				}	
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public boolean isCaller(SourceCodeChange scc) {
		if(scc instanceof Update)
			for(CallerPattern cp: callerPatterns) 
				if(fitPattern(scc,cp)) {
					return true;
				}
		return false;
	}
	
	public boolean isCaller(SourceCodeChange caller, SourceCodeChange callee) {
		SourceCodeEntity calleeEntity = callee.getChangedEntity();
		CallerPattern cp = new CallerPattern(calleeEntity.getUniqueName());
		return fitPattern(caller,cp);
	}
	
	public boolean fitPattern(SourceCodeChange caller, CallerPattern callee) {
		return fitMethodPattern(caller,callee) || fitFieldPattern(caller,callee) || fitClassPattern(caller, callee);
	}
	
	private boolean fitMethodPattern(SourceCodeChange caller, CallerPattern callee) {
		if(!caller.getChangedEntity().getType().isStatement())
			return false;
		if(callee.getType() != CallerType.Method)
			return false;
		String statment = caller.getChangedEntity().getUniqueName();
		
		String[] split = statment.split(callee.getShortName());
		for(int i = 1; i< split.length; i++) {
			if(split[i].matches("\\s*\\(.*")
					&& (split[i-1].isEmpty() || split[i-1].matches(".*\\W"))
					&& countParemetersFromInvocation(split[i])==callee.getnParameters())
				return true;
		}
		return false;
	}
	
	private boolean fitFieldPattern(SourceCodeChange caller, CallerPattern callee) {
		if(!caller.getChangedEntity().getType().isStatement())
			return false;
		if(callee.getType() != CallerType.Field)
			return false;
		
		String statment = caller.getChangedEntity().getUniqueName();
		String regex = ".*\\.\\s*"+callee.getShortName()+"(?!\\s*\\()\\W.*";
		if(statment.matches(regex))
			return true;
		
		Node root = caller instanceof Insert? 
				caller.getRootEntity().getBodyRigth() : caller.getRootEntity().getBodyLeft(); 
		if(bodyContaisVariable(root,callee.getShortName()))
			return false;
		
		return statment.matches(callee.getShortName()+"(?!\\s*\\()\\W.*")
				|| statment.matches(".*\\W"+callee.getShortName()+"(?!\\s*\\()\\W.*");
	}
	
	private boolean bodyContaisVariable(Node root, String name) {
		if(root==null)
			return false;
		Enumeration<Node> body = root.preorderEnumeration();
		while(body.hasMoreElements()) {
			SourceCodeEntity e = body.nextElement().getEntity();	
			if(e.getType()==JavaEntityType.VARIABLE_DECLARATION_STATEMENT
					&& isVariablesDeclaration(e.getUniqueName(),name))
				return true;
			else if(e.getType()==JavaEntityType.FOREACH_STATEMENT
					&& isVariablesDeclaration(e.getUniqueName(),name))
				return true;
		}
		return false;
	}
	
	private boolean isVariablesDeclaration(String statment, String name) {
		if(!statment.contains(name))
			return false;
		
		String declaration = statment;
		if(statment.contains("=")) {
			int index = statment.indexOf("=");
			declaration = statment.substring(0, index) + ";";
		}
		
		return declaration.matches(".*\\W+"+name+"\\W.*");
	}
	
	
	private boolean fitClassPattern(SourceCodeChange caller, CallerPattern callee) {
		if(callee.getType() != CallerType.Class)
			return false;
		switch(caller.getChangeType()) {
			case ATTRIBUTE_TYPE_CHANGE:
				return caller.getChangedEntity().getUniqueName().matches("(\\w|\\.)*"+callee.getShortName());
			case RETURN_TYPE_CHANGE:
			case PARAMETER_TYPE_CHANGE:
				return caller.getChangedEntity().getUniqueName().matches(".*: (\\w|\\.)*"+callee.getShortName());
			default:
				SourceCodeEntity sce = caller.getChangedEntity();
				if(sce.getType()==JavaEntityType.VARIABLE_DECLARATION_STATEMENT ||
						sce.getType()==JavaEntityType.FOREACH_STATEMENT)
					return sce.getUniqueName().matches("(\\w|\\.|<)*"+callee.getShortName()+"\\W.*");
				
		}
		return false;
	}
	
	public int countParemetersFromInvocation(String str) {
		int result = 0;
		char[] sequence = str.replaceAll("\\s","").toCharArray();
		
		int count = 0;
		int begin=0;
		int end=0;
		for(int i=0; i<sequence.length; i++) {
			if(sequence[i]=='(') {
				if(count==0)
					begin=i;
				count++;
			}else if(sequence[i]==')') {
				count--;
				if(count==0) {
					end=i;
					break;
				}
			}else if(sequence[i]==',' && count==1)
				result++;
		}
		if(count>0)
			return -1;
		if(end-begin > 1)
			result++;
		return result;
	}

}
