/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2023  Michael Sabin
 * All rights reserved.
 *
 * Redistribution and use of the jPSXdec code or any derivative works are
 * permitted provided that the following conditions are met:
 *
 *  * Redistributions may not be sold, nor may they be used in commercial
 *    or revenue-generating business activities.
 *
 *  * Redistributions that are modified from the original source must
 *    include the complete source code, including the source code for all
 *    components used by a binary built from the modified sources. However, as
 *    a special exception, the source code distributed need not include
 *    anything that is normally distributed (in either source or binary form)
 *    with the major components (compiler, kernel, and so on) of the operating
 *    system on which the executable runs, unless that component itself
 *    accompanies the executable.
 *
 *  * Redistributions must reproduce the above copyright notice, this list
 *    of conditions and the following disclaimer in the documentation and/or
 *    other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package jpsxdec.i18n;

import java.io.PrintStream;
import java.util.ArrayList;
import javax.annotation.Nonnull;
import jpsxdec.util.Misc;

/** Hopefully makes it easier to print information in a table like design. */
public class TabularFeedback {

    private static class LineIndentPair {
        public final int _iIndent;
        @Nonnull
        public final ILocalizedMessage _msg;
        public LineIndentPair(int iIndent, @Nonnull ILocalizedMessage msg) {
            _iIndent = iIndent;
            _msg = msg;
        }
    }

    public static class Cell {
        private final ArrayList<LineIndentPair> _lines = new ArrayList<LineIndentPair>();
        public Cell() {}
        public Cell(@Nonnull ILocalizedMessage ... aoLines) {
            for (ILocalizedMessage line : aoLines) {
                addLine(line);
            }
        }
        final public Cell addLine(@Nonnull ILocalizedMessage line) {
            return addLine(line, 0);
        }
        final public Cell addLine(@Nonnull ILocalizedMessage line, int iIndent) {
            _lines.add(new LineIndentPair(iIndent, line));
            return this;
        }

        private @Nonnull ArrayList<String> toLines() {
            ArrayList<String> lines = new ArrayList<String>();
            for (LineIndentPair linePair : _lines) {
                String s = linePair._msg.getLocalizedMessage();
                String[] asLines = s.split("\\r\\n?|\\n");
                for (String sLine : asLines) {
                    lines.add(Misc.dup(' ', linePair._iIndent) + sLine);
                }
            }
            return lines;
        }
    }

    private final ArrayList<ArrayList<Cell>> _rows =
            new ArrayList<ArrayList<Cell>>();

    private int _iRowSpacing = 0, _iColSpacing = 2;

    public void setRowSpacing(int i) {
        _iRowSpacing = i;
    }

    public TabularFeedback() {
        newRow();
    }

    public @Nonnull TabularFeedback addCell(@Nonnull Cell cell) {
        curRow().add(cell);
        return this;
    }

    public @Nonnull TabularFeedback addCell(@Nonnull ILocalizedMessage ... aoLines) {
        curRow().add(new Cell(aoLines));
        return this;
    }

    public void newRow() {
        _rows.add(new ArrayList<Cell>());
    }

    private @Nonnull ArrayList<Cell> curRow() {
        return _rows.get(_rows.size() - 1);
    }

    public void write(@Nonnull PrintStream ps) {
        int iRowCount = _rows.size();
        int iColCount = 0;
        for (ArrayList<Cell> row : _rows) {
            iColCount = Math.max(iColCount, row.size());
        }
        @SuppressWarnings({"unchecked", "rawtypes"})
        ArrayList<String>[][] aaoCells = new ArrayList[iRowCount][iColCount];

        int[] aiRowHeights = new int[iRowCount];
        for (int iRow = 0; iRow < iRowCount; iRow++) {
            ArrayList<Cell> row = _rows.get(iRow);

            for (int iColumn = 0; iColumn < row.size(); iColumn++) {
                ArrayList<String> cellStingLines = row.get(iColumn).toLines();
                aaoCells[iRow][iColumn] = cellStingLines;

                aiRowHeights[iRow] = Math.max(aiRowHeights[iRow], cellStingLines.size());
            }
        }

        int[] aiColWidths = new int[iColCount];
        for (int iColumn = 0; iColumn < iColCount; iColumn++) {
            int iColWidth = 0;
            for (int iRow = 0; iRow < iRowCount; iRow++) {
                ArrayList<String> cellLines = aaoCells[iRow][iColumn];
                int iCellWidth = 0;
                if (cellLines != null) {
                    for (String line : cellLines) {
                        iCellWidth = Math.max(iCellWidth, line.length());
                    }
                }
                iColWidth = Math.max(iColWidth, iCellWidth);
            }
            aiColWidths[iColumn] = iColWidth;
        }

        for (int iRow = 0; iRow < iRowCount; iRow++) {
            ArrayList<String>[] row = aaoCells[iRow];
            for (int iLine = 0; iLine < aiRowHeights[iRow]; iLine++) {
                for (int iColumn = 0; iColumn < iColCount; iColumn++) {
                    ArrayList<String> cell = row[iColumn];
                    if (cell == null)
                        break;

                    if (iLine < cell.size()) {
                        String sLine = cell.get(iLine);
                        ps.print(sLine);
                        ps.print(Misc.dup(' ', aiColWidths[iColumn] - sLine.length() + _iColSpacing));
                    } else {
                        ps.print(Misc.dup(' ', aiColWidths[iColumn] + _iColSpacing));
                    }
                }
                ps.println();
            }
            for (int j = 0; j < _iRowSpacing; j++)
                ps.println();
        }
    }


}
