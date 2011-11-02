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

final class Lanczos3Filter implements ResampleFilter {

	private double sincModified(double value)
	{
		return Math.sin(value) / value;
	}

	public final double apply(double value)
	{
		if (value==0){
			return 1.0;
		}
		if (value < 0.0)
		{
			value = -value;
		}

		if (value < 3.0)
		{
			value *= Math.PI;
			return sincModified(value) * sincModified(value / 3.0);
		}
		else
		{
			return 0.0;
		}
	}

    public double getSamplingRadius() {
        return 3.0;
    }

    public String getName()
	{
		return "Lanczos3";
	}
}
