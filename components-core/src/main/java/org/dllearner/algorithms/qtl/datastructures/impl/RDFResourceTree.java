/**
 * 
 */
package org.dllearner.algorithms.qtl.datastructures.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.SortedSet;
import java.util.TreeMap;

import org.dllearner.algorithms.qtl.datastructures.QueryTree;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.NodeFactory;
import com.hp.hpl.jena.graph.Node_URI;
import com.hp.hpl.jena.sparql.util.NodeComparator;

/**
 * @author Lorenz Buehmann
 *
 */
public class RDFResourceTree extends GenericTree<Node, RDFResourceTree>{
	
	private final int id;
	
	private static final Node DEFAULT_VAR_NODE = NodeFactory.createVariable("");
	
	private Map<RDFResourceTree, Object> child2Edge = new HashMap<>();
    private NavigableMap<Node, List<RDFResourceTree>> edge2Children = new TreeMap<Node, List<RDFResourceTree>>(new NodeComparator());
    
    /**
     * Creates an empty resource tree with a default variable as label.
     */
    public RDFResourceTree() {
		this(0, DEFAULT_VAR_NODE);
	}
    
	public RDFResourceTree(int id, Node data) {
		super(data);
		this.id = id;
	}
	
	public RDFResourceTree(RDFResourceTree tree) {
		super(tree.getData());
		this.id = getID();
	}
	
	/**
	 * @return the ID of the tree
	 */
	public int getID() {
		return id;
	}
	
	public void addChild(RDFResourceTree child, Node edge) {
		super.addChild(child);
		List<RDFResourceTree> childrenForEdge = edge2Children.get(edge);
		if(childrenForEdge == null) {
			childrenForEdge = new ArrayList<RDFResourceTree>();
			edge2Children.put(edge, childrenForEdge);
		}
		childrenForEdge.add(child);
	}
	
	public void addChildAt(int index, RDFResourceTree child, Node_URI edge) throws IndexOutOfBoundsException {
		super.addChildAt(index, child);
		child.setParent(this);
	}
	
	
	public List<RDFResourceTree> getChildren() {
		return super.getChildren();
	}
	
	public List<RDFResourceTree> getChildren(Node edge) {
		return edge2Children.get(edge);
	}
	
	/**
	 * Returns all outgoing different edges.
	 * @return
	 */
	public SortedSet<Node> getEdges() {
		return edge2Children.navigableKeySet();
	}
	
	public boolean isResourceNode() {
    	return data.isURI();
    }
	
	public boolean isLiteralNode() {
		return data.isLiteral();
	}
    
	public boolean isVarNode() {
    	return data.isVariable();
    }
	
	public String getStringRepresentation() {
		return getStringRepresentation(false);
	}
	    
	/**
	 * Prints the query tree and shows children of resources only if enabled.
	 * 
	 * @param stopWhenLeafNode
	 * @return
	 */
	public String getStringRepresentation(boolean stopIfChildIsResourceNode) {
		int indent = 3;
		StringBuilder sb = new StringBuilder();
		if (isRoot()) {
			sb.append("TREE\n\n");
		}
		String ren = this.getData().toString();
		ren = ren.replace("\n", "\n" + sb);
		sb.append(ren);
		sb.append("\n");
		if (isRoot() || !isResourceNode() || (isResourceNode() && !stopIfChildIsResourceNode)) {
			for (RDFResourceTree child : getChildren()) {
				for (int i = 0; i < indent; i++) {
					sb.append("\t");
				}
				Object edge = getEdge(child);
				if (edge != null) {
					sb.append("  ");
					sb.append(edge);
					sb.append(" ---> ");
				}
				sb.append(child.getStringRepresentation(stopIfChildIsResourceNode));
			}
		}
		return sb.toString();
	}
}
