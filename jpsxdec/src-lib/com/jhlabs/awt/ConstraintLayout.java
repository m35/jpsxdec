/* 
 * Copyright (C) Jerry Huxtable 1998-2001. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jhlabs.awt;

import java.awt.*;
import java.util.*;

/**
 * A base class for layouts which simplifies the business of building new
 * layouts with constraints.
 */
public class ConstraintLayout implements LayoutManager2 {

	protected final static int PREFERRED = 0;
	protected final static int MINIMUM = 1;
	protected final static int MAXIMUM = 2;

	protected int hMargin = 0;
	protected int vMargin = 0;
	private Hashtable<Component, Object> constraints;
	protected boolean includeInvisible = false;

	public void addLayoutComponent(String constraint, Component c) {
		setConstraint(c, constraint);
	}

	public void addLayoutComponent(Component c, Object constraint) {
		setConstraint(c, constraint);
	}

	public void removeLayoutComponent(Component c) {
		if (constraints != null)
			constraints.remove(c);
	}

	public void setConstraint(Component c, Object constraint) {
		if (constraint != null) {
			if (constraints == null)
				constraints = new Hashtable<Component, Object>();
			constraints.put(c, constraint);
		} else if (constraints != null)
			constraints.remove(c);
	}
	
	public Object getConstraint(Component c) {
		if (constraints != null)
			return constraints.get(c);
		return null;
	}
	
	public void setIncludeInvisible(boolean includeInvisible) {
		this.includeInvisible = includeInvisible;
	}

	public boolean getIncludeInvisible() {
		return includeInvisible;
	}

	protected boolean includeComponent(Component c) {
		return includeInvisible || c.isVisible();
	}
	
	public Dimension minimumLayoutSize (Container target) {
		return calcLayoutSize(target, MINIMUM);
	}
	
	public Dimension maximumLayoutSize (Container target) {
		return calcLayoutSize(target, MAXIMUM);
	}
	
	public Dimension preferredLayoutSize (Container target) {
		return calcLayoutSize(target, PREFERRED);
	}
	
	public Dimension calcLayoutSize (Container target, int type) {
		Dimension dim = new Dimension(0, 0);
		measureLayout(target, dim, type);
		Insets insets = target.getInsets();
		dim.width += insets.left + insets.right + 2*hMargin;
		dim.height += insets.top + insets.bottom + 2*vMargin;
		return dim;
	}

	public void invalidateLayout(Container target) {
	}
	
	public float getLayoutAlignmentX(Container parent) {
		return 0.5f;
	}

	public float getLayoutAlignmentY(Container parent) {
		return 0.5f;
	}

	public int getHMargin() {
		return hMargin;
	}

	public int getVMargin() {
		return vMargin;
	}

	public void layoutContainer(Container target)  {
		measureLayout(target, null, PREFERRED);
	}
	
	public void measureLayout(Container target, Dimension dimension, int type)  {
	}

	protected Dimension getComponentSize(Component c, int type) {
		if (type == MINIMUM)
			return c.getMinimumSize();
		if (type == MAXIMUM)
			return c.getMaximumSize();
		return c.getPreferredSize();
	}
}
