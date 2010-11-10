/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2010  Michael Sabin
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

package jpsxdec;

import java.awt.Component;
import java.awt.FontMetrics;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.event.TreeModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreePath;
import jpsxdec.discitems.DiscItem;
import jpsxdec.discitems.DiscItemAudioStream;
import jpsxdec.discitems.DiscItemISO9660File;
import jpsxdec.discitems.DiscItemSaverBuilder;
import jpsxdec.discitems.DiscItemVideoStream;
import jpsxdec.discitems.IDiscItemSaver;
import jpsxdec.discitems.IndexId;
import jpsxdec.indexing.DiscIndex;
import jpsxdec.util.player.PlayController;
import org.jdesktop.swingx.JXTreeTable;
import org.jdesktop.swingx.renderer.CheckBoxProvider;
import org.jdesktop.swingx.renderer.DefaultTableRenderer;
import org.jdesktop.swingx.treetable.TreeTableModel;

public class GuiTree extends JXTreeTable {

    public static final Icon FILE_ICON =
            new ImageIcon(Gui.class.getResource("layer-new-3.png"));
            //UIManager.getIcon("FileChooser.fileIcon");
            //UIManager.getIcon("Tree.leafIcon");
    public static final Icon FOLDER_ICON =
            //new ImageIcon(Gui.class.getResource("folder.png"));
            UIManager.getIcon("FileView.folderIcon");
    public static final ImageIcon VIDEO_ICON = new ImageIcon(Gui.class.getResource("film.png"));
    public static final ImageIcon AUDIO_ICON = new ImageIcon(Gui.class.getResource("knotify.png"));

    RootTreeSpot _root;

    public void formatTreeTable(DiscIndex index) {
        _root = buildTree(index.getRoot());

        FontMetrics fm = getFontMetrics(getFont());
        int iSectorWidth = fm.stringWidth("999999-999999");
        int iNumberWidth = fm.stringWidth(String.valueOf(index.size()) + "99");
        int iNameWidth = fm.stringWidth("MMMMMMMM.MMM[99.9.9]") + 25*3;

        setDefaultRenderer(Boolean.class, new OptionalBooleanTableCellRenderer());
        //_guiDiscTree.setDefaultRenderer(Integer.class, new CenteredIntegerTableCellRenderer());
        setTreeCellRenderer(new TreeIconRenderer());
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        setTreeTableModel(new DiscTreeModel(_root));
        packAll();
        getColumn(COLUMNS.Name.toString()).setPreferredWidth(iNameWidth + 10);
        getColumn(COLUMNS.Num.toString()).setPreferredWidth(iNumberWidth + 10);
        getColumn(COLUMNS.Sectors.toString()).setPreferredWidth(iSectorWidth + 10);
        getColumn(COLUMNS.Details.toString()).setPreferredWidth(Math.max(200, getColumn(COLUMNS.Details.toString()).getWidth()));
    }

    public TreeSpot getTreeTblSelection() {
        return (TreeSpot) getValueAt(getSelectedRow(), convertColumnIndexToView(COLUMNS.Name.ordinal()));
    }

    public void selectAllType(String sCmd) {
        _root.selectAllType(sCmd);
        repaint();
    }

    public int applySettings(DiscItemSaverBuilder builder) {
        return _root.applySettings(builder);
    }

    public ArrayList<IDiscItemSaver> collectSelected() {
        ArrayList<IDiscItemSaver> savers = new ArrayList<IDiscItemSaver>();
        _root.collectSelected(savers);
        return savers;
    }

    // #########################################################################

    private enum COLUMNS {

        Num(Integer.class) {
            Object val(TreeSpot item) {
                return item.getIndexNum();
            }
            public String toString() {
                return "#";
            }
        },
        Save(Boolean.class) { Object val(TreeSpot item) {
            return item.getSave();
        }},
        Name(TreeSpot.class) {
            Object val(TreeSpot item) {
                return item;
            }
            public String toString() {
                return "";
            }
        },
        Type(String.class) { Object val(TreeSpot item) {
            return item.getType();
        }},
        Sectors(String.class) { Object val(TreeSpot item) {
            return item.getSectorRange();
        }},
        Details(String.class) { Object val(TreeSpot item) {
            return item.getDetails();
        }},
        ;
        private final Class _type;
        COLUMNS(Class type) {
            _type = type;
        }
        abstract Object val(TreeSpot item);
    }

    // -------------------------------------------------------------------------

    public static abstract class TreeSpot {

        protected ArrayList<TreeSpot> _kids = new ArrayList<TreeSpot>();

        public TreeSpot getKid(int childIndex) {
            return _kids.get(childIndex);
        }

        public int kidCount() {
            return _kids.size();
        }

        public int indexOf(Object node) {
            return _kids.indexOf(node);
        }

        public void addKid(TreeSpot kid) {
            _kids.add(kid);
        }

        public Iterator<TreeSpot> iterator() {
            return _kids.iterator();
        }

        public DirectoryTreeSpot getOrCreateDir(String sName) {
            for (TreeSpot node : _kids) {
                if (node instanceof DirectoryTreeSpot) {
                    DirectoryTreeSpot dirNode = (DirectoryTreeSpot) node;
                    if ( dirNode.getName().equals(sName) ) {
                        return dirNode;
                    }
                }
            }
            DirectoryTreeSpot dirNode = new DirectoryTreeSpot(sName);
            _kids.add(dirNode);
            return dirNode;
        }

        abstract public Icon getIcon();

        abstract public DiscItemSaverBuilder getBuilder();
        abstract public String getIndexNum();
        abstract public String getType();
        abstract public String getDetails();
        abstract public String getSectorRange();

        abstract public boolean canPlay();
        abstract public PlayController getPlayer();
        abstract public DiscItem getItem();
        abstract public Boolean getSave();

        public void collectSelected(final ArrayList<IDiscItemSaver> savers) {
            Boolean oblnSave = getSave();
            if (oblnSave != null && oblnSave.booleanValue())
                savers.add(getBuilder().makeSaver());
            for (int i=0; i<kidCount(); i++)
                getKid(i).collectSelected(savers);
        }

        public int applySettings(DiscItemSaverBuilder otherBuilder) {
            int iCount = 0;
            DiscItemSaverBuilder thisBuilder = getBuilder();
            if (thisBuilder != null) {
                if (otherBuilder.copySettings(thisBuilder))
                    iCount++;
            }
            for (int i = 0; i < kidCount(); i++)
                iCount += getKid(i).applySettings(otherBuilder);
            return iCount;
        }

        public void selectAllType(final String sCmd) {
            // TODO: change this to an enum
            for (int i=0; i< kidCount(); i++)
                getKid(i).selectAllType(sCmd);
        }

    }

    private static class RootTreeSpot extends TreeSpot {
        @Override
        public DiscItemSaverBuilder getBuilder() { return null; }

        @Override
        public Icon getIcon() {
            return null;
        }
        public String toString() {
            return "[ROOT]";
        }
        @Override
        public String getDetails() { return ""; }
        @Override
        public String getIndexNum() { return ""; }
        @Override
        public String getType() { return ""; }
        @Override
        public String getSectorRange() { return ""; }
        @Override
        public PlayController getPlayer() { return null;}
        @Override
        public DiscItem getItem() { return null; }
        @Override
        public boolean canPlay() { return false; }
        @Override
        public Boolean getSave() { return null; }

    }

    private static class DirectoryTreeSpot extends RootTreeSpot {

        private String _sDirName;

        public DirectoryTreeSpot(String _sDirName) {
            this._sDirName = _sDirName;
        }

        private Object getName() {
            return _sDirName;
        }

        @Override
        public Icon getIcon() {
            return null;
        }

        public String toString() {
            return _sDirName + "/";
        }

    }

    private static class DiscItemTreeSpot extends TreeSpot {

        private DiscItem _item;
        private DiscItemSaverBuilder _builder;
        private boolean _blnSave = false;

        public DiscItemTreeSpot(IndexId indexId) {
            _item = indexId.getItem();
            // recursively add the kids
            for (IndexId childId : indexId) {
                _kids.add(new DiscItemTreeSpot(childId));
            }
        }

        @Override
        public DiscItem getItem() {
            return _item;
        }

        public DiscItemSaverBuilder getBuilder() {
            if (_builder == null) {
                _builder = _item.makeSaverBuilder();
            }
            return _builder;
        }

        @Override
        public Icon getIcon() {
            if (_item instanceof DiscItemVideoStream) {
                return VIDEO_ICON;
            } else if (_item instanceof DiscItemISO9660File) {
                return FILE_ICON;
            } else if (_item instanceof DiscItemAudioStream) {
                return AUDIO_ICON;
            } else
                return null;
        }

        public String toString() {
            return _item.getIndexId().getTopLevel();
        }

        @Override
        public String getIndexNum() {
            return String.valueOf(_item.getIndexId().getListIndex());
        }

        @Override
        public String getType() {
            return _item.getSerializationTypeId();
        }

        @Override
        public String getDetails() {
            return _item.getInterestingDescription();
        }

        public Boolean getSave() {
            return _blnSave;
        }
        @Override
        public String getSectorRange() {
            return _item.getStartSector() + "-" + _item.getEndSector();
        }

        public void setSave(boolean value) {
            _blnSave = value;
        }

        @Override
        public PlayController getPlayer() {
            try {
                if (_item instanceof DiscItemVideoStream) {
                    return ((DiscItemVideoStream)_item).makePlayController();
                } else if (_item instanceof DiscItemAudioStream) {
                    return ((DiscItemAudioStream)_item).makePlayController();
                } else {
                    return null;
                }
            } catch (Throwable ex) {
                ex.printStackTrace();
                return null;
            }
        }

        @Override
        public boolean canPlay() {
            return _item instanceof DiscItemVideoStream || _item instanceof DiscItemAudioStream;
        }

        @Override
        public void selectAllType(String sCmd) {
            // TODO: clean this up
            if (sCmd.equals("none")) {
                _blnSave = false;
            } else if (sCmd.equals("all Video")) {
                _blnSave = _blnSave || getItem() instanceof DiscItemVideoStream;
            } else if (sCmd.equals("all Audio (excluding video audio)")) {
                _blnSave = _blnSave || (getItem() instanceof DiscItemAudioStream
                                        && !((DiscItemAudioStream)getItem()).isPartOfVideo());
            } else if (sCmd.equals("all Audio (including video audio)")) {
                _blnSave = _blnSave || getItem() instanceof DiscItemAudioStream;
            } else if (sCmd.equals("all Files")) {
                _blnSave = _blnSave || getItem() instanceof DiscItemISO9660File;
            }
            super.selectAllType(sCmd);
        }


    }

    // .........................................................................

    private static RootTreeSpot buildTree(List<IndexId> indexIds) {
        RootTreeSpot root = new RootTreeSpot();
        for (IndexId id : indexIds) {
            if (id.getFile() != null) {
                String[] asDirs = splitFileDirs(id.getFile());

                TreeSpot tree = root;
                for (String sDir : asDirs) {
                    tree = tree.getOrCreateDir(sDir);
                }
                tree.addKid(new DiscItemTreeSpot(id));
            } else {
                root.addKid(new DiscItemTreeSpot(id));
            }
        }
        return root;
    }

    private static String[] splitFileDirs(File file) {
        ArrayList<String> dirs = new ArrayList<String>();
        File parent;
        while ((parent = file.getParentFile()) != null) {
            dirs.add(parent.getName());
            file = parent;
        }

        return dirs.toArray(new String[dirs.size()]);
    }

    // -------------------------------------------------------------------------

    private static class DiscTreeModel implements TreeTableModel {
        private TreeSpot _treeRoot;

        public DiscTreeModel(TreeSpot _treeRoot) {
            this._treeRoot = _treeRoot;
        }

        public int getHierarchicalColumn() {
            return COLUMNS.Name.ordinal();
        }

        public TreeSpot getRoot() {
            return _treeRoot;
        }

        public TreeSpot getChild(Object parent, int index) {
            return ((TreeSpot)parent).getKid(index);
        }

        public int getChildCount(Object parent) {
            return ((TreeSpot)parent).kidCount();
        }

        public boolean isLeaf(Object node) {
            return ((TreeSpot)node).kidCount() == 0;
        }

        public int getIndexOfChild(Object parent, Object child) {
            return ((TreeSpot)parent).indexOf(child);
        }

        public void addTreeModelListener(TreeModelListener l) {}
        public void removeTreeModelListener(TreeModelListener l) {}
        public void valueForPathChanged(TreePath path, Object newValue) {}

        public boolean isCellEditable(Object o, int i) {
            return (i == COLUMNS.Save.ordinal()) && o instanceof DiscItemTreeSpot;
        }

        public int getColumnCount() {
            return COLUMNS.values().length;
        }

        public Class<?> getColumnClass(int i) {
            return COLUMNS.values()[i]._type;
        }

        public String getColumnName(int i) {
            return COLUMNS.values()[i].toString();
        }
        
        public Object getValueAt(Object o, int i) {
            return COLUMNS.values()[i].val((TreeSpot)o);
        }

        public void setValueAt(Object value, Object node, int i) {
            ((DiscItemTreeSpot)node).setSave(((Boolean)value).booleanValue());
        }
    }

    // -------------------------------------------------------------------------

    private static class TreeIconRenderer extends DefaultTreeCellRenderer {

        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
            super.getTreeCellRendererComponent(tree, value, selected,
                                               expanded, leaf, row, hasFocus);
            Icon ico = ((TreeSpot)value).getIcon();
            if (ico != null)
                setIcon(ico);
            
            return this;
        }

    }

    private static class CenteredIntegerTableCellRenderer extends DefaultTableCellRenderer {
        public Component getTableCellRendererComponent(JTable table, Object value,
                          boolean isSelected, boolean hasFocus, int row, int column)
        {
            Component c = super.getTableCellRendererComponent(table,
                  value, isSelected, hasFocus, row, column);
            if (c instanceof JLabel) {
                ((JLabel)c).setHorizontalAlignment(JLabel.CENTER);
            }
            return c;
        }
    }

    private static class OptionalBooleanTableCellRenderer extends DefaultTableRenderer {

        public OptionalBooleanTableCellRenderer() {
            super(new CheckBoxProvider());
        }

        private DefaultTableRenderer _blank = new DefaultTableRenderer();
        public Component getTableCellRendererComponent(JTable table, Object value,
                          boolean isSelected, boolean hasFocus, int row, int column)
        {
            if ( value != null )
                return super.getTableCellRendererComponent(table,
                        value, isSelected, hasFocus, row, column);

            return _blank.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        }
    }

}
