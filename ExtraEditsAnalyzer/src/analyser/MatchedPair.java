package analyser;

import ch.uzh.ifi.seal.changedistiller.model.entities.SourceCodeChange;
import ch.uzh.ifi.seal.changedistiller.model.entities.SourceCodeEntity;
import ch.uzh.ifi.seal.changedistiller.treedifferencing.Node;

public class MatchedPair {
	
	private Node node;
	private SourceCodeChange change;
	
	public MatchedPair(Node node, SourceCodeChange change) {
		super();
		this.node = node;
		this.change = change;
	}
	
	public Node getNode() {
		return node;
	}
	
	public void setNode(Node node) {
		this.node = node;
	}
	
	public SourceCodeChange getChange() {
		return change;
	}
	
	public void setChange(SourceCodeChange change) {
		this.change = change;
	}
	
	public SourceCodeEntity getNodeParent() {
		Node parent = (Node)node.getParent();
		return parent.getEntity();
	}
	
	public SourceCodeEntity getSourceCodeChangeParent() {
		return change.getParentEntity();
	}
	
	public SourceCodeEntity getNodeEntity() {
		return node.getEntity();
	}
	
	public SourceCodeEntity getSourceCodeChangeEntity() {
		return change.getChangedEntity();
	}

}
