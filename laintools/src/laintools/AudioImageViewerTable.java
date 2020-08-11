/*
 * LainTools: PSX Serial Experiments Lain Hacking and Translation Tools
 * Copyright (C) 2011  Michael Sabin
 *
 * Redistribution and use of the LainTools code or any derivative works are
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

package laintools;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.AbstractCellEditor;
import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import jpsxdec.tim.Tim;
import jpsxdec.util.IO;
import jpsxdec.util.Misc;
import laintools.LainDataStructures.NodeTableItem;
import laintools.LainDataStructures.SiteImageTable;
import laintools.LainDataStructures.SiteImageTableItem;

public class AudioImageViewerTable extends JTable implements ListSelectionListener {

    private BufferedImagePanel _sourcePlanel;
    private BufferedImagePanel _renderPlanel;

    private ListSelectionListener _changer = new ListSelectionListener() {
        public void valueChanged(ListSelectionEvent e) {
            if (e.getValueIsAdjusting())
                return;
            updatePanels();
        }
    };

    private void updatePanels() {
        if (_sourcePlanel == null || _renderPlanel == null)
            return;
        SiteJTableItemModel item = getSelection();
        if (item != null) {
            _sourcePlanel.setImage(item.image());
            try {
                _renderPlanel.setImage(item.generateImage());
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public AudioImageViewerTable() {

        getSelectionModel().addListSelectionListener(_changer);

        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    }

    public SiteJTableItemModel getRow(int i) {
        return getSiteModel().getRow(i);
    }

    public SiteJTableItemModel getSelection() {
        int i = getSelectedRow();
        return i < 0 ? null : getRow(i);
    }

    public SiteJTableModel getSiteModel() {
        return ((SiteJTableModel)getModel());
    }

    public void setSiteModel(SiteImageTable dataSource) {

        setModel(new SiteJTableModel(dataSource));
        getSiteModel().addTableModelListener(new TableModelListener() {
            public void tableChanged(TableModelEvent e) {
                int i = e.getFirstRow();
                if (i == getSelectedRow())
                    updatePanels();
            }
        });
        TableColumn col = getColumnModel().getColumn(COLUMNS.Dimension.ordinal());
        //col.setCellEditor(new GenericEditor());
        col = getColumnModel().getColumn(COLUMNS.SaveAs.ordinal());
        col.setCellEditor(new MyComboBoxEditor(Formats.VALUES));
        col.setCellRenderer(new MyComboBoxRenderer(Formats.VALUES));
    }

    public void setImagePanels(BufferedImagePanel source, BufferedImagePanel render) {
        _sourcePlanel = source;
        _renderPlanel = render;
    }

    public void copySettings() {
        SiteJTableItemModel src = getSelection();
        if (src == null) return;
        for (int i = 0; i < getSiteModel().getRowCount(); i++) {
            SiteJTableItemModel dest = getSiteModel().getRow(i);
            dest.format(src.format());
            dest.cross(src.cross());
            dest.label(src.label());
            dest.width(src.width());
            dest.height(src.height());
        }
    }

    public void dump() throws Exception {
        getSiteModel().dump(getSiteModel().getSite());
    }

    public static class MyComboBoxRenderer extends JComboBox implements TableCellRenderer {
        public MyComboBoxRenderer(Formats[] items) {
            super(items);
        }

        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            if (isSelected) {
                setForeground(table.getSelectionForeground());
                super.setBackground(table.getSelectionBackground());
            } else {
                setForeground(table.getForeground());
                setBackground(table.getBackground());
            }

            // Select the current value
            setSelectedItem(value);
            return this;
        }
    }

    public static class MyComboBoxEditor extends DefaultCellEditor {
        public MyComboBoxEditor(Formats[] items) {
            super(new JComboBox(items));
        }
    }

    private static class MyTableCellEditor extends AbstractCellEditor implements TableCellEditor {
        // This is the component that will handle the editing of the cell value
        JComponent component = new JTextField();

        // This method is called when a cell value is edited by the user.
        public Component getTableCellEditorComponent(JTable table, Object value,
                    boolean isSelected, int rowIndex, int vColIndex)
        {
            // 'value' is value contained in the cell located at (rowIndex, vColIndex)

            if (isSelected) {
                // cell (and perhaps other cells) are selected
            }

            // Configure the component with the specified value
            ((JTextField) component).setText((String) value);

            // Return the configured component
            return component;
        }

        // This method is called when editing is completed.
        // It must return the new value to be stored in the cell.
        public Object getCellEditorValue() {
            return ((JTextField) component).getText();
        }
    }
}


enum COLUMNS {
    Details(SiteImageTableItem.class) {
        Object val(SiteJTableItemModel row) { return row; }
        boolean settable() { return false; }
    },
    SaveAs(Formats.class) {
        Object val(SiteJTableItemModel row) { return row.format(); }
        void set(SiteJTableItemModel row, Object val) {
            row.format((Formats)val);
        }
        public String toString() { return "Save As"; }
    },
    Label(Boolean.class) {
        Object val(SiteJTableItemModel row) { return row.label(); }
        void set(SiteJTableItemModel row, Object val) {
            row.label((Boolean)val);
        }
    },
    Dimension(String.class) {
        Object val(SiteJTableItemModel row) { return row.dimentions(); }
        void set(SiteJTableItemModel row, Object val) {
            row.dimentions((String)val);
        }
    },
    Crosshair(Boolean.class) {
        Object val(SiteJTableItemModel row) { return row.cross(); }
        void set(SiteJTableItemModel row, Object val) {
            row.cross((Boolean)val);
        }
    };

    private final Class _type;
    COLUMNS(Class type) {
        _type = type;
    }
    public Class getType() { return _type; }
    abstract Object val(SiteJTableItemModel item);
    void set(SiteJTableItemModel item, Object val) {}
    boolean settable() { return true; }

    public static final COLUMNS[] VALUES = values();
    public static final int COUNT = VALUES.length;
}

enum Formats { NoSave, Original, Tim, Png;
    public static final Formats[] VALUES = values();
}


class SiteJTableModel extends AbstractTableModel {

    private final String _sSite;

    private final ArrayList<SiteJTableItemModel> _items =
            new ArrayList<SiteJTableItemModel>();

    public SiteJTableModel(SiteImageTable dataSource) {
        _sSite = dataSource.getSite();
        for (int i = 0; i < dataSource.size(); i++) {
            _items.add(new SiteJTableItemModel(i, dataSource.get(i), this));
        }

    }

    public int getColumnCount() {
        return COLUMNS.COUNT;
    }

    public int getRowCount() {
        return _items.size();
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
        return COLUMNS.VALUES[columnIndex].val(_items.get(rowIndex));
    }

    public Class<?> getColumnClass(int columnIndex) {
        return COLUMNS.VALUES[columnIndex].getType();
    }

    public String getColumnName(int column) {
        return COLUMNS.VALUES[column].toString();
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return COLUMNS.VALUES[columnIndex].settable();
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        COLUMNS.VALUES[columnIndex].set(_items.get(rowIndex), aValue);
    }

    public SiteJTableItemModel getRow(int i) {
        return _items.get(i);
    }

    public void dump(String sDir) throws Exception {
        for (SiteJTableItemModel siteJTableItemModel : _items) {
            siteJTableItemModel.dump(sDir);
        }
    }

    public String getSite() {
        return _sSite;
    }

}


class SiteJTableItemModel {
    private final SiteImageTableItem _source;
    private int _iWidth = -1,
                _iHeight = -1;
    private boolean _blnLabel = false,
                    _blnCrosshair = false;
    private Formats _eFormat = Formats.Png;

    private final int _iRow;
    private final SiteJTableModel _tblMdl;

    public SiteJTableItemModel(int iRow, SiteImageTableItem source, SiteJTableModel tblMdl) {
        _iRow = iRow;
        _source = source;
        _tblMdl = tblMdl;
    }

    public String generateFilename() {
        StringBuilder sb = new StringBuilder();
        for (String s : getOwnedIdIndexes()) {
            if (sb.length() > 0)
                sb.append(',');
            sb.append(s);
        }
        if (_source.isUncompressed())
            sb.append("(uncompressed)");
        return "[" + _source.getIndex() + "]" + sb.toString();
    }

    private String[] getOwnedIdIndexes() {
        ArrayList<String> r = new ArrayList<String>();
        List<NodeTableItem> l = _source.ownedBy();
        Set<NodeTableItem> s = new TreeSet<NodeTableItem>(l); // remove duplicates
        for (NodeTableItem nodeItem : s) {
            //if (nodeItem.getName().startsWith("Env"))
            //    System.out.println();
            for (int i = 0; i < 3; i++) {
                if (nodeItem.getSiteTableIndex(i) == _source.getIndex()) {
                    r.add(nodeItem.getName() + '-' + i);
                }
            }
            for (int i = 0; i < 3; i++) {
                if (nodeItem.getEnvTableIndex(i) == _source.getIndex()) {
                    r.add(nodeItem.getName() + '-' + i);
                }
            }
        }
        return r.toArray(new String[r.size()]);
    }

    public SiteImageTableItem getItem() {
        return _source;
    }

    public boolean resize() {
        return _iWidth >= 1 && _iHeight >= 1;
    }

    public boolean label() { return _blnLabel; }
    public void label(boolean v) { _blnLabel = v;  fire(); }

    public boolean cross() { return _blnCrosshair; }
    public void cross(boolean v) { _blnCrosshair = v;  fire(); }

    public boolean originalDims() {
        return (_iWidth < 1 || _iHeight < 1);
    }
    public String dimentions() {
        if (originalDims())
            return "(original)";
        return _iWidth + "x" + _iHeight;
    }
    public void dimentions(String v) {
        if (v.length() == 0) {
            _iWidth = _iHeight = -1;
             fire();
            return;
        }
        try {
            int[] wh = Misc.splitInt(v, "x");
            _iWidth = wh[0] & ~1; // must be multiple of 2
            _iHeight = wh[1];
             fire();
        }
        catch (NumberFormatException ex) {}
        catch (NullPointerException ex) {}
        catch (ArrayIndexOutOfBoundsException ex) {}
    }

    public int width() { return _iWidth; }
    public void width(int v) { _iWidth = v; fire(); }

    public int height() { return _iHeight; }
    public void height(int v) { _iHeight = v; fire(); }

    public String[] text() {
        return new String[] {generateFilename()};
    }

    public Formats format() { return _eFormat; }
    public void format(Formats v) { _eFormat = v; fire(); }

    private void fire() { _tblMdl.fireTableRowsUpdated(_iRow, _iRow); }


    public BufferedImage image() {
        try {
            return _source.readImg();
        } catch (IOException ex) {
            Logger.getLogger(AudioImageViewerTable.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }

    @Override
    public String toString() {
        return generateFilename();
    }

    public BufferedImage generateImage() throws Exception {
        if (format() == Formats.NoSave)
            return null;

        SiteImageTableItem siteItem = getItem();

        Tim srcTim = siteItem.readTim();
        BufferedImage bi = srcTim.toBufferedImage(0);
        if (format() != Formats.Png)
            return bi;
        int iMaxOutputSize = (siteItem.readRaw().length + 2047) & ~2047;

        String[] text = null;
        if (label()) {
            ArrayList<String> t = new ArrayList<String>();
            t.add("["+_source.getIndex()+"]");
            t.addAll(Arrays.asList(getOwnedIdIndexes()));
            text = t.toArray(new String[t.size()]);
        }

        int iTry;
        if (resize())
            iTry = 2;
        else
            iTry = 1;

        ByteArrayOutputStream newTim = null;

        for (; iTry <= 3; iTry++) {
            switch (iTry) {
                case 1:
                    if (label())
                        drawCenteredText(bi, Color.red, 32, text);
                    break;
                case 2:
                    if (resize())
                        bi = new BufferedImage(width(), height(),
                                BufferedImage.TYPE_BYTE_BINARY);
                    else
                        bi = new BufferedImage(bi.getWidth(), bi.getHeight(),
                                BufferedImage.TYPE_BYTE_BINARY);
                    // fill white, and put big black text on it
                    fillWhite(bi);
                    if (label())
                        drawCenteredText(bi, Color.black, 32, text);
                    break;
                case 3:
                    // ack! ok, make the text smaller now :P
                    fillWhite(bi);
                    if (label())
                        drawCenteredText(bi, Color.black, 12, text);
                    break;
            }
            if (cross())
                drawDottedCross(bi);
            // convert back to tim and compress
            if (_source.isUncompressed()) {
                Tim t = Tim.create(bi, srcTim.getBitsPerPixel());
                newTim = new ByteArrayOutputStream();
                t.write(newTim);
            } else
                newTim = makeCompressedTim(bi, 8);
            if (newTim.size() <= iMaxOutputSize)
                break; // great it's small enough
        }

        if (newTim.size() > iMaxOutputSize)
            // did we make it out small enough?
            throw new RuntimeException("TIM will never fit!!");

        return bi;
    }

    public void dump(String sDir) throws Exception {
        switch (_eFormat) {
            case NoSave: break;
            case Original:
                String sExt;
                if (_source.isUncompressed())
                    sExt = ".tim";
                else
                    sExt = ".tim.napk";
                IO.writeFile(new File(sDir, generateFilename()+sExt), _source.readRaw());
                break;
            case Tim:
                FileOutputStream fos = new FileOutputStream(new File(sDir, generateFilename()+".tim"));
                _source.readTim().write(fos);
                fos.close();
                break;
            case Png:
                BufferedImage bi = generateImage();
                ImageIO.write(bi, "png", new File(sDir, generateFilename() + ".png"));
                break;
        }
    }

    private static ByteArrayOutputStream makeCompressedTim(Tim newTim) throws IOException {
        // write it out
        ByteArrayOutputStream temp = new ByteArrayOutputStream();
        newTim.write(temp);
        // and compress it
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        Lain_Pk.compress(temp.toByteArray(), out);
        return out;
    }


    private static ByteArrayOutputStream makeCompressedTim(BufferedImage bi, int iBitsPerPix) throws Exception {
        // convert to tim
        Tim newTim = Tim.create(bi, iBitsPerPix);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        newTim.write(baos);
        return makeCompressedTim(newTim);
    }


    /** Draw a cross of dots to visual measurement in game. */
    private static void drawDottedCross(BufferedImage bi) {
        int w = bi.getWidth() / 2;
        int h = bi.getHeight() / 2;
        for (int i = 0; i < bi.getWidth(); i+=2) {
            bi.setRGB(i, h, 0x00000000);
        }
        for (int j = 0; j < bi.getHeight(); j+=2) {
            bi.setRGB(w, j, 0x00000000);
        }

    }

    /** copy an image
     * http://forum.java.sun.com/thread.jspa?threadID=711821&messageID=4118293  */
    private static void fillWhite(BufferedImage src) {
        Graphics2D g2d = src.createGraphics();
        g2d.setColor(Color.white);
        g2d.fill(new Rectangle2D.Float(0, 0, src.getWidth(), src.getHeight()));
        g2d.dispose();
    }

    public static void drawCenteredText(BufferedImage src, Paint color, int size, String[] text)
    {
        int iImgW = src.getWidth();
        int iImgH = src.getHeight();
        Graphics2D g2 = src.createGraphics();
        // Draw text on top.
        Font font = g2.getFont().deriveFont(Font.BOLD, (float)size);
        g2.setFont(font);
        g2.setPaint(color);
        FontRenderContext frc = g2.getFontRenderContext();

        float lineHeight = g2.getFontMetrics().getHeight();
        float y = (iImgH - lineHeight * (text.length-1))/2.f;

        for (int i = 0; i < text.length; i++, y += lineHeight) {
            float width = (float)font.getStringBounds(text[i], frc).getWidth();
            // Locate text, this will draw it centered
            float x = (iImgW - width)/2.f;
            g2.drawString(text[i], x, y);
        }
        g2.dispose();
    }


}

