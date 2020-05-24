package io.github.oliviercailloux.minimax.strategies;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableSet;
import com.google.common.graph.EndpointPair;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.ImmutableGraph;

import io.github.oliviercailloux.y2018.j_voting.Alternative;

public class StrategyHelperTests {
	@Test
	void testGetIncomparablePairs() throws Exception {
		final ImmutableGraph.Builder<Alternative> builder = GraphBuilder.directed().<Alternative>immutable();
		final Alternative a1 = new Alternative(1);
		final Alternative a2 = new Alternative(2);
		final Alternative a3 = new Alternative(3);
		final Alternative a4 = new Alternative(4);
		builder.putEdge(a1, a2);
		builder.putEdge(a1, a3);
		builder.putEdge(a2, a4);
		builder.putEdge(a3, a4);
		builder.putEdge(a1, a4);
		final ImmutableGraph<Alternative> graph = builder.build();
		assertEquals(ImmutableSet.of(EndpointPair.unordered(a2, a3)), StrategyHelper.getIncomparablePairs(graph));
	}
}
