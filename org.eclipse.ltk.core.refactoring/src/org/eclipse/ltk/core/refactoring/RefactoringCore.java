/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ltk.core.refactoring;

import org.eclipse.ltk.internal.core.refactoring.RefactoringCorePreferences;
import org.eclipse.ltk.internal.core.refactoring.UndoManager;

/**
 * Central access point to access resources managed by the refactoring
 * core plug-in.
 * <p> 
 * Note: this class is not intended to be extended by clients.
 * </p>
 * 
 * @since 3.0
 */
public class RefactoringCore {

	private static IUndoManager fgUndoManager= null;

	private RefactoringCore() {
		// no instance
	}
	
	/**
	 * Returns the singleton undo manager for the refactoring undo
	 * stack.
	 * 
	 * @return the refactoring undo manager.
	 */
	public static IUndoManager getUndoManager() {
		if (fgUndoManager == null)
			fgUndoManager= createUndoManager();
		return fgUndoManager;
	}
	
	/**
	 * When condition checking is performed for a refactoring then the
	 * condition check is interpreted as failed if the refactoring status
	 * severity return from the condition checking operation is equal
	 * or greater than the value returned by this method. 
	 * 
	 * @return the condition checking failed severity
	 */
	public static int getConditionCheckingFailedSeverity() {
		return RefactoringCorePreferences.getStopSeverity();
	}
	
	/**
	 * Creates a new empty undo manager.
	 * 
	 * @return a new undo manager
	 */
	private static IUndoManager createUndoManager() {
		return new UndoManager();
	}
}
