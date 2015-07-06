/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2015  Michael Sabin
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

package jpsxdec.util;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import javax.annotation.Nonnull;
import jpsxdec.i18n.LocalizedMessage;

/** Hopefully makes it easier to print information in a table like design. */
public class TabularFeedback {

    private final ArrayList<ArrayList<ArrayList<StringBuilder>>> _rows =
            new ArrayList<ArrayList<ArrayList<StringBuilder>>>();

    private int _iRowSpacing = 0, _iColSpacing = 2;
    private int _iCurCellIndent = 0;

    public void setRowSpacing(int i) {
        _iRowSpacing = i;
    }

    public TabularFeedback() {
        addRow();
    }

    private @Nonnull ArrayList<ArrayList<StringBuilder>> curRow() {
        return _rows.get(_rows.size() - 1);
    }

    private @Nonnull ArrayList<StringBuilder> curCell() {
        ArrayList<ArrayList<StringBuilder>> curRow = curRow();
        return curRow.get(curRow.size() - 1);
    }

    private @Nonnull StringBuilder curLine() {
        ArrayList<StringBuilder> curCell = curCell();
        return curCell.get(curCell.size() - 1);
    }

    private void addLine() {
        StringBuilder newLine = new StringBuilder();
        if (_iCurCellIndent > 0)
            newLine.append(Misc.dup(' ', _iCurCellIndent));
        curCell().add(newLine);
    }

    private void addCell() {
        ArrayList<StringBuilder> newCell = new ArrayList<StringBuilder>();
        curRow().add(newCell);
        _iCurCellIndent = 0;
        addLine();
    }

    private void addRow() {
        ArrayList<ArrayList<StringBuilder>> newRow = new ArrayList<ArrayList<StringBuilder>>();
        _rows.add(newRow);
        addCell();
    }

    public void newRow() {
        addRow();
    }

    public @Nonnull TabularFeedback print(@Nonnull LocalizedMessage s) {
        StringBuilder curLine = curLine();
        if (curLine.length() == 0 && _iCurCellIndent > 0)
            curLine.append(Misc.dup(' ', _iCurCellIndent));
        String[] asLines = s.getLocalizedMessage().split("\\r\\n?|\\n");
        for (int i = 0; i < asLines.length-1; i++) {
            curLine().append(asLines[i]);
            addLine();
        }
        curLine().append(asLines[asLines.length-1]);
        return this;
    }

    public @Nonnull TabularFeedback ln() {
        addLine();
        return this;
    }
    public @Nonnull TabularFeedback println(@Nonnull LocalizedMessage s) {
        print(s);
        addLine();
        return this;
    }

    public @Nonnull TabularFeedback tab() {
        addCell();
        return this;
    }

    public @Nonnull TabularFeedback indent() {
        _iCurCellIndent += 2;
        return this;
    }

    public @Nonnull TabularFeedback outdent() {
        _iCurCellIndent -= 2;
        if (_iCurCellIndent < 0)
            _iCurCellIndent = 0;
        return this;
    }

    public void write(@Nonnull PrintStream ps) {
        int[] aiRowHeights = new int[_rows.size()];
        int iColCount = 0;
        for (int i = 0; i < _rows.size(); i++) {
            ArrayList<ArrayList<StringBuilder>> row = _rows.get(i);

            iColCount = Math.max(iColCount, row.size());

            int iRowHeight = 0;
            for (ArrayList<StringBuilder> cell : row) {
                iRowHeight = Math.max(iRowHeight, cell.size());
            }
            aiRowHeights[i] = iRowHeight;
        }

        int[] aiColWidths = new int[iColCount];
        Arrays.fill(aiColWidths, 0);
        for (ArrayList<ArrayList<StringBuilder>> row : _rows) {
            for (int i = 0; i < row.size(); i++) {
                ArrayList<StringBuilder> col = row.get(i);
                for (StringBuilder line : col) {
                    aiColWidths[i] = Math.max(aiColWidths[i], line.length());
                }
            }
        }

        for (int i = 0; i < _rows.size(); i++) {
            ArrayList<ArrayList<StringBuilder>> row = _rows.get(i);
            for (int iLine = 0; iLine < aiRowHeights[i]; iLine++) {
                for (int j = 0; j < row.size(); j++) {
                    ArrayList<StringBuilder> cell = row.get(j);
                    if (iLine < cell.size()) {
                        String sLine = cell.get(iLine).toString();
                        ps.print(sLine);
                        if (j < row.size()-1)
                            ps.print(Misc.dup(' ', aiColWidths[j] - sLine.length() + _iColSpacing));
                    } else {
                        if (j < row.size()-1)
                            ps.print(Misc.dup(' ', aiColWidths[j] + _iColSpacing));
                    }
                }
                ps.println();
            }
            for (int j = 0; j < _iRowSpacing; j++)
                ps.println();
        }
    }


}
