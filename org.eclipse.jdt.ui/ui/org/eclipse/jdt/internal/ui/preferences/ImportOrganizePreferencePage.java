/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.preferences;

import java.util.ArrayList;import java.util.StringTokenizer;import org.eclipse.swt.graphics.GC;import org.eclipse.swt.graphics.Point;import org.eclipse.swt.layout.GridData;import org.eclipse.swt.widgets.Composite;import org.eclipse.swt.widgets.Control;import org.eclipse.jface.dialogs.IInputValidator;import org.eclipse.jface.preference.FieldEditorPreferencePage;import org.eclipse.jface.preference.IPreferenceStore;import org.eclipse.jface.preference.IntegerFieldEditor;import org.eclipse.jface.preference.ListEditor;import org.eclipse.ui.IWorkbench;import org.eclipse.ui.IWorkbenchPreferencePage;import org.eclipse.ui.help.DialogPageContextComputer;import org.eclipse.ui.help.WorkbenchHelp;import org.eclipse.jdt.core.JavaConventions;import org.eclipse.jdt.ui.JavaUI;import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;import org.eclipse.jdt.internal.ui.JavaPlugin;import org.eclipse.jdt.internal.ui.dialogs.StringInputDialog;

/*
 * The page for setting the organize import settings
 */
public class ImportOrganizePreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	// Preference store keys
	private static final String PREF_IMPORTORDER= JavaUI.ID_PLUGIN + ".importorder";
	private static final String PREF_ONDEMANDTHRESHOLD= JavaUI.ID_PLUGIN + ".ondemandthreshold";
	
	private static final String IMPORTORDER_LABEL= "ImportOrganizePreferencePage.importorder.label";
	private static final String ONDEMANDTHRESHOLD_LABEL= "ImportOrganizePreferencePage.ondemandthreshold.label";
	private static final String PAGE_DESC= "ImportOrganizePreferencePage.description";

	private static final String NEWENTRY_DIALOG= "ImportOrganizePreferencePage.newentrydialog";

	public ImportOrganizePreferencePage() {
		super(GRID);
		setPreferenceStore(JavaPlugin.getDefault().getPreferenceStore());
		setDescription(JavaPlugin.getResourceString(PAGE_DESC));
	}
	
	/**
	 * @see PreferencePage#createControl(Composite)
	 */
	public void createControl(Composite parent) {
		// added for 1GEUGE6: ITPJUI:WIN2000 - Help is the same on all preference pages
		super.createControl(parent);
		WorkbenchHelp.setHelp(getControl(), new DialogPageContextComputer(this, IJavaHelpContextIds.ORGANIZE_IMPORTS_PREFERENCE_PAGE));
	}	

	protected void createFieldEditors() {
		Composite parent= getFieldEditorParent();
		ListEditor listEditor= new ListEditor(PREF_IMPORTORDER, JavaPlugin.getResourceString(IMPORTORDER_LABEL), parent) {
			protected String createList(String[] items) {
				StringBuffer buf= new StringBuffer();
				for (int i= 0; i < items.length; i++) {
					buf.append(items[i]);
					buf.append(';');
				}
				return buf.toString();
			}
			
			protected String getNewInputObject() {
				String title= JavaPlugin.getResourceString(NEWENTRY_DIALOG + ".title");				
				String message= JavaPlugin.getResourceString(NEWENTRY_DIALOG + ".message");
				final String errorMessage= JavaPlugin.getResourceString(NEWENTRY_DIALOG + ".errormessage");
				
				IInputValidator validator= new IInputValidator() {
					public String isValid(String newText) {
						if (JavaConventions.validatePackageName(newText).isOK()) {
							return null;
						} else if (!"".equals(newText)) {
							return errorMessage;
						} else {
							return "";
						}
					}
				};
				StringInputDialog dialog= new StringInputDialog(getShell(), title, null, message, "", validator);
				if (dialog.open() == dialog.OK) {
					return dialog.getValue();
				} else {
					return null;
				}
			}
		
			protected String[] parseString(String list) {
				StringTokenizer st= new StringTokenizer(list, ";");
				ArrayList v= new ArrayList();
				while (st.hasMoreElements()) {
					v.add(st.nextElement());
				}
				return (String[]) v.toArray(new String[v.size()]);				
			}
		};
		addField(listEditor);
	
		IntegerFieldEditor intEditor= new IntegerFieldEditor(PREF_ONDEMANDTHRESHOLD, JavaPlugin.getResourceString(ONDEMANDTHRESHOLD_LABEL), parent) {
			protected void doFillIntoGrid(Composite parent, int numColumns) {
				super.doFillIntoGrid(parent, numColumns);
				Control text= getTextControl();
				GridData gd= (GridData)text.getLayoutData();
				GC gc = new GC(text);
				try {
					Point extent = gc.textExtent("X");
					gd.widthHint = 5 * extent.x;
				} finally {
					gc.dispose();
				}
				gd.horizontalAlignment = gd.BEGINNING;
				gd.grabExcessHorizontalSpace = false;
			}
		};
		addField(intEditor);
	}

	public void init(IWorkbench workbench) {
	}
	
	/**
	 * Initializes the default values of this page in the preference bundle.
	 * Will be called on startup of the JavaPlugin
	 */
	public static void initDefaults(IPreferenceStore prefs) {
		prefs.setDefault(PREF_IMPORTORDER, "java;javax;com");
		prefs.setDefault(PREF_ONDEMANDTHRESHOLD, 99);
	}
		
	public static String[] getImportOrderPreference() {
		String[] res;
		
		IPreferenceStore prefs= JavaPlugin.getDefault().getPreferenceStore();
		String str= prefs.getString(PREF_IMPORTORDER);
		if (str != null) {
			StringTokenizer tok= new StringTokenizer(str, ";");
			int nTokens= tok.countTokens();
			res= new String[nTokens];
			for (int i= 0; i < nTokens; i++) {
				res[i]= tok.nextToken();
			}
		} else {
			res= new String[0];
		}
		return res;
	}
	
	public static int getImportNumberThreshold() {
		IPreferenceStore prefs= JavaPlugin.getDefault().getPreferenceStore();
		int threshold= prefs.getInt(PREF_ONDEMANDTHRESHOLD);
		if (threshold == 0) {
			threshold= Integer.MAX_VALUE;
		}		
		return threshold;
	}		
}


