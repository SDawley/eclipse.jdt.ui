/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
// AW
package org.eclipse.jdt.internal.ui.actions;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.viewers.ISelectionProvider;

import org.eclipse.jdt.ui.IContextMenuConstants;


public class GenerateGroup extends ContextMenuGroup {

	public static final String GROUP_NAME= IContextMenuConstants.GROUP_GENERATE;
	
	private AddUnimplementedMethodsAction fAddUnimplementedMethods;
	private AddGetterSetterAction fAddGetterSetter;
	private AddJavaDocStubAction fAddJavaDocStub;
	
	public void fill(IMenuManager manager, GroupContext context) {

		createActions(context.getSelectionProvider());

		if (fAddUnimplementedMethods.canActionBeAdded())
			manager.appendToGroup(GROUP_NAME, fAddUnimplementedMethods);
		
		if (fAddGetterSetter.canActionBeAdded())
			manager.appendToGroup(GROUP_NAME, fAddGetterSetter);
			
		if (fAddJavaDocStub.canActionBeAdded())
			manager.appendToGroup(GROUP_NAME, fAddJavaDocStub);
		
	}
	
	private void createActions(ISelectionProvider provider) {
		if (fAddUnimplementedMethods == null) {
			fAddUnimplementedMethods= new AddUnimplementedMethodsAction(provider);
			fAddGetterSetter= new AddGetterSetterAction(provider);
			fAddJavaDocStub= new AddJavaDocStubAction(provider);
		}
	}
}