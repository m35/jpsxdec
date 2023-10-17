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

import java.awt.Component;
import java.awt.FontMetrics;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
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
import jpsxdec.discitems.DiscItem;
import jpsxdec.discitems.DiscItemSaverBuilder;
import jpsxdec.i18n.I;
import jpsxdec.i18n.ILocalizedMessage;
import jpsxdec.indexing.DiscIndex;
import jpsxdec.modules.audio.sectorbased.DiscItemSectorBasedAudioStream;
import jpsxdec.modules.spu.DiscIndexerSpu;
import jpsxdec.modules.video.DiscItemVideoStream;
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
    public static final ImageIcon SOUND_ICON = new ImageIcon(GuiTree.class.getResource("audio-volume-medium-right.png")); // SPU support

    public static enum Select {
        NONE(I.GUI_SELECT_NONE()),
        ALL_VIDEO(I.GUI_SELECT_ALL_VIDEO()),
        ALL_AUDIO(I.GUI_SELECT_ALL_AUIO_EX_VID()),
        ALL_AUDIO_VIDEO(I.GUI_SELECT_ALL_AUDIO_INC_VID()),
        ALL_FILES(I.GUI_SELECT_ALL_FILES()),
        ALL_IMAGES(I.GUI_SELECT_ALL_IMAGES()),
        ALL_SOUND(I.GUI_SELECT_ALL_SOUNDS()), // SPU support
        ;

        public static Select[] getAvailableValues() {
            Select[] aoValues = values();
            if (DiscIndexerSpu.ENABLE_SPU_SUPPORT) {
                return aoValues;
            } else {
                return Arrays.copyOfRange(aoValues, 0, aoValues.length - 1);
            }
        }

        @Nonnull
        private final ILocalizedMessage _str;

        private Select(@Nonnull ILocalizedMessage str) { _str = str; }

        /** {@inheritDoc}
         *<p>
         * Used in a list control, so toString() must be localized. */
        @Override
        public String toString() { return _str.getLocalizedMessage(); }
    }

    @CheckForNull
    private RootTreeItem _root;

    public void clearTreeTable() {
        _root = buildTree(Collections.<DiscItem>emptyList());
        setTreeTableModel(new DiscTreeModel(_root));
    }
    public void formatTreeTable(@Nonnull DiscIndex index) {
        _root = buildTree(index.getRoot());

        FontMetrics fm = getFontMetrics(getFont());
        int iSectorWidth = fm.stringWidth("999999-999999");
        int iNumberWidth = fm.stringWidth(String.valueOf(index.size()) + "99");
        int iNameWidth = fm.stringWidth("MMMMMMMM.MMM[99.9.9]") + 25*3;
        int iTypeWidth = fm.stringWidth(DiscItem.GeneralType.Sound.getName().toString());

        setDefaultRenderer(Boolean.class, new OptionalBooleanTableCellRenderer());
        setTreeCellRenderer(new TreeIconRenderer());
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(JLabel.RIGHT);

        setTreeTableModel(new DiscTreeModel(_root));
        packAll();
        TableColumnModel colMod = getColumnModel();
        colMod.getColumn(COLUMNS.Name.ordinal()).setPreferredWidth(iNameWidth + 10);
        colMod.getColumn(COLUMNS.Num.ordinal()).setPreferredWidth(iNumberWidth + 10);
        colMod.getColumn(COLUMNS.Num.ordinal()).setCellRenderer(rightRenderer);
        colMod.getColumn(COLUMNS.Sectors.ordinal()).setPreferredWidth(iSectorWidth + 10);
        colMod.getColumn(COLUMNS.Type.ordinal()).setPreferredWidth(iTypeWidth + 10);
        TableColumn detailsCol = colMod.getColumn(COLUMNS.Details.ordinal());
        detailsCol.setPreferredWidth(Math.max(300, detailsCol.getWidth()));
    }

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

    public @Nonnull ArrayList<DiscItemSaverBuilder> collectSelected() {
        ArrayList<DiscItemSaverBuilder> builders = new ArrayList<DiscItemSaverBuilder>();
        _root.collectSelected(builders);
        return builders;
    }

    // #########################################################################

    private enum COLUMNS {

        Num(String.class, I.GUI_TREE_INDEX_NUMBER_COLUMN()) {
            // use String to avoid localizing (',') the number
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
                ILocalizedMessage details = item.getDetails();
                return details == null ? null : details.getLocalizedMessage();
            }
        };

        @Nonnull
        private final Class<?> _type;
        @Nonnull
        private final ILocalizedMessage _str;
        private COLUMNS(@Nonnull Class<?> type, @Nonnull ILocalizedMessage str) {
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

        protected final ArrayList<TreeItem> _kids = new ArrayList<TreeItem>();

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
        abstract public @CheckForNull ILocalizedMessage getDetails();
        abstract public @Nonnull String getSectorRange();

        abstract public boolean canPlay();
        abstract public @CheckForNull PlayController getPlayer();
        abstract public @CheckForNull DiscItem getItem();
        abstract public @CheckForNull Boolean getSave();

        public void collectSelected(@Nonnull final ArrayList<DiscItemSaverBuilder> builders) {
            Boolean oblnSave = getSave();
            if (oblnSave != null && oblnSave.booleanValue()) {
                DiscItemSaverBuilder thisBuilder = getBuilder();
                if (thisBuilder != null)
                    builders.add(thisBuilder);
            }
            for (int i=0; i<kidCount(); i++)
                getKid(i).collectSelected(builders);
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
        @Override
        public String toString() {
            return "[ROOT]";
        }
        @Override
        public ILocalizedMessage getDetails() { return null; }
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

        @Override
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

        @Override
        public @Nonnull DiscItemSaverBuilder getBuilder() {
            if (_builder == null) {
                _builder = _item.makeSaverBuilder();
            }
            return _builder;
        }

        @Override
        public @CheckForNull Icon getIcon() {
            switch (_item.getType()) {
                case Audio: return AUDIO_ICON;
                case File: return FILE_ICON;
                case Image: return IMAGE_ICON;
                case Video: return VIDEO_ICON;
                case Sound: return SOUND_ICON; // SPU support
                default: return null;
            }
        }

        @Override
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
        public @Nonnull ILocalizedMessage getDetails() {
            return _item.getInterestingDescription();
        }

        @Override
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
            } else if (_item instanceof DiscItemSectorBasedAudioStream) {
                return ((DiscItemSectorBasedAudioStream)_item).makePlayController();
            } else {
                return null;
            }
        }

        @Override
        public boolean canPlay() {
            return _item instanceof DiscItemVideoStream || _item instanceof DiscItemSectorBasedAudioStream;
        }

        @Override
        public void selectAllType(@Nonnull Select cmd) {
            // TODO: is there a better way to select by type?
            if (cmd == Select.NONE)
                _blnSave = false;
            else if (cmd == Select.ALL_VIDEO)
                _blnSave = _blnSave || getItem().getType() == DiscItem.GeneralType.Video;
            else if (cmd == Select.ALL_AUDIO)
                _blnSave = _blnSave || (getItem() instanceof DiscItemSectorBasedAudioStream
                                        && !((DiscItemSectorBasedAudioStream)getItem()).isPartOfVideo());
            else if (cmd == Select.ALL_AUDIO_VIDEO)
                _blnSave = _blnSave || getItem().getType() == DiscItem.GeneralType.Audio;
            else if (cmd == Select.ALL_FILES)
                _blnSave = _blnSave || getItem().getType() == DiscItem.GeneralType.File;
            else if (cmd == Select.ALL_IMAGES)
                _blnSave = _blnSave || getItem().getType() == DiscItem.GeneralType.Image;
            else if (cmd == Select.ALL_SOUND) // SPU support
                _blnSave = _blnSave || getItem().getType() == DiscItem.GeneralType.Sound;
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

    private static class DiscTreeModel implements TreeTableModel {
        @Nonnull
        private final TreeItem _treeRoot;

        public DiscTreeModel(@Nonnull TreeItem treeRoot) {
            _treeRoot = treeRoot;
        }

        @Override
        public int getHierarchicalColumn() {
            return COLUMNS.Name.ordinal();
        }

        @Override
        public @Nonnull TreeItem getRoot() {
            return _treeRoot;
        }

        @Override
        public @Nonnull TreeItem getChild(@Nonnull Object parent, int index) {
            return ((TreeItem)parent).getKid(index);
        }

        @Override
        public int getChildCount(@Nonnull Object parent) {
            return ((TreeItem)parent).kidCount();
        }

        @Override
        public boolean isLeaf(@Nonnull Object node) {
            return ((TreeItem)node).kidCount() == 0;
        }

        @Override
        public int getIndexOfChild(@Nonnull Object parent, @Nonnull Object child) {
            return ((TreeItem)parent).indexOf(child);
        }

        @Override
        public void addTreeModelListener(TreeModelListener l) {}
        @Override
        public void removeTreeModelListener(TreeModelListener l) {}
        @Override
        public void valueForPathChanged(TreePath path, Object newValue) {}

        @Override
        public boolean isCellEditable(@Nonnull Object o, int i) {
            return (i == COLUMNS.Save.ordinal()) && o instanceof DiscItemTreeItem;
        }

        @Override
        public int getColumnCount() {
            return COLUMNS.values().length;
        }

        @Override
        public @Nonnull Class<?> getColumnClass(int i) {
            return COLUMNS.values()[i]._type;
        }

        @Override
        public @Nonnull String getColumnName(int i) {
            return COLUMNS.values()[i]._str.getLocalizedMessage();
        }

        @Override
        public @CheckForNull Object getValueAt(@Nonnull Object o, int i) {
            return COLUMNS.values()[i].val((TreeItem)o);
        }

        @Override
        public void setValueAt(@Nonnull Object value, @Nonnull Object node, int i) {
            assert i == COLUMNS.Save.ordinal();
            ((DiscItemTreeItem)node).setSave(((Boolean)value).booleanValue());
        }
    }

    // -------------------------------------------------------------------------

    private static class TreeIconRenderer extends DefaultTreeCellRenderer {

        @Override
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
        @Override
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
        @Override
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
