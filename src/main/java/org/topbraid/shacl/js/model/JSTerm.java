package org.topbraid.shacl.js.model;

import org.apache.jena.graph.Node;

public abstract class JSTerm {
	
	protected org.apache.jena.graph.Node node;
	
	
	protected JSTerm(Node node) {
		this.node = node;
	}
	
	
	@Override
	public boolean equals(Object arg0) {
		if(arg0 instanceof JSTerm) {
			return node.equals(((JSTerm)arg0).node);
		}
		else {
			return false;
		}
	}
	
	
	public org.apache.jena.graph.Node getNode() {
		return node;
	}
	
	
	public abstract String getTermType();
	
	
	public abstract String getValue();

	
	@Override
	public int hashCode() {
		return node.hashCode();
	}


	@Override
	public String toString() {
		return node.toString();
	}
}
