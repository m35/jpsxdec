/*
 * Copyright 2009, Morten Nobel-Joergensen
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.mortennobel.imagescaling;

/**
 * The Mitchell resample filter.
 */
final class MitchellFilter implements ResampleFilter
{
	private static final double B = 1.0 / 3.0;
	private static final double C = 1.0 / 3.0;

	public double getSamplingRadius() {
		return 2.0;
	}

	public final double apply(double value)
	{
		if (value < 0.0)
		{
			value = -value;
		}
		double tt = value * value;
		if (value < 1.0)
		{
			value = (((12.0 - 9.0 * B - 6.0 * C) * (value * tt))
			+ ((-18.0 + 12.0 * B + 6.0 * C) * tt)
			+ (6.0 - 2.0 * B));
			return value / 6.0;
		}
		else
		if (value < 2.0)
		{
			value = (((-1.0 * B - 6.0 * C) * (value * tt))
			+ ((6.0 * B + 30.0 * C) * tt)
			+ ((-12.0 * B - 48.0 * C) * value)
			+ (8.0 * B + 24 * C));
			return value / 6.0;
		}
		else
		{
			return 0.0;
		}
	}

	public String getName() {
		return "BSpline";
	}
}

