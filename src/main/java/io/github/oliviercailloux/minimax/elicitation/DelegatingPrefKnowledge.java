package io.github.oliviercailloux.minimax.elicitation;

import org.apfloat.Aprational;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Range;
import com.google.common.graph.Graph;
import com.google.common.graph.Graphs;
import com.google.common.graph.ImmutableGraph;
import com.google.common.graph.MutableGraph;

import io.github.oliviercailloux.j_voting.Alternative;
import io.github.oliviercailloux.j_voting.Voter;
import io.github.oliviercailloux.j_voting.VoterPartialPreference;
import io.github.oliviercailloux.jlp.elements.ComparisonOperator;

public class DelegatingPrefKnowledge implements PreferenceKnowledge {

	private PrefKnowledgeImpl prefKnowledge;
	private PreferenceInformation newInformation;

	public static DelegatingPrefKnowledge given(PrefKnowledgeImpl prefKnowledge, PreferenceInformation newInfo) {
		return new DelegatingPrefKnowledge(prefKnowledge, newInfo);
	}

	public static DelegatingPrefKnowledge copyOf(DelegatingPrefKnowledge delKnowledge) {
		return new DelegatingPrefKnowledge(delKnowledge.prefKnowledge, delKnowledge.newInformation);
	}

	private DelegatingPrefKnowledge(PrefKnowledgeImpl knowledge, PreferenceInformation newInfo) {
		prefKnowledge = knowledge;
		newInformation = newInfo;
	}

	public ImmutableSet<Alternative> getAlternatives() {
		return prefKnowledge.getAlternatives();
	}

	public ImmutableSet<Voter> getVoters() {
		return prefKnowledge.getVoters();
	}

	public ImmutableMap<Voter, VoterPartialPreference> getProfile() {
		if (newInformation.getType() == QuestionType.COMMITTEE_QUESTION)
			return prefKnowledge.getProfile();

		final ImmutableMap.Builder<Voter, VoterPartialPreference> builder = ImmutableMap.builder();
		for (Voter voter : prefKnowledge.getVoters()) {
			builder.put(voter, getPartialPreference(voter));
		}
		return builder.build();
	}

	@Override
	public VoterPartialPreference getPartialPreference(Voter voter) {
		if (newInformation.getType() == QuestionType.COMMITTEE_QUESTION)
			return prefKnowledge.getPartialPreference(voter);

		VoterPreferenceInformation newVotPref = newInformation.asVoterInformation();
		if (!voter.equals(newVotPref.getVoter()))
			return prefKnowledge.getPartialPreference(voter);

		VoterPartialPreference partialPref = VoterPartialPreference.copyOf(prefKnowledge.getPartialPreference(voter));
		partialPref.asGraph().putEdge(newVotPref.getBetterAlternative(), newVotPref.getWorstAlternative());
		return partialPref;
	}

	@Override
	public ConstraintsOnWeights getConstraintsOnWeights() {
		// TODO
		return prefKnowledge.getConstraintsOnWeights();
	}

	@Override
	public Range<Aprational> getLambdaRange(int rank) {
		// TODO
		return prefKnowledge.getLambdaRange(rank);
	}

	@Override
	public boolean isProfileComplete() {
		if (newInformation.getType() == QuestionType.COMMITTEE_QUESTION)
			return prefKnowledge.isProfileComplete();
		
		int m = prefKnowledge.getAlternatives().size();
		VoterPreferenceInformation newVotPref = newInformation.asVoterInformation();	
		
		for (Voter voter : prefKnowledge.getProfile().keySet()) {
			Graph<Alternative> graph;
			if (voter.equals(newVotPref.getVoter())) {
				MutableGraph<Alternative> tempGraph = Graphs.copyOf(prefKnowledge.getPartialPreference(voter).asGraph());
				tempGraph.putEdge(newVotPref.getBetterAlternative(), newVotPref.getWorstAlternative());
				tempGraph = (MutableGraph<Alternative>) Graphs.transitiveClosure(tempGraph);
				graph = ImmutableGraph.copyOf(tempGraph);
			} else {
				graph = prefKnowledge.getPartialPreference(voter).asTransitiveGraph();
			}
			if (graph.edges().size() != m * (m - 1)/2) {
				return false;
			}
		}
		return true;
	}

	@Override
	public void addConstraint(int rank, ComparisonOperator op, Aprational lambda) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void update(PreferenceInformation information) {
		// TODO Auto-generated method stub
		
	}

}
