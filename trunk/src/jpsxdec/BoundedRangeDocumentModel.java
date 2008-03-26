package jpsxdec;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BoundedRangeModel;
import javax.swing.DefaultBoundedRangeModel;
import javax.swing.InputVerifier;
import javax.swing.JComponent;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.UndoableEditListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.PlainDocument;
import javax.swing.text.Position;
import javax.swing.text.Segment;



public class BoundedRangeDocumentModel //extends InputVerifier 
        implements 
        Document, BoundedRangeModel, 
        DocumentListener, ChangeListener
{
    private PlainDocument doc = new PlainDocument();
    private DefaultBoundedRangeModel range = new DefaultBoundedRangeModel();

    public BoundedRangeDocumentModel() {
        doc.addDocumentListener(this);
        //range.addChangeListener(this);
        
    }

    // Document
    //<editor-fold defaultstate="collapsed" desc="Document">
    
    public int getLength() {
        return doc.getLength();
    }

    public void addDocumentListener(DocumentListener listener) {
        doc.addDocumentListener(listener);
    }

    public void removeDocumentListener(DocumentListener listener) {
        doc.removeDocumentListener(listener);
    }

    public void addUndoableEditListener(UndoableEditListener listener) {
        doc.addUndoableEditListener(listener);
    }

    public void removeUndoableEditListener(UndoableEditListener listener) {
        doc.removeUndoableEditListener(listener);
    }

    public Object getProperty(Object key) {
        return doc.getProperty(key);
    }

    public void putProperty(Object key, Object value) {
        doc.putProperty(key, value);
    }

    public void remove(int offs, int len) throws BadLocationException {
        doc.remove(offs, len);
    }

    public void insertString(int offset, String str, AttributeSet a) throws BadLocationException {
        if (!str.matches("\\d*")) return;
        doc.insertString(offset, str, a);
    }

    public String getText(int offset, int length) throws BadLocationException {
        return doc.getText(offset, length);
    }

    public void getText(int offset, int length, Segment txt) throws BadLocationException {
        doc.getText(offset, length, txt);
    }

    public Position getStartPosition() {
        return doc.getStartPosition();
    }

    public Position getEndPosition() {
        return doc.getEndPosition();
    }

    public Position createPosition(int offs) throws BadLocationException {
        return doc.createPosition(offs);
    }

    public Element[] getRootElements() {
        return doc.getRootElements();
    }

    public Element getDefaultRootElement() {
        return doc.getDefaultRootElement();
    }

    public void render(Runnable r) {
        doc.render(r);
    }
    //</editor-fold>
    

    // BoundedRangeModel
    //<editor-fold defaultstate="collapsed" desc="BoundedRangeModel">
    
    public int getMinimum() {
        return range.getMinimum();
    }

    public void setMinimum(int newMinimum) {
        range.setMinimum(newMinimum);
    }

    public int getMaximum() {
        return range.getMaximum();
    }

    public void setMaximum(int newMaximum) {
        range.setMaximum(newMaximum);
    }

    public int getValue() {
        return range.getValue();
    }

    boolean tmp = true;
    public void setValue(int newValue) {
        range.setValue(newValue);
        
        try {
            doc.replace(0, doc.getLength(), Integer.toString(range.getValue()), null);
        } catch (BadLocationException ex) {
            
        }
         
    }

    public void setValueIsAdjusting(boolean b) {
        range.setValueIsAdjusting(b);
    }

    public boolean getValueIsAdjusting() {
        return range.getValueIsAdjusting();
    }

    public int getExtent() {
        return range.getExtent();
    }

    public void setExtent(int newExtent) {
        range.setExtent(newExtent);
    }

    public void setRangeProperties(int value, int extent, int min, int max, boolean adjusting) {
        range.setRangeProperties(value, extent, min, max, adjusting);
    }

    public void addChangeListener(ChangeListener x) {
        range.addChangeListener(x);
    }

    public void removeChangeListener(ChangeListener x) {
        range.removeChangeListener(x);
    }
    //</editor-fold>
    
    // DocumentListener
    
    public void insertUpdate(DocumentEvent e) {
        updateRange();
    }

    public void removeUpdate(DocumentEvent e) {
        updateRange();
    }

    public void changedUpdate(DocumentEvent e) {
        updateRange();
    }
    
    private void updateRange() {
        try {
            String txt = doc.getText(0, doc.getLength());
            if (txt.length() == 0)
                range.setValue(0);
            else
                range.setValue(Integer.parseInt(txt));
        } catch (BadLocationException ex) {
            
        }
    }

    /////////////////////////////////////////////////////////////////////////////
    
    // InputVerifier
    
    //@Override
    public boolean verify(JComponent input) {
        JTextField tf = (JTextField) input;
        String txt = tf.getText();
        try {
            int i = Integer.parseInt(txt);
            if (i < range.getMinimum()) return false;
            if (i > range.getMaximum()) return false;
        } catch (NumberFormatException numberFormatException) {
            return false;
        }
        return true;
    }

    public void stateChanged(ChangeEvent e) {
        try {
            if (!range.getValueIsAdjusting()) {
                doc.replace(0, doc.getLength(), Integer.toString(range.getValue()), null);
            }
        } catch (BadLocationException ex) {
            
        }
 
    }

    
}
