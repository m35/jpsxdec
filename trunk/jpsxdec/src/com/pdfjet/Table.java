/**
 *  Table.java
 *
Copyright (c) 2007, 2008, 2009, 2010 Innovatics Inc.

All rights reserved.

Redistribution and use in source and binary forms, with or without modification,
are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice,
      this list of conditions and the following disclaimer.
 
    * Redistributions in binary form must reproduce the above copyright notice,
      this list of conditions and the following disclaimer in the documentation
      and / or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package com.pdfjet;

import java.lang.*;
import java.io.*;
import java.util.*;


//>>>>pdfjet {
public class Table {

    public static int DATA_HAS_0_HEADER_ROWS = 0;
    public static int DATA_HAS_1_HEADER_ROWS = 1;
    public static int DATA_HAS_2_HEADER_ROWS = 2;
    public static int DATA_HAS_3_HEADER_ROWS = 3;
    public static int DATA_HAS_4_HEADER_ROWS = 4;
    public static int DATA_HAS_5_HEADER_ROWS = 5;
    public static int DATA_HAS_6_HEADER_ROWS = 6;
    public static int DATA_HAS_7_HEADER_ROWS = 7;
    public static int DATA_HAS_8_HEADER_ROWS = 8;
    public static int DATA_HAS_9_HEADER_ROWS = 9;

    private int rendered = 0;

    private List<List<Cell>> tableData = null;
    private int numOfHeaderRows = 0;

    private double x1 = 0.0;
    private double y1 = 0.0;
    private double w1 = 50.0;
    private double h1 = 30.0;

    private double lineWidth = 0.2;
    private double[] lineColor = RGB.BLACK;
    private double margin = 1.0;
    private double bottom_margin = 30.0;

    private Font f1 = null;     // head font
    private Font f2 = null;     // body font

    private Exception e = null;


    public Table(Font f1, Font f2) {
        this.f1 = f1;
        this.f2 = f2;
        tableData = new ArrayList<List<Cell>>();
    }


    public void setPosition(double x, double y) {
        this.x1 = x;
        this.y1 = y;
    }


    public void setSize(double w, double h) {
        this.w1 = w;
        this.h1 = h;
    }


    public void setLineWidth(double lineWidth) {
        this.lineWidth = lineWidth;
    }


    public void setLineColor(double[] color) {
        this.lineColor = color;
    }


    public void setLineColor(int[] rgb) {
        this.lineColor =
                new double[] {rgb[0]/255.0, rgb[1]/255.0, rgb[2]/255.0};
    }


    public void setCellPadding(double margin) {
        this.margin = margin;
    }


    public void setCellMargin(double margin) {
        this.margin = margin;
    }


    public void setBottomMargin(double bottom_margin) {
        this.bottom_margin = bottom_margin;
    }


    public void setData(
            List<List<Cell>> tableData) throws Exception {
        this.tableData = tableData;
        this.numOfHeaderRows = 0;
    }


    public void setData(
            List<List<Cell>> tableData, int numOfHeaderRows) throws Exception {
        this.tableData = tableData;
        this.numOfHeaderRows = numOfHeaderRows;
        this.rendered = numOfHeaderRows;
    }

    
    public void autoAdjustColumnWidths() {
        // Find the maximum text width for each column
        double[] max_col_widths = new double[tableData.get(0).size()];
        for (int i = 0; i < tableData.size(); i++) {
            List<Cell> row = tableData.get(i);
            for (int j = 0; j < row.size(); j++) {
                Cell cell = row.get(j);
                if (cell.colspan > 1) continue;
                cell.width = cell.font.stringWidth(cell.text);
                if (max_col_widths[j] == 0.0 ||
                        cell.width > max_col_widths[j]) {
                    max_col_widths[j] = cell.width;
                }
            }
        }

        for (int i = 0; i < tableData.size(); i++) {
            List<Cell> row = tableData.get(i);
            for (int j = 0; j < row.size(); j++) {
                Cell cell = row.get(j);
                if (max_col_widths[j] != 0.0) {
                    cell.width = max_col_widths[j] + 3 * margin;
                } else {
                    cell.width = cell.font.body_height;
                }
            }
        }
    }


    public void rightAlignNumbers() {
        for (int i = numOfHeaderRows; i < tableData.size(); i++) {
            List<Cell> row = tableData.get(i);
            for (int j = 0; j < row.size(); j++) {
                Cell cell = row.get(j);
                try {
                    Double.valueOf(cell.text.replace(",", ""));
                    cell.align = Align.RIGHT;
                } catch (Exception e) {
                    this.e = e;
                }
            }
        }
    }


    public void removeLineBetweenRows(int index1, int index2) {
        List<Cell> row = tableData.get(index1);
        Cell cell = null;
        for (int i = 0; i < row.size(); i++) {
            cell = row.get(i);
            cell.border.bottom = false;
        }
        row = tableData.get(index2);
        for (int i = 0; i < row.size(); i++) {
            cell = row.get(i);
            cell.border.top = false;
        }
    }


    public void setTextAlignInColumn(
            int index, int alignment) throws Exception {
        for (int i = 0; i < tableData.size(); i++) {
            List<Cell> row = tableData.get(i);
            if (index < row.size()) {
                row.get(index).align = alignment;
            }
        }
    }


    public void setTextColorInColumn(
            int index, double[] color) throws Exception {
        for (int i = 0; i < tableData.size(); i++) {
            List<Cell> row = tableData.get(i);
            if (index < row.size()) {
                row.get(index).brushColor = color;
            }
        }
    }


    public void setTextColorInColumn(
            int index, int[] rgb) throws Exception {
        for (int i = 0; i < tableData.size(); i++) {
            List<Cell> row = tableData.get(i);
            if (index < row.size()) {
                row.get(index).brushColor =
                        new double[] {rgb[0]/255.0, rgb[1]/255.0, rgb[2]/255.0};
            }
        }
    }


    public void setTextFontInColumn(
            int index, Font font, double size) throws Exception {
        for (int i = 0; i < tableData.size(); i++) {
            List<Cell> row = tableData.get(i);
            if (index < row.size()) {
                row.get(index).font = font;
                row.get(index).font.setSize(size);
            }
        }
    }


    public void setTextColorInRow(
            int index, double[] color) throws Exception {
        List<Cell> row = tableData.get(index);
        for (int i = 0; i < row.size(); i++) {
            row.get(i).brushColor = color;
        }
    }


    public void setTextColorInRow(
            int index, int[] rgb) throws Exception {
        List<Cell> row = tableData.get(index);
        for (int i = 0; i < row.size(); i++) {
            row.get(i).brushColor =
                    new double[] {rgb[0]/255.0, rgb[1]/255.0, rgb[2]/255.0};
        }
    }


    public void setTextFontInRow(
            int index, Font font, double size) throws Exception {
        List<Cell> row = tableData.get(index);
        for (int i = 0; i < row.size(); i++) {
            row.get(i).font = font;
            row.get(i).font.setSize(size);
        }
    }


    @Deprecated
    public void setWidthForColumn(int index, double width) throws Exception {
        setColumnWidth(index, width);
    }


    public void setColumnWidth(
            int index, double width) throws Exception {
        for (int i = 0; i < tableData.size(); i++) {
            List<Cell> row = tableData.get(i);
            if (index < row.size()) {
                row.get(index).width = width;
            }
        }
    }


    @Deprecated
    public Cell getCellAtRowColumn(
            int row, int col) throws Exception {
        return getCellAt(row, col);
    }


    public Cell getCellAt(
            int row, int col) throws Exception {
        if (row >= 0) {
            return tableData.get(row).get(col);
        }
        return tableData.get(tableData.size() + row).get(col);
    }


    public List<Cell> getRow(int index) throws Exception {
        return tableData.get(index);
    }


    public List<Cell> getColumn(int index) throws Exception {
        List<Cell> column = new ArrayList<Cell>();
        for (int i = 0; i < tableData.size(); i++) {
            List<Cell> row = tableData.get(i);
            if (index < row.size()) {
                column.add(row.get(index));
            }
        }
        return column;
    }


    public int getNumberOfPages(Page page) throws Exception {
        int numOfPages = 1;
        int j = numOfHeaderRows;
        double cell_h = 0.0;

        while (j != tableData.size()) {
        	double y = y1;

        	for (int i = 0; i < numOfHeaderRows; i++) {
                Cell cell = tableData.get(i).get(0);
                cell_h = cell.font.body_height + 2 * margin;
                y += cell_h;
            }

            for (; j < tableData.size(); j++) {
                Cell cell = tableData.get(j).get(0);
                cell_h = cell.font.body_height + 2 * margin;
                y += cell_h;

                if ((y + cell_h) > (page.height - bottom_margin)) {
                    numOfPages++;
                	break;
                }
            }
        }

        return numOfPages;
    }


    /**
     *  @param page Page
     *      The page to draw this table on.
     *
     *  @return point Point
     *      Top left corner of the next component to draw on the page.
     */
    public Point drawOn(Page page) throws Exception {
        double x = x1;
        double y = y1;
        double cell_w = 0.0;
        double cell_h = 0.0;

        page.setPenWidth(lineWidth);
        page.setPenColor(lineColor[0], lineColor[1], lineColor[2]);

        for (int i = 0; i < numOfHeaderRows; i++) {
            List<Cell> dataRow = tableData.get(i);
            for (int j = 0; j < dataRow.size(); j++) {
                Cell cell = dataRow.get(j);
                cell_h = cell.font.body_height + 2 * margin;
                cell_w = cell.width;
                for (int k = 1; k < cell.colspan; k++) {
                    cell_w += dataRow.get(++j).width;
                }

                page.setBrushColor(
                        cell.brushColor[0],
                        cell.brushColor[1],
                        cell.brushColor[2]);
                cell.paint(page, x, y, cell_w, cell_h, margin);

                x += cell_w;
            }
            x = x1;
            y += cell_h;
        }

        for (int i = rendered; i < tableData.size(); i++) {
            List<Cell> dataRow = tableData.get(i);
            for (int j = 0; j < dataRow.size(); j++) {
                Cell cell = dataRow.get(j);
                cell_h = cell.font.body_height + 2 * margin;
                cell_w = cell.width;
                for (int k = 1; k < cell.colspan; k++) {
                    cell_w += dataRow.get(++j).width;
                }

                page.setBrushColor(
                        cell.brushColor[0],
                        cell.brushColor[1],
                        cell.brushColor[2]);
                cell.paint(page, x, y, cell_w, cell_h, margin);

                x += cell_w;
            }
            x = x1;
            y += cell_h;

            if ((y + cell_h) > (page.height - bottom_margin)) {
                if (i == tableData.size() - 1) {
                    rendered = -1;
                } else {
                    rendered = i + 1;
                }
                return new Point(x, y);
            }
        }

        rendered = -1;

        return new Point(x, y);
    }


    public boolean hasMoreData() {
        if (rendered == -1) {
            return false;
        }
        return true;
    }


    public double getWidth() {
        double table_width = 0.0;
        List<Cell> row = tableData.get(0);
        for (int i = 0; i < row.size(); i++) {
            table_width += row.get(i).width;
        }
        return table_width;
    }


    /**
     *  Use the point.getY() value instead.
     */
    @Deprecated
    public double getHeight() {
        double table_height = 0.0;
        for (int i = 0; i < tableData.size(); i++) {
            List<Cell> row = tableData.get(i);
            table_height += row.get(0).height;
        }
        return table_height;
    }


    /**
     *  @return
     *      The number of data rows that have been rendered so far.
     */
    public int getRowsRendered() {
        return rendered == -1 ? rendered : rendered - numOfHeaderRows;
    }

}   // End of Table.java
//<<<<}
