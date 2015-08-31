/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.refactoring;

import junit.framework.Test;

import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;

public class InlineMethodTestSetup18 extends Java18Setup {

	private IPackageFragment fSimple18;

	public InlineMethodTestSetup18(Test test) {
		super(test);
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		IPackageFragmentRoot root= getDefaultSourceFolder();
		fSimple18= root.createPackageFragment("simple18_in", true, null);
	}

	public IPackageFragment getSimplePackage() {
		return fSimple18;
	}
}
