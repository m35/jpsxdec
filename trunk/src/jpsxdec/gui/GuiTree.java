/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2014  Michael Sabin
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

import java.awt.Component;
import java.awt.FontMetrics;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
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
import jpsxdec.I18N;
import jpsxdec.discitems.DiscItem;
import jpsxdec.discitems.DiscItemAudioStream;
import jpsxdec.discitems.DiscItemISO9660File;
import jpsxdec.discitems.DiscItemSaverBuilder;
import jpsxdec.discitems.DiscItemTim;
import jpsxdec.discitems.DiscItemVideoStream;
import jpsxdec.discitems.IDiscItemSaver;
import jpsxdec.indexing.DiscIndex;
import jpsxdec.util.player.PlayController;
import org.jdesktop.swingx.JXTreeTable;
import org.jdesktop.swingx.renderer.CheckBoxProvider;
import org.jdesktop.swingx.renderer.DefaultTableRenderer;
import org.jdesktop.swingx.treetable.TreeTableModel;

/** Subclassed {@link JXTreeTable} that maintains my own view-model and other tweaks. */
public class GuiTree extends JXTreeTable {

    public static final Icon FILE_ICON =
            new ImageIcon(GuiTree.class.getResource("layer-new-3.png"));
            //UIManager.getIcon("FileChooser.fileIcon");
            //UIManager.getIcon("Tree.leafIcon");
    public static final Icon FOLDER_ICON = UIManager.getIcon("FileView.folderIcon");
    public static final ImageIcon VIDEO_ICON = new ImageIcon(GuiTree.class.getResource("film.png"));
    public static final ImageIcon AUDIO_ICON = new ImageIcon(GuiTree.class.getResource("knotify.png"));
    public static final ImageIcon IMAGE_ICON = new ImageIcon(GuiTree.class.getResource("image-x-generic.png"));

    public static enum Select {
        NONE(I18N.S("none")), // I18N
        ALL_VIDEO(I18N.S("all Video")), // I18N
        ALL_AUDIO(I18N.S("all Audio (excluding video audio)")), // I18N
        ALL_AUDIO_VIDEO(I18N.S("all Audio (including video audio)")), // I18N
        ALL_FILES(I18N.S("all Files")), // I18N
        ALL_IMAGES(I18N.S("all Images")); // I18N

        private final String _str;

        private Select(String str) { _str = str; }

        public String toString() { return _str; }
    }

    private RootTreeItem _root;

    public void formatTreeTable(DiscIndex index) {
        _root = buildTree(index.getRoot());

        /* If using Netbeans Outline
        setDefaultRenderer(Boolean.class, new OptionalBooleanTableCellRenderer());
        DiscTreeModel ttModel = new DiscTreeModel(_root);
        OutlineModel om = DefaultOutlineModel.createOutlineModel(ttModel, ttModel);
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        setFillsViewportHeight(true);
        setIntercellSpacing(new Dimension(0, 0));
        setShowGrid(false);
        setRootVisible(false);
        setModel(om);
        setRowSorter(null);
        setRowHeight(getRowHeight() - 2);
        /*/

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
        //*/
    }

    //public void expandAll() {} public void collapseAll() {}
    //public void addTreeSelectionListener(TreeSelectionListener tsl) {}

    public TreeItem getTreeTblSelection() {
        return (TreeItem) getValueAt(getSelectedRow(), convertColumnIndexToView(COLUMNS.Name.ordinal()));
    }

    public void selectAllType(Select cmd) {
        _root.selectAllType(cmd);
        repaint();
    }

    public int applySettings(DiscItemSaverBuilder builder) {
        return _root.applySettings(builder);
    }

    public ArrayList<IDiscItemSaver> collectSelected(File dir) {
        ArrayList<IDiscItemSaver> savers = new ArrayList<IDiscItemSaver>();
        _root.collectSelected(savers, dir);
        return savers;
    }

    // #########################################################################

    private enum COLUMNS {

        Num(Integer.class) {
            Object val(TreeItem item) {
                return item.getIndexNum();
            }
            public String toString() {
                return "#";
            }
        },
        Save(Boolean.class) { Object val(TreeItem item) {
            return item.getSave();
        }},
        Name(TreeItem.class) {
            Object val(TreeItem item) {
                return item;
            }
            public String toString() {
                return "";
            }
        },
        Type(String.class) { Object val(TreeItem item) {
            return item.getType();
        }},
        Sectors(String.class) { Object val(TreeItem item) {
            return item.getSectorRange();
        }},
        Details(String.class) { Object val(TreeItem item) {
            return item.getDetails();
        }},
        ;
        private final Class _type;
        COLUMNS(Class type) {
            _type = type;
        }
        abstract Object val(TreeItem item);
    }

    // -------------------------------------------------------------------------

    public static abstract class TreeItem {

        protected ArrayList<TreeItem> _kids = new ArrayList<TreeItem>();

        public TreeItem getKid(int childIndex) {
            return _kids.get(childIndex);
        }

        public int kidCount() {
            return _kids.size();
        }

        public int indexOf(Object node) {
            return _kids.indexOf(node);
        }

        public void addKid(TreeItem kid) {
            _kids.add(kid);
        }

        public Iterator<TreeItem> iterator() {
            return _kids.iterator();
        }

        private DirectoryTreeItem getOrCreateDir(String sName) {
            for (TreeItem node : _kids) {
                if (node instanceof DirectoryTreeItem) {
                    DirectoryTreeItem dirNode = (DirectoryTreeItem) node;
                    if ( dirNode.getName().equals(sName) ) {
                        return dirNode;
                    }
                }
            }
            DirectoryTreeItem dirNode = new DirectoryTreeItem(sName);
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

        public void collectSelected(final ArrayList<IDiscItemSaver> savers, File dir) {
            Boolean oblnSave = getSave();
            if (oblnSave != null && oblnSave.booleanValue())
                savers.add(getBuilder().makeSaver(dir));
            for (int i=0; i<kidCount(); i++)
                getKid(i).collectSelected(savers, dir);
        }

        public int applySettings(DiscItemSaverBuilder otherBuilder) {
            int iCount = 0;
            DiscItemSaverBuilder thisBuilder = getBuilder();
            if (thisBuilder != null) {
                if (otherBuilder.copySettingsTo(thisBuilder))
                    iCount++;
            }
            for (int i = 0; i < kidCount(); i++)
                iCount += getKid(i).applySettings(otherBuilder);
            return iCount;
        }

        public void selectAllType(Select cmd) {
            for (int i=0; i< kidCount(); i++)
                getKid(i).selectAllType(cmd);
        }

    }

    private static class RootTreeItem extends TreeItem {
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

    private static class DirectoryTreeItem extends RootTreeItem {

        private String _sDirName;

        public DirectoryTreeItem(String _sDirName) {
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

    private static class DiscItemTreeItem extends TreeItem {

        private DiscItem _item;
        private DiscItemSaverBuilder _builder;
        private boolean _blnSave = false;

        public DiscItemTreeItem(DiscItem item) {
            _item = item;
            if (item.getChildCount() > 0) {
                // recursively add the kids
                for (DiscItem child : item.getChildren()) {
                    _kids.add(new DiscItemTreeItem(child));
                }
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
            // Optional TODO: let the item create the icon
            if (_item instanceof DiscItemVideoStream) {
                return VIDEO_ICON;
            } else if (_item instanceof DiscItemISO9660File) {
                return FILE_ICON;
            } else if (_item instanceof DiscItemAudioStream) {
                return AUDIO_ICON;
            } else if (_item instanceof DiscItemTim) {
                return IMAGE_ICON;
            } else
                return null;
        }

        public String toString() {
            return _item.getIndexId().getTopLevel();
        }

        @Override
        public String getIndexNum() {
            return String.valueOf(_item.getIndex());
        }

        @Override
        public String getType() {
            return _item.getType().name();
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
            if (_item instanceof DiscItemVideoStream) {
                return ((DiscItemVideoStream)_item).makePlayController();
            } else if (_item instanceof DiscItemAudioStream) {
                return ((DiscItemAudioStream)_item).makePlayController();
            } else {
                return null;
            }
        }

        @Override
        public boolean canPlay() {
            return _item instanceof DiscItemVideoStream || _item instanceof DiscItemAudioStream;
        }

        @Override
        public void selectAllType(Select cmd) {
            // TODO: is there a better way to select by type?
            if (cmd == Select.NONE)
                _blnSave = false;
            else if (cmd == Select.ALL_VIDEO)
                _blnSave = _blnSave || getItem().getType() == DiscItem.GeneralType.Video;
            else if (cmd == Select.ALL_AUDIO)
                _blnSave = _blnSave || (getItem() instanceof DiscItemAudioStream
                                        && !((DiscItemAudioStream)getItem()).isPartOfVideo());
            else if (cmd == Select.ALL_AUDIO_VIDEO)
                _blnSave = _blnSave || getItem().getType() == DiscItem.GeneralType.Audio;
            else if (cmd == Select.ALL_FILES)
                _blnSave = _blnSave || getItem().getType() == DiscItem.GeneralType.File;
            else if (cmd == Select.ALL_IMAGES)
                _blnSave = _blnSave || getItem().getType() == DiscItem.GeneralType.Image;
            super.selectAllType(cmd);
        }


    }

    // .........................................................................

    private static RootTreeItem buildTree(List<DiscItem> discItems) {
        RootTreeItem root = new RootTreeItem();
        for (DiscItem item : discItems) {
            if (item.getIndexId().getFile() != null) {
                String[] asDirs = splitFileDirs(item.getIndexId().getFile());

                TreeItem tree = root;
                for (String sDir : asDirs) {
                    tree = tree.getOrCreateDir(sDir);
                }
                tree.addKid(new DiscItemTreeItem(item));
            } else {
                root.addKid(new DiscItemTreeItem(item));
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

        Collections.reverse(dirs);
        return dirs.toArray(new String[dirs.size()]);
    }

    // -------------------------------------------------------------------------

    private static class DiscTreeModel implements TreeTableModel
        //, RowModel // Outline
    {
        private TreeItem _treeRoot;

        public DiscTreeModel(TreeItem _treeRoot) {
            this._treeRoot = _treeRoot;
        }

        public int getHierarchicalColumn() {
            return COLUMNS.Name.ordinal();
        }

        public TreeItem getRoot() {
            return _treeRoot;
        }

        public TreeItem getChild(Object parent, int index) {
            return ((TreeItem)parent).getKid(index);
        }

        public int getChildCount(Object parent) {
            return ((TreeItem)parent).kidCount();
        }

        public boolean isLeaf(Object node) {
            return ((TreeItem)node).kidCount() == 0;
        }

        public int getIndexOfChild(Object parent, Object child) {
            return ((TreeItem)parent).indexOf(child);
        }

        public void addTreeModelListener(TreeModelListener l) {}
        public void removeTreeModelListener(TreeModelListener l) {}
        public void valueForPathChanged(TreePath path, Object newValue) {}

        public boolean isCellEditable(Object o, int i) {
            return (i == COLUMNS.Save.ordinal()) && o instanceof DiscItemTreeItem;
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
            return COLUMNS.values()[i].val((TreeItem)o);
        }

        public void setValueAt(Object value, Object node, int i) {
            ((DiscItemTreeItem)node).setSave(((Boolean)value).booleanValue());
        }

        public Object getValueFor(Object node, int column) {
            return getValueAt(node, column);
        }

        public void setValueFor(Object node, int column, Object value) {
            setValueAt(value, node, column);
        }
    }

    // -------------------------------------------------------------------------

    private static class TreeIconRenderer extends DefaultTreeCellRenderer {

        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
            super.getTreeCellRendererComponent(tree, value, selected,
                                               expanded, leaf, row, hasFocus);
            Icon ico = ((TreeItem)value).getIcon();
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
