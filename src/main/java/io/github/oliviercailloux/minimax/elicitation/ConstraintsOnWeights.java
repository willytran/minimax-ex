package io.github.oliviercailloux.minimax.elicitation;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import java.util.LinkedList;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Range;

import io.github.oliviercailloux.jlp.elements.ComparisonOperator;
import io.github.oliviercailloux.jlp.elements.Constraint;
import io.github.oliviercailloux.jlp.elements.Objective;
import io.github.oliviercailloux.jlp.elements.RangeOfDouble;
import io.github.oliviercailloux.jlp.elements.Sense;
import io.github.oliviercailloux.jlp.elements.SumTerms;
import io.github.oliviercailloux.jlp.elements.SumTermsBuilder;
import io.github.oliviercailloux.jlp.elements.Term;
import io.github.oliviercailloux.jlp.elements.Variable;
import io.github.oliviercailloux.jlp.elements.VariableDomain;
import io.github.oliviercailloux.jlp.mp.IMP;
import io.github.oliviercailloux.jlp.mp.MP;
import io.github.oliviercailloux.jlp.mp.MPBuilder;
import io.github.oliviercailloux.jlp.or_tools.OrToolsSolver;
import io.github.oliviercailloux.jlp.result.Result;
import io.github.oliviercailloux.jlp.result.ResultStatus;
import io.github.oliviercailloux.jlp.result.Solution;
import io.github.oliviercailloux.minimax.utils.Rounder;

/**
 *
 * The weight of rank 1 is 1. If there is more than one rank, the weight of
 * lowest rank is 0.
 *
 * Because of imprecision in linear programming optimization, we could end up
 * with weights that are non convex up to a very minor error. This is solved by
 * adding a small epsilon ({@link ConstraintsOnWeights#EPSILON}) to the
 * constraints.
 *
 * Because of this epsilon constraint, the weights that are returned are not
 * permitted to be equal. TODO is this a problem?
 *
 * @author Olivier Cailloux
 *
 */
public class ConstraintsOnWeights {
	private MPBuilder builder;
	private OrToolsSolver solver;
	private Solution lastSolution;
	private boolean convexityConstraintSet;
	public static final double EPSILON = 1e-5;
	public Rounder rounder;

	/**
	 * @param m at least one: the number of ranks, or equivalently, the number of
	 *          alternatives.
	 */
	public static ConstraintsOnWeights withRankNumber(int m) {
		return new ConstraintsOnWeights(m);
	}

	public static ConstraintsOnWeights copyOf(ConstraintsOnWeights cw) {
		ConstraintsOnWeights c = new ConstraintsOnWeights(cw.getM());
		c.builder = MPBuilder.copyOf(cw.getBuilder());
		return c;
	}

	private MPBuilder getBuilder() {
		return builder;
	}

	public void setRounder(Rounder r) {
		rounder = r;
	}

	ConstraintsOnWeights(int m) {
		checkArgument(m >= 1);
		builder = MP.builder();
		builder.addVariable(
				Variable.of("w", VariableDomain.REAL_DOMAIN, RangeOfDouble.closed(1d, 1d), ImmutableSet.of(1)));
		for (int rank = 2; rank < m; ++rank) {
			builder.addVariable(
					Variable.of("w", VariableDomain.REAL_DOMAIN, RangeOfDouble.ZERO_ONE_RANGE, ImmutableSet.of(rank)));
		}
		if (m >= 2) {
			builder.addVariable(
					Variable.of("w", VariableDomain.REAL_DOMAIN, RangeOfDouble.closed(0d, 0d), ImmutableSet.of(m)));
		}
		solver = new OrToolsSolver();
		lastSolution = null;
		convexityConstraintSet = false;
		rounder = Rounder.given(Rounder.Mode.NULL, 0);
	}

	/**
	 * Adds the constraint: (w_i − w_{i+1}) OP λ (w_{i+1} − w_{i+2}).
	 *
	 * @param i      1 ≤ i ≤ m-2.
	 * @param op     the operator.
	 * @param lambda a finite double.
	 */
	void addConstraint(int i, ComparisonOperator op, double lambda) {
		checkArgument(i >= 1);
		checkArgument(i <= getM() - 2);
		checkArgument(Double.isFinite(lambda));

		final SumTermsBuilder sumBuilder = SumTerms.builder();
		sumBuilder.addTerm(1, getVariable(i));
		sumBuilder.addTerm(-lambda - 1d, getVariable(i + 1));
		sumBuilder.addTerm(lambda, getVariable(i + 2));
		final Constraint cst = Constraint.of(sumBuilder.build(), op, 0d);
		builder.addConstraint(cst);
	}

	/**
	 * May be called only once.
	 */
	public void setConvexityConstraint() {
		checkState(!convexityConstraintSet);
		for (int rank = 1; rank <= getM() - 2; ++rank) {
			builder.addConstraint(Constraint.of("Convexity rank " + rank,
					SumTerms.of(1d, getVariable(rank), -2d, getVariable(rank + 1), 1d, getVariable(rank + 2)),
					ComparisonOperator.GE, EPSILON));
		}
		convexityConstraintSet = true;
	}

	public Range<Double> getWeightRange(int rank) {
		checkArgument(rank >= 1);
		checkArgument(rank <= getM());

		return boundObjective(SumTerms.of(1d, getVariable(rank)));
	}

	/**
	 * @return at least one.
	 */
	public int getM() {
		return builder.getVariables().size();
	}

	public Term getTerm(double coefficient, int rank) {
		return Term.of(coefficient, getVariable(rank));
	}

	public double maximize(SumTerms sum) {
		final Objective obj = Objective.max(sum);
		return optimize(obj);
	}

	private double optimize(Objective obj) {
		builder.setObjective(obj);
		final Result result = solver.solve(builder);
		checkArgument(result.getResultStatus().equals(ResultStatus.OPTIMAL));
		lastSolution = result.getSolution().get();
		return lastSolution.getObjectiveValue();
	}

	public double minimize(SumTerms sum) {
		final Objective obj;
		if (sum.size() == 0) {
			obj = Objective.ZERO;
		} else {
			obj = Objective.min(sum);
		}
		return optimize(obj);
	}

	private Variable getVariable(int rank) {
		checkArgument(rank >= 1);
		checkArgument(rank <= getM());
		return builder.getVariable(getVariableDescription(rank));
	}

	private String getVariableDescription(int rank) {
		return Variable.getDefaultDescription("w", ImmutableList.of(rank));
	}

	private double bound(IMP mp) {
		final double bound;

		final Result result = solver.solve(mp);
		switch (result.getResultStatus()) {
		case INFEASIBLE:
		case MEMORY_LIMIT_REACHED:
		case TIME_LIMIT_REACHED:
			throw new IllegalStateException();
		case UNBOUNDED:
			if (mp.getObjective().getSense() == Sense.MAX) {
				bound = Double.POSITIVE_INFINITY;
			} else {
				bound = Double.NEGATIVE_INFINITY;
			}
			break;
		case OPTIMAL:
			bound = result.getSolution().get().getObjectiveValue();
			break;
		default:
			throw new AssertionError();
		}
		return bound;
	}

	private Range<Double> boundObjective(SumTerms objectiveFunction) {
		builder.setObjective(Objective.min(objectiveFunction));
		final double lBound = bound(builder);

		builder.setObjective(Objective.max(objectiveFunction));
		final double uBound = bound(builder);

		return RangeOfDouble.using(lBound, uBound);
	}

	public String rangesAsString() {
		StringBuilder sb = new StringBuilder();
		for (int i = 1; i <= builder.getVariables().size(); i++) {
			sb.append("Rank " + i + " ");
			sb.append(getWeightRange(i).toString());
			sb.append("\n");
		}
		return sb.toString();
	}

	public PSRWeights getLastSolution() {
		/** PSRWeights only accept convex weights. */
		checkState(convexityConstraintSet);
		final List<Double> weights = new LinkedList<Double>();
		for (int r = 1; r <= getM(); ++r) {
			final double value = rounder.round(lastSolution.getValue(getVariable(r)));
			weights.add(value);
		}
		return PSRWeights.given(weights);
	}
}
