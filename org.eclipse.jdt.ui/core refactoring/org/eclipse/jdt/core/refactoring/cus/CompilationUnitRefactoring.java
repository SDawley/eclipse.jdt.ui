package org.eclipse.jdt.core.refactoring.cus;import org.eclipse.jdt.core.ICompilationUnit;import org.eclipse.jdt.core.refactoring.Refactoring;import org.eclipse.jdt.core.refactoring.text.ITextBufferChangeCreator;import org.eclipse.jdt.internal.core.refactoring.Assert;
public abstract class CompilationUnitRefactoring extends Refactoring{
	
	private ICompilationUnit fCu;
	private ITextBufferChangeCreator fTextBufferChangeCreator;
	
	CompilationUnitRefactoring(ITextBufferChangeCreator changeCreator, ICompilationUnit cu){
		Assert.isNotNull(cu);
		fCu= cu;
		Assert.isNotNull(changeCreator, "change creator");
		fTextBufferChangeCreator= changeCreator;
	}
	
	/**
	 * Gets the cu
	 * @return Returns a ICompilationUnit
	 */
	protected final ICompilationUnit getCu() {
		return fCu;
	}

	/**
	 * Gets the textBufferChangeCreator
	 * @return Returns a ITextBufferChangeCreator
	 */
	protected final ITextBufferChangeCreator getTextBufferChangeCreator() {
		return fTextBufferChangeCreator;
	}
	
	protected String getSimpleCUName(){
		return removeFileNameExtension(fCu.getElementName());
	}

	protected static String removeFileNameExtension(String fileName){
		return fileName.substring(0, fileName.lastIndexOf("."));
	}
}
