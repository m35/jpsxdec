/*
 * $Id: DefaultSelectionMapper.java,v 1.5 2008/10/14 22:31:38 rah003 Exp $
 *
 * Copyright 2006 Sun Microsystems, Inc., 4150 Network Circle,
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



import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;

/**
 * Responsible for keeping track of selection in model coordinates.<p>
 *
 * updates view selection on pipeline change.
 * updates model selection on view selection change.
 *
 * @author Jeanette Winzenburg
 */
public class DefaultSelectionMapper implements SelectionMapper {

    /** selection in view coordinates. */
    private ListSelectionModel viewSelection;

    /** selection in model coordinates. */
    protected final DefaultListSelectionModel modelSelection = new DefaultListSelectionModel();

    /** mapping pipeline. */
    private FilterPipeline pipeline;

    /** listener to view selection. */
    private ListSelectionListener viewSelectionListener;

    /**
     * Whether selection mapping is enabled. If true, we're currently
     * observing the view selection, using that to keep the model selection
     * up-to-date.
     */
    private boolean enabled = false;

    /** listener to mapping pipeline. */
    private PipelineListener pipelineListener;

    /**
     * PRE: selection != null;
     *
     * @param pipeline
     * @param selection
     */
    public DefaultSelectionMapper(FilterPipeline pipeline, ListSelectionModel selection) {
        setViewSelectionModel(selection);
        setEnabled(true);
        setFilters(pipeline);
    }

    /** {@inheritDoc} */
    public void setViewSelectionModel(ListSelectionModel viewSelectionModel) {
        if(viewSelectionModel == null) throw new IllegalArgumentException();

        boolean wasEnabled = isEnabled();
        setEnabled(false);
        try {
            clearModelSelection();
            this.viewSelection = viewSelectionModel;
            mapTowardsModel();
        } finally {
            setEnabled(wasEnabled);
        }
    }

    /** {@inheritDoc} */
    public ListSelectionModel getViewSelectionModel() {
        return viewSelection;
    }

    public void setFilters(FilterPipeline pipeline) {
        FilterPipeline old = this.pipeline;
        if (old != null) {
            old.removePipelineListener(pipelineListener);
        }
        this.pipeline = pipeline;
        if (pipeline != null) {
            pipeline.addPipelineListener(getPipelineListener());
        }
        mapTowardsView();
    }


    /**
     * Populate view selection from model selection. This is used to keep the
     * view's logical selection in sync whenever the model changes due to
     * filtering or sorting.
     */
    protected void mapTowardsView() {
        if(!enabled) return;

        setEnabled(false);
        try {
            clearViewSelection();

            int[] selected = getSelectedRows(modelSelection);
            for (int i = 0; i < selected.length; i++) {
              int index = convertToView(selected[i]);
              // index might be -1, but then addSelectionInterval ignores it.
              viewSelection.addSelectionInterval(index, index);
            }
            int lead = modelSelection.getLeadSelectionIndex();
            // TODO: PENDING: JW - this is a quick hack for spurious AIOB - need to enquire why
            // they happen in the first place
            if (lead >= 0) {
                lead = convertToView(lead);
            }
            if (viewSelection instanceof DefaultListSelectionModel) {
                ((DefaultListSelectionModel) viewSelection).moveLeadSelectionIndex(lead);
            } else {
                // PENDING: not tested, don't have a non-DefaultXX handy
//                viewSelection.removeSelectionInterval(lead, lead);
//                viewSelection.addSelectionInterval(lead, lead);
            }
        } finally {
            setEnabled(true);
        }
    }

    /** {@inheritDoc} */
    public void setEnabled(boolean enabled) {
        if(enabled == this.enabled) return;
        this.enabled = enabled;

        if (enabled) {
            viewSelection.setValueIsAdjusting(false);
            viewSelection.addListSelectionListener(getViewSelectionListener());
        } else {
            viewSelection.removeListSelectionListener(viewSelectionListener);
            viewSelection.setValueIsAdjusting(true);
        }
    }

    /** {@inheritDoc} */
    public boolean isEnabled() {
        return enabled;
    }

    public void clearModelSelection() {
        if(modelSelection == null) return;
        // TODO: JW: need to reset anchor/lead?
        modelSelection.clearSelection();
        modelSelection.setAnchorSelectionIndex(-1);
        modelSelection.setLeadSelectionIndex(-1);
    }

    /**
     *
     */
    private void clearViewSelection() {
        // TODO: JW - hmm... clearSelection doesn't reset the lead/anchor. Why not?
        viewSelection.clearSelection();
        viewSelection.setAnchorSelectionIndex(-1);
        viewSelection.setLeadSelectionIndex(-1);
    }

    public void insertIndexInterval(int start, int length, boolean before) {
        modelSelection.insertIndexInterval(start, length, before);
    }

    public void removeIndexInterval(int start, int end) {
        modelSelection.removeIndexInterval(start, end);
    }

    /**
     * Populate view selection from model selection.
     */
    private void mapTowardsModel() {
        if(modelSelection == null) return;

        clearModelSelection();
        int[] selected = getSelectedRows(viewSelection);
        for (int i = 0; i < selected.length; i++) {
            int modelIndex = convertToModel(selected[i]);
            modelSelection.addSelectionInterval(modelIndex, modelIndex);
        }
        if (selected.length > 0) {
            // convert lead selection index to model coordinates
            modelSelection.moveLeadSelectionIndex(convertToModel(viewSelection.getLeadSelectionIndex()));
        }
    }

    private int convertToModel(int index) {
        // TODO: JW: check for valid index? must be < pipeline.getOutputSize()
        return (pipeline != null) && pipeline.isAssigned() ? pipeline.convertRowIndexToModel(index) : index;
    }

    private int convertToView(int index) {
        // TODO: JW: check for valid index? must be < pipeline.getInputSize()
        return (pipeline != null) && pipeline.isAssigned() ? pipeline.convertRowIndexToView(index) : index;
    }

    /**
     * Respond to a change in the view selection by updating the view selection.
     *
     * @param firstIndex the first view index that changed, inclusive
     * @param lastIndex the last view index that changed, inclusive
     */
    private void mapTowardsModel(int firstIndex, int lastIndex) {
        int safeFirstIndex = Math.max(0, firstIndex);
        // Fix for #855-swingx: JXList AIOOB on select after remove/add data items
        int safeLastIndex = getSafeLastIndex(lastIndex);
        for (int i = safeFirstIndex; i <= safeLastIndex; i++) {
            int modelIndex = convertToModel(i);
            if (viewSelection.isSelectedIndex(i)) {
                modelSelection.addSelectionInterval(modelIndex, modelIndex);
            } else {
                modelSelection.removeSelectionInterval(modelIndex, modelIndex);
            }
        }
        int lead = viewSelection.getLeadSelectionIndex();
        if (lead >= 0) {
            modelSelection.moveLeadSelectionIndex(convertToModel(lead));
        }

    }

    /**
     * @param lastIndex the view index to limit against the pipeline's output size
     * @return a valid view index (can be passed into convertToModel)
     */
    private int getSafeLastIndex(int lastIndex) {
        if ((pipeline == null) || !pipeline.isAssigned()) return lastIndex;
        // PENDING JW: negative? 
        return Math.min(lastIndex, pipeline.getOutputSize() - 1);
    }

    private int[] getSelectedRows(ListSelectionModel selection) {
        int iMin = selection.getMinSelectionIndex();
        int iMax = selection.getMaxSelectionIndex();

        if ((iMin == -1) || (iMax == -1)) {
            return new int[0];
        }

        int[] rvTmp = new int[1 + (iMax - iMin)];
        int n = 0;
        for (int i = iMin; i <= iMax; i++) {
            if (selection.isSelectedIndex(i)) {
                rvTmp[n++] = i;
            }
        }
        int[] rv = new int[n];
        System.arraycopy(rvTmp, 0, rv, 0, n);
        return rv;
    }

    /**
     * When the filter pipeline changes, update our view selection.
     */
    private PipelineListener getPipelineListener() {
        if (pipelineListener == null) {
            pipelineListener = new PipelineListener() {

                public void contentsChanged(PipelineEvent e) {
                    mapTowardsView();
                }

            };
        }
        return pipelineListener;
    }

    /**
     * When the view selection changes, update our model selection.
     */
    private ListSelectionListener getViewSelectionListener() {
        if (viewSelectionListener == null) {
            viewSelectionListener = new ListSelectionListener() {

                public void valueChanged(ListSelectionEvent e) {
                    if (e.getValueIsAdjusting()) return;
                    mapTowardsModel(e.getFirstIndex(), e.getLastIndex());
                }

            };
        }
        return viewSelectionListener;
    }

}
