/*******************************************************************************
 * Copyright (c) 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jsp;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;

import org.eclipse.jface.text.source.ITagHandler;
import org.eclipse.jface.text.source.ITagHandlerFactory;
import org.eclipse.jface.text.source.ITranslator;

import org.eclipse.jdt.internal.ui.examples.jspeditor.Jsp2JavaTagHandlerFactory;
import org.eclipse.jdt.internal.ui.examples.jspeditor.JspReconcilingStrategy;
import org.eclipse.jdt.internal.ui.examples.jspeditor.JspTranslatorResultCollector;

/**
 * @author weinand
 */
public class JspTranslator extends AbstractJspParser implements ITranslator {
	
	boolean DEBUG= false;

	private StringBuffer fDeclarations= new StringBuffer();
	private StringBuffer fContent= new StringBuffer();
	private StringBuffer fLocalDeclarations= new StringBuffer();
	
	private ArrayList fContentLines= new ArrayList();
	private ArrayList fDeclarationLines= new ArrayList();
	private ArrayList fLocalDeclarationLines= new ArrayList();
	private int[] fSmap;
	
	private ITagHandlerFactory fTagHandlerFactor;
	private ITagHandler fCurrentTagHandler;

	private StringBuffer[] fBuffers;
	private ArrayList[] fLineMappingInfos;
	
	private JspTranslatorResultCollector fResultCollector;
	
	
	public JspTranslator() {
		
		// Links for passing parameters to the tag handlers 
		fResultCollector= new JspTranslatorResultCollector(fDeclarations, fLocalDeclarations, fContent, fDeclarationLines, fLocalDeclarationLines, fContentLines);
	}
		
	protected void startTag(boolean endTag, String name, int startName) {

		fCurrentTagHandler= fTagHandlerFactor.getHandler(name);

		if (DEBUG) {
			if (endTag)
				System.out.println("   </" + name + ">");
			else
				System.out.println("   <" + name);
		}
	}
	
	protected void tagAttribute(String attrName, String value, int startName, int startValue) {

		if (fCurrentTagHandler != null)
			fCurrentTagHandler.addAttribute(attrName, value, fLines);

		if (DEBUG)
			System.out.println("     " + attrName + "=\"" + value + "\"");
	}
	
	protected void endTag(boolean end) {

		if (fCurrentTagHandler != null)
			try  {
				fCurrentTagHandler.processEndTag(fResultCollector, fLines);
			} catch (IOException ex)  {
				ex.printStackTrace();
			}

		if (DEBUG) {
			if (end)
				System.out.println("   />");
			else
				System.out.println("   >");
		}
	}
	
	protected void java(char ch, String java, int line) {
		int i= 0;
		StringBuffer out= new StringBuffer();
		while (i < java.length()) {
			char c= java.charAt(i++);
			if (c == '\n') {
				if (ch == '!')  {
					fDeclarations.append(out.toString() + "\n");
					fDeclarationLines.add(new Integer(line++));
				} else  {
					fContent.append(out.toString() + "\n");
					fContentLines.add(new Integer(line++));
				}
				out.setLength(0);
			} else {
				out.append(c);	
			}
		}
		if (out.length() > 0)  {
			if (ch == '!')  {
				fDeclarations.append(out.toString() + "\n");
				fDeclarationLines.add(new Integer(line));
			} else  {
				fContent.append(out.toString() + "\n");
				fContentLines.add(new Integer(line));
			}
		}
	}
	
	protected void text(String t, int line) {
		int i= 0;
		StringBuffer out= new StringBuffer();
		while (i < t.length()) {
			char c= t.charAt(i++);
			if (c == '\n') {
				fContent.append("    System.out.println(\"" + out.toString() + "\");  //$NON-NLS-1$\n");
				fContentLines.add(new Integer(line++));
				out.setLength(0);
			} else {
				out.append(c);	
			}
		}
		if (out.length() > 0)  {
			fContent.append("    System.out.print(\"" + out.toString() + "\");  //$NON-NLS-1$\n");
			fContentLines.add(new Integer(line));
		}
	}
	
	private void resetTranslator() {
		fDeclarations.setLength(0);
		fContent.setLength(0);
		fLocalDeclarations.setLength(0);
		
		fLocalDeclarationLines.clear();
		fContentLines.clear();
		fDeclarationLines.clear();
		
	}

	public String translate(Reader reader, String name) throws IOException  {

		StringBuffer buffer= new StringBuffer();
		
		resetTranslator();
		parse(reader);

		int lineCount= 2 + fDeclarationLines.size() + 1 + 1 + fLocalDeclarationLines.size() + fContentLines.size() + 3;
		fSmap= new int[lineCount];
		int line= 0;
		fSmap[line++]= 1;

		buffer.append("public class " + name + " {\n\n");
		fSmap[line++]= 1;
		fSmap[line++]= 1;

		buffer.append(fDeclarations.toString() + "\n");
		System.out.println(fDeclarations.toString());
		for (int i= 0; i < fDeclarationLines.size(); i++)  {
			fSmap[line++]= ((Integer)fDeclarationLines.get(i)).intValue();
			System.out.println("" + ((Integer)fDeclarationLines.get(i)).intValue());
		}
		fSmap[line]= fSmap[line - 1] + 1;
		line++;

		buffer.append("  public void out() {\n");
		fSmap[line]= fSmap[line - 1] + 1;
		line++;
		
		if (fLocalDeclarations.length() > 0)  {
			buffer.append(fLocalDeclarations.toString());
			System.out.println(fLocalDeclarations);
			for (int i= 0; i < fLocalDeclarationLines.size(); i++) {
				System.out.println("" + ((Integer)fLocalDeclarationLines.get(i)).intValue());
				fSmap[line++]= ((Integer)fLocalDeclarationLines.get(i)).intValue();
			}
		}
		
		buffer.append(fContent.toString());
		System.out.println(fContent);
		for (int i= 0; i < fContentLines.size(); i++)  {
			fSmap[line++]= ((Integer)fContentLines.get(i)).intValue();
			System.out.println("" + ((Integer)fContentLines.get(i)).intValue());
		}

		buffer.append("  }\n");
		fSmap[line]= fSmap[line - 1];

		line++;
		
		buffer.append("}\n");
		fSmap[line]= fSmap[line - 2];
		
		for (int i= 0; i < fSmap.length; i++)
			System.out.println("" + i + " -> " + fSmap[i]);
		
		System.out.println(buffer.toString());
		
		return buffer.toString();
	}
	
	public int[] getSmap()  {
		return fSmap;
	}

	/*
	 * @see org.eclipse.jface.text.source.ITranslator#setTagHandlerFactory(org.eclipse.jface.text.source.ITagHandlerFactory)
	 */
	public void setTagHandlerFactory(ITagHandlerFactory tagHandlerFactory) {
		fTagHandlerFactor= tagHandlerFactory;
	}

	/*
	 * @see ITranslator#backTranslateOffsetInLine(String, String, int)
	 */
	public int backTranslateOffsetInLine(String originalLine, String translatedLine, int offsetInTranslatedLine, String tag)  {

		ITagHandler handler;
		if (tag != null)
			handler= fTagHandlerFactor.getHandler(tag);
		else
			handler= fTagHandlerFactor.findHandler(originalLine);

		if (handler != null)
			return handler.backTranslateOffsetInLine(originalLine, translatedLine, offsetInTranslatedLine);

		return -1;
	}
}
