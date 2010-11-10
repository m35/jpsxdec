/*
 * $Id: FilterPipeline.java,v 1.26 2009/05/25 01:52:13 kschaefe Exp $
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

package org.jdesktop.swingx.decorator;


import java.util.BitSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Vector;

import javax.swing.event.EventListenerList;

/**
 * <p>A <b><code>FilterPipeline</code></b> is used to define the set of
 * {@link org.jdesktop.swingx.decorator.Filter filters}
 * for a data-aware component such as a {@link org.jdesktop.swingx.JXList} or a
 * {@link org.jdesktop.swingx.JXTable}. Filtering involves interposing one or
 * more filters in a {@link org.jdesktop.swingx.decorator.FilterPipeline} between
 * a data model and a view to change the apparent order and/or number of records
 * in the data model. The order of filters in the filter pipeline determines the
 * order in which each filter is applied. The output from one filter in the
 * pipeline is piped as the input to the next filter in the pipeline.</p>
 *
 * <pre>
 *  {@link org.jdesktop.swingx.decorator.Filter}[]   filters = new {@link org.jdesktop.swingx.decorator.Filter}[] {
 *      new {@link org.jdesktop.swingx.decorator.PatternFilter}("S.*", 0, 1),    // regex, matchflags, column
 *      new {@link org.jdesktop.swingx.decorator.ShuttleSorter}(1, false),   // column 1, descending
 *      new {@link org.jdesktop.swingx.decorator.ShuttleSorter}(0, true),    // column 0, ascending
 *  };
 *  {@link org.jdesktop.swingx.decorator.FilterPipeline} pipeline = new {@link org.jdesktop.swingx.decorator.FilterPipeline}(filters);
 *  {@link org.jdesktop.swingx.JXTable}  table = new {@link org.jdesktop.swingx.JXTable}(model);
 *  table.setFilters(pipeline);
 * </pre>
 *
 * This is all you need to do in order to use <code>FilterPipeline</code>. Most
 * of the methods in this class are only for advanced developers who want to write
 * their own filter subclasses and want to override the way a filter pipeline works.
 *
 * @author Ramesh Gupta
 * @see org.jdesktop.swingx.decorator.Filter
 */
public class FilterPipeline {
    protected EventListenerList     listenerList = new EventListenerList();
    private ComponentAdapter    adapter = null;
    private Sorter                  sorter = null;
    private final Filter[]          filters;
    private SortController sortController;

    /**
     * Creates an empty open pipeline.
     *
     */
    public FilterPipeline() {
        this(new Filter[] {});
    }
    /**
     * Constructs a new <code>FilterPipeline</code> populated with the specified
     * filters that are applied in the order they appear in the list. Since filters
     * maintain state about the view to which they are attached, an instance of
     * a filter may not ever be used in more than one pipeline.
     *
     * @param inList array of filters
     */
    public FilterPipeline(Filter... inList) {
        filters = reorderSorters(inList, locateSorters(inList));
        assignFilters();
    }

    /*
     * JW let each contained filter assign both order and pipeline.
     * Now we have a invariant 
     * 
     * (containedFilter.order >= 0) && (containedFilter.pipeline != null)
     * 
     * which simplifies access logic (IMO)
     *
     */
    private void assignFilters() {
        for (int i = 0; i < filters.length; i++) {
            // JW: changed to bind early and move 
            // binding responsibility to filter
            // instead of fiddling around in other's bowels
            filters[i].assign(this, i);
        }
    }

    /**
     * Sets the sorter that the output of the filter pipeline is piped through.
     * This is the sorter that is installed interactively on a view by a user
     * action. 
     *
     * This method is responsible for doing all the bookkeeping to assign/cleanup
     * pipeline/adapter assignments. 
     * 
     * @param sorter the interactive sorter, if any; null otherwise.
     */
    protected void setSorter(Sorter sorter) {
        Sorter oldSorter = getSorter();
        if (oldSorter == sorter) return;
        if (oldSorter != null) {
            oldSorter.assign((FilterPipeline) null);
        }
        this.sorter = sorter;
        if (sorter != null) {
            sorter.assign((FilterPipeline) null);
            sorter.assign(this);
            if (adapter != null) {
                sorter.assign(adapter);
                sorter.refresh();
            }
        } 
        if ((sorter == null) && isAssigned()) { 
            fireContentsChanged();
        }
    }

    /**
     * Returns the sorter that the output of the filter pipeline is piped through.
     * This is the sorter that is installed interactively on a view by a user
     * action. 
     * 
     * @return the interactive sorter, if any; null otherwise.
     */
    protected Sorter getSorter() {
        return sorter;
    }
    
    public SortController getSortController() {
        if (sortController == null) {
            sortController = createDefaultSortController();
        }
        return sortController;
    }
    
    protected SortController createDefaultSortController() {
        return new SorterBasedSortController();
    }
    
    protected class SorterBasedSortController implements SortController {
        public void toggleSortOrder(int column) {
            toggleSortOrder(column, null);
        }

        
        public void toggleSortOrder(int column, Comparator comparator) {
            Sorter currentSorter = getSorter();
            if ((currentSorter != null) && (currentSorter.getColumnIndex() == column)) {
               // JW: think about logic - need to update comparator?
                currentSorter.toggle(); 
            } else {
               setSorter(createDefaultSorter(new SortKey(SortOrder.ASCENDING, column, comparator))); 
            }
       }


        public void setSortKeys(List<? extends SortKey> keys) {
            if ((keys == null) || keys.isEmpty()) {
                setSorter(null);
                return;
            }
            SortKey sortKey = SortKey.getFirstSortingKey(keys);
            // only crappy unsorted...
            if (sortKey == null) return;
            Sorter sorter = getSorter();
            if (sorter == null) {
                sorter = createDefaultSorter();
            }
            sorter.setSortKey(sortKey);
            // technically, we could re-use the sorter
            // and only reset column, comparator and direction
            // need to detangle from TableColumn before going there...
            // so for now we only change the order if we have a sorter
            // for the given column, create a new default sorter if not
//            if ((currentSorter == null) || 
//                    (currentSorter.getColumnIndex() != sortKey.getColumn())) {
//                currentSorter = createDefaultSorter(sortKey);
//            }
//            if (currentSorter.isAscending() != sortKey.getSortOrder().isAscending()) {
//                currentSorter.setAscending(sortKey.getSortOrder().isAscending());
//            }
            setSorter(sorter);
            
        }
        
        /**
         * creates a Sorter initialized with sortKey
         * @param sortKey the properties to use
         * @return <code>Sorter</code> initialized with the specified <code>sortKey</code>
         */
        protected Sorter createDefaultSorter(SortKey sortKey) {
            Sorter sorter = createDefaultSorter();
            sorter.setSortKey(sortKey);
            return sorter;
        }


        protected Sorter createDefaultSorter() {
            return new ShuttleSorter();
        }
        
        @SuppressWarnings("unchecked")
        public List<? extends SortKey> getSortKeys() {
            Sorter sorter = getSorter();
            if (sorter == null) {
                return Collections.EMPTY_LIST;
            }
            return Collections.singletonList(sorter.getSortKey());
        }


        public SortOrder getSortOrder(int column) {
            Sorter sorter = getSorter();
            if ((sorter == null) || (sorter.getColumnIndex() != column)) {
                return SortOrder.UNSORTED;
            }
            return sorter.getSortOrder();
        }
        
    }
    /**
     * Assigns a {@link org.jdesktop.swingx.decorator.ComponentAdapter} to this
     * pipeline if no adapter has previously been assigned to the pipeline. Once an
     * adapter has been assigned to this pipeline, any attempt to change that will
     * cause an exception to be thrown.
     *
     * @param adapter the <code>ComponentAdapter</code> to assign
     * @throws IllegalArgumentException if adapter is null
     * @throws IllegalStateException if an adapter is already assigned to this
     * pipeline and the new adapter is not the same the existing adapter
     */
     public final void assign(ComponentAdapter adapter) {
        if (adapter == null) {
            throw new IllegalArgumentException("null adapter");
        }

    // also assign individual filters when adapter is bound
        if (this.adapter == null) {
            this.adapter = adapter;
            for (int i = 0; i < filters.length; i++) {
                filters[i].assign(adapter);
            }
            if (sorter != null) {
                sorter.assign(adapter);
            }
            flush();
        }
        else if (this.adapter != adapter){
            throw new IllegalStateException("Can't bind to a different adapter");
        }
    }

     /**
      * 
      * @return true if an adapter has been assigned, false otherwise
      */
     public boolean isAssigned() {
         return adapter != null;
     }

    /**
     * Returns true if this pipeline contains the specified filter;
     * otherwise it returns false.
     *
     * @param filter filter whose membership in this pipeline is tested
     * @return true if this pipeline contains the specified filter;
     * otherwise it returns false
     * @throws NullPointerException if filter == null
     * 
     */
    boolean contains(Filter filter) {
        return (filter.equals(sorter)) || 
            (filter.order >= 0) &&
                (filters.length > 0) && 
                (filters[filter.order] == filter);
    }

    /**
     * Returns the first filter, if any, in this pipeline, or null, if there are
     * no filters in this pipeline.
     *
     * @return the first filter, if any, in this pipeline, or null, if there are
     * no filters in this pipeline
     */
    Filter first() {
        return (filters.length > 0) ? filters[0] : null;
    }

    /**
     * Returns the last filter, if any, in this pipeline, or null, if there are
     * no filters in this pipeline.
     *
     * @return the last filter, if any, in this pipeline, or null, if there are
     * no filters in this pipeline
     */
    Filter last() {
        if (sorter != null) return sorter;
        return (filters.length > 0) ? filters[filters.length - 1] : null;
    }

    /**
     * Returns the filter after the supplied filter in this pipeline, or null,
     * if there aren't any filters after the supplied filter.
     * 
     * @param filter a filter in this pipeline
     * @return the filter after the supplied filter in this pipeline, or null,
     *         if there aren't any filters after the supplied filter
     */
    Filter next(Filter filter) {
        if (last().equals(filter))
            return null;
        return filter.order + 1 < filters.length ? filters[filter.order + 1]
                : getSorter();
    }

    /**
     * Returns the filter before the supplied filter in this pipeline,
     * or null, if there aren't any filters before the supplied filter.
     *
     * @param filter a filter in this pipeline
     * @return the filter before the supplied filter in this pipeline,
     * or null, if there aren't any filters before the supplied filter
     */
    Filter previous(Filter filter) {
        if (filter.equals(sorter)) return filters.length > 0 ? filters[filters.length - 1] : null;
        return first().equals(filter) ? null : filters[filter.order - 1];
    }

    /**
     * Called when the specified filter has changed.
     * Cascades <b><code>filterChanged</code></b> notifications to the next
     * filter in the pipeline after the specified filter. If the specified filter
     * is the last filter in the pipeline, this method broadcasts a
     * <b><code>filterChanged</code></b> notification to all
     * <b><code>PipelineListener</code></b> objects registered with this pipeline.
     *
     * @param filter a filter in this pipeline that has changed in any way
     */
    protected void filterChanged(Filter filter) {
        // JW: quick partial fix for #370-swingx: don't
        // fire if there are no active filters/and no sorter.
        // can't do anything if we have filters/sorters
        // because we have no notion to turn "auto-flush on model update" off 
        // (Mustang does - and has it off by default)
        // JW: reverted - wrong place, depends on a specific 
        // sortController implementation and "ripples" 
        // (JXList selection not correctly updated)
//        if ((filter instanceof IdentityFilter) && (getSorter() == null)) return;
        Filter  next = next(filter); 
        if (next == null) {
            // prepared for additional event type
//            if (filter == getSorter()) {
//                fireSortOrderChanged();
//            }
            fireContentsChanged();
        }
        else {
            next.refresh(); // Cascade to next filter
        }
    }


    /**
     * returns the unfiltered data adapter size or 0 if unassigned.
     * 
     * @return the unfiltered data adapter size or 0 if unassigned
     */
    public int getInputSize() {
        return isAssigned() ? adapter.getRowCount() : 0;
    }

    /**
     * @param filter
     * @return returns the unfiltered data adapter size or 0 if unassigned.
     */
    int getInputSize(Filter filter) {
        Filter  previous = previous(filter);
        if (previous != null) {
            return previous.getSize();
        }
        // fixed issue #64-swingx - removed precondition... (was: isAssigned())
        return getInputSize();
    }

    /**
     * Returns the number of records in the filtered view.
     *
     * @return the number of records in the filtered view
     */
    public int getOutputSize() {
        // JW: don't need to check - but that's heavily dependent on the
        // implementation detail that there's always the identityFilter
        // (which might change any time)
        if (!isAssigned()) return 0;
        Filter last = last();
        return (last == null) ? adapter.getRowCount() : last.getSize();
    }

    /**
     * Convert row index from view coordinates to model coordinates
     * accounting for the presence of sorters and filters. This is essentially
     * a pass-through to the {@link org.jdesktop.swingx.decorator.Filter#convertRowIndexToModel(int) convertRowIndexToModel}
     * method of the <em>last</em> {@link org.jdesktop.swingx.decorator.Filter},
     * if any, in this pipeline.
     *
     * @param row row index in view coordinates
     * @return row index in model coordinates
     */
    public int convertRowIndexToModel(int row) {
        Filter last = last();
        return (last == null) ? row : last.convertRowIndexToModel(row);
    }

    /**
     * Convert row index from model coordinates to view coordinates
     * accounting for the presence of sorters and filters. This is essentially
     * a pass-through to the {@link org.jdesktop.swingx.decorator.Filter#convertRowIndexToView(int) convertRowIndexToModel}
     * method of the <em>last</em> {@link org.jdesktop.swingx.decorator.Filter},
     * if any, in this pipeline.
     *
     * @param row row index in model coordinates
     * @return row index in view coordinates
     */
    public int convertRowIndexToView(int row) {
        Filter last = last();
        return (last == null) ? row : last.convertRowIndexToView(row);
    }

    /**
     * Returns the value of the cell at the specified coordinates.
     *
     * 
     * @param row in view coordinates
     * @param column in model coordinates
     * @return the value of the cell at the specified coordinates
     */
    public Object getValueAt(int row, int column) {
        // JW: this impl relies on the fact that there's always the
        // identity filter installed
        // should use adapter if assigned and no filter
        Filter last = last();
        return (last == null) ? null : last.getValueAt(row, column);
    }

    public void setValueAt(Object aValue, int row, int column) {
        // JW: this impl relies on the fact that there's always the
        // identity filter installed
        // should use adapter if assigned and no filter
        Filter last = last();
        if (last != null) {
            last.setValueAt(aValue, row, column);
        }
    }

    public boolean isCellEditable(int row, int column) {
        // JW: this impl relies on the fact that there's always the
        // identity filter installed
        // should use adapter if assigned and no filter
        Filter last = last();
        return (last == null) ? false : last.isCellEditable(row, column);
    }

    /**
     * Flushes the pipeline by initiating a {@link org.jdesktop.swingx.decorator.Filter#refresh() refresh}
     * on the <em>first</em> {@link org.jdesktop.swingx.decorator.Filter filter},
     * if any, in this pipeline. After that filter has refreshed itself, it sends a
     * {@link #filterChanged(org.jdesktop.swingx.decorator.Filter) filterChanged}
     * notification to this pipeline, and the pipeline responds by initiating a
     * {@link org.jdesktop.swingx.decorator.Filter#refresh() refresh}
     * on the <em>next</em> {@link org.jdesktop.swingx.decorator.Filter filter},
     * if any, in this pipeline. Eventualy, when there are no more filters left
     * in the pipeline, it broadcasts a {@link org.jdesktop.swingx.decorator.PipelineEvent}
     * signaling a {@link org.jdesktop.swingx.decorator.PipelineEvent#CONTENTS_CHANGED}
     * message to all {@link org.jdesktop.swingx.decorator.PipelineListener} objects
     * registered with this pipeline.
     */
    public void flush() {
        // JW PENDING: use first!
        if ((filters != null) && (filters.length > 0)) {
            filters[0].refresh();
        }
        else if (sorter != null) {
            sorter.refresh();
        }
    }

    /**
     * Adds a listener to the list that's notified each time there is a change
     * to this pipeline.
     *
     * @param l the <code>PipelineListener</code> to be added
     */
    public void addPipelineListener(PipelineListener l) {
        listenerList.add(PipelineListener.class, l);
    }

    /**
     * Removes a listener from the list that's notified each time there is a change
     * to this pipeline.
     *
     * @param l the <code>PipelineListener</code> to be removed
     */
    public void removePipelineListener(PipelineListener l) {
        listenerList.remove(PipelineListener.class, l);
    }

    /**
     * Returns an array of all the pipeline listeners
     * registered on this <code>FilterPipeline</code>.
     *
     * @return all of this pipeline's <code>PipelineListener</code>s,
     *         or an empty array if no pipeline listeners
     *         are currently registered
     *
     * @see #addPipelineListener
     * @see #removePipelineListener
     */
    public PipelineListener[] getPipelineListeners() {
        return (PipelineListener[]) listenerList.getListeners(
            PipelineListener.class);
    }

    /**
     * Notifies all registered {@link org.jdesktop.swingx.decorator.PipelineListener}
     * objects that the contents of this pipeline has changed. The event instance
     * is lazily created.
     */
    protected void fireContentsChanged() {
        Object[] listeners = listenerList.getListenerList();
        PipelineEvent e = null;

        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == PipelineListener.class) {
                if (e == null) {
                    e = new PipelineEvent(this, PipelineEvent.CONTENTS_CHANGED);
                }
                ( (PipelineListener) listeners[i + 1]).contentsChanged(e);
            }
        }
    }

    /**
     * Notifies all registered {@link org.jdesktop.swingx.decorator.PipelineListener}
     * objects that the contents of this pipeline has changed. 
     */
    protected void fireSortOrderChanged() {
        Object[] listeners = listenerList.getListenerList();
        PipelineEvent e = null;

        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == PipelineListener.class) {
                if (e == null) {
                    e = new PipelineEvent(this, PipelineEvent.SORT_ORDER_CHANGED);
                }
                ( (PipelineListener) listeners[i + 1]).contentsChanged(e);
            }
        }
    }
    private List locateSorters(Filter[] inList) {
        BitSet  sortableColumns = new BitSet(); // temporary structure for checking
        List<Integer>    sorterLocations = new Vector<Integer>();
        for (int i = 0; i < inList.length; i++) {
            if (inList[i] instanceof Sorter) {
                int columnIndex = inList[i].getColumnIndex();
                if (columnIndex < 0) {
                    throw new IndexOutOfBoundsException(
                        "Negative column index for filter: " + inList[i]);
                }

                if (sortableColumns.get(columnIndex)) {
                    throw new IllegalArgumentException(
                        "Filter "+ i +" attempting to overwrite sorter for column "
                        + columnIndex);
                }

                sortableColumns.set(columnIndex);       // mark column index
                sorterLocations.add(i);    // mark sorter index
                //columnSorterMap.put(new Integer(columnIndex), inList[i]);
            }
        }
        return sorterLocations;
    }

    private Filter[] reorderSorters(Filter[] inList, List sorterLocations) {
        // quick hack for issue #46-swingx: make sure we are open
        // without filter.
        if (inList.length == 0) {
            return new Filter[] {new IdentityFilter()};
        }
        // always returns a new copy of inList
        Filter[]    outList = (Filter[]) inList.clone();

        // Invert the order of sorters, if any, in outList
        int max = sorterLocations.size() - 1;
        for (int i = 0; i <= max; i++) {
            int orig = ((Integer) sorterLocations.get(max - i)).intValue();
            int copy = ((Integer) sorterLocations.get(i)).intValue();
            outList[copy] = inList[orig];
        }

        return outList;
    }

    public static class IdentityFilter extends Filter {
        
        
        /**
         * PENDING JW: fires always, even without sorter ..
         * Could do better - but will break behaviour of apps which relied on
         * the (buggy) side-effect of repainting on each change.
         * 
         */
        @Override
        public void refresh() {
//            if ((sortController == null) 
//                    || (sortController.getSortKeys().size() == 0)) return;
            super.refresh();
        }

        @Override
        protected void init() {

        }

        @Override
        protected void reset() {

        }

        @Override
        protected void filter() {

        }

        @Override
        public int getSize() {
            return this.getInputSize();
        }

        @Override
        protected int mapTowardModel(int row) {
            return row;
        }

        @Override
        protected int mapTowardView(int row) {
            return row;
        }
    }


}
