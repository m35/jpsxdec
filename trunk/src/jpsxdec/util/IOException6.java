/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package jpsxdec.util;

import java.io.IOException;


public class IOException6 extends IOException {

    public IOException6(Throwable cause) {
        super();
        initCause(cause);
    }

    public IOException6(String message, Throwable cause) {
        super(message);
        initCause(cause);
    }

    public IOException6(String message) {
        super(message);
    }

    public IOException6() {
    }

}
