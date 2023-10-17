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

public class ParagraphLayout extends ConstraintLayout {

	public final static int TYPE_MASK = 0x03;
	public final static int STRETCH_H_MASK = 0x04;
	public final static int STRETCH_V_MASK = 0x08;

	public final static int NEW_PARAGRAPH_VALUE = 1;
	public final static int NEW_PARAGRAPH_TOP_VALUE = 2;
	public final static int NEW_LINE_VALUE = 3;

	public final static Integer NEW_PARAGRAPH = Integer.valueOf(0x01);
	public final static Integer NEW_PARAGRAPH_TOP = Integer.valueOf(0x02);
	public final static Integer NEW_LINE = Integer.valueOf(0x03);
	public final static Integer STRETCH_H = Integer.valueOf(0x04);
	public final static Integer STRETCH_V = Integer.valueOf(0x08);
	public final static Integer STRETCH_HV = Integer.valueOf(0x0c);
	public final static Integer NEW_LINE_STRETCH_H = Integer.valueOf(0x07);
	public final static Integer NEW_LINE_STRETCH_V = Integer.valueOf(0x0b);
	public final static Integer NEW_LINE_STRETCH_HV = Integer.valueOf(0x0f);

	protected int hGapMajor, vGapMajor;
	protected int hGapMinor, vGapMinor;
	protected int rows;
	protected int colWidth1;
	protected int colWidth2;

	public ParagraphLayout() {
		this(10, 10, 12, 11, 4, 4);
	}
	
	public ParagraphLayout(int hMargin, int vMargin, int hGapMajor, int vGapMajor, int hGapMinor, int vGapMinor) {
		this.hMargin = hMargin;
		this.vMargin = vMargin;
		this.hGapMajor = hGapMajor;
		this.vGapMajor = vGapMajor;
		this.hGapMinor = hGapMinor;
		this.vGapMinor = vGapMinor;
	}
	
	public void measureLayout(Container target, Dimension dimension, int type)  {
		int count = target.getComponentCount();
		if (count > 0) {
			Insets insets = target.getInsets();
			Dimension size = target.getSize();
			int x = 0;
			int y = 0;
			int rowHeight = 0;
			int colWidth = 0;
			int numRows = 0;
			boolean lastWasParagraph = false;

			Dimension[] sizes = new Dimension[count];

			colWidth1 = colWidth2 = 0;

			// First pass: work out the column widths and row heights
			for (int i = 0; i < count; i++) {
				Component c = target.getComponent(i);
				if (includeComponent(c)) {
					Dimension d = getComponentSize(c, type);
					int w = d.width;
					int h = d.height;
					sizes[i] = d;
					Integer n = (Integer)getConstraint(c);

					if (i == 0 || n == NEW_PARAGRAPH || n == NEW_PARAGRAPH_TOP) {
						if (i != 0)
							y += rowHeight+vGapMajor;
						colWidth1 = Math.max(colWidth1, w);
						colWidth = 0;
						rowHeight = 0;
						lastWasParagraph = true;
					} else if (n == NEW_LINE || lastWasParagraph) {
						x = 0;
						if (!lastWasParagraph && i != 0)
							y += rowHeight+vGapMinor;
						colWidth = w;
						colWidth2 = Math.max(colWidth2, colWidth);
						if (!lastWasParagraph)
							rowHeight = 0;
						lastWasParagraph = false;
					} else {
						colWidth += w+hGapMinor;
						colWidth2 = Math.max(colWidth2, colWidth);
						lastWasParagraph = false;
					}
					rowHeight = Math.max(h, rowHeight);
				}
			}

			// Second pass: actually lay out the components
			if (dimension != null) {
				dimension.width = colWidth1 + hGapMajor + colWidth2;
				dimension.height = y + rowHeight;
			} else {
				int spareHeight = size.height-(y+rowHeight)-insets.top-insets.bottom-2*vMargin;
				x = 0;
				y = 0;
				lastWasParagraph = false;
				int start = 0;
				int rowWidth = 0;
				Integer paragraphType = NEW_PARAGRAPH;
				boolean stretchV = false;
				
				boolean firstLine = true;
				for (int i = 0; i < count; i++) {
					Component c = target.getComponent(i);
					if (includeComponent(c)) {
						Dimension d = sizes[i];
						int w = d.width;
						int h = d.height;
						Integer n = (Integer)getConstraint(c);
						int nv = n != null ? n.intValue() : 0;

						if (i == 0 || n == NEW_PARAGRAPH || n == NEW_PARAGRAPH_TOP) {
							if (i != 0)
								layoutRow(target, sizes, start, i-1, y, rowWidth, rowHeight, firstLine, type, paragraphType);
							stretchV = false;
							paragraphType = n;
							start = i;
							firstLine = true;
							if (i != 0)
								y += rowHeight+vGapMajor;
							rowHeight = 0;
							rowWidth = colWidth1+hGapMajor-hGapMinor;
							lastWasParagraph = true;
						} else if (n == NEW_LINE || lastWasParagraph) {
							if (!lastWasParagraph) {
								layoutRow(target, sizes, start, count-1, y, rowWidth, rowHeight, firstLine, type, paragraphType);
								stretchV = false;
								start = i;
								firstLine = false;
								y += rowHeight+vGapMinor;
								rowHeight = 0;
							}
							rowWidth += sizes[i].width+hGapMinor;
							lastWasParagraph = false;
						} else {
							rowWidth += sizes[i].width+hGapMinor;
							lastWasParagraph = false;
						}
						if ((nv & STRETCH_V_MASK) != 0 && !stretchV) {
							stretchV = true;
							h += spareHeight;
						}
						rowHeight = Math.max(h, rowHeight);
					}
				}
				layoutRow(target, sizes, start, count-1, y, rowWidth, rowHeight, firstLine, type, paragraphType);
			}
		}

	}

	protected void layoutRow(Container target, Dimension[] sizes, int start, int end, int y, int rowWidth, int rowHeight, boolean paragraph, int type, Integer paragraphType) {
		int x = 0;
		Insets insets = target.getInsets();
		Dimension size = target.getSize();
		int spareWidth = size.width-rowWidth-insets.left-insets.right-2*hMargin;

		for (int i = start; i <= end; i++) {
			Component c = target.getComponent(i);
			if (includeComponent(c)) {
				Integer n = (Integer)getConstraint(c);
				int nv = n != null ? n.intValue() : 0;
				Dimension d = sizes[i];
				int w = d.width;
				int h = d.height;

				if ((nv & STRETCH_H_MASK) != 0) {
					w += spareWidth;
					Dimension max = getComponentSize(c, MAXIMUM);
					Dimension min = getComponentSize(c, MINIMUM);
					w = Math.max(min.width, Math.min(max.width, w));
				}
				if ((nv & STRETCH_V_MASK) != 0) {
					h = rowHeight;
					Dimension max = getComponentSize(c, MAXIMUM);
					Dimension min = getComponentSize(c, MINIMUM);
					h = Math.max(min.height, Math.min(max.height, h));
				}

				if (i == start) {
					if (paragraph)
						x = colWidth1-w;
					else
						x = colWidth1 + hGapMajor;
				} else if (paragraph && i == start+1) {
					x = colWidth1 + hGapMajor;
				}
				int yOffset = paragraphType == NEW_PARAGRAPH_TOP ? 0 : (rowHeight-h)/2;
				if (target.getComponentOrientation().isLeftToRight())
					c.setBounds(insets.left+hMargin+x, insets.top+vMargin+y+yOffset, w, h);
				else
					c.setBounds(size.width-insets.right-insets.left-hMargin-x-w, insets.top+vMargin+y+yOffset, w, h);
				x += w + hGapMinor;
			}
		}
	}

}
