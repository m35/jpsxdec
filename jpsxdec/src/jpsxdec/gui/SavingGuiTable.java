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

package jpsxdec.gui;

import java.awt.Color;
import java.awt.Component;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import jpsxdec.discitems.DiscItemSaverBuilder;
import jpsxdec.i18n.I;
import jpsxdec.i18n.ILocalizedMessage;


public class SavingGuiTable extends AbstractTableModel {

    private static enum COLUMNS {
        Source(String.class, I.GUI_SRC_COLUMN()) {
            Object val(Row row) { return row._builder.getInput(); }
        },
        SaveAs(String.class, I.GUI_SAVE_AS_COLUMN()) {
            Object val(Row row) { return row._builder.getOutputSummary(); }
        },
        Progress(Integer.class, I.GUI_PROGRESS_COLUMN()) {
            Object val(Row row) { return row.Progress; }
        },
        Message(String.class, I.GUI_MESSAGE_COLUMN()) {
            Object val(Row row) { return row.Message; }
        },
        Warn(Integer.class, I.GUI_WARN_COLUMN()) {
            Object val(Row row) { return row.Warnings; }
        },
        Err(Integer.class, I.GUI_ERR_COLUMN()) {
            Object val(Row row) { return row.Errors; }
        };

        @Nonnull
        private final Class<?> _type;
        @Nonnull
        private final ILocalizedMessage _name;
        COLUMNS(@Nonnull Class<?> type, @Nonnull ILocalizedMessage name) {
            _type = type;
            _name = name;
        }
        abstract @CheckForNull Object val(Row item);

        @Override
        final public String toString() {
            return _name.getLocalizedMessage();
        }
    }

    private static class WarnErrRenderer extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable table,
                                                       @Nonnull Object value,
                                                       boolean isSelected,
                                                       boolean hasFocus,
                                                       int row, int column)
        {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (((Integer)value) > 0)
                setForeground(Color.red);
            setHorizontalAlignment(JLabel.CENTER);
            return c;
        }

    }

    public final static int PROGRESS_FAILED = -3;
    public final static int PROGRESS_CANCELED = -2;
    public final static int PROGRESS_WAITING = -1;
    public final static int PROGRESS_DONE = 101; // work can still be happening at 100%, use 101 to really mean done
    public final static int PROGRESS_STARTED = 0;

    private static class ProgressRenderer extends DefaultTableCellRenderer {

        private final JProgressBar _bar = new JProgressBar(0, 100);

        public ProgressRenderer() {
            setOpaque(true);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table,
                                                       @Nonnull Object value,
                                                       boolean isSelected,
                                                       boolean hasFocus,
                                                       int row, int column)
        {
            int i = (Integer)value;
            ILocalizedMessage text;
            if(i == PROGRESS_FAILED)
                text = I.GUI_SAVE_STATUS_FAILED();
            else if (i == PROGRESS_CANCELED)
                text = I.GUI_SAVE_STATUS_CANCELED();
            else if (i == PROGRESS_WAITING)
                text = I.GUI_SAVE_STATUS_WAITING();
            else if (i >= PROGRESS_DONE)
                text = I.GUI_SAVE_STATUS_DONE();
            else { // >= PROGRESS_STARTED
                _bar.setValue(i);
                return _bar;
            }

            return super.getTableCellRendererComponent(table, text, isSelected, hasFocus, row, column);
        }

    }


    public class Row {

        @Nonnull
        public final DiscItemSaverBuilder _builder;
        private int Progress = 0;
        private int Warnings = 0;
        private int Errors = 0;
        @CheckForNull
        private String Message;


        private Row(@Nonnull DiscItemSaverBuilder builder) {
            _builder = builder;
        }

        public void setProgress(int i) {
            if (i != Progress) {
                Progress = i;
                update(this, COLUMNS.Progress);
            }
        }

        public void setMessage(@Nonnull String s) {
            if (!s.equals(Message)) {
                Message = s;
                update(this, COLUMNS.Message);
            }
        }

        public void incWarn() {
            Warnings++;
            update(this, COLUMNS.Warn);
        }
        public void incErr() {
            Errors++;
            update(this, COLUMNS.Err);
        }

    }


    public final ArrayList<Row> _rows = new ArrayList<Row>();

    public SavingGuiTable(@Nonnull List<DiscItemSaverBuilder> builders, @Nonnull JTable table) {
        for (DiscItemSaverBuilder builder : builders) {
            _rows.add(new Row(builder));
        }

        table.setModel(this);
        table.getColumnModel().getColumn(COLUMNS.Progress.ordinal()).setCellRenderer(new ProgressRenderer());
        table.getColumnModel().getColumn(COLUMNS.Warn.ordinal()).setCellRenderer(new WarnErrRenderer());
        table.getColumnModel().getColumn(COLUMNS.Err.ordinal()).setCellRenderer(new WarnErrRenderer());
        autoResizeColWidth(table);
    }

    private void update(@Nonnull Row row, @Nonnull COLUMNS col) {
        fireTableCellUpdated(_rows.indexOf(row), col.ordinal());
    }

    @Override
    public int getRowCount() {
        return _rows.size();
    }

    @Override
    public int getColumnCount() {
        return COLUMNS.values().length;
    }

    @Override
    public @CheckForNull Object getValueAt(int rowIndex, int columnIndex) {
        return COLUMNS.values()[columnIndex].val(_rows.get(rowIndex));
    }

    @Override
    public @Nonnull Class<?> getColumnClass(int columnIndex) {
        return COLUMNS.values()[columnIndex]._type;
    }

    @Override
    public @Nonnull String getColumnName(int column) {
        return COLUMNS.values()[column]._name.getLocalizedMessage();
    }


    // http://www.pikopong.com/blog/2008/08/13/auto-resize-jtable-column-width/
    public static void autoResizeColWidth(@Nonnull JTable table) {
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        int margin = 5;

        for (int i = 0; i < table.getColumnCount(); i++) {
            int                     vColIndex = i;
            DefaultTableColumnModel colModel  = (DefaultTableColumnModel) table.getColumnModel();
            TableColumn             col       = colModel.getColumn(vColIndex);
            int                     width     = 0;

            // Get width of column header
            TableCellRenderer renderer = col.getHeaderRenderer();

            if (renderer == null) {
                renderer = table.getTableHeader().getDefaultRenderer();
            }

            Component comp = renderer.getTableCellRendererComponent(table, col.getHeaderValue(), false, false, 0, 0);

            width = comp.getPreferredSize().width;

            // Get maximum width of column data
            for (int r = 0; r < table.getRowCount(); r++) {
                renderer = table.getCellRenderer(r, vColIndex);
                comp     = renderer.getTableCellRendererComponent(table, table.getValueAt(r, vColIndex), false, false,
                        r, vColIndex);
                width = Math.max(width, comp.getPreferredSize().width);
            }

            // Add margin
            width += 2 * margin;

            // Set the width
            col.setPreferredWidth(width);
        }

        ((DefaultTableCellRenderer) table.getTableHeader().getDefaultRenderer()).setHorizontalAlignment(
            SwingConstants.LEFT);

        // table.setAutoCreateRowSorter(true);
        table.getTableHeader().setReorderingAllowed(false);
    }


}
