/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.viewsupport;

import java.util.Set;

public interface IJavaProblemListener {
	
	void severitiesChanged(Set elements);
}