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
 * @author Heinz Doerr
 */
class BiCubicFilter implements ResampleFilter {

	final protected double a;

	public BiCubicFilter() {
		a= -0.5;
	}

	protected BiCubicFilter(double a) {
		this.a= a;
	}

	public final double apply(double value) {
		if (value == 0)
			return 1.0;
		if (value < 0.0)
			value = -value;
		double vv= value * value;
		if (value < 1.0) {
			return (a + 2.0) * vv * value - (a + 3.0) * vv + 1.0;
		}
		if (value < 2.0) {
			return a * vv * value - 5 * a * vv + 8 * a * value - 4 * a;
		}
		return 0.0;
	}

    public double getSamplingRadius() {
        return 2.0;
    }

    public String getName()
	{
		return "BiCubic"; // also called cardinal cubic spline
	}
}
