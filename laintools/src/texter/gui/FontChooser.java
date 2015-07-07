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

package texter.gui;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.DefaultListModel;
import javax.swing.JFrame;

public class FontChooser {

    public static String showFontNameChooser(JFrame parent) {
        final FontNameChooser frm = new FontNameChooser(parent);
        frm.setVisible(true);
        frm.dispose();
        return frm.getChosenFont();
    }
    
    
    private static class FontNameChooser extends javax.swing.JDialog {

        private javax.swing.JButton btnOk;
        private javax.swing.JButton btnClose;
        private javax.swing.JLabel lblPreview;
        private javax.swing.JList lstFonts;
        private javax.swing.JScrollPane lstFontsScroll;
        
        private String m_sFont = null;
        
        public String getChosenFont() {
            return m_sFont;
        }

        public FontNameChooser(JFrame parent) {
            super(parent, "Choose font", true);
            
            lstFontsScroll = new javax.swing.JScrollPane();
            lstFonts = new javax.swing.JList();
            btnClose = new javax.swing.JButton();
            lblPreview = new javax.swing.JLabel();
            btnOk = new javax.swing.JButton();

            setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
            getContentPane().setLayout(new java.awt.BorderLayout(10, 5));

            lstFontsScroll.setViewportView(lstFonts);

            getContentPane().add(lstFontsScroll, java.awt.BorderLayout.NORTH);

            lblPreview.setText("AaBbCcDdEeFf");
            getContentPane().add(lblPreview, java.awt.BorderLayout.CENTER);

            btnClose.setText("Cancel");
//            btnClose.setMinimumSize(new java.awt.Dimension(100, 23));
//            btnClose.setPreferredSize(new java.awt.Dimension(100, 23));
//            btnClose.setMaximumSize(new java.awt.Dimension(100, 23));
            getContentPane().add(btnClose, java.awt.BorderLayout.WEST);

            btnOk.setText("OK");
//            btnOk.setMinimumSize(new java.awt.Dimension(100, 23));
//            btnOk.setPreferredSize(new java.awt.Dimension(100, 23));
//            btnClose.setMaximumSize(new java.awt.Dimension(100, 23));
            getContentPane().add(btnOk, java.awt.BorderLayout.EAST);

            String[] fontNames = GraphicsEnvironment
                    .getLocalGraphicsEnvironment()
                    .getAvailableFontFamilyNames();

            DefaultListModel model = new DefaultListModel();
            for (String name : fontNames) {
                model.addElement(name);
            }
            lstFonts.setModel(model);
            lstFonts.setSelectedIndex(0);


            btnOk.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    Object o = lstFonts.getSelectedValue();
                    if (o != null) m_sFont = o.toString();
                    setVisible(false);
                }
            });        

            btnClose.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    setVisible(false);
                }
            });        

            lstFonts.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
                public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                    lblPreview.setFont(new Font(lstFonts.getSelectedValue().toString(), 0, 14));
                }
            });

            lstFonts.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2) {
                        Object o = lstFonts.getSelectedValue();
                        if (o != null) m_sFont = o.toString();
                        setVisible(false);
                    }
                }
            });
            
            this.pack();
            this.setLocationRelativeTo(parent);
            
        }



    }
}