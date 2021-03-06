package org.topbraid.shacl.validation.sparql;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryParseException;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.QuerySolutionMap;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.topbraid.shacl.model.SHParameterizableTarget;
import org.topbraid.shacl.validation.SHACLException;
import org.topbraid.shacl.validation.TargetPlugin;
import org.topbraid.shacl.vocabulary.SH;
import org.topbraid.spin.arq.ARQFactory;
import org.topbraid.spin.util.JenaUtil;

public class SPARQLTargetPlugin implements TargetPlugin {

	@Override
	public boolean canExecuteTarget(Resource target) {
		return target.hasProperty(SH.select);
	}

	
	@Override
	public Iterable<RDFNode> executeTarget(Dataset dataset, Resource target,
			SHParameterizableTarget parameterizableTarget) {

		String sparql = JenaUtil.getStringProperty(target, SH.select);
		String queryString = SPARQLSubstitutions.withPrefixes(sparql, target);
		Query query;
		try {
			query = getSPARQLWithSelect(target);
		}
		catch(QueryParseException ex) {
			throw new SHACLException("Invalid SPARQL target (" + ex.getLocalizedMessage() + "):\n" + queryString);
		}
		
		QuerySolutionMap bindings = null;
		if(parameterizableTarget != null) {
			bindings = new QuerySolutionMap();
			parameterizableTarget.addBindings(bindings);
		}
		try(QueryExecution qexec = SPARQLSubstitutions.createQueryExecution(query, dataset, bindings)) {
		    Set<RDFNode> results = new HashSet<RDFNode>();
		    ResultSet rs = qexec.execSelect();
		    List<String> varNames = rs.getResultVars();
		    while(rs.hasNext()) {
		        QuerySolution qs = rs.next();
		        for(String varName : varNames) {
		            RDFNode value = qs.get(varName);
		            if(value != null) {
		                results.add(value);
		            }
		        }
		    }
		    return results;
		}
	}


	@Override
	public boolean isNodeInTarget(RDFNode focusNode, Dataset dataset, Resource executable, SHParameterizableTarget parameterizableTarget) {

		// If sh:sparql exists only, then we expect run the query with ?this pre-bound
		String sparql = JenaUtil.getStringProperty(executable, SH.select);
		String queryString = SPARQLSubstitutions.withPrefixes(sparql, executable);
		Query query;
		try {
			query = ARQFactory.get().createQuery(queryString);
		}
		catch(QueryParseException ex) {
			throw new SHACLException("Invalid SPARQL target (" + ex.getLocalizedMessage() + "):\n" + queryString);
		}

		QuerySolutionMap bindings = new QuerySolutionMap();
		bindings.add(SH.thisVar.getVarName(), focusNode);
		if(parameterizableTarget != null) {
			parameterizableTarget.addBindings(bindings);
		}
		try(QueryExecution qexec = SPARQLSubstitutions.createQueryExecution(query, dataset, bindings)) {
		    ResultSet rs = qexec.execSelect();
		    boolean hasNext = rs.hasNext();
		    return hasNext;
		}

		/* Alternative: a stupid brute-force algorithm
		Iterator<Resource> it = getResourcesInTarget(dataset, executable, templateCall).iterator();
		while(it.hasNext()) {
			if(focusNode.equals(it.next())) {
				return true;
			}
		}
		return false;*/
	}
	
	
	private static Query getSPARQLWithSelect(Resource host) {
		String sparql = JenaUtil.getStringProperty(host, SH.select);
		if(sparql == null) {
			throw new SHACLException("Missing sh:sparql at " + host);
		}
		try {
			return ARQFactory.get().createQuery(SPARQLSubstitutions.withPrefixes(sparql, host));
		}
		catch(Exception ex) {
			return ARQFactory.get().createQuery(SPARQLSubstitutions.withPrefixes("SELECT ?this WHERE {" + sparql + "}", host));
		}
	}
}
