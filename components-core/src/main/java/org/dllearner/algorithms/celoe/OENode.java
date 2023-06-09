/**
 * Copyright (C) 2007 - 2016, Jens Lehmann
 *
 * This file is part of DL-Learner.
 *
 * DL-Learner is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * DL-Learner is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.dllearner.algorithms.celoe;

import org.dllearner.core.AbstractSearchTreeNode;
import org.dllearner.core.LearningProblem;
import org.dllearner.learningproblems.PosNegLP;
import org.dllearner.utilities.datastructures.SearchTreeNode;
import org.dllearner.utilities.owl.OWLAPIRenderers;
import org.dllearner.utilities.owl.OWLClassExpressionUtils;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLIndividual;

import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * A node in the search tree of the ontology engineering algorithm.
 * 
 * Differences to the node structures in other algorithms (this may change):
 * - covered examples are not stored in node (i.e. coverage needs to be recomputed
 * for child nodes, which costs time but saves memory)
 * - only evaluated nodes are stored
 * - too weak nodes are not stored
 * - redundant nodes are not stored (?)
 * - only accuracy is stored to make the node structure reusable for different
 *   learning problems and -algorithms
 * 
 * @author Jens Lehmann
 *
 */
public class OENode extends AbstractSearchTreeNode<OENode> implements SearchTreeNode {

	protected OWLClassExpression description;
	
	protected double accuracy;
	
	protected int horizontalExpansion;
		
	// the refinement count corresponds to the number of refinements of the
	// OWLClassExpression in this node - it is a better heuristic indicator than child count
	// (and avoids the problem that adding children changes the heuristic value)
	private int refinementCount = 0;

	private static boolean useCompactedCoverage = false;

	private static OWLIndividual[] allPositiveExamples;
	private static OWLIndividual[] allNegativeExamples;

    private static Map<OWLIndividual, Integer> positiveExamplesIndices;
    private static Map<OWLIndividual, Integer> negativeExamplesIndices;

	private int[] coveredPositiveExamplesCompact;
	private int[] coveredNegativeExamplesCompact;

	private final Set<OWLIndividual> coveredPositiveExamples = new TreeSet<>();
	private final Set<OWLIndividual> coveredNegativeExamples = new TreeSet<>();

	private static DecimalFormat dfPercent = new DecimalFormat("0.00%");
	
	public OENode(OWLClassExpression description, double accuracy) {
		this.description = description;
		this.accuracy = accuracy;
		this.horizontalExpansion = OWLClassExpressionUtils.getLength(description);
	}
	
//	public OENode(OENode parentNode, OWLClassExpression description, double accuracy) {
//		this(description, accuracy);
//		this.setParent(parentNode);
//	}

	public void incHorizontalExpansion() {
		horizontalExpansion++;
	}
	
	public boolean isRoot() {
		return (parent == null);
	}
	
	/**
	 * @return the description
	 */
	public OWLClassExpression getDescription() {
		return description;
	}

	@Override
	public OWLClassExpression getExpression() {
		return getDescription();
	}
	
	/**
	 * @return the accuracy
	 */
	public double getAccuracy() {
		return accuracy;
	}

	/**
	 * @return the horizontalExpansion
	 */
	public int getHorizontalExpansion() {
		return horizontalExpansion;
	}
	
	public String getShortDescription(String baseURI) {
		return getShortDescription(baseURI, null);
	}
	
	public String getShortDescription(String baseURI, Map<String, String> prefixes) {
		String ret = OWLAPIRenderers.toDLSyntax(description) + " [";
//		String ret = OWLAPIRenderers.toManchesterOWLSyntax(description) + " [";
//		ret += "score" + NLPHeuristic.getNodeScore(this) + ",";
		ret += "acc:" + dfPercent.format(accuracy) + ", ";
		ret += "he:" + horizontalExpansion + ", ";
		ret += "c:" + children.size() + ", ";
		ret += "ref:" + refinementCount + "]";
		return ret;
	}
	
	@Override
	public String toString() {
		return getShortDescription(null);
	}

	/**
	 * @return the refinementCount
	 */
	public int getRefinementCount() {
		return refinementCount;
	}

	/**
	 * @param refinementCount the refinementCount to set
	 */
	public void setRefinementCount(int refinementCount) {
		this.refinementCount = refinementCount;
	}

	public Set<OWLIndividual> getCoveredPositiveExamples() {
		if (useCompactedCoverage) {
			return getCoveredPositiveExamplesCompact();
		}

		return coveredPositiveExamples;
	}

	private Set<OWLIndividual> getCoveredPositiveExamplesCompact() {
		return Arrays.stream(coveredPositiveExamplesCompact).mapToObj(i -> allPositiveExamples[i])
            .collect(Collectors.toSet());
	}

    public int getNumberOfCoveredPositiveExamples() {
        return useCompactedCoverage ? coveredPositiveExamplesCompact.length : coveredPositiveExamples.size();
    }

	public Set<OWLIndividual> getCoveredNegativeExamples() {
		if (useCompactedCoverage) {
			return getCoveredNegativeExamplesCompact();
		}

		return coveredNegativeExamples;
	}

	private Set<OWLIndividual> getCoveredNegativeExamplesCompact() {
		return Arrays.stream(coveredNegativeExamplesCompact).mapToObj(i -> allNegativeExamples[i])
            .collect(Collectors.toSet());
	}

    public int getNumberOfCoveredNegativeExamples() {
        return useCompactedCoverage ? coveredNegativeExamplesCompact.length : coveredNegativeExamples.size();
    }

	public void setCoveredPositiveExamples(Set<OWLIndividual> coveredPositiveExamples) {
		if (useCompactedCoverage) {
			setCoveredPositiveExamplesCompact(coveredPositiveExamples);
			return;
		}

		this.coveredPositiveExamples.clear();

		if (coveredPositiveExamples != null) {
			this.coveredPositiveExamples.addAll(coveredPositiveExamples);
		}
	}

	private void setCoveredPositiveExamplesCompact(Set<OWLIndividual> coveredPositiveExamples) {
		coveredPositiveExamplesCompact = new int[coveredPositiveExamples.size()];

        int ind = 0;
        for (OWLIndividual ex : coveredPositiveExamples) {
            coveredPositiveExamplesCompact[ind] = positiveExamplesIndices.get(ex);
            ind++;
        }
	}

	public void setCoveredNegativeExamples(Set<OWLIndividual> coveredNegativeExamples) {
		if (useCompactedCoverage) {
			setCoveredNegativeExamplesCompact(coveredNegativeExamples);
			return;
		}

		this.coveredNegativeExamples.clear();

		if (coveredNegativeExamples != null) {
			this.coveredNegativeExamples.addAll(coveredNegativeExamples);
		}
	}

	private void setCoveredNegativeExamplesCompact(Set<OWLIndividual> coveredNegativeExamples) {
        coveredNegativeExamplesCompact = new int[coveredNegativeExamples.size()];

        int ind = 0;
        for (OWLIndividual ex : coveredNegativeExamples) {
            coveredNegativeExamplesCompact[ind] = negativeExamplesIndices.get(ex);
            ind++;
        }
	}

	public static void enableCompactCoverageRepresentation(LearningProblem learningProblem) {
		if (!(learningProblem instanceof PosNegLP)) {
			throw new UnsupportedOperationException("Compacted coverage representation is only supported for PosNegLP learning problems.");
		}

		Set<OWLIndividual> positives = ((PosNegLP) learningProblem).getPositiveExamples();
		Set<OWLIndividual> negatives = ((PosNegLP) learningProblem).getNegativeExamples();

		enableCompactCoverageRepresentation(positives, negatives);
	}

	protected static void enableCompactCoverageRepresentation(Set<OWLIndividual> allPositiveExamples, Set<OWLIndividual> allNegativeExamples) {
		OENode.allPositiveExamples = allPositiveExamples.toArray(OWLIndividual[]::new);
		OENode.allNegativeExamples = allNegativeExamples.toArray(OWLIndividual[]::new);

        Map<OWLIndividual, Integer> positiveExamplesIndices = new TreeMap<>();
        Map<OWLIndividual, Integer> negativeExamplesIndices = new TreeMap<>();

        int ind = 0;
        for (OWLIndividual ex : OENode.allPositiveExamples) {
            positiveExamplesIndices.put(ex, ind);
            ind++;
        }

        ind = 0;
        for (OWLIndividual ex : OENode.allNegativeExamples) {
            negativeExamplesIndices.put(ex, ind);
            ind++;
        }

        OENode.positiveExamplesIndices = positiveExamplesIndices;
        OENode.negativeExamplesIndices = negativeExamplesIndices;

		useCompactedCoverage = true;
	}
}