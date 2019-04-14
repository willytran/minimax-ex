package io.github.oliviercailloux.minimax;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashSet;
import java.util.Set;

import org.apfloat.Apint;
import org.apfloat.Aprational;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.MutableGraph;

import io.github.oliviercailloux.jlp.elements.ComparisonOperator;
import io.github.oliviercailloux.minimax.Regret;
import io.github.oliviercailloux.minimax.elicitation.PrefKnowledge;
import io.github.oliviercailloux.y2018.j_voting.Alternative;
import io.github.oliviercailloux.y2018.j_voting.Generator;
import io.github.oliviercailloux.y2018.j_voting.Voter;

public class RegretTest {

	@Test
	void testMMR() throws Exception {
		Voter v1 = new Voter(1);
		Voter v2 = new Voter(2);
		Voter v3 = new Voter(3);
		Set<Voter> voters = new HashSet<Voter>();
		voters.add(v1);
		voters.add(v2);
		voters.add(v3);

		Alternative a = new Alternative(1);
		Alternative b = new Alternative(2);
		Alternative c = new Alternative(3);
		Alternative d = new Alternative(4);
		Set<Alternative> alt = new HashSet<Alternative>();
		alt.add(a);
		alt.add(b);
		alt.add(c);
		alt.add(d);

		PrefKnowledge knowledge = PrefKnowledge.given(alt, voters);

		MutableGraph<Alternative> pref1 = knowledge.getProfile().get(v1).asGraph();
		pref1.putEdge(a, b);
		pref1.putEdge(b, c);
		pref1.putEdge(c, d);

		MutableGraph<Alternative> pref2 = knowledge.getProfile().get(v2).asGraph();
		pref2.putEdge(d, a);
		pref2.putEdge(a, b);
		pref2.putEdge(b, c);

		MutableGraph<Alternative> pref3 = knowledge.getProfile().get(v3).asGraph();
		pref3.putEdge(c, a);
		pref3.putEdge(a, b);
		pref3.putEdge(b, d);

		assertEquals(ImmutableList.of(a), Regret.getMMRAlternatives(knowledge));
	}

	@Test
	void testPMR1() throws Exception {
		/** Test with complete knowledge about voters' preferences **/
		Voter v1 = new Voter(1);
		Voter v2 = new Voter(2);
		Voter v3 = new Voter(3);
		Set<Voter> voters = new HashSet<Voter>();
		voters.add(v1);
		voters.add(v2);
		voters.add(v3);

		Alternative a = new Alternative(1);
		Alternative b = new Alternative(2);
		Alternative c = new Alternative(3);
		Alternative d = new Alternative(4);
		Set<Alternative> alt = new HashSet<Alternative>();
		alt.add(a);
		alt.add(b);
		alt.add(c);
		alt.add(d);

		PrefKnowledge knowledge = PrefKnowledge.given(alt, voters);

		MutableGraph<Alternative> pref1 = knowledge.getProfile().get(v1).asGraph();
		pref1.putEdge(a, b);
		pref1.putEdge(b, c);
		pref1.putEdge(c, d);

		MutableGraph<Alternative> pref2 = knowledge.getProfile().get(v2).asGraph();
		pref2.putEdge(d, a);
		pref2.putEdge(a, b);
		pref2.putEdge(b, c);

		MutableGraph<Alternative> pref3 = knowledge.getProfile().get(v3).asGraph();
		pref3.putEdge(c, a);
		pref3.putEdge(a, b);
		pref3.putEdge(b, d);

		/** changed the visibility of the method in class Regret **/
//		assertEquals(-1d,Regret.getPMR(a, b, knowledge));
//		assertEquals(0d,Regret.getPMR(a, c, knowledge));
//		assertEquals(0d,Regret.getPMR(a, d, knowledge));
	}

	@Test
	void testPMR2() throws Exception {
		/** Test with zero knowledge **/
		Voter v1 = new Voter(1);
		Voter v2 = new Voter(2);
		Voter v3 = new Voter(3);
		Set<Voter> voters = new HashSet<Voter>();
		voters.add(v1);
		voters.add(v2);
		voters.add(v3);

		Alternative a = new Alternative(1);
		Alternative b = new Alternative(2);
		Alternative c = new Alternative(3);
		Alternative d = new Alternative(4);
		Set<Alternative> alt = new HashSet<Alternative>();
		alt.add(a);
		alt.add(b);
		alt.add(c);
		alt.add(d);

		PrefKnowledge knowledge = PrefKnowledge.given(alt, voters);
		/** changed the visibility of the method in class Regret **/
		assertEquals(3d, Regret.getPMR(a, b, knowledge));
	}

	@Test
	void testPMR3() throws Exception {
		/** Test with knowledge about weights **/
		Voter v1 = new Voter(1);
		Voter v2 = new Voter(2);
		Voter v3 = new Voter(3);
		Set<Voter> voters = new HashSet<Voter>();
		voters.add(v1);
		voters.add(v2);
		voters.add(v3);

		Alternative a = new Alternative(1);
		Alternative b = new Alternative(2);
		Alternative c = new Alternative(3);
		Alternative d = new Alternative(4);
		Set<Alternative> alt = new HashSet<Alternative>();
		alt.add(a);
		alt.add(b);
		alt.add(c);
		alt.add(d);

		PrefKnowledge knowledge = PrefKnowledge.given(alt, voters);
		Apint ap2 = new Apint(2);
		Apint ap3 = new Apint(3);
		Aprational lambda = new Aprational(ap3, ap2);
		knowledge.addConstraint(1, ComparisonOperator.GE, lambda);
		knowledge.addConstraint(2, ComparisonOperator.GE, lambda);

		MutableGraph<Alternative> pref1 = knowledge.getProfile().get(v1).asGraph();
		pref1.putEdge(c, b);

		MutableGraph<Alternative> pref2 = knowledge.getProfile().get(v2).asGraph();
		pref2.putEdge(c, b);

		MutableGraph<Alternative> pref3 = knowledge.getProfile().get(v3).asGraph();
		pref3.putEdge(c, b);

		/** changed the visibility of the method in class Regret **/
//		double PMR = Regret.getPMR(a, b, knowledge);
//		double w2= PMR/3;
//		System.out.println(PMR + "  "+ w2);
//		assertTrue(w2<=0.55);
//		assertTrue(w2>=0.45);
	}

	@Test
	void testRanksCase1() throws Exception {
		Voter v1 = new Voter(1);
		Set<Voter> voters = new HashSet<Voter>();
		voters.add(v1);

		Alternative x = new Alternative(1);
		Alternative y = new Alternative(2);

		Alternative a = new Alternative(3);
		Alternative u = new Alternative(4);
		Alternative c = new Alternative(5);
		Alternative b = new Alternative(6);
		Alternative d = new Alternative(7);
		Alternative f = new Alternative(8);
		Alternative a1 = new Alternative(9);
		Alternative b1 = new Alternative(10);
		Alternative c1 = new Alternative(11);
		Alternative d1 = new Alternative(12);
		Alternative u1 = new Alternative(13);

		Set<Alternative> alt = new HashSet<Alternative>();
		alt.add(a);
		alt.add(b);
		alt.add(c);
		alt.add(d);
		alt.add(x);
		alt.add(y);
		alt.add(u);
		alt.add(f);
		alt.add(a1);
		alt.add(b1);
		alt.add(c1);
		alt.add(d1);
		alt.add(u1);

		PrefKnowledge knowledge = PrefKnowledge.given(alt, voters);

		MutableGraph<Alternative> pref = knowledge.getProfile().get(v1).asGraph();

		pref.putEdge(a, x);
		pref.putEdge(x, b);
		pref.putEdge(x, d);
		pref.putEdge(b, y);
		pref.putEdge(c, y);
		pref.putEdge(y, f);
		pref.addNode(u);
		pref.putEdge(a1, a);
		pref.putEdge(c1, c);
		pref.putEdge(x, d1);
		pref.putEdge(b1, y);
		pref.putEdge(b, b1);
		pref.putEdge(a1, u1);

		/** changed the visibility of the method in class Regret **/
		assertEquals(7, Regret.getWorstRanks(x, y, knowledge.getPartialPreference(v1))[0]);
		assertEquals(10, Regret.getWorstRanks(x, y, knowledge.getPartialPreference(v1))[1]);
	}

	@Test
	void testRanksCase2() throws Exception {
		final MutableGraph<Alternative> pref = GraphBuilder.directed().build();
		Alternative x = new Alternative(1);
		Alternative y = new Alternative(2);

		Alternative a = new Alternative(3);
		Alternative u = new Alternative(4);
		Alternative c = new Alternative(5);
		Alternative b = new Alternative(6);
		Alternative d = new Alternative(7);
		Alternative f = new Alternative(8);

		pref.putEdge(a, y);
		pref.putEdge(y, b);
		pref.putEdge(y, d);
		pref.putEdge(b, x);
		pref.putEdge(c, x);
		pref.putEdge(x, f);
		pref.addNode(u);

		/** changed the visibility of the method in class Regret **/
		// assertEquals(7,Regret.getWorstRanks(x, y, pref)[0]);
		// assertEquals(2,Regret.getWorstRanks(x, y, pref)[1]);
	}

	@Test
	void newTestRanksCase2() throws Exception {
		final MutableGraph<Alternative> pref = GraphBuilder.directed().build();
		Alternative x = new Alternative(1);
		Alternative y = new Alternative(2);

		Alternative a = new Alternative(3);
		Alternative u = new Alternative(4);
		Alternative c = new Alternative(5);
		Alternative b = new Alternative(6);
		Alternative d = new Alternative(7);
		Alternative f = new Alternative(8);
		Alternative a1 = new Alternative(9);
		Alternative b1 = new Alternative(10);
		Alternative c1 = new Alternative(11);
		Alternative d1 = new Alternative(12);
		Alternative u1 = new Alternative(13);

		pref.putEdge(a, y);
		pref.putEdge(y, b);
		pref.putEdge(y, d);
		pref.putEdge(b, x);
		pref.putEdge(c, x);
		pref.putEdge(x, f);
		pref.addNode(u);
		pref.putEdge(a1, a);
		pref.putEdge(c1, c);
		pref.putEdge(x, d1);
		pref.putEdge(b1, x);
		pref.putEdge(b, b1);
		pref.putEdge(a1, u1);

		/** changed the visibility of the method in class Regret **/
		// assertEquals(11,Regret.getWorstRanks(x, y, pref)[0]);
		// assertEquals(3,Regret.getWorstRanks(x, y, pref)[1]);
	}

	@Test
	void testRanksCase3() throws Exception {
		final MutableGraph<Alternative> pref = GraphBuilder.directed().build();
		Alternative x = new Alternative(1);
		Alternative y = new Alternative(2);

		Alternative a = new Alternative(3);
		Alternative u = new Alternative(4);
		Alternative c = new Alternative(5);
		Alternative b = new Alternative(6);
		Alternative d = new Alternative(7);
		Alternative f = new Alternative(8);

		pref.addNode(x);
		pref.addNode(y);
		pref.addNode(a);
		pref.addNode(c);
		pref.addNode(u);
		pref.addNode(d);
		pref.addNode(f);
		pref.addNode(b);

		/** changed the visibility of the method in class Regret **/
		// assertEquals(8,Regret.getWorstRanks(x, y, pref)[0]);
		// assertEquals(1,Regret.getWorstRanks(x, y, pref)[1]);
	}

	@Test
	void testTau() throws Exception {
		final PrefKnowledge k = PrefKnowledge.given(Generator.getAlternatives(3), Generator.getVoters(3));
		k.getProfile().get(new Voter(1)).asGraph().putEdge(new Alternative(1), new Alternative(2));
		k.getProfile().get(new Voter(1)).asGraph().putEdge(new Alternative(2), new Alternative(3));
		k.getProfile().get(new Voter(2)).asGraph().putEdge(new Alternative(1), new Alternative(2));
		k.getProfile().get(new Voter(3)).asGraph().putEdge(new Alternative(3), new Alternative(1));

		assertTrue(Regret.getTau1(k) == 0.5);
		assertTrue(Regret.getTau2(k) == -2);
		Regret.getMMRAlternatives(k);
		System.out.println(Regret.getMMR());
//		assertTrue(Regret.tau1SmallerThanTau2(k));
	}

}
