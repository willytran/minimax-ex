package io.github.oliviercailloux.minimax.utils;

import java.math.BigDecimal;

/**
 * The Rounding class provides a method to round a double value, given the
 * rounding mode and the scale. It uses the BigDecimal rounding methods. If the
 * rounding mode is NULL then the value is not rounded.
 */

public class Rounder {

	public static enum Mode {
		ROUND_CEILING, ROUND_DOWN, ROUND_FLOOR, ROUND_HALF_DOWN, ROUND_HALF_EVEN, ROUND_HALF_UP, ROUND_UP, NULL
	}

	private Mode mode;
	private int granularity;

	public static Rounder given(Mode roundingMode, int decimalPlaces) {
		return new Rounder(roundingMode, decimalPlaces);
	}

	private Rounder(Mode roundingMode, int decimalPlaces) {
		mode = roundingMode;
		granularity = decimalPlaces;
	}

	public double round(double value) {
		switch (mode) {
		case ROUND_CEILING:
			return BigDecimal.valueOf(value).setScale(granularity, BigDecimal.ROUND_CEILING).doubleValue();
		case ROUND_DOWN:
			return BigDecimal.valueOf(value).setScale(granularity, BigDecimal.ROUND_DOWN).doubleValue();
		case ROUND_FLOOR:
			return BigDecimal.valueOf(value).setScale(granularity, BigDecimal.ROUND_FLOOR).doubleValue();
		case ROUND_HALF_DOWN:
			return BigDecimal.valueOf(value).setScale(granularity, BigDecimal.ROUND_HALF_DOWN).doubleValue();
		case ROUND_HALF_EVEN:
			return BigDecimal.valueOf(value).setScale(granularity, BigDecimal.ROUND_HALF_EVEN).doubleValue();
		case ROUND_HALF_UP:
			return BigDecimal.valueOf(value).setScale(granularity, BigDecimal.ROUND_HALF_UP).doubleValue();
		case ROUND_UP:
			return BigDecimal.valueOf(value).setScale(granularity, BigDecimal.ROUND_UP).doubleValue();
		case NULL:
			return value;
		default:
			throw new IllegalStateException();
		}
	}

}
