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
 * A B-spline resample filter.
 */
final class BSplineFilter implements ResampleFilter
{
	public double getSamplingRadius() {
		return 2.0;
	}

	public final double apply(double value)
	{
		if (value < 0.0)
		{
			value = - value;
		}
		if (value < 1.0)
		{
			double tt = value * value;
			return 0.5 * tt * value - tt + (2.0 / 3.0);
		}
		else
		if (value < 2.0)
		{
			value = 2.0 - value;
			return (1.0 / 6.0) * value * value * value;
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
