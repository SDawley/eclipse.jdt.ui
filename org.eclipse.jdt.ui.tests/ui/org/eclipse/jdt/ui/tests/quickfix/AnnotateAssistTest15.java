/*******************************************************************************
 * Copyright (c) 2015 GK Software AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Stephan Herrmann - Initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.quickfix;

import java.io.ByteArrayInputStream;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.core.runtime.Path;

import org.eclipse.core.resources.IFile;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.text.contentassist.ContentAssistEvent;
import org.eclipse.jface.text.contentassist.ICompletionListener;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.source.SourceViewer;

import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.tests.core.ProjectTestSetup;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.internal.ui.javaeditor.JavaSourceViewer;

public class AnnotateAssistTest15 extends AbstractAnnotateAssistTests {

	protected static final String ANNOTATION_PATH= "annots";
	
	protected static final Class<?> THIS= AnnotateAssistTest15.class;
	
	public static Test suite() {
		return setUpTest(new TestSuite(THIS));
	}

	public static Test setUpTest(Test test) {
		return new ProjectTestSetup(test);
	}
	
	public AnnotateAssistTest15(String name) {
		super(name);
	}

	protected void setUp() throws Exception {
		fJProject1= ProjectTestSetup.getProject();
		fJProject1.getProject().getFolder(ANNOTATION_PATH).create(true, true, null);
		fJProject1.setOption(JavaCore.COMPILER_ANNOTATION_NULL_ANALYSIS, JavaCore.ENABLED);
	}
	
	// === Tests ===

	/**
	 * Assert that the "Annotate" command can be invoked on a ClassFileEditor
	 * @throws Exception 
	 */
	public void testAnnotateReturn() throws Exception {
		
		String MY_MAP_PATH= "pack/age/MyMap";
		String[] pathAndContents= new String[] { 
					MY_MAP_PATH+".java", 
					"package pack.age;\n" +
					"public interface MyMap<K,V> {\n" +
					"    public V get(K key);\n" +
					"}\n"
				};
		addLibrary(fJProject1, "lib.jar", "lib.zip", pathAndContents, ANNOTATION_PATH, JavaCore.VERSION_1_5);
		IType type= fJProject1.findType(MY_MAP_PATH.replace('/', '.'));
		JavaEditor javaEditor= (JavaEditor) JavaUI.openInEditor(type);

		try {
			SourceViewer viewer= (SourceViewer)javaEditor.getViewer();

			// invoke the full command and asynchronously collect the result:
			final ICompletionProposal[] proposalBox= new ICompletionProposal[1];
			viewer.getQuickAssistAssistant().addCompletionListener(new ICompletionListener() {
				public void selectionChanged(ICompletionProposal proposal, boolean smartToggle) {
					proposalBox[0]= proposal;
				}				
				public void assistSessionStarted(ContentAssistEvent event) { /* nop */ }
				public void assistSessionEnded(ContentAssistEvent event) { /* nop */ }
			});

			int offset= pathAndContents[1].indexOf("V get");		
			viewer.setSelection(new TextSelection(offset, 0));
			viewer.doOperation(JavaSourceViewer.ANNOTATE_CLASS_FILE);

			int count = 10;
			while (proposalBox[0] == null && count-- > 0)
				Thread.sleep(200);
			ICompletionProposal proposal= proposalBox[0];
			assertNotNull("should have a proposal", proposal);
			
			viewer.getQuickAssistAssistant().uninstall();
			JavaProjectHelper.emptyDisplayLoop();

			assertEquals("expect proposal", "Annotate as '@NonNull V'", proposal.getDisplayString());
			String expectedInfo=
					"<dl><dt>get</dt>" +
					"<dd>(TK;)TV;</dd>" +
					"<dd>(TK;)T<b>1</b>V;</dd>" + // <= 1
					"</dl>";
			assertEquals("expect detail", expectedInfo, proposal.getAdditionalProposalInfo());

			IDocument document= javaEditor.getDocumentProvider().getDocument(javaEditor.getEditorInput());
			proposal.apply(document);
			
			IFile annotationFile= fJProject1.getProject().getFile(new Path(ANNOTATION_PATH).append("pack/age/MyMap.eea"));
			assertTrue("Annotation file should have been created", annotationFile.exists());

			String expectedContent=
					"class pack/age/MyMap\n" +
					"get\n" +
					" (TK;)TV;\n" +
					" (TK;)T1V;\n";
			checkContentOfFile("annotation file content", annotationFile, expectedContent);
		} finally {
			JavaPlugin.getActivePage().closeAllEditors(false);
		}
	}

	/**
	 * Assert two proposals ("@NonNull" and "@Nullable") on a simple return type (type variable).
	 * Apply the second proposal and check the effect.
	 * @throws Exception
	 */
	public void testAnnotateReturn2() throws Exception {
		
		String MY_MAP_PATH= "pack/age/MyMap";
		String[] pathAndContents= new String[] { 
					MY_MAP_PATH+".java",
					"package pack.age;\n" +
					"public interface MyMap<K,V> {\n" +
					"    public V get(K key);\n" +
					"}\n"
				};
		addLibrary(fJProject1, "lib.jar", "lib.zip", pathAndContents, ANNOTATION_PATH, JavaCore.VERSION_1_5);
		IType type= fJProject1.findType(MY_MAP_PATH.replace('/', '.'));
		JavaEditor javaEditor= (JavaEditor) JavaUI.openInEditor(type);

		try {
			int offset= pathAndContents[1].indexOf("V get");

			List<ICompletionProposal> list= collectAnnotateProposals(javaEditor, offset);
			
			assertCorrectLabels(list);
			assertNumberOfProposals(list, 2);
			
			ICompletionProposal proposal= findProposalByName("Annotate as '@NonNull V'", list);
			String expectedInfo=
					"<dl><dt>get</dt>" +
					"<dd>(TK;)TV;</dd>" +
					"<dd>(TK;)T<b>1</b>V;</dd>" + // <= 1
					"</dl>";
			assertEquals("expect detail", expectedInfo, proposal.getAdditionalProposalInfo());

			proposal= findProposalByName("Annotate as '@Nullable V'", list);
			expectedInfo=
					"<dl><dt>get</dt>" +
					"<dd>(TK;)TV;</dd>" +
					"<dd>(TK;)T<b>0</b>V;</dd>" + // <= 0
					"</dl>";
			assertEquals("expect detail", expectedInfo, proposal.getAdditionalProposalInfo());

			IDocument document= javaEditor.getDocumentProvider().getDocument(javaEditor.getEditorInput());
			proposal.apply(document);
			
			IFile annotationFile= fJProject1.getProject().getFile(new Path(ANNOTATION_PATH).append(MY_MAP_PATH+".eea"));
			assertTrue("Annotation file should have been created", annotationFile.exists());

			String expectedContent=
					"class pack/age/MyMap\n" +
					"get\n" +
					" (TK;)TV;\n" +
					" (TK;)T0V;\n";
			checkContentOfFile("annotation file content", annotationFile, expectedContent);
		} finally {
			JavaPlugin.getActivePage().closeAllEditors(false);
		}
	}

	/**
	 * Assert two proposals ("@NonNull" and "Remove") if annotation file already says "@Nullable".
	 * Apply the second proposal and check the effect.
	 * @throws Exception
	 */
	public void testAnnotateRemove() throws Exception {
		
		String MY_MAP_PATH= "pack/age/MyMap";

		String[] pathAndContents= new String[] { 
				MY_MAP_PATH+".java", 
				"package pack.age;\n" +
				"public interface MyMap<K,V> {\n" +
				"    public V get(K key);\n" +
				"}\n"
			};
		addLibrary(fJProject1, "lib.jar", "lib.zip", pathAndContents, ANNOTATION_PATH, JavaCore.VERSION_1_5);
		IType type= fJProject1.findType(MY_MAP_PATH.replace('/', '.'));
		IFile annotationFile= fJProject1.getProject().getFile(new Path(ANNOTATION_PATH).append(MY_MAP_PATH+".eea"));
		String initialContent=
				"class pack/age/MyMap\n" +
				"get\n" +
				" (TK;)TV;\n" +
				" (TK;)T0V;\n";
		ensureExists(annotationFile.getParent());
		annotationFile.create(new ByteArrayInputStream(initialContent.getBytes("UTF-8")), 0, null);
		
		JavaEditor javaEditor= (JavaEditor) JavaUI.openInEditor(type);

		try {
			int offset= pathAndContents[1].indexOf("V get");

			List<ICompletionProposal> list= collectAnnotateProposals(javaEditor, offset);
			
			assertCorrectLabels(list);
			assertNumberOfProposals(list, 2);
			
			ICompletionProposal proposal= findProposalByName("Annotate as '@NonNull V'", list);
			String expectedInfo=
					"<dl><dt>get</dt>" +
					"<dd>(TK;)TV;</dd>" +
					"<dd>(TK;)T<b>1</b>V;</dd>" + // <= 1
					"</dl>";
			assertEquals("expect detail", expectedInfo, proposal.getAdditionalProposalInfo());

			proposal= findProposalByName("Remove nullness annotation from type 'V'", list);
			expectedInfo=
					"<dl><dt>get</dt>" +
					"<dd>(TK;)TV;</dd>" +
					"<dd>(TK;)T<del>0</del>V;</dd>" + // <= <strike>0</strike>
					"</dl>";
			assertEquals("expect detail", expectedInfo, proposal.getAdditionalProposalInfo());

			IDocument document= javaEditor.getDocumentProvider().getDocument(javaEditor.getEditorInput());
			proposal.apply(document);
			
			assertTrue("Annotation file should still exist", annotationFile.exists());

			String expectedContent=
					"class pack/age/MyMap\n" +
					"get\n" +
					" (TK;)TV;\n" +
					" (TK;)TV;\n";
			checkContentOfFile("annotation file content", annotationFile, expectedContent);
		} finally {
			JavaPlugin.getActivePage().closeAllEditors(false);
		}
	}
	

	/**
	 * Assert two proposals ("@NonNull" and "@Nullable") on an (outer) array type (in parameter position).
	 * The method already has a 2-line entry (i.e., not yet annotated).
	 * Apply the second proposal and check the effect.
	 * @throws Exception
	 */
	public void testAnnotateParameter_Array1() throws Exception {
		
		String X_PATH= "pack/age/X";
		String[] pathAndContents= new String[] { 
					X_PATH+".java",
					"package pack.age;\n" +
					"import java.util.List;\n" +
					"public interface X {\n" +
					"    public String test(int[][] ints, List<String> list);\n" +
					"}\n"
				};
		addLibrary(fJProject1, "lib.jar", "lib.zip", pathAndContents, ANNOTATION_PATH, JavaCore.VERSION_1_8);
		
		IFile annotationFile= fJProject1.getProject().getFile(new Path(ANNOTATION_PATH).append(X_PATH+".eea"));
		String initialContent=
				"class pack/age/X\n" +
				"test\n" +
				" ([[ILjava/util/List<Ljava/lang/String;>;)Ljava/lang/String;\n";
		ensureExists(annotationFile.getParent());
		annotationFile.create(new ByteArrayInputStream(initialContent.getBytes("UTF-8")), 0, null);

		IType type= fJProject1.findType(X_PATH.replace('/', '.'));
		JavaEditor javaEditor= (JavaEditor) JavaUI.openInEditor(type);

		try {
			int offset= pathAndContents[1].indexOf("[][] ints");

			List<ICompletionProposal> list= collectAnnotateProposals(javaEditor, offset);
			
			assertCorrectLabels(list);
			assertNumberOfProposals(list, 2);
			
			ICompletionProposal proposal= findProposalByName("Annotate as 'int @NonNull [][]'", list);
			String expectedInfo=
					"<dl><dt>test</dt>" +
					"<dd>([[ILjava/util/List&lt;Ljava/lang/String;&gt;;)Ljava/lang/String;</dd>" +
					"<dd>([<b>1</b>[ILjava/util/List&lt;Ljava/lang/String;&gt;;)Ljava/lang/String;</dd>" + // <= 1
					"</dl>";
			assertEquals("expect detail", expectedInfo, proposal.getAdditionalProposalInfo());

			proposal= findProposalByName("Annotate as 'int @Nullable [][]'", list);
			expectedInfo=
					"<dl><dt>test</dt>" +
					"<dd>([[ILjava/util/List&lt;Ljava/lang/String;&gt;;)Ljava/lang/String;</dd>" +
					"<dd>([<b>0</b>[ILjava/util/List&lt;Ljava/lang/String;&gt;;)Ljava/lang/String;</dd>" + // <= 0
					"</dl>";
			assertEquals("expect detail", expectedInfo, proposal.getAdditionalProposalInfo());

			IDocument document= javaEditor.getDocumentProvider().getDocument(javaEditor.getEditorInput());
			proposal.apply(document);
			
			annotationFile= fJProject1.getProject().getFile(new Path(ANNOTATION_PATH).append(X_PATH+".eea"));
			assertTrue("Annotation file should have been created", annotationFile.exists());

			String expectedContent=
					"class pack/age/X\n" +
					"test\n" +
					" ([[ILjava/util/List<Ljava/lang/String;>;)Ljava/lang/String;\n" +
					" ([0[ILjava/util/List<Ljava/lang/String;>;)Ljava/lang/String;\n";
			checkContentOfFile("annotation file content", annotationFile, expectedContent);
		} finally {
			JavaPlugin.getActivePage().closeAllEditors(false);
		}
	}

	/**
	 * Assert two proposals ("@NonNull" and "@Nullable") on a simple field type (type variable).
	 * Apply the second proposal and check the effect.
	 * @throws Exception
	 */
	// FIXME(stephan): enable once implemented
	public void _testAnnotateField1() throws Exception {
		
		String NODE_PATH= "pack/age/Node";
		String[] pathAndContents= new String[] { 
					NODE_PATH+".java",
					"package pack.age;\n" +
					"public class Node<V> {\n" +
					"    V value;\n" +
					"}\n"
				};
		addLibrary(fJProject1, "lib.jar", "lib.zip", pathAndContents, ANNOTATION_PATH, JavaCore.VERSION_1_5);
		IType type= fJProject1.findType(NODE_PATH.replace('/', '.'));
		JavaEditor javaEditor= (JavaEditor) JavaUI.openInEditor(type);

		try {
			int offset= pathAndContents[1].indexOf("V value");

			List<ICompletionProposal> list= collectAnnotateProposals(javaEditor, offset);
			
			assertCorrectLabels(list);
			assertNumberOfProposals(list, 2);
			
			ICompletionProposal proposal= findProposalByName("Annotate as '@NonNull V'", list);
			String expectedInfo=
					"<dl><dt>value</dt>" +
					"<dd>TV;</dd>" +
					"<dd>T<b>1</b>V;</dd>" + // <= 1
					"</dl>";
			assertEquals("expect detail", expectedInfo, proposal.getAdditionalProposalInfo());

			proposal= findProposalByName("Annotate as '@Nullable V'", list);
			expectedInfo=
					"<dl><dt>value</dt>" +
					"<dd>TV;</dd>" +
					"<dd>T<b>0</b>V;</dd>" + // <= 0
					"</dl>";
			assertEquals("expect detail", expectedInfo, proposal.getAdditionalProposalInfo());

			IDocument document= javaEditor.getDocumentProvider().getDocument(javaEditor.getEditorInput());
			proposal.apply(document);
			
			IFile annotationFile= fJProject1.getProject().getFile(new Path(ANNOTATION_PATH).append(NODE_PATH+".eea"));
			assertTrue("Annotation file should have been created", annotationFile.exists());

			String expectedContent=
					"class pack/age/Node\n" +
					"value\n" +
					" TV;\n" +
					" T0V;\n";
			checkContentOfFile("annotation file content", annotationFile, expectedContent);
		} finally {
			JavaPlugin.getActivePage().closeAllEditors(false);
		}
	}

}