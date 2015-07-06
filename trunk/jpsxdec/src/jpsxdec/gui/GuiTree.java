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

package jpsxdec.gui;

import java.awt.Component;
import java.awt.FontMetrics;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.event.TreeModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreePath;
import jpsxdec.i18n.I;
import jpsxdec.i18n.LocalizedMessage;
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

/** Subclassed {@link JXTreeTable} that maintains my own view-model and other tweaks. 
 * Note that {@link #formatTreeTable(jpsxdec.indexing.DiscIndex)} MUST be called
 * before using the object. */
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
        NONE(I.GUI_SELECT_NONE()),
        ALL_VIDEO(I.GUI_SELECT_ALL_VIDEO()),
        ALL_AUDIO(I.GUI_SELECT_ALL_AUIO_EX_VID()),
        ALL_AUDIO_VIDEO(I.GUI_SELECT_ALL_AUDIO_INC_VID()),
        ALL_FILES(I.GUI_SELECT_ALL_FILES()),
        ALL_IMAGES(I.GUI_SELECT_ALL_IMAGES());

        @Nonnull
        private final LocalizedMessage _str;

        private Select(@Nonnull LocalizedMessage str) { _str = str; }

        /** {@inheritDoc}
         *<p>
         * Used in a list control, so toString() must be localized. */
        public String toString() { return _str.getLocalizedMessage(); }
    }

    @CheckForNull
    private RootTreeItem _root;

    public void formatTreeTable(@Nonnull DiscIndex index) {
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
        / */

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
        TableColumnModel colMod = getColumnModel();
        colMod.getColumn(COLUMNS.Name.ordinal()).setPreferredWidth(iNameWidth + 10);
        colMod.getColumn(COLUMNS.Num.ordinal()).setPreferredWidth(iNumberWidth + 10);
        colMod.getColumn(COLUMNS.Sectors.ordinal()).setPreferredWidth(iSectorWidth + 10);
        TableColumn detailsCol = colMod.getColumn(COLUMNS.Details.ordinal());
        detailsCol.setPreferredWidth(Math.max(250, detailsCol.getWidth()));
        // */
    }

    //public void expandAll() {} public void collapseAll() {}
    //public void addTreeSelectionListener(TreeSelectionListener tsl) {}

    public @CheckForNull TreeItem getTreeTblSelection() {
        return (TreeItem) getValueAt(getSelectedRow(), convertColumnIndexToView(COLUMNS.Name.ordinal()));
    }

    public void selectAllType(@Nonnull Select cmd) {
        _root.selectAllType(cmd);
        repaint();
    }

    public int applySettings(@Nonnull DiscItemSaverBuilder builder) {
        return _root.applySettings(builder);
    }

    public @Nonnull ArrayList<IDiscItemSaver> collectSelected(@CheckForNull File dir) {
        ArrayList<IDiscItemSaver> savers = new ArrayList<IDiscItemSaver>();
        _root.collectSelected(savers, dir);
        return savers;
    }

    // #########################################################################

    private enum COLUMNS {

        // use Integer to get nicer alignment
        Num(Integer.class, I.GUI_TREE_INDEX_NUMBER_COLUMN()) {
            String val(TreeItem item) { return item.getIndexNum(); }
        },
        Save(Boolean.class, I.GUI_TREE_SAVE_COLUMN()) {
            Boolean val(TreeItem item) { return item.getSave(); }
        },
        Name(TreeItem.class, I.GUI_TREE_NAME_COLUMN()) {
            TreeItem val(TreeItem item) { return item; }
        },
        Type(String.class, I.GUI_TREE_TYPE_COLUMN()) {
            String val(TreeItem item) {
                DiscItem.GeneralType type = item.getType();
                if (type == null)
                    return null;
                return type.getName().getLocalizedMessage();
            }
        },
        Sectors(String.class, I.GUI_SECTORS_COLUMN()) {
            String val(TreeItem item) { return item.getSectorRange(); }
        },
        Details(String.class, I.GUI_TREE_DETAILS_COLUMN()) {
            String val(TreeItem item) { 
                LocalizedMessage details = item.getDetails();
                return details == null ? null : details.getLocalizedMessage();
            }
        };

        @Nonnull
        private final Class _type;
        @Nonnull
        private final LocalizedMessage _str;
        private COLUMNS(@Nonnull Class type, @Nonnull LocalizedMessage str) {
            _type = type;
            _str = str;
        }
        abstract @CheckForNull Object val(@Nonnull TreeItem item);

        /** {@inheritDoc}
         *<p>
         * Must be localized so {@link JXTreeTable#getColumn(java.lang.Object)}
         * can find it.
         * @see GuiTree#formatTreeTable(jpsxdec.indexing.DiscIndex) */
        @Override
        public String toString() {
            return _str.getLocalizedMessage();
        }
    }

    // -------------------------------------------------------------------------

    public static abstract class TreeItem {

        protected ArrayList<TreeItem> _kids = new ArrayList<TreeItem>();

        public @Nonnull TreeItem getKid(int childIndex) {
            return _kids.get(childIndex);
        }

        public int kidCount() {
            return _kids.size();
        }

        public int indexOf(@Nonnull Object node) {
            return _kids.indexOf(node);
        }

        public void addKid(@Nonnull TreeItem kid) {
            _kids.add(kid);
        }

        public @Nonnull Iterator<TreeItem> iterator() {
            return _kids.iterator();
        }

        private @Nonnull DirectoryTreeItem getOrCreateDir(@Nonnull String sName) {
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

        abstract public @CheckForNull Icon getIcon();

        abstract public @CheckForNull DiscItemSaverBuilder getBuilder();
        abstract public @Nonnull String getIndexNum();
        abstract public @CheckForNull DiscItem.GeneralType getType();
        abstract public @CheckForNull LocalizedMessage getDetails();
        abstract public @Nonnull String getSectorRange();

        abstract public boolean canPlay();
        abstract public @CheckForNull PlayController getPlayer();
        abstract public @CheckForNull DiscItem getItem();
        abstract public @CheckForNull Boolean getSave();

        public void collectSelected(@Nonnull final ArrayList<IDiscItemSaver> savers,
                                    @CheckForNull File dir)
        {
            Boolean oblnSave = getSave();
            if (oblnSave != null && oblnSave.booleanValue()) {
                DiscItemSaverBuilder thisBuilder = getBuilder();
                if (thisBuilder != null)
                    savers.add(thisBuilder.makeSaver(dir));
            }
            for (int i=0; i<kidCount(); i++)
                getKid(i).collectSelected(savers, dir);
        }

        public int applySettings(@Nonnull DiscItemSaverBuilder otherBuilder) {
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

        public void selectAllType(@Nonnull Select cmd) {
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
        public LocalizedMessage getDetails() { return null; }
        @Override
        public String getIndexNum() { return ""; }
        @Override
        public DiscItem.GeneralType getType() { return null; }
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

        @Nonnull
        private final String _sDirName;

        public DirectoryTreeItem(@Nonnull String sDirName) {
            _sDirName = sDirName;
        }

        private @Nonnull Object getName() { return _sDirName; }

        @Override
        public Icon getIcon() { return null; }

        public String toString() {
            return _sDirName + "/";
        }
    }

    private static class DiscItemTreeItem extends TreeItem {

        @Nonnull
        private final DiscItem _item;
        @CheckForNull
        private DiscItemSaverBuilder _builder;
        private boolean _blnSave = false;

        public DiscItemTreeItem(@Nonnull DiscItem item) {
            _item = item;
            if (item.getChildCount() > 0) {
                // recursively add the kids
                for (DiscItem child : item.getChildren()) {
                    _kids.add(new DiscItemTreeItem(child));
                }
            }
        }

        @Override
        public @Nonnull DiscItem getItem() {
            return _item;
        }

        public @Nonnull DiscItemSaverBuilder getBuilder() {
            if (_builder == null) {
                _builder = _item.makeSaverBuilder();
            }
            return _builder;
        }

        @Override
        public @CheckForNull Icon getIcon() {
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
        public @Nonnull String getIndexNum() {
            return String.valueOf(_item.getIndex());
        }

        @Override
        public @Nonnull DiscItem.GeneralType getType() {
            return _item.getType();
        }

        @Override
        public @Nonnull LocalizedMessage getDetails() {
            return _item.getInterestingDescription();
        }

        public @CheckForNull Boolean getSave() {
            return _blnSave;
        }
        @Override
        public @Nonnull String getSectorRange() {
            return _item.getStartSector() + "-" + _item.getEndSector();
        }

        public void setSave(boolean value) {
            _blnSave = value;
        }

        @Override
        public @CheckForNull PlayController getPlayer() {
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
        public void selectAllType(@Nonnull Select cmd) {
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

    private static @Nonnull RootTreeItem buildTree(@Nonnull List<DiscItem> discItems) {
        RootTreeItem root = new RootTreeItem();
        for (DiscItem item : discItems) {
            File file = item.getIndexId().getFile();
            if (file != null) {
                String[] asDirs = splitFileDirs(file);

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

    private static @Nonnull String[] splitFileDirs(@Nonnull File file) {
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
        @Nonnull
        private final TreeItem _treeRoot;

        public DiscTreeModel(@Nonnull TreeItem treeRoot) {
            _treeRoot = treeRoot;
        }

        public int getHierarchicalColumn() {
            return COLUMNS.Name.ordinal();
        }

        public @Nonnull TreeItem getRoot() {
            return _treeRoot;
        }

        public @Nonnull TreeItem getChild(@Nonnull Object parent, int index) {
            return ((TreeItem)parent).getKid(index);
        }

        public int getChildCount(@Nonnull Object parent) {
            return ((TreeItem)parent).kidCount();
        }

        public boolean isLeaf(@Nonnull Object node) {
            return ((TreeItem)node).kidCount() == 0;
        }

        public int getIndexOfChild(@Nonnull Object parent, @Nonnull Object child) {
            return ((TreeItem)parent).indexOf(child);
        }

        public void addTreeModelListener(TreeModelListener l) {}
        public void removeTreeModelListener(TreeModelListener l) {}
        public void valueForPathChanged(TreePath path, Object newValue) {}

        public boolean isCellEditable(@Nonnull Object o, int i) {
            return (i == COLUMNS.Save.ordinal()) && o instanceof DiscItemTreeItem;
        }

        public int getColumnCount() {
            return COLUMNS.values().length;
        }

        public @Nonnull Class<?> getColumnClass(int i) {
            return COLUMNS.values()[i]._type;
        }

        public @Nonnull String getColumnName(int i) {
            return COLUMNS.values()[i]._str.getLocalizedMessage();
        }
        
        public @CheckForNull Object getValueAt(@Nonnull Object o, int i) {
            return COLUMNS.values()[i].val((TreeItem)o);
        }

        public void setValueAt(@Nonnull Object value, @Nonnull Object node, int i) {
            assert i == COLUMNS.Save.ordinal();
            ((DiscItemTreeItem)node).setSave(((Boolean)value).booleanValue());
        }

        // for Netbeans Outline
        public @CheckForNull Object getValueFor(@Nonnull Object node, int column) {
            return getValueAt(node, column);
        }

        // for Netbeans Outline
        public void setValueFor(@Nonnull Object node, int column, @Nonnull Object value) {
            setValueAt(value, node, column);
        }
    }

    // -------------------------------------------------------------------------

    private static class TreeIconRenderer extends DefaultTreeCellRenderer {

        public @Nonnull Component getTreeCellRendererComponent(JTree tree,
                                                               @Nonnull Object value,
                                                               boolean selected,
                                                               boolean expanded,
                                                               boolean leaf, int row,
                                                               boolean hasFocus)
        {
            super.getTreeCellRendererComponent(tree, value, selected,
                                               expanded, leaf, row, hasFocus);
            Icon ico = ((TreeItem)value).getIcon();
            if (ico != null)
                setIcon(ico);
            
            return this;
        }

    }

    private static class CenteredIntegerTableCellRenderer extends DefaultTableCellRenderer {
        public @Nonnull Component getTableCellRendererComponent(JTable table,
                                                                Object value,
                                                                boolean isSelected,
                                                                boolean hasFocus,
                                                                int row, int column)
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

        private final DefaultTableRenderer _blank = new DefaultTableRenderer();
        public @Nonnull Component getTableCellRendererComponent(JTable table,
                                                                Object value,
                                                                boolean isSelected,
                                                                boolean hasFocus,
                                                                int row, int column)
        {
            if ( value != null )
                return super.getTableCellRendererComponent(table,
                        value, isSelected, hasFocus, row, column);

            return _blank.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        }
    }

}
