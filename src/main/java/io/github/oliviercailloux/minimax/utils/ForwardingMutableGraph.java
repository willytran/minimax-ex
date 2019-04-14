package io.github.oliviercailloux.minimax.utils;

import java.util.Set;

import com.google.common.graph.ElementOrder;
import com.google.common.graph.EndpointPair;
import com.google.common.graph.MutableGraph;

public class ForwardingMutableGraph<N> implements MutableGraph<N> {
	public static <N> ForwardingMutableGraph<N> using(MutableGraph<N> delegate) {
		return new ForwardingMutableGraph<>(delegate);
	}

	private MutableGraph<N> delegate;

	protected ForwardingMutableGraph(MutableGraph<N> delegate) {
		this.delegate = delegate;
	}

	@Override
	public Set<N> nodes() {
		return delegate.nodes();
	}

	@Override
	public Set<EndpointPair<N>> edges() {
		return delegate.edges();
	}

	@Override
	public boolean isDirected() {
		return delegate.isDirected();
	}

	@Override
	public boolean allowsSelfLoops() {
		return delegate.allowsSelfLoops();
	}

	@Override
	public ElementOrder<N> nodeOrder() {
		return delegate.nodeOrder();
	}

	@Override
	public Set<N> adjacentNodes(N node) {
		return delegate.adjacentNodes(node);
	}

	@Override
	public Set<N> predecessors(N node) {
		return delegate.predecessors(node);
	}

	@Override
	public Set<N> successors(N node) {
		return delegate.successors(node);
	}

	@Override
	public Set<EndpointPair<N>> incidentEdges(N node) {
		return delegate.incidentEdges(node);
	}

	@Override
	public int degree(N node) {
		return delegate.degree(node);
	}

	@Override
	public int inDegree(N node) {
		return delegate.inDegree(node);
	}

	@Override
	public int outDegree(N node) {
		return delegate.outDegree(node);
	}

	@Override
	public boolean hasEdgeConnecting(N nodeU, N nodeV) {
		return delegate.hasEdgeConnecting(nodeU, nodeV);
	}

	@Override
	public boolean addNode(N node) {
		return delegate.addNode(node);
	}

	@Override
	public boolean putEdge(N nodeU, N nodeV) {
		return delegate.putEdge(nodeU, nodeV);
	}

	@Override
	public boolean removeNode(N node) {
		return delegate.removeNode(node);
	}

	@Override
	public boolean removeEdge(N nodeU, N nodeV) {
		return delegate.removeEdge(nodeU, nodeV);
	}
}
