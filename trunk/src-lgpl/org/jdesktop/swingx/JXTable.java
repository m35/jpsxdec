/*
 * $Id: JXTable.java,v 1.271 2009/05/07 09:21:22 kleopatra Exp $
 *
 * Copyright 2004 Sun Microsystems, Inc., 4150 Network Circle,
 * Santa Clara, California 95054, U.S.A. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */

package org.jdesktop.swingx;

import java.applet.Applet;
import java.awt.Color;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.print.PrinterException;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.EventObject;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.DefaultCellEditor;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JViewport;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SizeSequence;
import javax.swing.SwingUtilities;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.border.LineBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableModelEvent;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

import org.jdesktop.swingx.action.AbstractActionExt;
import org.jdesktop.swingx.action.BoundAction;
import org.jdesktop.swingx.decorator.ComponentAdapter;
import org.jdesktop.swingx.decorator.CompoundHighlighter;
import org.jdesktop.swingx.decorator.DefaultSelectionMapper;
import org.jdesktop.swingx.decorator.FilterPipeline;
import org.jdesktop.swingx.decorator.Highlighter;
import org.jdesktop.swingx.decorator.PipelineEvent;
import org.jdesktop.swingx.decorator.PipelineListener;
import org.jdesktop.swingx.decorator.ResetDTCRColorHighlighter;
import org.jdesktop.swingx.decorator.SelectionMapper;
import org.jdesktop.swingx.decorator.SizeSequenceMapper;
import org.jdesktop.swingx.decorator.SortController;
import org.jdesktop.swingx.decorator.SortKey;
import org.jdesktop.swingx.decorator.SortOrder;
import org.jdesktop.swingx.decorator.UIDependent;
import org.jdesktop.swingx.event.TableColumnModelExtListener;
import org.jdesktop.swingx.plaf.LookAndFeelAddons;
import org.jdesktop.swingx.plaf.UIManagerExt;
import org.jdesktop.swingx.renderer.AbstractRenderer;
import org.jdesktop.swingx.renderer.CheckBoxProvider;
import org.jdesktop.swingx.renderer.DefaultTableRenderer;
import org.jdesktop.swingx.renderer.IconValues;
import org.jdesktop.swingx.renderer.MappedValue;
import org.jdesktop.swingx.renderer.StringValue;
import org.jdesktop.swingx.renderer.StringValues;
import org.jdesktop.swingx.rollover.RolloverProducer;
import org.jdesktop.swingx.rollover.TableRolloverController;
import org.jdesktop.swingx.rollover.TableRolloverProducer;
import org.jdesktop.swingx.table.ColumnControlButton;
import org.jdesktop.swingx.table.ColumnFactory;
import org.jdesktop.swingx.table.DefaultTableColumnModelExt;
import org.jdesktop.swingx.table.TableColumnExt;
import org.jdesktop.swingx.table.TableColumnModelExt;

/**
 * Enhanced Table component with support for general SwingX sorting/filtering,
 * rendering, highlighting, rollover and search functionality. Table specific
 * enhancements include runtime configuration options like toggle column
 * visibility, column sizing, PENDING JW ...
 * 
 * <h2>Sorting and Filtering</h2>
 * 
 * JXTable supports sorting and filtering of rows. 
 * 
 * Sorting support is single column only. It provides api to apply
 * a specific sort order or to toggle the sort order of columns identified 
 * by view index or column identifier or reset all sorts. 
 * 
 * <pre><code>
 * table.setSortOrder("PERSON_ID", SortOrder.DESCENDING);
 * table.toggleSortOder(4);
 * table.resetSortOrder();
 * </code></pre>
 * 
 * Sorting sequence can be configured per column by setting the TableColumnExt's
 * "comparator" property. Sorting can be disabled per column or per table by
 * {@link #setSortable(boolean)}. 
 * 
 * <p>
 * Typically, a JXTable is sortable by left clicking on column headers. By default, each
 * subsequent click on a header reverses the order of the sort, and a sort arrow
 * icon is automatically drawn on the header. The exact mapping of a user gesture to
 * a sort effect is configurable by installing a custom SortGestureRecognizer on the
 * JXTableHeader.
 * 
 * <p>
 * Rows can be filtered from a JXTable using a Filter class and a
 * FilterPipeline. One assigns a FilterPipeline to the table using
 * {@link #setFilters(FilterPipeline)}. Filtering hides, but does not delete nor
 * permanently remove rows from a JXTable. Filters are used to provide sorting
 * to the table--rows are not removed, but the table is made to believe rows in
 * the model are in a sorted order.
 * 
 * <b>NOTE:</b> SwingX sorting/filtering is incompatible with core sorting/filtering in 
 * JDK 6+. Will be replaced by core functionality after switching the target jdk
 * version from 5 to 6.
 * 
 * <h2>Rendering and Highlighting</h2>
 * 
 * As all SwingX collection views, a JXTable is a HighlighterClient (PENDING JW:
 * formally define and implement, like in AbstractTestHighlighter), that is it
 * provides consistent api to add and remove Highlighters which can visually
 * decorate the rendering component.
 * 
 * <p>
 * An example multiple highlighting (default striping as appropriate for the
 * current LookAndFeel, cell foreground on matching pattern, and shading a
 * column):
 * 
 * <pre><code>
 * 
 * Highlighter simpleStriping = HighlighterFactory.createSimpleStriping();
 * PatternPredicate patternPredicate = new PatternPredicate(&quot;&circ;M&quot;, 1);
 * ColorHighlighter magenta = new ColorHighlighter(patternPredicate, null,
 *       Color.MAGENTA, null, Color.MAGENTA);
 * Highlighter shading = new ShadingColorHighlighter(
 *       new HighlightPredicate.ColumnHighlightPredicate(1));
 * 
 * table.setHighlighters(simpleStriping,
 *        magenta,
 *        shading);
 * </code></pre>
 * 
 * <p>
 * To fully support, JXTable registers SwingX default table renderers instead of
 * core defaults (see {@link DefaultTableRenderer}) The recommended approach for
 * customizing rendered content it to intall a DefaultTableRenderer configured
 * with a custom String- and/or IconValue. F.i. assuming the cell value is a
 * File and should be rendered by showing its name followed and date of last
 * change:
 * 
 * <pre><code>
 * StringValue sv = new StringValue() {
 *      public String getString(Object value) {
 *        if (!(value instanceof File)) return StringValues.TO_STRING.getString(value);
 *        return StringValues.FILE_NAME.getString(value) + &quot;, &quot; 
 *           + StringValues.DATE_TO_STRING.getString(((File) value).lastModified());
 * }};
 * table.setCellRenderer(File.class, new DefaultTableRenderer(sv));
 * </code></pre>
 * 
 * <p>
 * <b>Note</b>: DefaultTableCellRenderer and subclasses require a hack to play
 * nicely with Highlighters because it has an internal "color memory" in
 * setForeground/setBackground. The hack is applied by default which might lead
 * to unexpected side-effects in custom renderers subclassing DTCR. See
 * {@link #resetDefaultTableCellRendererHighlighter} for details.
 * 
 * 
 * <h2>Rollover</h2>
 * 
 * As all SwingX collection views, a JXTable supports per-cell rollover which is
 * enabled by default. If enabled, the component fires rollover events on
 * enter/exit of a cell which by default is promoted to the renderer if it
 * implements RolloverRenderer, that is simulates live behaviour. The rollover
 * events can be used by client code as well, f.i. to decorate the rollover row
 * using a Highlighter.
 * 
 * <pre><code>
 * JXTable table = new JXTable();
 * table.addHighlighter(new ColorHighlighter(HighlightPredicate.ROLLOVER_ROW, 
 *      null, Color.RED);      
 * </code></pre>
 * 
 * <h2>Search</h2>
 * 
 * As all SwingX collection views, a JXTable is searchable. A search action is
 * registered in its ActionMap under the key "find". The default behaviour is to
 * ask the SearchFactory to open a search component on this component. The
 * default keybinding is retrieved from the SearchFactory, typically ctrl-f (or
 * cmd-f for Mac). Client code can register custom actions and/or bindings as
 * appropriate.
 * <p>
 * 
 * JXTable provides api to vend a renderer-controlled String representation of
 * cell content. This allows the Searchable and Highlighters to use WYSIWYM
 * (What-You-See-Is-What-You-Match), that is pattern matching against the actual
 * string as seen by the user.
 * 
 * <h2>Column Configuration</h2>
 * 
 * JXTable's default column model
 * is of type TableColumnModelExt which allows management of hidden columns. 
 * Furthermore, it guarantees to delegate creation and configuration of table columns
 * to its ColumnFactory. The factory is meant as the central place to 
 * customize column configuration.
 * 
 * <p>
 * Columns can be hidden or shown by setting the visible property on the
 * TableColumnExt using {@link TableColumnExt#setVisible(boolean)}. Columns can
 * also be shown or hidden from the column control popup.
 * 
 * <p>
 * The column control popup is triggered by an icon drawn to the far right of
 * the column headers, above the table's scrollbar (when installed in a
 * JScrollPane). The popup allows the user to select which columns should be
 * shown or hidden, as well as to pack columns and turn on horizontal scrolling.
 * To show or hide the column control, use the
 * {@link #setColumnControlVisible(boolean show)}method.
 * 
 * <p>
 * You can resize all columns, selected columns, or a single column using the
 * methods like {@link #packAll()}. Packing combines several other aspects of a
 * JXTable. If horizontal scrolling is enabled using
 * {@link #setHorizontalScrollEnabled(boolean)}, then the scrollpane will allow
 * the table to scroll right-left, and columns will be sized to their preferred
 * size. To control the preferred sizing of a column, you can provide a
 * prototype value for the column in the TableColumnExt using
 * {@link TableColumnExt#setPrototypeValue(Object)}. The prototype is used as an
 * indicator of the preferred size of the column. This can be useful if some
 * data in a given column is very long, but where the resize algorithm would
 * normally not pick this up.
 * 
 * <p>
 * 
 * 
 * <p>
 * Keys/Actions registered with this component:
 * 
 * <ul>
 * <li>"find" - open an appropriate search widget for searching cell content.
 * The default action registeres itself with the SearchFactory as search target.
 * <li>"print" - print the table
 * <li> {@link JXTable#HORIZONTALSCROLL_ACTION_COMMAND} - toggle the horizontal
 * scrollbar
 * <li> {@link JXTable#PACKSELECTED_ACTION_COMMAND} - resize the selected column
 * to fit the widest cell content
 * <li> {@link JXTable#PACKALL_ACTION_COMMAND} - resize all columns to fit the
 * widest cell content in each column
 * 
 * </ul>
 * 
 * <p>
 * Key bindings.
 * 
 * <ul>
 * <li>"control F" - bound to actionKey "find".
 * </ul>
 * 
 * <p>
 * Client Properties.
 * 
 * <ul>
 * <li> {@link JXTable#MATCH_HIGHLIGHTER} - set to Boolean.TRUE to use a
 * SearchHighlighter to mark a cell as matching.
 * </ul>
 * 
 * @author Ramesh Gupta
 * @author Amy Fowler
 * @author Mark Davidson
 * @author Jeanette Winzenburg
 * 
 * @see JXTableHeader.SortGestureRecognizer
 */
public class JXTable extends JTable implements TableColumnModelExtListener {

    /**
     * 
     */
    public static final String FOCUS_PREVIOUS_COMPONENT = "focusPreviousComponent";

    /**
     * 
     */
    public static final String FOCUS_NEXT_COMPONENT = "focusNextComponent";

    private static final Logger LOG = Logger.getLogger(JXTable.class.getName());

    /**
     * Identifier of show horizontal scroll action, used in JXTable's
     * <code>ActionMap</code>.
     * 
     */
    public static final String HORIZONTALSCROLL_ACTION_COMMAND = ColumnControlButton.COLUMN_CONTROL_MARKER
            + "horizontalScroll";

    /**
     * Identifier of pack table action, used in JXTable's <code>ActionMap</code>
     * .
     */
    public static final String PACKALL_ACTION_COMMAND = ColumnControlButton.COLUMN_CONTROL_MARKER
            + "packAll";

    /**
     * Identifier of pack selected column action, used in JXTable's
     * <code>ActionMap</code>.
     */
    public static final String PACKSELECTED_ACTION_COMMAND = ColumnControlButton.COLUMN_CONTROL_MARKER
            + "packSelected";

    /**
     * The prefix marker to find table related properties in the
     * <code>ResourceBundle</code>.
     */
    public static final String UIPREFIX = "JXTable.";

    static {
        // Hack: make sure the resource bundle is loaded
        LookAndFeelAddons.getAddon();
    }

    /** The FilterPipeline for the table. */
    protected FilterPipeline filters;

    /** The CompoundHighlighter for the table. */
    protected CompoundHighlighter compoundHighlighter;

    /**
     * The key for the client property deciding about whether the color memory
     * hack for DefaultTableCellRenderer should be used.
     * 
     * @see #resetDefaultTableCellRendererHighlighter
     */
    public static final String USE_DTCR_COLORMEMORY_HACK = "useDTCRColorMemoryHack";

    /**
     * The Highlighter used to hack around DefaultTableCellRenderer's color
     * memory.
     */
    protected Highlighter resetDefaultTableCellRendererHighlighter;

    /** The ComponentAdapter for model data access. */
    protected ComponentAdapter dataAdapter;

    /**
     * The handler for mapping view/model coordinates of row selection. Widened
     * access to allow lazy instantiation in subclasses (#746-swingx).
     */
    protected SelectionMapper selectionMapper;

    /** flag to indicate if table is interactively sortable. */
    private boolean sortable;

    /** Listens for changes from the filters. */
    private PipelineListener pipelineListener;

    /** Listens for changes from the highlighters. */
    private ChangeListener highlighterChangeListener;

    /** the factory to use for column creation and configuration. */
    private ColumnFactory columnFactory;

    /** The default number of visible rows (in a ScrollPane). */
    private int visibleRowCount = 20;

    /** The default number of visible columns (in a ScrollPane). */
    private int visibleColumnCount = -1;

    private SizeSequenceMapper rowModelMapper;

    private Field rowModelField;

    private boolean rowHeightEnabled;

    /**
     * Flag to indicate if the column control is visible.
     */
    private boolean columnControlVisible;

    /**
     * ScrollPane's original vertical scroll policy. If the column control is
     * visible the policy is set to ALWAYS.
     */
    private int verticalScrollPolicy;

    /**
     * The component used a column control in the upper trailing corner of an
     * enclosing <code>JScrollPane</code>.
     */
    private JComponent columnControlButton;

    /**
     * Mouse/Motion/Listener keeping track of mouse moved in cell coordinates.
     */
    private RolloverProducer rolloverProducer;

    /**
     * RolloverController: listens to cell over events and repaints
     * entered/exited rows.
     */
    private TableRolloverController<JXTable> linkController;

    /**
     * field to store the autoResizeMode while interactively setting horizontal
     * scrollbar to visible.
     */
    private int oldAutoResizeMode;

    /** property to control the tracksViewportHeight behaviour. */
    private boolean fillsViewportHeight;

    /**
     * flag to indicate enhanced auto-resize-off behaviour is on. This is
     * set/reset in setHorizontalScrollEnabled.
     */
    private boolean intelliMode;

    /**
     * internal flag indicating that we are in super.doLayout(). (used in
     * columnMarginChanged to not update the resizingCol's prefWidth).
     */
    private boolean inLayout;

    /**
     * Flag to distinguish internal settings of row height from client code
     * settings. The rowHeight will be internally adjusted to font size on
     * instantiation and in updateUI if the height has not been set explicitly
     * by the application.
     * 
     * @see #adminSetRowHeight(int)
     * @see #setRowHeight(int)
     */
    protected boolean isXTableRowHeightSet;

    /** property to control table's editability as a whole. */
    private boolean editable;

    private Dimension calculatedPrefScrollableViewportSize;

    /** Instantiates a JXTable with a default table model, no data. */
    public JXTable() {
        init();
    }

    /**
     * Instantiates a JXTable with a specific table model.
     * 
     * @param dm The model to use.
     */
    public JXTable(TableModel dm) {
        super(dm);
        init();
    }

    /**
     * Instantiates a JXTable with a specific table model.
     * 
     * @param dm The model to use.
     */
    public JXTable(TableModel dm, TableColumnModel cm) {
        super(dm, cm);
        init();
    }

    /**
     * Instantiates a JXTable with a specific table model, column model, and
     * selection model.
     * 
     * @param dm The table model to use.
     * @param cm The column model to use.
     * @param sm The list selection model to use.
     */
    public JXTable(TableModel dm, TableColumnModel cm, ListSelectionModel sm) {
        super(dm, cm, sm);
        init();
    }

    /**
     * Instantiates a JXTable for a given number of columns and rows.
     * 
     * @param numRows Count of rows to accommodate.
     * @param numColumns Count of columns to accommodate.
     */
    public JXTable(int numRows, int numColumns) {
        super(numRows, numColumns);
        init();
    }

    /**
     * Instantiates a JXTable with data in a vector or rows and column names.
     * 
     * @param rowData Row data, as a Vector of Objects.
     * @param columnNames Column names, as a Vector of Strings.
     */
    public JXTable(Vector rowData, Vector columnNames) {
        super(rowData, columnNames);
        init();
    }

    /**
     * Instantiates a JXTable with data in a array or rows and column names.
     * 
     * @param rowData Row data, as a two-dimensional Array of Objects (by row,
     *        for column).
     * @param columnNames Column names, as a Array of Strings.
     */
    public JXTable(Object[][] rowData, Object[] columnNames) {
        super(rowData, columnNames);
        init();
    }

    /**
     * Initializes the table for use.
     * 
     */
    private void init() {
        putClientProperty(USE_DTCR_COLORMEMORY_HACK, Boolean.TRUE);
        setEditable(true);
        setRolloverEnabled(true);
        setTerminateEditOnFocusLost(true);
        // guarantee getFilters() to return != null
        initActionsAndBindings();
        initFocusBindings();
        // instantiate row height depending ui setting or font size.
        updateRowHeightUI(false);
        // set to null - don't want hard-coded pixel sizes.
        setPreferredScrollableViewportSize(null);
        // PENDING: need to duplicate here..
        // why doesn't the call in tableChanged work?
        initializeColumnWidths();
        setFillsViewportHeight(true);
        updateLocaleState(getLocale());
    }

//--------------- Rollover support    
    /**
     * Sets the property to enable/disable rollover support. If enabled, this component
     * fires property changes on per-cell mouse rollover state, i.e. 
     * when the mouse enters/leaves a list cell. <p>
     * 
     * This can be enabled to show "live" rollover behaviour, f.i. the cursor over a cell 
     * rendered by a JXHyperlink.<p>
     * 
     * The default value is true.
     * 
     * @param rolloverEnabled a boolean indicating whether or not the rollover
     *   functionality should be enabled.
     * 
     * @see #isRolloverEnabled()
     * @see #getLinkController()
     * @see #createRolloverProducer()
     * @see org.jdesktop.swingx.rollover.RolloverRenderer  
     */
    public void setRolloverEnabled(boolean rolloverEnabled) {
        boolean old = isRolloverEnabled();
        if (rolloverEnabled == old)
            return;
        if (rolloverEnabled) {
            rolloverProducer = createRolloverProducer();
            addMouseListener(rolloverProducer);
            addMouseMotionListener(rolloverProducer);
            getLinkController().install(this);

        } else {
            removeMouseListener(rolloverProducer);
            removeMouseMotionListener(rolloverProducer);
            rolloverProducer = null;
            getLinkController().release();
        }
        firePropertyChange("rolloverEnabled", old, isRolloverEnabled());
    }

    /**
     * Returns a boolean indicating whether or not rollover support is enabled. 
     *
     * @return a boolean indicating whether or not rollover support is enabled. 
     * 
     * @see #setRolloverEnabled(boolean)
     */
    public boolean isRolloverEnabled() {
        return rolloverProducer != null;
    }

    /**
     * Returns the RolloverController for this component. Lazyly creates the 
     * controller if necessary, that is the return value is guaranteed to be 
     * not null. <p>
     * 
     * PENDING JW: rename to getRolloverController
     * 
     * @return the RolloverController for this tree, guaranteed to be not null.
     * 
     * @see #setRolloverEnabled(boolean)
     * @see #createLinkController()
     * @see org.jdesktop.swingx.rollover.RolloverController
     */
    protected TableRolloverController<JXTable> getLinkController() {
        if (linkController == null) {
            linkController = createLinkController();
        }
        return linkController;
    }

    /**
     * Creates and returns a RolloverController appropriate for this component.
     * 
     * @return a RolloverController appropriate for this component.
     * 
     * @see #getLinkController()
     * @see org.jdesktop.swingx.rollover.RolloverController
     */
    protected TableRolloverController<JXTable> createLinkController() {
        return new TableRolloverController<JXTable>();
    }

    /**
     * Creates and returns the RolloverProducer to use with this component.
     * <p>
     * 
     * @return <code>RolloverProducer</code> to use with this component
     * 
     * @see #setRolloverEnabled(boolean)
     */
    protected RolloverProducer createRolloverProducer() {
        return new TableRolloverProducer();
    }


    /**
     * Returns the column control visible property.
     * <p>
     * 
     * @return boolean to indicate whether the column control is visible.
     * @see #setColumnControlVisible(boolean)
     * @see #setColumnControl(JComponent)
     */
    public boolean isColumnControlVisible() {
        return columnControlVisible;
    }

    /**
     * Sets the column control visible property. If true and
     * <code>JXTable</code> is contained in a <code>JScrollPane</code>, the
     * table adds the column control to the trailing corner of the scroll pane.
     * <p>
     * 
     * Note: if the table is not inside a <code>JScrollPane</code> the column
     * control is not shown even if this returns true. In this case it's the
     * responsibility of the client code to actually show it.
     * <p>
     * 
     * The default value is <code>false</code>.
     * 
     * @param visible boolean to indicate if the column control should be shown
     * @see #isColumnControlVisible()
     * @see #setColumnControl(JComponent)
     * 
     */
    public void setColumnControlVisible(boolean visible) {
        if (isColumnControlVisible() == visible)
            return;
        boolean old = isColumnControlVisible();
        if (old) {
            unconfigureColumnControl();
        }
        this.columnControlVisible = visible;
        if (isColumnControlVisible()) {
            configureColumnControl();
        }
        firePropertyChange("columnControlVisible", old, !old);

    }

    /**
     * Returns the component used as column control. Lazily creates the control
     * to the default if it is <code>null</code>.
     * 
     * @return component for column control, guaranteed to be != null.
     * @see #setColumnControl(JComponent)
     * @see #createDefaultColumnControl()
     */
    public JComponent getColumnControl() {
        if (columnControlButton == null) {
            columnControlButton = createDefaultColumnControl();
        }
        return columnControlButton;
    }

    /**
     * Sets the component used as column control. Updates the enclosing
     * <code>JScrollPane</code> if appropriate. Passing a <code>null</code>
     * parameter restores the column control to the default.
     * <p>
     * The component is automatically visible only if the
     * <code>columnControlVisible</code> property is <code>true</code> and the
     * table is contained in a <code>JScrollPane</code>.
     * 
     * <p>
     * NOTE: from the table's perspective, the column control is simply a
     * <code>JComponent</code> to add to and keep in the trailing corner of the
     * scrollpane. (if any). It's up the concrete control to configure itself
     * from and keep synchronized to the columns' states.
     * <p>
     * 
     * @param columnControl the <code>JComponent</code> to use as columnControl.
     * @see #getColumnControl()
     * @see #createDefaultColumnControl()
     * @see #setColumnControlVisible(boolean)
     * 
     */
    public void setColumnControl(JComponent columnControl) {
        // PENDING JW: release old column control? who's responsible?
        // Could implement CCB.autoRelease()?
        JComponent old = columnControlButton;
        this.columnControlButton = columnControl;
        configureColumnControl();
        firePropertyChange("columnControl", old, getColumnControl());
    }

    /**
     * Creates the default column control used by this table. This
     * implementation returns a <code>ColumnControlButton</code> configured with
     * default <code>ColumnControlIcon</code>.
     * 
     * @return the default component used as column control.
     * @see #setColumnControl(JComponent)
     * @see org.jdesktop.swingx.table.ColumnControlButton
     * @see org.jdesktop.swingx.icon.ColumnControlIcon
     */
    protected JComponent createDefaultColumnControl() {
        return new ColumnControlButton(this);
    }

    /**
     * Sets the language-sensitive orientation that is to be used to order the
     * elements or text within this component.
     * <p>
     * 
     * Overridden to work around a core bug: <code>JScrollPane</code> can't cope
     * with corners when changing component orientation at runtime. This method
     * explicitly re-configures the column control.
     * <p>
     * 
     * @param o the ComponentOrientation for this table.
     * @see java.awt.Component#setComponentOrientation(ComponentOrientation)
     */
    @Override
    public void setComponentOrientation(ComponentOrientation o) {
        super.setComponentOrientation(o);
        configureColumnControl();
    }

    /**
     * Configures the enclosing <code>JScrollPane</code>.
     * <p>
     * 
     * Overridden to addionally configure the upper trailing corner with the
     * column control.
     * 
     * @see #configureColumnControl()
     * 
     */
    @Override
    protected void configureEnclosingScrollPane() {
        super.configureEnclosingScrollPane();
        configureColumnControl();
    }

    /**
     * Unconfigures the enclosing <code>JScrollPane</code>.
     * <p>
     * 
     * Overridden to addionally unconfigure the upper trailing corner with the
     * column control.
     * 
     * @see #unconfigureColumnControl()
     * 
     */
    @Override
    protected void unconfigureEnclosingScrollPane() {
        unconfigureColumnControl();
        super.unconfigureEnclosingScrollPane();
    }

    /**
     * /** Unconfigures the upper trailing corner of an enclosing
     * <code>JScrollPane</code>.
     * 
     * Here: removes the upper trailing corner and resets.
     * 
     * @see #setColumnControlVisible(boolean)
     * @see #setColumnControl(JComponent)
     */
    protected void unconfigureColumnControl() {
        Container p = getParent();
        if (p instanceof JViewport) {
            Container gp = p.getParent();
            if (gp instanceof JScrollPane) {
                JScrollPane scrollPane = (JScrollPane) gp;
                // Make certain we are the viewPort's view and not, for
                // example, the rowHeaderView of the scrollPane -
                // an implementor of fixed columns might do this.
                JViewport viewport = scrollPane.getViewport();
                if (viewport == null || viewport.getView() != this) {
                    return;
                }
                if (verticalScrollPolicy != 0) {
                    // Fix #155-swingx: reset only if we had force always before
                    // PENDING: JW - doesn't cope with dynamically changing the
                    // policy
                    // shouldn't be much of a problem because doesn't happen too
                    // often??
                    scrollPane.setVerticalScrollBarPolicy(verticalScrollPolicy);
                    verticalScrollPolicy = 0;
                }
                if (isColumnControlVisible()) {
                    scrollPane.setCorner(JScrollPane.UPPER_TRAILING_CORNER,
                            null);
                }
            }
        }

    }

    /**
     * Configures the upper trailing corner of an enclosing
     * <code>JScrollPane</code>.
     * 
     * Adds the <code>ColumnControl</code> if the
     * <code>columnControlVisible</code> property is true.
     * <p>
     * 
     * @see #setColumnControlVisible(boolean)
     * @see #setColumnControl(JComponent)
     */
    protected void configureColumnControl() {
        Container p = getParent();
        if (p instanceof JViewport) {
            Container gp = p.getParent();
            if (gp instanceof JScrollPane) {
                JScrollPane scrollPane = (JScrollPane) gp;
                // Make certain we are the viewPort's view and not, for
                // example, the rowHeaderView of the scrollPane -
                // an implementor of fixed columns might do this.
                JViewport viewport = scrollPane.getViewport();
                if (viewport == null || viewport.getView() != this) {
                    return;
                }
                if (isColumnControlVisible()) {
                    if (verticalScrollPolicy == 0) {
                        verticalScrollPolicy = scrollPane
                                .getVerticalScrollBarPolicy();
                    }
                    scrollPane.setCorner(JScrollPane.UPPER_TRAILING_CORNER,
                            getColumnControl());

                    scrollPane
                            .setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
                }
                // else {
                // if (verticalScrollPolicy != 0) {
                // // Fix #155-swingx: reset only if we had force always before
                // // PENDING: JW - doesn't cope with dynamically changing the
                // policy
                // // shouldn't be much of a problem because doesn't happen too
                // often??
                // scrollPane.setVerticalScrollBarPolicy(verticalScrollPolicy);
                // }
                // try {
                // scrollPane.setCorner(JScrollPane.UPPER_TRAILING_CORNER,
                // null);
                // } catch (Exception ex) {
                // // Ignore spurious exception thrown by JScrollPane. This
                // // is a Swing bug!
                // }
                //
                // }
            }
        }
    }

    // --------------------- actions
    /**
     * Take over ctrl-tab.
     * 
     */
    private void initFocusBindings() {
        setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS,
                new TreeSet<KeyStroke>());
        setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS,
                new TreeSet<KeyStroke>());
        getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
                KeyStroke.getKeyStroke("ctrl TAB"), FOCUS_NEXT_COMPONENT);
        getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
                KeyStroke.getKeyStroke("shift ctrl TAB"),
                FOCUS_PREVIOUS_COMPONENT);
        getActionMap().put(FOCUS_NEXT_COMPONENT,
                createFocusTransferAction(true));
        getActionMap().put(FOCUS_PREVIOUS_COMPONENT,
                createFocusTransferAction(false));
    }

    /**
     * Creates and returns an action for forward/backward focus transfer,
     * depending on the given flag.
     * 
     * @param forward a boolean indicating the direction of the required focus
     *        transfer
     * @return the action bound to focusTraversal.
     */
    private Action createFocusTransferAction(final boolean forward) {
        BoundAction action = new BoundAction(null,
                forward ? FOCUS_NEXT_COMPONENT : FOCUS_PREVIOUS_COMPONENT);
        action.registerCallback(this, forward ? "transferFocus"
                : "transferFocusBackward");
        return action;
    }

    /**
     * A small class which dispatches actions.
     * <p>
     * TODO (?): Is there a way that we can make this static?
     * <p>
     * 
     * PENDING JW: don't use UIAction ... we are in OO-land!
     */
    private class Actions extends UIAction {
        Actions(String name) {
            super(name);
        }

        public void actionPerformed(ActionEvent evt) {
            if ("print".equals(getName())) {
                try {
                    print();
                } catch (PrinterException ex) {
                    // REMIND(aim): should invoke pluggable application error
                    // handler
                    LOG.log(Level.WARNING, "", ex);
                }
            }
        }

    }

    /**
     * Registers additional, per-instance <code>Action</code>s to the this
     * table's ActionMap. Binds the search accelerator (as returned by the
     * SearchFactory) to the find action.
     * 
     * 
     */
    private void initActionsAndBindings() {
        // Register the actions that this class can handle.
        ActionMap map = getActionMap();
        map.put("print", new Actions("print"));
        map.put("find", new Actions("find"));
        // hack around core bug: cancel editing doesn't fire
        // reported against SwingX as of #610-swingx
        map.put("cancel", createCancelAction());
        map.put(PACKALL_ACTION_COMMAND, createPackAllAction());
        map.put(PACKSELECTED_ACTION_COMMAND, createPackSelectedAction());
        map
                .put(HORIZONTALSCROLL_ACTION_COMMAND,
                        createHorizontalScrollAction());

    }

    /**
     * Creates and returns an Action which cancels an ongoing edit correctly.
     * Note: the correct thing to do is to call the editor's cancelEditing, the
     * wrong thing to do is to call table removeEditor (as core JTable does...).
     * So this is a quick hack around a core bug, reported against SwingX in
     * #610-swingx.
     * 
     * @return an Action which cancels an edit.
     */
    private Action createCancelAction() {
        Action action = new AbstractActionExt() {

            public void actionPerformed(ActionEvent e) {
                if (!isEditing())
                    return;
                getCellEditor().cancelCellEditing();
            }

            @Override
            public boolean isEnabled() {
                return isEditing();
            }

        };
        return action;
    }

    /**
     * Creates and returns the default <code>Action</code> for toggling the
     * horizontal scrollBar.
     */
    private Action createHorizontalScrollAction() {
        BoundAction action = new BoundAction(null,
                HORIZONTALSCROLL_ACTION_COMMAND);
        action.setStateAction();
        action.registerCallback(this, "setHorizontalScrollEnabled");
        action.setSelected(isHorizontalScrollEnabled());
        return action;
    }

    /**
     * Returns a potentially localized value from the UIManager. The given key
     * is prefixed by this table's <code>UIPREFIX</code> before doing the
     * lookup. The lookup respects this table's current <code>locale</code>
     * property. Returns the key, if no value is found.
     * 
     * @param key the bare key to look up in the UIManager.
     * @return the value mapped to UIPREFIX + key or key if no value is found.
     */
    protected String getUIString(String key) {
        return getUIString(key, getLocale());
    }

    /**
     * Returns a potentially localized value from the UIManager for the given
     * locale. The given key is prefixed by this table's <code>UIPREFIX</code>
     * before doing the lookup. Returns the key, if no value is found.
     * 
     * @param key the bare key to look up in the UIManager.
     * @param locale the locale use for lookup
     * @return the value mapped to UIPREFIX + key in the given locale, or key if
     *         no value is found.
     */
    protected String getUIString(String key, Locale locale) {
        String text = UIManagerExt.getString(UIPREFIX + key, locale);
        return text != null ? text : key;
    }

    /**
     * Creates and returns the default <code>Action</code> for packing the
     * selected column.
     */
    private Action createPackSelectedAction() {
        BoundAction action = new BoundAction(null, PACKSELECTED_ACTION_COMMAND);
        action.registerCallback(this, "packSelected");
        action.setEnabled(getSelectedColumnCount() > 0);
        return action;
    }

    /**
     * Creates and returns the default <b>Action </b> for packing all columns.
     */
    private Action createPackAllAction() {
        BoundAction action = new BoundAction(null, PACKALL_ACTION_COMMAND);
        action.registerCallback(this, "packAll");
        return action;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Overridden to update locale-dependent properties.
     * 
     * @see #updateLocaleState(Locale)
     */
    @Override
    public void setLocale(Locale locale) {
        updateLocaleState(locale);
        super.setLocale(locale);
    }

    /**
     * Updates locale-dependent state to the given <code>Locale</code>.
     * 
     * Here: updates registered column actions' locale-dependent state.
     * <p>
     * 
     * PENDING: Try better to find all column actions including custom
     * additions? Or move to columnControl?
     * 
     * @param locale the Locale to use for value lookup
     * @see #setLocale(Locale)
     * @see #updateLocaleActionState(String, Locale)
     */
    protected void updateLocaleState(Locale locale) {
        updateLocaleActionState(HORIZONTALSCROLL_ACTION_COMMAND, locale);
        updateLocaleActionState(PACKALL_ACTION_COMMAND, locale);
        updateLocaleActionState(PACKSELECTED_ACTION_COMMAND, locale);
    }

    /**
     * Updates locale-dependent state of action registered with key in
     * <code>ActionMap</code>. Does nothing if no action with key is found.
     * <p>
     * 
     * Here: updates the <code>Action</code>'s name property.
     * 
     * @param key the string for lookup in this table's ActionMap
     * @see #updateLocaleState(Locale)
     */
    protected void updateLocaleActionState(String key, Locale locale) {
        Action action = getActionMap().get(key);
        if (action == null)
            return;
        action.putValue(Action.NAME, getUIString(key, locale));
    }

    // ------------------ bound action callback methods

    /**
     * Resizes all columns to fit their content.
     * <p>
     * 
     * By default this method is bound to the pack all columns
     * <code>Action</code> and registered in the table's <code>ActionMap</code>.
     * 
     */
    public void packAll() {
        packTable(-1);
    }

    /**
     * Resizes the lead column to fit its content.
     * <p>
     * 
     * By default this method is bound to the pack selected column
     * <code>Action</code> and registered in the table's <code>ActionMap</code>.
     */
    public void packSelected() {
        int selected = getColumnModel().getSelectionModel()
                .getLeadSelectionIndex();
        if (selected >= 0) {
            packColumn(selected, -1);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * 
     * Overridden to update the enabled state of the pack selected column
     * <code>Action</code>.
     */
    @Override
    public void columnSelectionChanged(ListSelectionEvent e) {
        super.columnSelectionChanged(e);
        if (e.getValueIsAdjusting())
            return;
        Action packSelected = getActionMap().get(PACKSELECTED_ACTION_COMMAND);
        if ((packSelected != null)) {
            packSelected.setEnabled(!((ListSelectionModel) e.getSource())
                    .isSelectionEmpty());
        }
    }

    // ----------------------- scrollable control

    /**
     * Sets the enablement of enhanced horizontal scrolling. If enabled, it
     * toggles an auto-resize mode which always fills the <code>JViewport</code>
     * horizontally and shows the horizontal scrollbar if necessary.
     * <p>
     * 
     * The default value is <code>false</code>.
     * <p>
     * 
     * Note: this is <strong>not</strong> a bound property, though it follows
     * bean naming conventions.
     * 
     * PENDING: Probably should be... If so, could be taken by a listening
     * Action as in the app-framework.
     * <p>
     * PENDING JW: the name is mis-leading?
     * 
     * @param enabled a boolean indicating whether enhanced auto-resize mode is
     *        enabled.
     * @see #isHorizontalScrollEnabled()
     */
    public void setHorizontalScrollEnabled(boolean enabled) {
        /*
         * PENDING JW: add a "real" mode? Problematic because there are several
         * places in core which check for #AUTO_RESIZE_OFF, can't use different
         * value without unwanted side-effects. The current solution with
         * tagging the #AUTO_RESIZE_OFF by a boolean flag #intelliMode is
         * brittle - need to be very careful to turn off again ... Another
         * problem is to keep the horizontalScrollEnabled toggling action in
         * synch with this property. Yet another problem is the change
         * notification: currently this is _not_ a bound property.
         */
        if (enabled == (isHorizontalScrollEnabled())) {
            return;
        }
        boolean old = isHorizontalScrollEnabled();
        if (enabled) {
            // remember the resizeOn mode if any
            if (getAutoResizeMode() != AUTO_RESIZE_OFF) {
                oldAutoResizeMode = getAutoResizeMode();
            }
            setAutoResizeMode(AUTO_RESIZE_OFF);
            // setAutoResizeModel always disables the intelliMode
            // must set after calling and update the action again
            intelliMode = true;
            updateHorizontalAction();
        } else {
            setAutoResizeMode(oldAutoResizeMode);
        }
        firePropertyChange("horizontalScrollEnabled", old,
                isHorizontalScrollEnabled());
    }

    /**
     * Returns the current setting for horizontal scrolling.
     * 
     * @return the enablement of enhanced horizontal scrolling.
     * @see #setHorizontalScrollEnabled(boolean)
     */
    public boolean isHorizontalScrollEnabled() {
        return intelliMode && getAutoResizeMode() == AUTO_RESIZE_OFF;
    }

    /**
     * {@inheritDoc}
     * <p>
     * 
     * Overridden for internal bookkeeping related to the enhanced auto-resize
     * behaviour.
     * <p>
     * 
     * Note: to enable/disable the enhanced auto-resize mode use exclusively
     * <code>setHorizontalScrollEnabled</code>, this method can't cope with it.
     * 
     * @see #setHorizontalScrollEnabled(boolean)
     * 
     */
    @Override
    public void setAutoResizeMode(int mode) {
        if (mode != AUTO_RESIZE_OFF) {
            oldAutoResizeMode = mode;
        }
        intelliMode = false;
        super.setAutoResizeMode(mode);
        updateHorizontalAction();
    }

    /**
     * Synchs selected state of horizontal scrolling <code>Action</code> to
     * enablement of enhanced auto-resize behaviour.
     */
    protected void updateHorizontalAction() {
        Action showHorizontal = getActionMap().get(
                HORIZONTALSCROLL_ACTION_COMMAND);
        if (showHorizontal instanceof BoundAction) {
            ((BoundAction) showHorizontal)
                    .setSelected(isHorizontalScrollEnabled());
        }
    }

    /**
     *{@inheritDoc}
     * <p>
     * 
     * Overridden to support enhanced auto-resize behaviour enabled and
     * necessary.
     * 
     * @see #setHorizontalScrollEnabled(boolean)
     */
    @Override
    public boolean getScrollableTracksViewportWidth() {
        boolean shouldTrack = super.getScrollableTracksViewportWidth();
        if (isHorizontalScrollEnabled()) {
            return hasExcessWidth();
        }
        return shouldTrack;
    }

    /**
     * Layouts column width. The exact behaviour depends on the
     * <code>autoResizeMode</code> property.
     * <p>
     * Overridden to support enhanced auto-resize behaviour enabled and
     * necessary.
     * 
     * @see #setAutoResizeMode(int)
     * @see #setHorizontalScrollEnabled(boolean)
     */
    @Override
    public void doLayout() {
        int resizeMode = getAutoResizeMode();
        // fool super...
        if (isHorizontalScrollEnabled() && hasRealizedParent()
                && hasExcessWidth()) {
            autoResizeMode = oldAutoResizeMode;
        }
        inLayout = true;
        super.doLayout();
        inLayout = false;
        autoResizeMode = resizeMode;
    }

    /**
     * 
     * @return boolean to indicate whether the table has a realized parent.
     */
    private boolean hasRealizedParent() {
        return (getWidth() > 0) && (getParent() != null)
                && (getParent().getWidth() > 0);
    }

    /**
     * PRE: hasRealizedParent()
     * 
     * @return boolean to indicate whether the table has widths excessing
     *         parent's width
     */
    private boolean hasExcessWidth() {
        return getPreferredSize().width < getParent().getWidth();
    }

    /**
     * {@inheritDoc}
     * <p>
     * 
     * Overridden to support enhanced auto-resize behaviour enabled and
     * necessary.
     * 
     * @see #setHorizontalScrollEnabled(boolean)
     */
    @Override
    public void columnMarginChanged(ChangeEvent e) {
        if (isEditing()) {
            removeEditor();
        }
        TableColumn resizingColumn = getResizingColumn();
        // Need to do this here, before the parent's
        // layout manager calls getPreferredSize().
        if (resizingColumn != null && autoResizeMode == AUTO_RESIZE_OFF
                && !inLayout) {
            resizingColumn.setPreferredWidth(resizingColumn.getWidth());
        }
        resizeAndRepaint();
    }

    /**
     * Returns the column which is interactively resized. The return value is
     * null if the header is null or has no resizing column.
     * 
     * @return the resizing column.
     */
    private TableColumn getResizingColumn() {
        return (tableHeader == null) ? null : tableHeader.getResizingColumn();
    }

    /**
     * Sets the flag which controls the scrollableTracksViewportHeight property.
     * If true the table's height will be always at least as large as the
     * containing parent, if false the table's height will be independent of
     * parent's height.
     * <p>
     * 
     * The default value is <code>true</code>.
     * <p>
     * 
     * Note: this a backport from Mustang's <code>JTable</code>.
     * 
     * @param fillsViewportHeight boolean to indicate whether the table should
     *        always fill parent's height.
     * @see #getFillsViewportHeight()
     * @see #getScrollableTracksViewportHeight()
     */
    public void setFillsViewportHeight(boolean fillsViewportHeight) {
        if (fillsViewportHeight == getFillsViewportHeight())
            return;
        boolean old = getFillsViewportHeight();
        this.fillsViewportHeight = fillsViewportHeight;
        firePropertyChange("fillsViewportHeight", old, getFillsViewportHeight());
        revalidate();
    }

    /**
     * Returns the flag which controls the scrollableTracksViewportHeight
     * property.
     * 
     * @return true if the table's height will always be at least as large as
     *         the containing parent, false if it is independent
     * @see #setFillsViewportHeight(boolean)
     * @see #getScrollableTracksViewportHeight()
     */
    public boolean getFillsViewportHeight() {
        return fillsViewportHeight;
    }

    /**
     * {@inheritDoc}
     * <p>
     * 
     * Overridden to control the tracksHeight property depending on
     * fillsViewportHeight and relative size to containing parent.
     * 
     * @return true if the control flag is true and the containing parent height
     *         > prefHeight, else returns false.
     * @see #setFillsViewportHeight(boolean)
     * 
     */
    @Override
    public boolean getScrollableTracksViewportHeight() {
        return getFillsViewportHeight()
                && getParent() instanceof JViewport
                && (((JViewport) getParent()).getHeight() > getPreferredSize().height);
    }

    // ------------------------ override super because of filter-awareness

    /**
     * Returns the row count in the table; if filters are applied, this is the
     * filtered row count.
     */
    @Override
    public int getRowCount() {
        // RG: If there are no filters, call superclass version rather than
        // accessing model directly
        return filters == null ? super.getRowCount() : filters.getOutputSize();
    }

    /**
     * Convert row index from view coordinates to model coordinates accounting
     * for the presence of sorters and filters.
     * 
     * @param row row index in view coordinates
     * @return row index in model coordinates
     */
    public int convertRowIndexToModel(int row) {
        return row;
    }

    /**
     * Convert row index from model coordinates to view coordinates accounting
     * for the presence of sorters and filters.
     * 
     * @param row row index in model coordinates
     * @return row index in view coordinates
     */
    public int convertRowIndexToView(int row) {
        return row;
    }

    /**
     * Overridden to account for row index mapping. {@inheritDoc}
     */
    @Override
    public Object getValueAt(int row, int column) {
        return getModel().getValueAt(convertRowIndexToModel(row),
                convertColumnIndexToModel(column));
    }

    /**
     * Overridden to account for row index mapping. This implementation respects
     * the cell's editability, that is it has no effect if
     * <code>!isCellEditable(row, column)</code>.
     * 
     * {@inheritDoc}
     * 
     * @see #isCellEditable(int, int)
     */
    @Override
    public void setValueAt(Object aValue, int row, int column) {
        if (!isCellEditable(row, column))
            return;
        getModel().setValueAt(aValue, convertRowIndexToModel(row),
                convertColumnIndexToModel(column));
    }

    /**
     * Returns true if the cell at <code>row</code> and <code>column</code> is
     * editable. Otherwise, invoking <code>setValueAt</code> on the cell will
     * have no effect.
     * <p>
     * Overridden to account for row index mapping and to support a layered
     * editability control:
     * <ul>
     * <li>per-table: <code>JXTable.isEditable()</code>
     * <li>per-column: <code>TableColumnExt.isEditable()</code>
     * <li>per-cell: controlled by the model
     * <code>TableModel.isCellEditable()</code>
     * </ul>
     * The view cell is considered editable only if all three layers are
     * enabled.
     * 
     * @param row the row index in view coordinates
     * @param column the column index in view coordinates
     * @return true if the cell is editable
     * 
     * @see #setValueAt(Object, int, int)
     * @see #isEditable()
     * @see TableColumnExt#isEditable
     * @see TableModel#isCellEditable
     */
    @Override
    public boolean isCellEditable(int row, int column) {
        if (!isEditable())
            return false;
        boolean editable = getModel().isCellEditable(
                convertRowIndexToModel(row), convertColumnIndexToModel(column));
        if (editable) {
            TableColumnExt tableColumn = getColumnExt(column);
            if (tableColumn != null) {
                editable = tableColumn.isEditable();
            }
        }
        return editable;
    }

    /**
     * Overridden to update selectionMapper
     */
    @Override
    public void setSelectionModel(ListSelectionModel newModel) {
        super.setSelectionModel(newModel);
        getSelectionMapper().setViewSelectionModel(getSelectionModel());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setModel(TableModel newModel) {
        // JW: need to look here? is done in tableChanged as well.
        boolean wasEnabled = getSelectionMapper().isEnabled();
        getSelectionMapper().setEnabled(false);
        try {
            super.setModel(newModel);
        } finally {
            getSelectionMapper().setEnabled(wasEnabled);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * 
     * Overridden for documentation clarification. The property has the same
     * meaning as super, that is if true to re-create all table columns on
     * either setting a new TableModel or receiving a structureChanged from the
     * existing. The most obvious visual effect is that custom column properties
     * appear to be "lost".
     * <p>
     * 
     * JXTable does support additonal custom configuration (via a custom
     * ColumnFactory) which can (and incorrectly was) called independently from
     * the creation. Setting this property to false guarantees that no column
     * configuration is applied.
     * 
     * @see #tableChanged(TableModelEvent)
     * @see org.jdesktop.swingx.table.ColumnFactory
     * 
     */
    @Override
    public boolean getAutoCreateColumnsFromModel() {
        return super.getAutoCreateColumnsFromModel();
    }

    /**
     * additionally updates filtered state. {@inheritDoc}
     */
    @Override
    public void tableChanged(TableModelEvent e) {
        if (getSelectionModel().getValueIsAdjusting()) {
            // this may happen if the uidelegate/editor changed selection
            // and adjusting state
            // before firing a editingStopped
            // need to enforce update of model selection
            getSelectionModel().setValueIsAdjusting(false);
        }
        // JW: make SelectionMapper deaf ... super doesn't know about row
        // mapping and sets rowSelection in model coordinates
        // causing complete confusion.
        boolean wasEnabled = getSelectionMapper().isEnabled();
        getSelectionMapper().setEnabled(false);
        try {
            SizeSequence rowModel = nullSuperRowModel(e);
            super.tableChanged(e);
            if (rowModel != null) {
                retoreSuperRowModel(rowModel);
            }
            updateSelectionAndRowModel(e);
        } finally {
            getSelectionMapper().setEnabled(wasEnabled);
        }
        if (isStructureChanged(e) && getAutoCreateColumnsFromModel()) {
            initializeColumnWidths();
            resetCalculatedScrollableSize(true);
        }
    }

    /**
     * Sets super's rowModel to the given SizeSequence if not null, does nothing
     * if null.
     * 
     * Hack around #924-swingx.
     * 
     * @param rowModel the SizeSequence to set super's rowModel to.
     */
    private void retoreSuperRowModel(SizeSequence rowModel) {
        if (rowModel == null)
            return;
        setSuperRowModel(rowModel);
    }

    /**
     * Nulls super's rowModel and returns the old one if rowHeightEnabled and
     * the event is either a remove or a insert. Does nothing and returns null
     * otherwise.
     * 
     * Hack around #924-swingx.
     * 
     * @param e the TableModelEvent used to decide whether a nulling is needed.
     * @return super's old SizeSequence or null
     */
    private SizeSequence nullSuperRowModel(TableModelEvent e) {
        if (!isRowHeightEnabled())
            return null;
        if (!isInsertRemove(e))
            return null;
        SizeSequence result = getSuperRowModel();
        setSuperRowModel(null);
        return result;
    }

    /**
     * @param e
     * @return
     */
    private boolean isInsertRemove(TableModelEvent e) {
        if (isStructureChanged(e))
            return false;
        if ((e.getType() == TableModelEvent.INSERT)
                || (e.getType() == TableModelEvent.DELETE))
            return true;
        return false;
    }

    /**
     * reset model selection coordinates in SelectionMapper after model events.
     * 
     * @param e
     */
    private void updateSelectionAndRowModel(TableModelEvent e) {
        if (isStructureChanged(e) || isDataChanged(e)) {

            // JW fixing part of #172 - trying to adjust lead/anchor to valid
            // indices (at least in model coordinates) after super's default
            // clearSelection
            // in dataChanged/structureChanged.
            hackLeadAnchor(e);

            getSelectionMapper().clearModelSelection();
            getRowModelMapper().clearModelSizes();
            updateViewSizeSequence();

            // JW: c&p from JTable
        } else if (e.getType() == TableModelEvent.INSERT) {
            int start = e.getFirstRow();
            int end = e.getLastRow();
            if (start < 0) {
                start = 0;
            }
            if (end < 0) {
                end = getModel().getRowCount() - 1;
            }

            // Adjust the selectionMapper to account for the new rows.
            int length = end - start + 1;
            getSelectionMapper().insertIndexInterval(start, length, true);
            getRowModelMapper().insertIndexInterval(start, length,
                    getRowHeight());

        } else if (e.getType() == TableModelEvent.DELETE) {
            int start = e.getFirstRow();
            int end = e.getLastRow();
            if (start < 0) {
                start = 0;
            }
            if (end < 0) {
                end = getModel().getRowCount() - 1;
            }

            int deletedCount = end - start + 1;
            // Adjust the selectionMapper to account for the new rows
            getSelectionMapper().removeIndexInterval(start, end);
            getRowModelMapper().removeIndexInterval(start, deletedCount);

        }
        // nothing to do on TableEvent.updated

    }

    /**
     * Convenience method to detect dataChanged table event type.
     * 
     * @param e the event to examine.
     * @return true if the event is of type dataChanged, false else.
     */
    protected boolean isDataChanged(TableModelEvent e) {
        if (e == null)
            return false;
        return e.getType() == TableModelEvent.UPDATE && e.getFirstRow() == 0
                && e.getLastRow() == Integer.MAX_VALUE;
    }

    /**
     * Convenience method to detect update table event type.
     * 
     * @param e the event to examine.
     * @return true if the event is of type update and not dataChanged, false
     *         else.
     */
    protected boolean isUpdate(TableModelEvent e) {
        if (isStructureChanged(e))
            return false;
        return e.getType() == TableModelEvent.UPDATE
                && e.getLastRow() < Integer.MAX_VALUE;
    }

    /**
     * Convenience method to detect a structureChanged table event type.
     * 
     * @param e the event to examine.
     * @return true if the event is of type structureChanged or null, false
     *         else.
     */
    protected boolean isStructureChanged(TableModelEvent e) {
        return e == null || e.getFirstRow() == TableModelEvent.HEADER_ROW;
    }

    /**
     * Trying to hack around #172-swingx: lead/anchor of row selection model is
     * not adjusted to valid (not even model indices!) in the usual
     * clearSelection after dataChanged/structureChanged.
     * 
     * Note: as of jdk1.5U6 the anchor/lead of the view selectionModel is
     * unconditionally set to -1 after data/structureChanged.
     * 
     * @param e
     */
    private void hackLeadAnchor(TableModelEvent e) {
        int lead = getSelectionModel().getLeadSelectionIndex();
        int anchor = getSelectionModel().getAnchorSelectionIndex();
        int lastRow = getModel().getRowCount() - 1;
        if ((lead > lastRow) || (anchor > lastRow)) {
            lead = lastRow;
            getSelectionModel().setAnchorSelectionIndex(lead);
            getSelectionModel().setLeadSelectionIndex(lead);
        }
    }

    /**
     * Called if individual row height mapping need to be updated. This
     * implementation guards against unnessary access of super's private
     * rowModel field.
     */
    protected void updateViewSizeSequence() {
        SizeSequence sizeSequence = null;
        if (isRowHeightEnabled()) {
            sizeSequence = getSuperRowModel();
        }
        getRowModelMapper().setViewSizeSequence(sizeSequence, getRowHeight());
    }

    /**
     * @return <code>SelectionMapper</code>
     */
    public SelectionMapper getSelectionMapper() {
        // JW: why is this public? Probably made so accidentally?
        // maybe not: was introduced in version 1.148 when applying
        // Jesse's patch to #386-swingx (added functionality to
        // turn off the mapping
        if (selectionMapper == null) {
            selectionMapper = new DefaultSelectionMapper(filters,
                    getSelectionModel());
        }
        return selectionMapper;
    }

    // ----------------- enhanced column support: delegation to TableColumnModel
    /**
     * Returns the <code>TableColumn</code> at view position
     * <code>columnIndex</code>. The return value is not <code>null</code>.
     * 
     * <p>
     * NOTE: This delegate method is added to protect developer's from
     * unexpected exceptions in jdk1.5+. Super does not expose the
     * <code>TableColumn</code> access by index which may lead to unexpected
     * <code>IllegalArgumentException</code>: If client code assumes the
     * delegate method is available, autoboxing will convert the given int to an
     * Integer which will call the getColumn(Object) method.
     * 
     * 
     * @param viewColumnIndex index of the column with the object in question
     * 
     * @return the <code>TableColumn</code> object that matches the column index
     * @throws ArrayIndexOutOfBoundsException if viewColumnIndex out of allowed
     *         range.
     * 
     * @see #getColumn(Object)
     * @see #getColumnExt(int)
     * @see TableColumnModel#getColumn(int)
     */
    public TableColumn getColumn(int viewColumnIndex) {
        return getColumnModel().getColumn(viewColumnIndex);
    }

    /**
     * Returns a <code>List</code> of visible <code>TableColumn</code>s.
     * 
     * @return a <code>List</code> of visible columns.
     * @see #getColumns(boolean)
     */
    public List<TableColumn> getColumns() {
        return Collections.list(getColumnModel().getColumns());
    }

    /**
     * Returns the margin between columns.
     * <p>
     * 
     * Convenience to expose column model properties through
     * <code>JXTable</code> api.
     * 
     * @return the margin between columns
     * 
     * @see #setColumnMargin(int)
     * @see TableColumnModel#getColumnMargin()
     */
    public int getColumnMargin() {
        return getColumnModel().getColumnMargin();
    }

    /**
     * Sets the margin between columns.
     * 
     * Convenience to expose column model properties through
     * <code>JXTable</code> api.
     * 
     * @param value margin between columns; must be greater than or equal to
     *        zero.
     * @see #getColumnMargin()
     * @see TableColumnModel#setColumnMargin(int)
     */
    public void setColumnMargin(int value) {
        getColumnModel().setColumnMargin(value);
    }

    // ----------------- enhanced column support: delegation to
    // TableColumnModelExt

    /**
     * Returns the number of contained columns. The count includes or excludes
     * invisible columns, depending on whether the <code>includeHidden</code> is
     * true or false, respectively. If false, this method returns the same count
     * as <code>getColumnCount()</code>. If the columnModel is not of type
     * <code>TableColumnModelExt</code>, the parameter value has no effect.
     * 
     * @param includeHidden a boolean to indicate whether invisible columns
     *        should be included
     * @return the number of contained columns, including or excluding the
     *         invisible as specified.
     * @see #getColumnCount()
     * @see TableColumnModelExt#getColumnCount(boolean)
     */
    public int getColumnCount(boolean includeHidden) {
        if (getColumnModel() instanceof TableColumnModelExt) {
            return ((TableColumnModelExt) getColumnModel())
                    .getColumnCount(includeHidden);
        }
        return getColumnCount();
    }

    /**
     * Returns a <code>List</code> of contained <code>TableColumn</code>s.
     * Includes or excludes invisible columns, depending on whether the
     * <code>includeHidden</code> is true or false, respectively. If false, an
     * <code>Iterator</code> over the List is equivalent to the
     * <code>Enumeration</code> returned by <code>getColumns()</code>. If the
     * columnModel is not of type <code>TableColumnModelExt</code>, the
     * parameter value has no effect.
     * <p>
     * 
     * NOTE: the order of columns in the List depends on whether or not the
     * invisible columns are included, in the former case it's the insertion
     * order in the latter it's the current order of the visible columns.
     * 
     * @param includeHidden a boolean to indicate whether invisible columns
     *        should be included
     * @return a <code>List</code> of contained columns.
     * 
     * @see #getColumns()
     * @see TableColumnModelExt#getColumns(boolean)
     */
    public List<TableColumn> getColumns(boolean includeHidden) {
        if (getColumnModel() instanceof TableColumnModelExt) {
            return ((TableColumnModelExt) getColumnModel())
                    .getColumns(includeHidden);
        }
        return getColumns();
    }

    /**
     * Returns the first <code>TableColumnExt</code> with the given
     * <code>identifier</code>. The return value is null if there is no
     * contained column with <b>identifier</b> or if the column with
     * <code>identifier</code> is not of type <code>TableColumnExt</code>. The
     * returned column may be visible or hidden.
     * 
     * @param identifier the object used as column identifier
     * @return first <code>TableColumnExt</code> with the given identifier or
     *         null if none is found
     * 
     * @see #getColumnExt(int)
     * @see #getColumn(Object)
     * @see TableColumnModelExt#getColumnExt(Object)
     */
    public TableColumnExt getColumnExt(Object identifier) {
        if (getColumnModel() instanceof TableColumnModelExt) {
            return ((TableColumnModelExt) getColumnModel())
                    .getColumnExt(identifier);
        } else {
            // PENDING: not tested!
            try {
                TableColumn column = getColumn(identifier);
                if (column instanceof TableColumnExt) {
                    return (TableColumnExt) column;
                }
            } catch (Exception e) {
                // TODO: handle exception
            }
        }
        return null;
    }

    /**
     * Returns the <code>TableColumnExt</code> at view position
     * <code>columnIndex</code>. The return value is null, if the column at
     * position <code>columnIndex</code> is not of type
     * <code>TableColumnExt</code>. The returned column is visible.
     * 
     * @param viewColumnIndex the index of the column desired
     * @return the <code>TableColumnExt</code> object that matches the column
     *         index
     * @throws ArrayIndexOutOfBoundsException if columnIndex out of allowed
     *         range, that is if
     *         <code> (columnIndex < 0) || (columnIndex >= getColumnCount())</code>
     *         .
     * 
     * @see #getColumnExt(Object)
     * @see #getColumn(int)
     * @see TableColumnModelExt#getColumnExt(int)
     */
    public TableColumnExt getColumnExt(int viewColumnIndex) {
        TableColumn column = getColumn(viewColumnIndex);
        if (column instanceof TableColumnExt) {
            return (TableColumnExt) column;
        }
        return null;
    }

    // ---------------------- enhanced TableColumn/Model support: convenience

    /**
     * Reorders the columns in the sequence given array. Logical names that do
     * not correspond to any column in the model will be ignored. Columns with
     * logical names not contained are added at the end.
     * 
     * PENDING JW - do we want this? It's used by JNTable.
     * 
     * @param identifiers array of logical column names
     * 
     * @see #getColumns(boolean)
     */
    public void setColumnSequence(Object[] identifiers) {
        /*
         * JW: not properly tested (not in all in fact) ...
         */
        List<TableColumn> columns = getColumns(true);
        Map<Object, TableColumn> map = new HashMap<Object, TableColumn>();
        for (Iterator<TableColumn> iter = columns.iterator(); iter.hasNext();) {
            // PENDING: handle duplicate identifiers ...
            TableColumn column = iter.next();
            map.put(column.getIdentifier(), column);
            getColumnModel().removeColumn(column);
        }
        for (int i = 0; i < identifiers.length; i++) {
            TableColumn column = map.get(identifiers[i]);
            if (column != null) {
                getColumnModel().addColumn(column);
                columns.remove(column);
            }
        }
        for (Iterator<TableColumn> iter = columns.iterator(); iter.hasNext();) {
            TableColumn column = (TableColumn) iter.next();
            getColumnModel().addColumn(column);
        }
    }

    // --------------- implement TableColumnModelExtListener

    /**
     * {@inheritDoc}
     * 
     * Listens to column property changes.
     * 
     */
    public void columnPropertyChange(PropertyChangeEvent event) {
        if (event.getPropertyName().equals("editable")) {
            updateEditingAfterColumnChanged((TableColumn) event.getSource(),
                    (Boolean) event.getNewValue());
        } else if (event.getPropertyName().startsWith("highlighter")) {
            if (event.getSource() instanceof TableColumnExt
                    && getRowCount() > 0) {
                TableColumnExt column = (TableColumnExt) event.getSource();

                Rectangle r = getCellRect(0, convertColumnIndexToView(column
                        .getModelIndex()), true);
                r.height = getHeight();
                repaint(r);
            } else {
                repaint();
            }
        }

    }

    /**
     * Adjusts editing state after column's property change. Cancels ongoing
     * editing if the sending column is the editingColumn and the column's
     * editable changed to <code>false</code>, otherwise does nothing.
     * 
     * @param column the <code>TableColumn</code> which sent the change
     *        notifcation
     * @param editable the new value of the column's editable property
     */
    private void updateEditingAfterColumnChanged(TableColumn column,
            boolean editable) {
        if (!isEditing())
            return;
        int viewIndex = convertColumnIndexToView(column.getModelIndex());
        if ((viewIndex < 0) || (viewIndex != getEditingColumn()))
            return;
        getCellEditor().cancelCellEditing();
    }

    // -------------------------- ColumnFactory

    /**
     * Creates, configures and adds default <code>TableColumn</code>s for
     * columns in this table's <code>TableModel</code>. Removes all currently
     * contained <code>TableColumn</code>s. The exact type and configuration of
     * the columns is controlled completely by the <code>ColumnFactory</code>.
     * Client code can use {@link #setColumnFactory(ColumnFactory)} to plug-in a
     * custom ColumnFactory implementing their own default column creation and
     * behaviour.
     * <p>
     * 
     * <b>Note</b>: this method will probably become final (Issue #961-SwingX)
     * so it's strongly recommended to not override now (and replace existing
     * overrides by a custom ColumnFactory)!
     * 
     * @see #setColumnFactory(ColumnFactory)
     * @see org.jdesktop.swingx.table.ColumnFactory
     * 
     */
    @Override
    public void createDefaultColumnsFromModel() {
        // JW: when could this happen?
        if (getModel() == null)
            return;
        // Remove any current columns
        removeColumns();
        createAndAddColumns();
    }

    /**
     * Creates and adds <code>TableColumn</code>s for each column of the table
     * model.
     * <p>
     * 
     * 
     */
    private void createAndAddColumns() {
        /*
         * PENDING: go the whole distance and let the factory decide which model
         * columns to map to view columns? That would introduce an collection
         * managing operation into the factory, sprawling? Can't (and probably
         * don't want to) move all collection related operations over - the
         * ColumnFactory relies on TableColumnExt type columns, while the
         * JXTable has to cope with all the base types.
         */
        for (int i = 0; i < getModel().getColumnCount(); i++) {
            // add directly to columnModel - don't go through this.addColumn
            // to guarantee full control of ColumnFactory
            // addColumn has the side-effect to set the header!
            TableColumnExt tableColumn = getColumnFactory()
                    .createAndConfigureTableColumn(getModel(), i);
            if (tableColumn != null) {
                getColumnModel().addColumn(tableColumn);
            }
        }
    }

    /**
     * Remove all columns, make sure to include hidden.
     * <p>
     */
    private void removeColumns() {
        /*
         * TODO: promote this method to superclass, and change
         * createDefaultColumnsFromModel() to call this method
         */
        List<TableColumn> columns = getColumns(true);
        for (Iterator<TableColumn> iter = columns.iterator(); iter.hasNext();) {
            getColumnModel().removeColumn(iter.next());

        }
    }

    /**
     * Returns the ColumnFactory.
     * <p>
     * 
     * @return the columnFactory to use for column creation and configuration,
     *         guaranteed to not be null.
     * 
     * @see #setColumnFactory(ColumnFactory)
     * @see org.jdesktop.swingx.table.ColumnFactory
     */
    public ColumnFactory getColumnFactory() {
        /*
         * TODO JW: think about implications of not/ copying the reference to
         * the shared instance into the table's field? Better access the
         * getInstance() on each call? We are on single thread anyway...
         * Furthermore, we don't expect the instance to change often, typically
         * it is configured on startup. So we don't really have to worry about
         * changes which would destabilize column state?
         */
        if (columnFactory == null) {
            return ColumnFactory.getInstance();
            // columnFactory = ColumnFactory.getInstance();
        }
        return columnFactory;
    }

    /**
     * Sets the <code>ColumnFactory</code> to use for column creation and
     * configuration. The default value is the shared application ColumnFactory.
     * <p>
     * 
     * Note: this method has no side-effect, that is existing columns are
     * <b>not</b> re-created automatically, client code must trigger it
     * manually.
     * 
     * @param columnFactory the factory to use, <code>null</code> indicates to
     *        use the shared application factory.
     * 
     * @see #getColumnFactory()
     * @see org.jdesktop.swingx.table.ColumnFactory
     */
    public void setColumnFactory(ColumnFactory columnFactory) {
        /*
         * 
         * TODO auto-configure columns on set? or add public table api to do so?
         * Mostly, this is meant to be done once in the lifetime of the table,
         * preferably before a model is set ... overshoot?
         */
        ColumnFactory old = getColumnFactory();
        this.columnFactory = columnFactory;
        firePropertyChange("columnFactory", old, getColumnFactory());
    }

    // -------------------------------- enhanced sizing support

    /**
     * Packs all the columns to their optimal size. Works best with auto
     * resizing turned off.
     * 
     * @param margin the margin to apply to each column.
     * 
     * @see #packColumn(int, int)
     * @see #packColumn(int, int, int)
     */
    public void packTable(int margin) {
        for (int c = 0; c < getColumnCount(); c++)
            packColumn(c, margin, -1);
    }

    /**
     * Packs an indivudal column in the table.
     * 
     * @param column The Column index to pack in View Coordinates
     * @param margin The Margin to apply to the column width.
     * 
     * @see #packColumn(int, int, int)
     * @see #packTable(int)
     */
    public void packColumn(int column, int margin) {
        packColumn(column, margin, -1);
    }

    /**
     * Packs an indivual column in the table to less than or equal to the
     * maximum witdth. If maximum is -1 then the column is made as wide as it
     * needs.
     * 
     * @param column the column index to pack in view coordinates
     * @param margin the margin to apply to the column
     * @param max the maximum width the column can be resized to, -1 means no
     *        limit
     * 
     * @see #packColumn(int, int)
     * @see #packTable(int)
     * @see ColumnFactory#packColumn(JXTable, TableColumnExt, int, int)
     */
    public void packColumn(int column, int margin, int max) {
        getColumnFactory().packColumn(this, getColumnExt(column), margin, max);
    }

    /**
     * Returns the preferred number of rows to show in a
     * <code>JScrollPane</code>.
     * 
     * @return the number of rows to show in a <code>JScrollPane</code>
     * @see #setVisibleRowCount(int)
     */
    public int getVisibleRowCount() {
        return visibleRowCount;
    }

    /**
     * Sets the preferred number of rows to show in a <code>JScrollPane</code>.
     * <p>
     * 
     * This is a bound property. The default value is 20.
     * <p>
     * 
     * PENDING: allow negative for use-all? Analogous to visColumnCount.
     * 
     * @param visibleRowCount number of rows to show in a
     *        <code>JScrollPane</code>
     * @throws IllegalArgumentException if given count is negative.
     * 
     * @see #getVisibleRowCount()
     */
    public void setVisibleRowCount(int visibleRowCount) {
        if (visibleRowCount < 0)
            throw new IllegalArgumentException(
                    "visible row count must not be negative " + visibleRowCount);
        if (getVisibleRowCount() == visibleRowCount)
            return;
        int old = getVisibleRowCount();
        this.visibleRowCount = visibleRowCount;
        resetCalculatedScrollableSize(false);
        firePropertyChange("visibleRowCount", old, getVisibleRowCount());
    }

    /**
     * Returns the preferred number of columns to show in the
     * <code>JScrollPane</code>.
     * 
     * @return the number of columns to show in the scroll pane.
     * 
     * @see #setVisibleColumnCount
     */
    public int getVisibleColumnCount() {
        return visibleColumnCount;
    }

    /**
     * Sets the preferred number of Columns to show in a
     * <code>JScrollPane</code>. A negative number is interpreted as use-all
     * available visible columns.
     * <p>
     * 
     * This is a bound property. The default value is -1 (effectively the same
     * as before the introduction of this property).
     * 
     * @param visibleColumnCount number of rows to show in a
     *        <code>JScrollPane</code>
     * @see #getVisibleColumnCount()
     */
    public void setVisibleColumnCount(int visibleColumnCount) {
        if (getVisibleColumnCount() == visibleColumnCount)
            return;
        int old = getVisibleColumnCount();
        this.visibleColumnCount = visibleColumnCount;
        resetCalculatedScrollableSize(true);
        firePropertyChange("visibleColumnCount", old, getVisibleColumnCount());
    }

    /**
     * Resets the calculated scrollable size in one dimension, if appropriate.
     * 
     * @param isColumn flag to denote which dimension to reset, true for width,
     *        false for height
     * 
     */
    private void resetCalculatedScrollableSize(boolean isColumn) {
        if (calculatedPrefScrollableViewportSize != null) {
            if (isColumn) {
                calculatedPrefScrollableViewportSize.width = -1;
            } else {
                calculatedPrefScrollableViewportSize.height = -1;
            }
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * 
     * If the given dimension is null, the auto-calculation of the pref
     * scrollable size is enabled, otherwise the behaviour is the same as super.
     * 
     * The default is auto-calc enabled on.
     * 
     * @see #getPreferredScrollableViewportSize()
     */
    @Override
    public void setPreferredScrollableViewportSize(Dimension size) {
        // TODO: figure out why firing the event screws the
        // JXTableUnitTest.testPrefScrollableUpdatedOnStructureChanged
        // Dimension old = getPreferredScrollableViewportSize();
        super.setPreferredScrollableViewportSize(size);
        // firePropertyChange("preferredScrollableViewportSize", old,
        // getPreferredScrollableViewportSize());
    }

    /**
     * {@inheritDoc}
     * <p>
     * Overridden to support auto-calculation of pref scrollable size, dependent
     * on the visible row/column count properties. The auto-calc is on if
     * there's no explicit pref scrollable size set. Otherwise the fixed size is
     * returned
     * <p>
     * 
     * The calculation of the preferred scrollable width is delegated to the
     * ColumnFactory to allow configuration with custom strategies implemented
     * in custom factories.
     * 
     * @see #setPreferredScrollableViewportSize(Dimension)
     * @see org.jdesktop.swingx.table.ColumnFactory#getPreferredScrollableViewportWidth(JXTable)
     */
    @Override
    public Dimension getPreferredScrollableViewportSize() {
        // client code has set this - takes precedence.
        Dimension prefSize = super.getPreferredScrollableViewportSize();
        if (prefSize != null) {
            return new Dimension(prefSize);
        }
        if (calculatedPrefScrollableViewportSize == null) {
            calculatedPrefScrollableViewportSize = new Dimension();
            // JW: hmm... fishy ... shouldn't be necessary here?
            // maybe its the "early init" in super's tableChanged();
            // moved to init which looks okay so far
            // initializeColumnPreferredWidths();
        }
        // the width is reset to -1 in setVisibleColumnCount
        if (calculatedPrefScrollableViewportSize.width <= 0) {
            calculatedPrefScrollableViewportSize.width = getColumnFactory()
                    .getPreferredScrollableViewportWidth(this);
        }
        // the heigth is reset in setVisualRowCount
        if (calculatedPrefScrollableViewportSize.height <= 0) {
            calculatedPrefScrollableViewportSize.height = getVisibleRowCount()
                    * getRowHeight();
        }
        return new Dimension(calculatedPrefScrollableViewportSize);
    }

    /**
     * Initialize the width related properties of all contained TableColumns,
     * both visible and hidden.
     * <p>
     * <ul>
     * <li>PENDING: move into ColumnFactory?
     * <li>PENDING: what to do if autoCreateColumn off?
     * <li>PENDING: public? to allow manual setting of column properties which
     * might effect their default sizing. Needed in testing - but real-world?
     * the factory is meant to do the property setting, based on tableModel and
     * meta-data (from where?). But leads to funny call sequence for per-table
     * factory (new JXTable(), table.setColumnFactory(..), table.setModel(...))
     * </ul>
     * 
     * @see #initializeColumnPreferredWidth(TableColumn)
     */
    protected void initializeColumnWidths() {
        for (TableColumn column : getColumns(true)) {
            initializeColumnPreferredWidth(column);
        }
    }

    /**
     * Initialize the width related properties of the specified column. The
     * details are specified by the current <code>ColumnFactory</code> if the
     * column is of type <code>TableColumnExt</code>. Otherwise nothing is
     * changed.
     * <p>
     * 
     * TODO JW - need to cleanup getScrollablePreferred (refactor and inline)
     * 
     * @param column TableColumn object representing view column
     * @see org.jdesktop.swingx.table.ColumnFactory#configureColumnWidths
     */
    protected void initializeColumnPreferredWidth(TableColumn column) {
        if (column instanceof TableColumnExt) {
            getColumnFactory().configureColumnWidths(this,
                    (TableColumnExt) column);
        }
    }

    // ----------------- scrolling support
    /**
     * Scrolls vertically to make the given row visible. This might not have any
     * effect if the table isn't contained in a <code>JViewport</code>.
     * <p>
     * 
     * Note: this method has no precondition as it internally uses
     * <code>getCellRect</code> which is lenient to off-range coordinates.
     * 
     * @param row the view row index of the cell
     * 
     * @see #scrollColumnToVisible(int)
     * @see #scrollCellToVisible(int, int)
     * @see #scrollRectToVisible(Rectangle)
     */
    public void scrollRowToVisible(int row) {
        Rectangle cellRect = getCellRect(row, 0, false);
        Rectangle visibleRect = getVisibleRect();
        cellRect.x = visibleRect.x;
        cellRect.width = visibleRect.width;
        scrollRectToVisible(cellRect);
    }

    /**
     * Scrolls horizontally to make the given column visible. This might not
     * have any effect if the table isn't contained in a <code>JViewport</code>.
     * <p>
     * 
     * Note: this method has no precondition as it internally uses
     * <code>getCellRect</code> which is lenient to off-range coordinates.
     * 
     * @param column the view column index of the cell
     * 
     * @see #scrollRowToVisible(int)
     * @see #scrollCellToVisible(int, int)
     * @see #scrollRectToVisible(Rectangle)
     */
    public void scrollColumnToVisible(int column) {
        Rectangle cellRect = getCellRect(0, column, false);
        Rectangle visibleRect = getVisibleRect();
        cellRect.y = visibleRect.y;
        cellRect.height = visibleRect.height;
        scrollRectToVisible(cellRect);
    }

    /**
     * Scrolls to make the cell at row and column visible. This might not have
     * any effect if the table isn't contained in a <code>JViewport</code>.
     * <p>
     * 
     * Note: this method has no precondition as it internally uses
     * <code>getCellRect</code> which is lenient to off-range coordinates.
     * 
     * @param row the view row index of the cell
     * @param column the view column index of the cell
     * 
     * @see #scrollColumnToVisible(int)
     * @see #scrollRowToVisible(int)
     * @see #scrollRectToVisible(Rectangle)
     */
    public void scrollCellToVisible(int row, int column) {
        Rectangle cellRect = getCellRect(row, column, false);
        scrollRectToVisible(cellRect);
    }

    // ----------------------- delegating methods?? from super
    /**
     * Returns the selection mode used by this table's selection model.
     * <p>
     * PENDING JW - setter?
     * 
     * @return the selection mode used by this table's selection model
     * @see ListSelectionModel#getSelectionMode()
     */
    public int getSelectionMode() {
        return getSelectionModel().getSelectionMode();
    }

    // ----------------------------------- uniform data model access
    /**
     * @return the unconfigured ComponentAdapter.
     */
    protected ComponentAdapter getComponentAdapter() {
        if (dataAdapter == null) {
            dataAdapter = new TableAdapter(this);
        }
        return dataAdapter;
    }

    /**
     * Convenience to access a configured ComponentAdapter.
     * 
     * @param row the row index in view coordinates.
     * @param column the column index in view coordinates.
     * @return the configured ComponentAdapter.
     */
    protected ComponentAdapter getComponentAdapter(int row, int column) {
        ComponentAdapter adapter = getComponentAdapter();
        adapter.row = row;
        adapter.column = column;
        return adapter;
    }

    protected static class TableAdapter extends ComponentAdapter {
        private final JXTable table;

        /**
         * Constructs a <code>TableDataAdapter</code> for the specified target
         * component.
         * 
         * @param component the target component
         */
        public TableAdapter(JXTable component) {
            super(component);
            table = component;
        }

        /**
         * Typesafe accessor for the target component.
         * 
         * @return the target component as a {@link javax.swing.JTable}
         */
        public JXTable getTable() {
            return table;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getColumnName(int columnIndex) {
            TableColumn column = getColumnByModelIndex(columnIndex);
            return column == null ? "" : column.getHeaderValue().toString();
        }

        protected TableColumn getColumnByModelIndex(int modelColumn) {
            // throwing here makes a filter test fail .. it's probably an issue
            // but don't want to touch (swingx filters will be gone soon)
            // if ((modelColumn < 0) || (modelColumn >= getColumnCount())) {
            // throw new IllegalArgumentException("invalid column index: " +
            // modelColumn);
            // }
            List<TableColumn> columns = table.getColumns(true);
            for (Iterator<TableColumn> iter = columns.iterator(); iter
                    .hasNext();) {
                TableColumn column = iter.next();
                if (column.getModelIndex() == modelColumn) {
                    return column;
                }
            }
            return null;
        }

        // @Override
        // public String getColumnIdentifier(int columnIndex) {
        // // TableColumn column = getColumnByModelIndex(columnIndex);
        // // Object identifier = column != null ? column.getIdentifier() :
        // null;
        // Object identifier = getColumnIdentifierAt(columnIndex);
        // return identifier != null ? identifier.toString() : null;
        // }

        /**
         * {@inheritDoc}
         */
        @Override
        public Object getColumnIdentifierAt(int columnIndex) {
            if ((columnIndex < 0) || (columnIndex >= getColumnCount())) {
                throw new ArrayIndexOutOfBoundsException(
                        "invalid column index: " + columnIndex);
            }
            TableColumn column = getColumnByModelIndex(columnIndex);
            Object identifier = column != null ? column.getIdentifier() : null;
            return identifier;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getColumnIndex(Object identifier) {
            TableColumn column = table.getColumnExt(identifier);
            return column != null ? column.getModelIndex() : -1;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getColumnCount() {
            return table.getModel().getColumnCount();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getRowCount() {
            return table.getModel().getRowCount();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Object getValueAt(int row, int column) {
            return table.getModel().getValueAt(row, column);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void setValueAt(Object aValue, int row, int column) {
            table.getModel().setValueAt(aValue, row, column);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isCellEditable(int row, int column) {
            return table.getModel().isCellEditable(row, column);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isTestable(int column) {
            return getColumnByModelIndex(column) != null;
        }

        // -------------------------- accessing view state/values

        /**
         * {@inheritDoc}
         */
        @Override
        public Object getFilteredValueAt(int row, int column) {
            return getValueAt(table.convertRowIndexToModel(row), column);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Object getValue() {
            return table.getValueAt(row, column);
        }

        /**
         * {@inheritDoc}
         * <p>
         * 
         * PENDING JW: this is implemented to duplicate this table's lookup code
         * if the column is not visible. That's not good enough if subclasses
         * implemented a different strategy! We do it anyway for now, mostly we
         * will be lucky and improve the situation against using toString
         * always.
         * 
         */
        @Override
        public String getFilteredStringAt(int row, int column) {
            int viewColumn = modelToView(column);
            if (viewColumn >= 0) {
                return table.getStringAt(row, viewColumn);
            }
            // PENDING JW: how to get a String rep for invisible cells?
            // rows may be filtered, columns hidden.
            TableCellRenderer renderer = getRendererByModelColumn(column);
            if (renderer instanceof StringValue) {
                return ((StringValue) renderer).getString(getFilteredValueAt(
                        row, column));
            }
            return super.getFilteredStringAt(row, column);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getString() {
            return table.getStringAt(row, column);
        }

        /**
         * {@inheritDoc}
         * 
         * PENDING JW: this is implemented to duplicate this table's lookup code
         * if either the row or the column is not visible. That's not good
         * enough if subclasses implemented a different strategy! We do it
         * anyway for now, mostly we will be lucky and improve the situation
         * against using toString always.
         * 
         */
        @Override
        public String getStringAt(int row, int column) {
            int viewRow = table.convertRowIndexToView(row);
            int viewColumn = table.convertColumnIndexToView(column);
            if ((viewRow >= 0) && (viewColumn >= 0)) {
                return table.getStringAt(viewRow, viewColumn);
            }

            TableCellRenderer renderer = getRendererByModelColumn(column);
            if (renderer instanceof StringValue) {
                return ((StringValue) renderer).getString(getValueAt(row,
                        column));
            }
            // no luck - return default
            return super.getStringAt(row, column);
        }

        /**
         * Returns a suitable renderer for the column index in model
         * coordinates.
         * 
         * PENDING JW: this duplicates this table's lookup code if column is not
         * visible. That's not good enough if subclasses implemented a different
         * strategy! We do it anyway for now, mostly we will be lucky and
         * improve the situation against using toString always.
         * 
         * @param column the columnIndex in model coordinates
         * @return a renderer suitable for rendering cells in the given column
         */
        private TableCellRenderer getRendererByModelColumn(int column) {
            // PENDING JW: here we are tricksing - duplicating JXTable renderer
            // lookup strategy
            // that's inherently unsafe, as subclasses may decide to do it
            // differently
            TableColumn tableColumn = getColumnByModelIndex(column);
            TableCellRenderer renderer = tableColumn.getCellRenderer();
            if (renderer == null) {
                renderer = table.getDefaultRenderer(table.getModel()
                        .getColumnClass(column));
            }
            if (renderer == null) {
                renderer = table.getDefaultRenderer(Object.class);
            }
            return renderer;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isEditable() {
            return table.isCellEditable(row, column);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isSelected() {
            return table.isCellSelected(row, column);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean hasFocus() {
            boolean rowIsLead = (table.getSelectionModel()
                    .getLeadSelectionIndex() == row);
            boolean colIsLead = (table.getColumnModel().getSelectionModel()
                    .getLeadSelectionIndex() == column);
            return table.isFocusOwner() && (rowIsLead && colIsLead);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int modelToView(int columnIndex) {
            return table.convertColumnIndexToView(columnIndex);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int viewToModel(int columnIndex) {
            return table.convertColumnIndexToModel(columnIndex);
        }

    }

    // --------------------- managing renderers/editors

    /**
     * Sets the <code>Highlighter</code>s to the table, replacing any old
     * settings. None of the given Highlighters must be null.
     * <p>
     * 
     * This is a bound property.
     * <p>
     * 
     * Note: as of version #1.257 the null constraint is enforced strictly. To
     * remove all highlighters use this method without param.
     * 
     * @param highlighters zero or more not null highlighters to use for
     *        renderer decoration.
     * @throws NullPointerException if array is null or array contains null
     *         values.
     * 
     * @see #getHighlighters()
     * @see #addHighlighter(Highlighter)
     * @see #removeHighlighter(Highlighter)
     * 
     */
    public void setHighlighters(Highlighter... highlighters) {
        Highlighter[] old = getHighlighters();
        getCompoundHighlighter().setHighlighters(highlighters);
        firePropertyChange("highlighters", old, getHighlighters());
    }

    /**
     * Returns the <code>Highlighter</code>s used by this table. Maybe empty,
     * but guarantees to be never null.
     * 
     * @return the Highlighters used by this table, guaranteed to never null.
     * @see #setHighlighters(Highlighter[])
     */
    public Highlighter[] getHighlighters() {
        return getCompoundHighlighter().getHighlighters();
    }

    /**
     * Appends a <code>Highlighter</code> to the end of the list of used
     * <code>Highlighter</code>s. The argument must not be null.
     * <p>
     * 
     * @param highlighter the <code>Highlighter</code> to add, must not be null.
     * @throws NullPointerException if <code>Highlighter</code> is null.
     * 
     * @see #removeHighlighter(Highlighter)
     * @see #setHighlighters(Highlighter[])
     */
    public void addHighlighter(Highlighter highlighter) {
        Highlighter[] old = getHighlighters();
        getCompoundHighlighter().addHighlighter(highlighter);
        firePropertyChange("highlighters", old, getHighlighters());
    }

    /**
     * Removes the given Highlighter.
     * <p>
     * 
     * Does nothing if the Highlighter is not contained.
     * 
     * @param highlighter the Highlighter to remove.
     * @see #addHighlighter(Highlighter)
     * @see #setHighlighters(Highlighter...)
     */
    public void removeHighlighter(Highlighter highlighter) {
        Highlighter[] old = getHighlighters();
        getCompoundHighlighter().removeHighlighter(highlighter);
        firePropertyChange("highlighters", old, getHighlighters());
    }

    /**
     * Returns the CompoundHighlighter assigned to the table, null if none.
     * PENDING: open up for subclasses again?.
     * 
     * @return the CompoundHighlighter assigned to the table.
     */
    protected CompoundHighlighter getCompoundHighlighter() {
        if (compoundHighlighter == null) {
            compoundHighlighter = new CompoundHighlighter();
            compoundHighlighter
                    .addChangeListener(getHighlighterChangeListener());
        }
        return compoundHighlighter;
    }

    /**
     * Returns the <code>ChangeListener</code> to use with highlighters. Lazily
     * creates the listener.
     * 
     * @return the ChangeListener for observing changes of highlighters,
     *         guaranteed to be <code>not-null</code>
     */
    protected ChangeListener getHighlighterChangeListener() {
        if (highlighterChangeListener == null) {
            highlighterChangeListener = createHighlighterChangeListener();
        }
        return highlighterChangeListener;
    }

    /**
     * Creates and returns the ChangeListener observing Highlighters.
     * <p>
     * Here: repaints the table on receiving a stateChanged.
     * 
     * @return the ChangeListener defining the reaction to changes of
     *         highlighters.
     */
    protected ChangeListener createHighlighterChangeListener() {
        return new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                repaint();
            }
        };
    }

    /**
     * Returns the string representation of the cell value at the given
     * position.
     * 
     * @param row the row index of the cell in view coordinates
     * @param column the column index of the cell in view coordinates.
     * @return the string representation of the cell value as it will appear in
     *         the table.
     */
    public String getStringAt(int row, int column) {
        TableCellRenderer renderer = getCellRenderer(row, column);
        if (renderer instanceof StringValue) {
            return ((StringValue) renderer).getString(getValueAt(row, column));
        }
        return StringValues.TO_STRING.getString(getValueAt(row, column));
    }

    /**
     * {@inheritDoc}
     * <p>
     * 
     * Overridden to fix core bug #4614616 (NPE if <code>TableModel</code>'s
     * <code>Class</code> for the column is an interface). This method
     * guarantees to always return a <code>not null</code> value. Returns the
     * default renderer for <code>Object</code> if super returns
     * <code>null</code>.
     * 
     * 
     */
    @Override
    public TableCellRenderer getCellRenderer(int row, int column) {
        TableCellRenderer renderer = super.getCellRenderer(row, column);
        if (renderer == null) {
            renderer = getDefaultRenderer(Object.class);
        }
        return renderer;
    }

    /**
     * Returns the decorated <code>Component</code> used as a stamp to render
     * the specified cell. Overrides superclass version to provide support for
     * cell decorators.
     * <p>
     * 
     * Adjusts component orientation (guaranteed to happen before applying
     * Highlighters).
     * <p>
     * 
     * Per-column highlighters contained in
     * {@link TableColumnExt#getHighlighters()} are applied to the renderer
     * <i>after</i> the table highlighters.
     * <p>
     * 
     * TODO kgs: interaction of search highlighter and column highlighters
     * <p>
     * 
     * Note: DefaultTableCellRenderer and subclasses require a hack to play
     * nicely with Highlighters because it has an internal "color memory" in
     * setForeground/setBackground. The hack is applied in
     * <code>resetDefaultTableCellRendererColors</code> which is called after
     * super.prepareRenderer and before applying the Highlighters. The method is
     * called always and for all renderers.
     * 
     * @param renderer the <code>TableCellRenderer</code> to prepare
     * @param row the row of the cell to render, where 0 is the first row
     * @param column the column of the cell to render, where 0 is the first
     *        column
     * @return the decorated <code>Component</code> used as a stamp to render
     *         the specified cell
     * @see #resetDefaultTableCellRendererColors(Component, int, int)
     * @see org.jdesktop.swingx.decorator.Highlighter
     */
    @Override
    public Component prepareRenderer(TableCellRenderer renderer, int row,
            int column) {
        Component stamp = super.prepareRenderer(renderer, row, column);
        // #145-swingx: default renderers don't respect componentOrientation.
        adjustComponentOrientation(stamp);
        // #258-swingx: hacking around DefaultTableCellRenderer color memory.
        resetDefaultTableCellRendererColors(stamp, row, column);

        ComponentAdapter adapter = getComponentAdapter(row, column);
        // a very slight optimization: if this instance never had a highlighter
        // added then don't create a compound here.
        if (compoundHighlighter != null) {
            stamp = compoundHighlighter.highlight(stamp, adapter);
        }

        TableColumnExt columnExt = getColumnExt(column);

        if (columnExt != null) {
            // JW: fix for #838 - artificial compound installs listener
            // PENDING JW: instead of doing the looping ourselves, how
            // about adding a method prepareRenderer to the TableColumnExt
            for (Highlighter highlighter : columnExt.getHighlighters()) {
                stamp = highlighter.highlight(stamp, adapter);

            }
            // CompoundHighlighter columnHighlighters
            // = new CompoundHighlighter(columnExt.getHighlighters());

        }

        return stamp;
    }

    /**
     * 
     * Method to apply a hack around DefaultTableCellRenderer "color memory"
     * (Issue #258-swingx). Applies the hack if the client property
     * <code>USE_DTCR_COLORMEMORY_HACK</code> having the value of
     * <code>Boolean.TRUE</code>, does nothing otherwise. The property is true
     * by default.
     * <p>
     * 
     * The hack consists of applying a specialized <code>Highlighter</code> to
     * force reset the color "memory" of <code>DefaultTableCellRenderer</code>.
     * Note that the hack is applied always, that is even if there are no custom
     * Highlighters.
     * <p>
     * 
     * Client code which solves the problem at the core (that is in a
     * well-behaved <code>DefaultTableCellRenderer</code>) can disable the hack
     * by removing the client property or by subclassing and override this to do
     * nothing.
     * 
     * @param renderer the <code>TableCellRenderer</code> to hack
     * @param row the row of the cell to render
     * @param column the column index of the cell to render
     * 
     * @see #prepareRenderer(TableCellRenderer, int, int)
     * @see #USE_DTCR_COLORMEMORY_HACK
     * @see org.jdesktop.swingx.decorator.ResetDTCRColorHighlighter
     */
    protected void resetDefaultTableCellRendererColors(Component renderer,
            int row, int column) {
        if (!Boolean.TRUE.equals(getClientProperty(USE_DTCR_COLORMEMORY_HACK)))
            return;
        ComponentAdapter adapter = getComponentAdapter(row, column);
        if (resetDefaultTableCellRendererHighlighter == null) {
            resetDefaultTableCellRendererHighlighter = new ResetDTCRColorHighlighter();
        }
        // hacking around DefaultTableCellRenderer color memory.
        resetDefaultTableCellRendererHighlighter.highlight(renderer, adapter);
    }

    /**
     * {@inheritDoc}
     * <p>
     * 
     * Overridden to adjust the editor's component orientation.
     */
    @Override
    public Component prepareEditor(TableCellEditor editor, int row, int column) {
        Component comp = super.prepareEditor(editor, row, column);
        // JW: might be null if generic editor barks about constructor
        // super silently backs out - we do the same here
        if (comp != null) {
            adjustComponentOrientation(comp);
        }
        return comp;
    }

    /**
     * Adjusts the <code>Component</code>'s orientation to this
     * <code>JXTable</code>'s CO if appropriate. The parameter must not be
     * <code>null</code>.
     * <p>
     * 
     * This implementation synchs the CO always.
     * 
     * @param stamp the <code>Component</code> who's CO may need to be synched,
     *        must not be <code>null</code>.
     */
    protected void adjustComponentOrientation(Component stamp) {
        if (stamp.getComponentOrientation().equals(getComponentOrientation()))
            return;
        stamp.applyComponentOrientation(getComponentOrientation());
    }

    /**
     * Returns a new instance of the default renderer for the specified class.
     * This differs from <code>getDefaultRenderer()</code> in that it returns a
     * <b>new </b> instance each time so that the renderer may be set and
     * customized on a particular column.
     * <p>
     * 
     * NOTE: this doesn't work with swingx renderers! Do we really need it? It
     * had been used in JNTable which is practically obsolete. If needed, we
     * could make all renderer support classes clonable.
     * 
     * @param columnClass Class of value being rendered
     * @return TableCellRenderer instance which renders values of the specified
     *         type
     * @see #getDefaultRenderer(Class)
     */
    public TableCellRenderer getNewDefaultRenderer(Class<?> columnClass) {
        TableCellRenderer renderer = getDefaultRenderer(columnClass);
        if (renderer != null) {
            try {
                return renderer.getClass().newInstance();
            } catch (Exception e) {
                LOG.fine("could not create renderer for " + columnClass);
            }
        }
        // JW PENDING: must not return null!
        return null;
    }

    /**
     * Creates default cell renderers for <code>Object</code>s,
     * <code>Number</code>s, <code>Date</code>s, <code>Boolean</code>s, and
     * <code>Icon/Image/</code>s.
     * <p>
     * Overridden to install SwingX renderers plus hacking around huge memory
     * consumption of UIDefaults (see #6345050 in core Bug parade)
     * <p>
     * {@inheritDoc}
     * 
     * @see org.jdesktop.swingx.renderer.DefaultTableRenderer
     * @see org.jdesktop.swingx.renderer.ComponentProvider
     */
    @Override
    protected void createDefaultRenderers() {
        // super.createDefaultRenderers();
        // This duplicates JTable's functionality in order to make the renderers
        // available in getNewDefaultRenderer(); If JTable's renderers either
        // were public, or it provided a factory for *new* renderers, this would
        // not be needed

        // hack around #6345050 - new UIDefaults()
        // is created with a huge initialCapacity
        // giving a dummy key/value array as parameter reduces that capacity
        // to length/2.
        Object[] dummies = new Object[] { 1, 0, 2, 0, 3, 0, 4, 0, 5, 0, 6, 0,
                7, 0, 8, 0, 9, 0, 10, 0, };
        defaultRenderersByColumnClass = new UIDefaults(dummies);
        defaultRenderersByColumnClass.clear();
        // configured default table renderer (internally LabelProvider)
        setDefaultRenderer(Object.class, new DefaultTableRenderer());
        setDefaultRenderer(Number.class, new DefaultTableRenderer(
                StringValues.NUMBER_TO_STRING, JLabel.RIGHT));
        setDefaultRenderer(Date.class, new DefaultTableRenderer(
                StringValues.DATE_TO_STRING));
        // use the same center aligned default for Image/Icon
        TableCellRenderer renderer = new DefaultTableRenderer(new MappedValue(
                StringValues.EMPTY, IconValues.ICON), JLabel.CENTER);
        setDefaultRenderer(Icon.class, renderer);
        setDefaultRenderer(ImageIcon.class, renderer);
        // use a ButtonProvider for booleans
        setDefaultRenderer(Boolean.class, new DefaultTableRenderer(
                new CheckBoxProvider()));

        // // standard renderers
        // // Objects
        // setLazyRenderer(Object.class,
        // "javax.swing.table.DefaultTableCellRenderer");
        //
        // // Numbers
        // setLazyRenderer(Number.class,
        // "org.jdesktop.swingx.JXTable$NumberRenderer");
        //
        // // Doubles and Floats
        // setLazyRenderer(Float.class,
        // "org.jdesktop.swingx.JXTable$DoubleRenderer");
        // setLazyRenderer(Double.class,
        // "org.jdesktop.swingx.JXTable$DoubleRenderer");
        //
        // // Dates
        // setLazyRenderer(Date.class,
        // "org.jdesktop.swingx.JXTable$DateRenderer");
        //
        // // Icons and ImageIcons
        // setLazyRenderer(Icon.class,
        // "org.jdesktop.swingx.JXTable$IconRenderer");
        // setLazyRenderer(ImageIcon.class,
        // "org.jdesktop.swingx.JXTable$IconRenderer");
        //
        // // Booleans
        // setLazyRenderer(Boolean.class,
        // "org.jdesktop.swingx.JXTable$BooleanRenderer");

    }

    /** c&p'ed from super */
    @SuppressWarnings("unchecked")
    private void setLazyValue(Hashtable h, Class c, String s) {
        h.put(c, new UIDefaults.ProxyLazyValue(s));
    }

    /** c&p'ed from super */
    private void setLazyEditor(Class<?> c, String s) {
        setLazyValue(defaultEditorsByColumnClass, c, s);
    }

    /**
     * Creates default cell editors for objects, numbers, and boolean values.
     * <p>
     * Overridden to hook enhanced editors (f.i. <code>NumberEditorExt</code>
     * )plus hacking around huge memory consumption of UIDefaults (see #6345050
     * in core Bug parade)
     * 
     * @see DefaultCellEditor
     */
    @Override
    protected void createDefaultEditors() {
        Object[] dummies = new Object[] { 1, 0, 2, 0, 3, 0, 4, 0, 5, 0, 6, 0,
                7, 0, 8, 0, 9, 0, 10, 0,

        };
        defaultEditorsByColumnClass = new UIDefaults(dummies);
        defaultEditorsByColumnClass.clear();
        // defaultEditorsByColumnClass = new UIDefaults();

        // Objects
        setLazyEditor(Object.class, "org.jdesktop.swingx.JXTable$GenericEditor");

        // Numbers
        // setLazyEditor(Number.class,
        // "org.jdesktop.swingx.JXTable$NumberEditor");
        setLazyEditor(Number.class, "org.jdesktop.swingx.table.NumberEditorExt");

        // Booleans
        setLazyEditor(Boolean.class,
                "org.jdesktop.swingx.JXTable$BooleanEditor");

    }

    /**
     * Default editor registered for <code>Object</code>. The editor tries to
     * create a new instance of the column's class by reflection. It assumes
     * that the class has a constructor taking a single <code>String</code>
     * parameter.
     * <p>
     * 
     * The editor can be configured with a custom <code>JTextField</code>.
     * 
     */
    public static class GenericEditor extends DefaultCellEditor {

        Class<?>[] argTypes = new Class<?>[] { String.class };

        java.lang.reflect.Constructor<?> constructor;

        Object value;

        public GenericEditor() {
            this(new JTextField());
        }

        public GenericEditor(JTextField textField) {
            super(textField);
            getComponent().setName("Table.editor");
        }

        @Override
        public boolean stopCellEditing() {
            String s = (String) super.getCellEditorValue();
            // Here we are dealing with the case where a user
            // has deleted the string value in a cell, possibly
            // after a failed validation. Return null, so that
            // they have the option to replace the value with
            // null or use escape to restore the original.
            // For Strings, return "" for backward compatibility.
            if ("".equals(s)) {
                if (constructor.getDeclaringClass() == String.class) {
                    value = s;
                }
                super.stopCellEditing();
            }

            try {
                value = constructor.newInstance(new Object[] { s });
            } catch (Exception e) {
                ((JComponent) getComponent()).setBorder(new LineBorder(
                        Color.red));
                return false;
            }
            return super.stopCellEditing();
        }

        @Override
        public Component getTableCellEditorComponent(JTable table,
                Object value, boolean isSelected, int row, int column) {
            this.value = null;
            ((JComponent) getComponent())
                    .setBorder(new LineBorder(Color.black));
            try {
                Class<?> type = table.getColumnClass(column);
                // Since our obligation is to produce a value which is
                // assignable for the required type it is OK to use the
                // String constructor for columns which are declared
                // to contain Objects. A String is an Object.
                if (type == Object.class) {
                    type = String.class;
                }
                constructor = type.getConstructor(argTypes);
            } catch (Exception e) {
                return null;
            }
            return super.getTableCellEditorComponent(table, value, isSelected,
                    row, column);
        }

        @Override
        public Object getCellEditorValue() {
            return value;
        }
    }

    /**
     * 
     * Editor for <code>Number</code>s.
     * <p>
     * Note: this is no longer registered by default. The current default is
     * <code>NumberEditorExt</code> which differs from this in being
     * locale-aware.
     * 
     */
    public static class NumberEditor extends GenericEditor {

        public NumberEditor() {
            ((JTextField) getComponent())
                    .setHorizontalAlignment(JTextField.RIGHT);
        }
    }

    /**
     * The default editor for <code>Boolean</code> types.
     */
    public static class BooleanEditor extends DefaultCellEditor {
        public BooleanEditor() {
            super(new JCheckBox());
            JCheckBox checkBox = (JCheckBox) getComponent();
            checkBox.setHorizontalAlignment(JCheckBox.CENTER);
        }
    }

    // ----------------------------- enhanced editing support

    /**
     * Returns the editable property of the <code>JXTable</code> as a whole.
     * 
     * @return boolean to indicate if the table is editable.
     * @see #setEditable
     */
    public boolean isEditable() {
        return editable;
    }

    /**
     * Sets the editable property. This property allows to mark all cells in a
     * table as read-only, independent of their per-column editability as
     * returned by <code>TableColumnExt.isEditable</code> and their per-cell
     * editability as returned by the <code>TableModel.isCellEditable</code>. If
     * a cell is read-only in its column or model layer, this property has no
     * effect.
     * <p>
     * 
     * The default value is <code>true</code>.
     * 
     * @param editable the flag to indicate if the table is editable.
     * @see #isEditable
     * @see #isCellEditable(int, int)
     */
    public void setEditable(boolean editable) {
        boolean old = isEditable();
        this.editable = editable;
        firePropertyChange("editable", old, isEditable());
    }

    /**
     * Returns the property which determines the edit termination behaviour on
     * focus lost.
     * 
     * @return boolean to indicate whether an ongoing edit should be terminated
     *         if the focus is moved to somewhere outside of the table.
     * @see #setTerminateEditOnFocusLost(boolean)
     */
    public boolean isTerminateEditOnFocusLost() {
        return Boolean.TRUE
                .equals(getClientProperty("terminateEditOnFocusLost"));
    }

    /**
     * Sets the property to determine whether an ongoing edit should be
     * terminated if the focus is moved to somewhere outside of the table. If
     * true, terminates the edit, does nothing otherwise. The exact behaviour is
     * implemented in <code>JTable.CellEditorRemover</code>: "outside" is
     * interpreted to be on a component which is not under the table hierarchy
     * but inside the same toplevel window, "terminate" does so in any case,
     * first tries to stop the edit, if that's unsuccessful it cancels the edit.
     * <p>
     * The default value is <code>true</code>.
     * 
     * @param terminate the flag to determine whether or not to terminate the
     *        edit
     * @see #isTerminateEditOnFocusLost()
     */
    public void setTerminateEditOnFocusLost(boolean terminate) {
        // JW: we can leave the propertyChange notification to the
        // putClientProperty - the key and method name are the same
        putClientProperty("terminateEditOnFocusLost", terminate);
    }

    /**
     * Returns the autoStartsEdit property.
     * 
     * @return boolean to indicate whether a keyStroke should try to start
     *         editing.
     * @see #setAutoStartEditOnKeyStroke(boolean)
     */
    public boolean isAutoStartEditOnKeyStroke() {
        return !Boolean.FALSE
                .equals(getClientProperty("JTable.autoStartsEdit"));
    }

    /**
     * Sets the autoStartsEdit property. If true, keystrokes are passed-on to
     * the cellEditor of the lead cell to let it decide whether to start an
     * edit.
     * <p>
     * The default value is <code>true</code>.
     * <p>
     * 
     * @param autoStart boolean to determine whether a keyStroke should try to
     *        start editing.
     * @see #isAutoStartEditOnKeyStroke()
     */
    public void setAutoStartEditOnKeyStroke(boolean autoStart) {
        boolean old = isAutoStartEditOnKeyStroke();
        // JW: we have to take over propertyChange notification
        // because the key and method name are different.
        // As a consequence, there are two events fired: one for
        // the client prop and one for this method.
        putClientProperty("JTable.autoStartsEdit", autoStart);
        firePropertyChange("autoStartEditOnKeyStroke", old,
                isAutoStartEditOnKeyStroke());
    }

    /**
     * {@inheritDoc}
     * <p>
     * 
     * overridden to install a custom editor remover.
     */
    @Override
    public boolean editCellAt(int row, int column, EventObject e) {
        boolean started = super.editCellAt(row, column, e);
        if (started) {
            hackEditorRemover();
        }
        return started;
    }

    /**
     * Overridden with backport from Mustang fix for #4684090, #4887999.
     */
    @Override
    public void removeEditor() {
        // if (editorRemover != null) {
        // editorRemover.uninstall();
        // editorRemover = null;
        // }
        boolean isFocusOwnerInTheTable = isFocusOwnerDescending();
        // let super do its stuff
        super.removeEditor();
        if (isFocusOwnerInTheTable) {
            requestFocusInWindow();
        }
    }

    /**
     * Returns a boolean to indicate if the current focus owner is descending
     * from this table. Returns false if not editing, otherwise walks the
     * focusOwner hierarchy, taking popups into account.
     * 
     * @return a boolean to indicate if the current focus owner is contained.
     */
    private boolean isFocusOwnerDescending() {
        if (!isEditing())
            return false;
        Component focusOwner = KeyboardFocusManager
                .getCurrentKeyboardFocusManager().getFocusOwner();
        // PENDING JW: special casing to not fall through ... really wanted?
        if (focusOwner == null)
            return false;
        if (SwingXUtilities.isDescendingFrom(focusOwner, this))
            return true;
        // same with permanent focus owner
        Component permanent = KeyboardFocusManager
                .getCurrentKeyboardFocusManager().getPermanentFocusOwner();
        return SwingXUtilities.isDescendingFrom(permanent, this);
    }

    // /**
    // * @param focusOwner
    // * @return
    // */
    // private boolean isDescending(Component focusOwner) {
    // while (focusOwner != null) {
    // if (focusOwner instanceof JPopupMenu) {
    // focusOwner = ((JPopupMenu) focusOwner).getInvoker();
    // if (focusOwner == null) {
    // return false;
    // }
    // }
    // if (focusOwner == this) {
    // return true;
    // }
    // focusOwner = focusOwner.getParent();
    // }
    // return false;
    // }

    protected transient CellEditorRemover editorRemover;

    /**
     * removes the standard editor remover and adds the custom remover.
     * 
     */
    private void hackEditorRemover() {
        KeyboardFocusManager manager = KeyboardFocusManager
                .getCurrentKeyboardFocusManager();
        PropertyChangeListener[] listeners = manager
                .getPropertyChangeListeners("permanentFocusOwner");
        for (int i = listeners.length - 1; i >= 0; i--) {
            if (listeners[i].getClass().getName().startsWith(
                    "javax.swing.JTable")) {
                manager.removePropertyChangeListener("permanentFocusOwner",
                        listeners[i]);
                break;
            }
        }
        if (editorRemover == null) {
            editorRemover = new CellEditorRemover();
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * 
     * Overridden to uninstall the custom editor remover.
     */
    @Override
    public void removeNotify() {
        if (editorRemover != null) {
            editorRemover.uninstall();
            editorRemover = null;
        }
        super.removeNotify();
    }

    /**
     * {@inheritDoc}
     * <p>
     * 
     * Overridden to prevent spurious focus loss to outside of table while
     * removing the editor. This is essentially a hack around core bug #6210779.
     * 
     * PENDING: add link to wiki!
     */
    @Override
    public boolean isFocusCycleRoot() {
        if (isEditingFocusCycleRoot()) {
            return true;
        }
        return super.isFocusCycleRoot();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Overridden to try to stop the edit, if appropriate. Calls super if
     * succeeded, does not yield otherwise.
     * 
     */
    @Override
    public void transferFocus() {
        if (isEditingFocusCycleRoot() && !getCellEditor().stopCellEditing())
            return;
        super.transferFocus();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Overridden to try to stop the edit, if appropiate. Calls super if
     * succeeded, does not yield otherwise.
     * 
     */
    @Override
    public void transferFocusBackward() {
        if (isEditingFocusCycleRoot() && !getCellEditor().stopCellEditing())
            return;
        super.transferFocusBackward();
    }

    /**
     * 
     * @return a boolean to indicate whether the table needs to fake being focus
     *         cycle root.
     */
    private boolean isEditingFocusCycleRoot() {
        return isEditing() && isTerminateEditOnFocusLost();
    }

    /**
     * This class tracks changes in the keyboard focus state. It is used when
     * the JTable is editing to determine when to cancel the edit. If focus
     * switches to a component outside of the jtable, but in the same window,
     * this will cancel editing.
     */
    class CellEditorRemover implements PropertyChangeListener {
        KeyboardFocusManager focusManager;

        public CellEditorRemover() {
            install();
        }

        private void install() {
            focusManager = KeyboardFocusManager
                    .getCurrentKeyboardFocusManager();
            focusManager.addPropertyChangeListener("permanentFocusOwner", this);
            focusManager.addPropertyChangeListener("managingFocus", this);
        }

        /**
         * remove all listener registrations.
         * 
         */
        public void uninstall() {
            focusManager.removePropertyChangeListener("permanentFocusOwner",
                    this);
            focusManager.removePropertyChangeListener("managingFocus", this);
            focusManager = null;
        }

        public void propertyChange(PropertyChangeEvent ev) {
            if (ev == null)
                return;
            if ("permanentFocusOwner".equals(ev.getPropertyName())) {
                permanentFocusOwnerChange();
            } else if ("managingFocus".equals(ev.getPropertyName())) {
                // TODO uninstall/install after manager changed.
            }
        }

        /**
         * 
         */
        private void permanentFocusOwnerChange() {
            if (!isEditing() || !isTerminateEditOnFocusLost()) {
                return;
            }

            Component c = focusManager.getPermanentFocusOwner();
            while (c != null) {
                // PENDING JW: logic untested!
                if (c instanceof JPopupMenu) {
                    c = ((JPopupMenu) c).getInvoker();
                } else {
                    if (c == JXTable.this) {
                        // focus remains inside the table
                        return;
                    } else if (c instanceof JPopupMenu) {
                        // PENDING JW: left-over? we should never reach this ...
                        // need to switch the hierarchy to a popups invoker
                    } else if ((c instanceof Window)
                            || (c instanceof Applet && c.getParent() == null)) {
                        if (c == SwingUtilities.getRoot(JXTable.this)) {
                            if (!getCellEditor().stopCellEditing()) {
                                getCellEditor().cancelCellEditing();
                            }
                        }
                        break;
                    }
                    c = c.getParent();
                }
            }
        }
    }

    // ---------------------------- updateUI support

    /**
     * {@inheritDoc}
     * <p>
     * Additionally updates auto-adjusted row height and highlighters.
     * <p>
     * Another of the override motivation is to fix core issue (?? ID): super
     * fails to update <b>all</b> renderers/editors.
     */
    @Override
    public void updateUI() {
        super.updateUI();
        updateColumnControlUI();
        for (Enumeration<?> defaultEditors = defaultEditorsByColumnClass
                .elements(); defaultEditors.hasMoreElements();) {
            updateEditorUI(defaultEditors.nextElement());
        }

        for (Enumeration<?> defaultRenderers = defaultRenderersByColumnClass
                .elements(); defaultRenderers.hasMoreElements();) {
            updateRendererUI(defaultRenderers.nextElement());
        }
        for (TableColumn column : getColumns(true)) {
            updateColumnUI(column);
        }
        updateRowHeightUI(true);
        updateHighlighterUI();
    }

    /**
     * Updates the ui of the columnControl if appropriate.
     */
    protected void updateColumnControlUI() {
        if ((columnControlButton != null)
                && (columnControlButton.getParent() == null)) {
            SwingUtilities.updateComponentTreeUI(columnControlButton);
        }
    }

    /**
     * Tries its best to <code>updateUI</code> of the potential
     * <code>TableCellEditor</code>.
     * 
     * @param maybeEditor the potential editor.
     */
    private void updateEditorUI(Object maybeEditor) {
        // maybe null or proxyValue
        if (!(maybeEditor instanceof TableCellEditor))
            return;
        // super handled this
        if ((maybeEditor instanceof JComponent)
                || (maybeEditor instanceof DefaultCellEditor))
            return;
        // custom editors might balk about fake rows/columns
        try {
            Component comp = ((TableCellEditor) maybeEditor)
                    .getTableCellEditorComponent(this, null, false, -1, -1);
            if (comp != null) {
                SwingUtilities.updateComponentTreeUI(comp);
            }
        } catch (Exception e) {
            // ignore - can't do anything
        }
    }

    /**
     * Tries its best to <code>updateUI</code> of the potential
     * <code>TableCellRenderer</code>.
     * 
     * @param maybeRenderer the potential renderer.
     */
    private void updateRendererUI(Object maybeRenderer) {
        // maybe null or proxyValue
        if (!(maybeRenderer instanceof TableCellRenderer))
            return;
        // super handled this
        if (maybeRenderer instanceof JComponent)
            return;
        Component comp = null;
        if (maybeRenderer instanceof AbstractRenderer) {
            comp = ((AbstractRenderer) maybeRenderer).getComponentProvider()
                    .getRendererComponent(null);
        } else {
            try {
                // custom editors might balk about fake rows/columns
                comp = ((TableCellRenderer) maybeRenderer)
                        .getTableCellRendererComponent(this, null, false,
                                false, -1, -1);

            } catch (Exception e) {
                // can't do anything - renderer can't cope with off-range cells
            }
        }
        if (comp != null) {
            SwingUtilities.updateComponentTreeUI(comp);
        }
    }

    /**
     * Updates TableColumn after updateUI changes. This implementation delegates
     * to the column if it is of type UIDependent, takes over to try an update
     * of the column's cellEditor, Cell-/HeaderRenderer otherwise.
     * 
     * @param column the tableColumn to update.
     */
    protected void updateColumnUI(TableColumn column) {
        if (column instanceof UIDependent) {
            ((UIDependent) column).updateUI();
        } else {
            updateEditorUI(column.getCellEditor());
            updateRendererUI(column.getCellRenderer());
            updateRendererUI(column.getHeaderRenderer());
        }
    }

    /**
     * Updates highlighter after <code>updateUI</code> changes.
     * 
     * @see org.jdesktop.swingx.decorator.UIDependent
     */
    protected void updateHighlighterUI() {
        if (compoundHighlighter == null)
            return;
        compoundHighlighter.updateUI();
    }

    /**
     * Auto-adjusts rowHeight to something more pleasing then the default. This
     * method is called after instantiation and after updating the UI. Does
     * nothing if the given parameter is <code>true</code> and the rowHeight had
     * been already set by client code. The underlying problem is that raw types
     * can't implement UIResource.
     * <p>
     * This implementation asks the UIManager for a default value (stored with
     * key "JXTable.rowHeight"). If none is available, calculates a "reasonable"
     * height from the table's fontMetrics, assuming that most renderers/editors
     * will have a border with top/bottom of 1.
     * <p>
     * 
     * @param respectRowSetFlag a boolean to indicate whether client-code flag
     *        should be respected.
     * @see #isXTableRowHeightSet
     */
    protected void updateRowHeightUI(boolean respectRowSetFlag) {
        if (respectRowSetFlag && isXTableRowHeightSet)
            return;
        int uiHeight = UIManager.getInt(UIPREFIX + "rowHeight");
        if (uiHeight > 0) {
            setRowHeight(uiHeight);
        } else {
            int fontBasedHeight = getFontMetrics(getFont()).getHeight() + 2;
            int magicMinimum = 18;
            setRowHeight(Math.max(fontBasedHeight, magicMinimum));
        }
        isXTableRowHeightSet = false;
    }

    /**
     * Convenience to set both grid line visibility and default margin for
     * horizontal/vertical lines. The margin defaults to 1 or 0 if the grid
     * lines are drawn or not drawn.
     * <p>
     * 
     * @param showHorizontalLines boolean to decide whether to draw horizontal
     *        grid lines.
     * @param showVerticalLines boolean to decide whether to draw vertical grid
     *        lines.
     * @see javax.swing.JTable#setShowGrid(boolean)
     * @see javax.swing.JTable#setIntercellSpacing(Dimension)
     */
    public void setShowGrid(boolean showHorizontalLines,
            boolean showVerticalLines) {
        int defaultRowMargin = showHorizontalLines ? 1 : 0;
        setRowMargin(defaultRowMargin);
        setShowHorizontalLines(showHorizontalLines);
        int defaultColumnMargin = showVerticalLines ? 1 : 0;
        setColumnMargin(defaultColumnMargin);
        setShowVerticalLines(showVerticalLines);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Behaves exactly like super.
     * <p>
     * It's overridden to warn against a frequent programming error: this method
     * toggles only the <b>visibility</b> of the grid lines, it <b>does not</b>
     * update the row/column margins - which may lead to visual artefacts, as
     * f.i. not showing the lines at all or showing normal table background in
     * selected state where the lines should have been.
     * 
     * @see #setShowGrid(boolean, boolean)
     */
    @Override
    public void setShowGrid(boolean showGrid) {
        super.setShowGrid(showGrid);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Overriden to keep view/model coordinates of SizeSequence in synch. Marks
     * the request as client-code induced.
     * 
     * @see #isXTableRowHeightSet
     */
    @Override
    public void setRowHeight(int rowHeight) {
        super.setRowHeight(rowHeight);
        if (rowHeight > 0) {
            isXTableRowHeightSet = true;
        }
        updateViewSizeSequence();

    }

    /**
     * {@inheritDoc}
     * <p>
     * Does nothing if support of individual rowHeights is not enabled.
     * Overriden to keep view/model coordinates of SizeSequence in synch.
     * 
     * @see #isRowHeightEnabled()
     */
    @Override
    public void setRowHeight(int row, int rowHeight) {
        if (!isRowHeightEnabled())
            return;
        super.setRowHeight(row, rowHeight);
        updateViewSizeSequence();
        resizeAndRepaint();
    }

    /**
     * Sets enablement of individual rowHeight support. Enabling the support
     * involves reflective access to super's private field rowModel which may
     * fail due to security issues. If failing the support is not enabled.
     * <p>
     * The default value is <code>false</code>.
     * 
     * @param enabled a boolean to indicate whether per-row heights should be
     *        enabled.
     * @see #isRowHeightEnabled()
     * @see #setRowHeight(int, int)
     */
    public void setRowHeightEnabled(boolean enabled) {
        // PENDING: should we throw an Exception if the enabled fails?
        // Or silently fail - depends on runtime context,
        // can't do anything about it.
        boolean old = isRowHeightEnabled();
        if (old == enabled)
            return;
        if (enabled && !canEnableRowHeight())
            return;
        rowHeightEnabled = enabled;
        if (!enabled) {
            adminSetRowHeight(getRowHeight());
        }
        firePropertyChange("rowHeightEnabled", old, rowHeightEnabled);
    }

    /**
     * Returns a boolean to indicate whether individual row height is enabled.
     * 
     * @return a boolean to indicate whether individual row height support is
     *         enabled.
     * @see #setRowHeightEnabled(boolean)
     * @see #setRowHeight(int, int)
     */
    public boolean isRowHeightEnabled() {
        return rowHeightEnabled;
    }

    /**
     * Returns if it's possible to enable individual row height support.
     * 
     * @return a boolean to indicate whether access of super's private
     *         <code>rowModel</code> is allowed.
     */
    private boolean canEnableRowHeight() {
        return getRowModelField() != null;
    }

    /**
     * Returns super's private <code>rowModel</code> which holds the individual
     * rowHeights. This method will return <code>null</code> if the access
     * failed, f.i. in sandbox restricted applications.
     * 
     * @return super's rowModel field or null if the access was not successful.
     */
    private SizeSequence getSuperRowModel() {
        try {
            Field field = getRowModelField();
            if (field != null) {
                return (SizeSequence) field.get(this);
            }
        } catch (SecurityException e) {
            LOG.fine("cannot use reflection "
                    + " - expected behaviour in sandbox");
        } catch (IllegalArgumentException e) {
            LOG
                    .fine("problem while accessing super's private field - private api changed?");
        } catch (IllegalAccessException e) {
            LOG
                    .fine("cannot access private field "
                            + " - expected behaviour in sandbox. "
                            + "Could be program logic running wild in unrestricted contexts");
        }
        return null;
    }

    /**
     * Sets super's private <code>rowModel</code> which holds the individual
     * rowHeights. This method will do nothing if the access failed, f.i. in
     * sandbox restricted applications.
     * 
     * @param rowModel the SizeSequence to set super's rowModel to.
     */
    private void setSuperRowModel(SizeSequence rowModel) {
        try {
            Field field = getRowModelField();
            if (field != null) {
                field.set(this, rowModel);
            }
        } catch (SecurityException e) {
            LOG.fine("cannot use reflection "
                    + " - expected behaviour in sandbox");
        } catch (IllegalArgumentException e) {
            LOG
                    .fine("problem while accessing super's private field - private api changed?");
        } catch (IllegalAccessException e) {
            LOG
                    .fine("cannot access private field "
                            + " - expected behaviour in sandbox. "
                            + "Could be program logic running wild in unrestricted contexts");
        }
    }

    /**
     * Returns super's private field which holds the individual rowHeights. This
     * method will return <code>null</code> if the access failed, f.i. in
     * sandbox restricted applications.
     * 
     * @return the super's field with access allowed or null if an Exception
     *         caught while trying to access.
     */
    private Field getRowModelField() {
        if (rowModelField == null) {
            try {
                rowModelField = JTable.class.getDeclaredField("rowModel");
                rowModelField.setAccessible(true);
            } catch (SecurityException e) {
                rowModelField = null;
                LOG.fine("cannot access JTable private field rowModel "
                        + "- expected behaviour in sandbox");
            } catch (NoSuchFieldException e) {
                LOG.fine("problem while accessing super's private field"
                        + " - private api changed?");
            }
        }
        return rowModelField;
    }

    /**
     * Returns the mapper used synch individual rowHeights in view/model
     * coordinates.
     * 
     * @return the <code>SizeSequenceMapper</code> used to synch view/model
     *         coordinates for individual row heights
     * @see org.jdesktop.swingx.decorator.SizeSequenceMapper
     */
    protected SizeSequenceMapper getRowModelMapper() {
        if (rowModelMapper == null) {
            rowModelMapper = new SizeSequenceMapper(filters);
        }
        return rowModelMapper;
    }

    /**
     * Sets the rowHeight for all rows to the given value. Keeps the flag
     * <code>isXTableRowHeight</code> unchanged. This enables the distinction
     * between setting the height for internal reasons from doing so by client
     * code.
     * 
     * @param rowHeight new height in pixel.
     * @see #setRowHeight(int)
     * @see #isXTableRowHeightSet
     */
    protected void adminSetRowHeight(int rowHeight) {
        boolean heightSet = isXTableRowHeightSet;
        setRowHeight(rowHeight);
        isXTableRowHeightSet = heightSet;
    }

    // ---------------------------- overriding super factory methods and buggy
    /**
     * {@inheritDoc}
     * <p>
     * Overridden to work around core Bug (ID #6291631): negative y is mapped to
     * row 0).
     * 
     */
    @Override
    public int rowAtPoint(Point point) {
        if (point.y < 0)
            return -1;
        return super.rowAtPoint(point);
    }

    /**
     * 
     * {@inheritDoc}
     * <p>
     * 
     * Overridden to return a <code>JXTableHeader</code>.
     * 
     * @see JXTableHeader
     */
    @Override
    protected JTableHeader createDefaultTableHeader() {
        return new JXTableHeader(columnModel);
    }

    /**
     * 
     * {@inheritDoc}
     * <p>
     * 
     * Overridden to return a <code>DefaultTableColumnModelExt</code>.
     * 
     * @see org.jdesktop.swingx.table.DefaultTableColumnModelExt
     */
    @Override
    protected TableColumnModel createDefaultColumnModel() {
        return new DefaultTableColumnModelExt();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Overridden because super throws NPE on null param.
     */
    @Override
    public void setSelectionBackground(Color selectionBackground) {
        Color old = getSelectionBackground();
        this.selectionBackground = selectionBackground;
        firePropertyChange("selectionBackground", old, getSelectionBackground());
        repaint();
        // super.setSelectionBackground(selectionBackground);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Overridden because super throws NPE on null param.
     */
    @Override
    public void setSelectionForeground(Color selectionForeground) {
        Color old = getSelectionForeground();
        this.selectionForeground = selectionForeground;
        firePropertyChange("selectionForeground", old, getSelectionForeground());
        repaint();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Overridden because super throws NPE on null param.
     */
    @Override
    public void setGridColor(Color gridColor) {
        Color old = getGridColor();
        this.gridColor = gridColor;
        firePropertyChange("gridColor", old, getGridColor());
        repaint();
    }

}