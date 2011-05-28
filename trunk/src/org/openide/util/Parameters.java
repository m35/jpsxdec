/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2010 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Contributor(s):
 *
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
 * Microsystems, Inc. All Rights Reserved.
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 */

package org.openide.util;

/**
 * Utilities for checking the values of method parameters.
 *
 * Methods in this class generally take the name of
 * the parameter to check and its value and throw exceptions
 * with messages according to the method name or just return. For example,
 * if you have a <code>myMethod()</code> method taking a <code>myParam</code>
 * parameter whose value must be a Java identifier, you usually check that
 * by doing:
 *
 * <pre>
 * public void myMethod(String myParam) {
 *     if (!Utilities.isJavaIdentifier(myParam)) {
 *         throw new IllegalArgumentException("The myParam parameter is not a valid Java identifier");
 *     }
 * }
 * </pre>
 *
 * Using this class you can do the same in a simpler way:
 *
 * <pre>
 * public void myMethod(String myParam) {
 *     Parameters.javaIdentifier("myParam", myParam);
 * }
 * </pre>
 *
 * @author Andrei Badea
 * @since org.openide.util 7.6
 */
public class Parameters {

    private Parameters() {}

    /**
     * Asserts the parameter value is not <code>null</code>.
     *
     * @param  name the parameter name.
     * @param  value the parameter value.
     * @throws NullPointerException if the parameter value is <code>null</code>.
     */
    public static void notNull(CharSequence name, Object value) {
        if (value == null) {
            throw new NullPointerException("The " + name + " parameter cannot be null"); // NOI18N
        }
    }

}
