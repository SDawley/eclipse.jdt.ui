/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.java;

import org.eclipse.jface.resource.ImageDescriptor;

import org.eclipse.jface.text.Assert;

import org.eclipse.jdt.core.CompletionProposal;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.Signature;

import org.eclipse.jdt.internal.corext.template.java.SignatureUtil;

import org.eclipse.jdt.ui.JavaElementImageDescriptor;

import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementImageProvider;


/**
 * Provides labels for java content assist proposals. The functionality is
 * similar to the one provided by {@link org.eclipse.jdt.ui.JavaElementLabels},
 * but the based on signatures and {@link CompletionProposal}s.
 * 
 * @see Signature
 * @since 3.1
 */
public final class ProposalLabelProvider {
	/**
	 * Creates and returns a parameter list of the given method proposal
	 * suitable for display. The list does not include parentheses. The
	 * parameter types are filtered trough
	 * {@link SignatureUtil#getLowerBound(char[])}.
	 * <p>
	 * Examples:
	 * <pre>
	 *  &quot;void method(int i, Strings)&quot; -&gt; &quot;int i, String s&quot;
	 *  &quot;? extends Number method(java.lang.String s, ? super Number n)&quot; -&gt; &quot;String s, Number n&quot;
	 * </pre>
	 * </p>
	 * 
	 * @param methodProposal the method proposal to create the parameter list
	 *        for. Must be of kind {@link CompletionProposal#METHOD_REF}.
	 * @return the list of comma-separated parameters suitable for display
	 */
	public String createUnboundedParameterList(CompletionProposal methodProposal) {
		Assert.isTrue(methodProposal.getKind() == CompletionProposal.METHOD_REF);
		return appendUnboundedParameterList(new StringBuffer(), methodProposal).toString();
	}
	
	/**
	 * Appends the parameter list to <code>buffer</code>. See
	 * <code>createUnboundedParameterList</code> for details.
	 * 
	 * @param buffer the buffer to append to
	 * @param methodProposal the method proposal
	 * @return the modified <code>buffer</code>
	 */
	private StringBuffer appendUnboundedParameterList(StringBuffer buffer, CompletionProposal methodProposal) {
		// TODO remove once https://bugs.eclipse.org/bugs/show_bug.cgi?id=85293
		// gets fixed.
		char[] signature= SignatureUtil.fix83600(methodProposal.getSignature());
		char[][] parameterNames= methodProposal.findParameterNames(null);
		char[][] parameterTypes= Signature.getParameterTypes(signature);
		for (int i= 0; i < parameterTypes.length; i++) {
			parameterTypes[i]= createTypeDisplayName(SignatureUtil.getLowerBound(parameterTypes[i]));
		}
		return appendParameterSignature(buffer, parameterTypes, parameterNames);
	}
	
	/**
	 * Returns the display string for a java type signature.
	 * 
	 * @param typeSignature the type signature to create a display name for
	 * @return the display name for <code>typeSignature</code>
	 * @throws IllegalArgumentException if <code>typeSignature</code> is not a
	 *         valid signature
	 * @see Signature#toCharArray(char[])
	 * @see Signature#getSimpleName(char[])
	 */
	private char[] createTypeDisplayName(char[] typeSignature) throws IllegalArgumentException {
		char[] displayName= Signature.getSimpleName(Signature.toCharArray(typeSignature));

		// XXX see https://bugs.eclipse.org/bugs/show_bug.cgi?id=84675
		boolean useShortGenerics= false;
		if (useShortGenerics) {
			StringBuffer buf= new StringBuffer();
			buf.append(displayName);
			int pos;
			do {
				pos= buf.indexOf("? extends "); //$NON-NLS-1$
				if (pos >= 0) {
					buf.replace(pos, pos + 10, "+"); //$NON-NLS-1$
				} else {
					pos= buf.indexOf("? super "); //$NON-NLS-1$
					if (pos >= 0)
						buf.replace(pos, pos + 8, "-"); //$NON-NLS-1$
				}
			} while (pos >= 0);
			return buf.toString().toCharArray();
		}
		return displayName;
	}
	
	/**
	 * Creates a display string of a parameter list (without the parentheses)
	 * for the given parameter types and names.
	 * 
	 * @param parameterTypes the parameter types
	 * @param parameterNames the parameter names
	 * @return the display string of the parameter list defined by the passed
	 *         arguments
	 */
	private final StringBuffer appendParameterSignature(StringBuffer buffer, char[][] parameterTypes, char[][] parameterNames) {
		if (parameterTypes != null) {
			for (int i = 0; i < parameterTypes.length; i++) {
				if (i > 0) {
					buffer.append(',');
					buffer.append(' ');
				}
				buffer.append(parameterTypes[i]);
				if (parameterNames != null && parameterNames[i] != null) {
					buffer.append(' ');
					buffer.append(parameterNames[i]);
				}
			}
		}
		return buffer;
	}
	
	/**
	 * Creates a display label for the given method proposal. The display label
	 * consists of:
	 * <ul>
	 *   <li>the method name</li>
	 *   <li>the parameter list (see {@link #createUnboundedParameterList(CompletionProposal)})</li>
	 *   <li>the upper bound of the return type (see {@link SignatureUtil#getUpperBound(String)})</li>
	 *   <li>the raw simple name of the declaring type</li>
	 * </ul>
	 * <p>
	 * Examples:
	 * For the <code>get(int)</code> method of a variable of type <code>List<? extends Number></code>, the following
	 * display name is returned: <code>get(int index)  Number - List</code>.<br>
	 * For the <code>add(E)</code> method of a variable of type <code>List<? super Number></code>, the following
	 * display name is returned: <code>add(Number o)  void - List</code>.<br>
	 * </p>
	 * 
	 * @param methodProposal the method proposal to display
	 * @return the display label for the given method proposal
	 */
	public String createMethodProposalLabel(CompletionProposal methodProposal) {
		StringBuffer nameBuffer= new StringBuffer();
		
		// method name
		nameBuffer.append(methodProposal.getName());
		
		// parameters
		nameBuffer.append('(');
		appendUnboundedParameterList(nameBuffer, methodProposal);
		nameBuffer.append(")  "); //$NON-NLS-1$
		
		// return type
		// TODO remove fix83600 call when bugs are fixed
		char[] returnType= createTypeDisplayName(SignatureUtil.getUpperBound(Signature.getReturnType(SignatureUtil.fix83600(methodProposal.getSignature()))));
		nameBuffer.append(returnType);

		// declaring type
		nameBuffer.append(" - "); //$NON-NLS-1$
		String declaringType= extractDeclaringTypeFQN(methodProposal);
		declaringType= Signature.getSimpleName(declaringType);
		nameBuffer.append(declaringType);

		return nameBuffer.toString();
	}

	public String createOverrideMethodProposalLabel(CompletionProposal methodProposal) {
		StringBuffer nameBuffer= new StringBuffer();
		
		// method name
		nameBuffer.append(methodProposal.getName());
		
		// parameters
		nameBuffer.append('(');
		appendUnboundedParameterList(nameBuffer, methodProposal);
		nameBuffer.append(")  "); //$NON-NLS-1$
		
		// return type
		// TODO remove fix83600 call when bugs are fixed
		char[] returnType= createTypeDisplayName(SignatureUtil.getUpperBound(Signature.getReturnType(SignatureUtil.fix83600(methodProposal.getSignature()))));
		nameBuffer.append(returnType);

		// declaring type
		nameBuffer.append(" - "); //$NON-NLS-1$

		String declaringType= extractDeclaringTypeFQN(methodProposal);
		declaringType= Signature.getSimpleName(declaringType);
		nameBuffer.append(JavaTextMessages.getFormattedString("ResultCollector.overridingmethod", new String(declaringType))); //$NON-NLS-1$

		return nameBuffer.toString();
	}

	/**
	 * Extracts the fully qualified name of the declaring type of a method
	 * reference.
	 * 
	 * @param methodProposal a proposed method
	 * @return the qualified name of the declaring type
	 */
	private String extractDeclaringTypeFQN(CompletionProposal methodProposal) {
		char[] declaringTypeSignature= methodProposal.getDeclarationSignature();
		// special methods may not have a declaring type: methods defined on arrays etc.
		// TODO remove when bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=84690 gets fixed
		if (declaringTypeSignature == null)
			return "java.lang.Object"; //$NON-NLS-1$
		return SignatureUtil.stripSignatureToFQN(String.valueOf(declaringTypeSignature));
	}
	
	/**
	 * Creates a display label for a given type proposal. The display label
	 * consists of:
	 * <ul>
	 *   <li>the simple type name</li>
	 *   <li>the package name</li>
	 * </ul>
	 * <p>
	 * Examples:
	 * A proposal for the generic type <code>java.util.List&lt;E&gt;</code>, the display label
	 * is: <code>List<E> - java.util</code>. 
	 * </p>
	 * 
	 * @param typeProposal the method proposal to display
	 * @return the display label for the given type proposal
	 */
	public String createTypeProposalLabel(CompletionProposal typeProposal) {
		char[] signature= typeProposal.getSignature();
		char[] packageName= Signature.getSignatureQualifier(signature); 
		char[] typeName= Signature.getSignatureSimpleName(signature);
		
		StringBuffer buf= new StringBuffer();
		buf.append(typeName);
		if (packageName.length > 0) {
			buf.append(" - "); //$NON-NLS-1$
			buf.append(packageName);
		}
		return buf.toString();
	}
	
	public String createSimpleLabelWithType(CompletionProposal proposal) {
		StringBuffer buf= new StringBuffer();
		buf.append(proposal.getCompletion());
		char[] typeName= Signature.getSignatureSimpleName(proposal.getSignature());
		if (typeName.length > 0) {
			buf.append("    "); //$NON-NLS-1$
			buf.append(typeName);
		}
		return buf.toString();
	}
	
	public String createLabelWithTypeAndDeclaration(CompletionProposal proposal) {
		StringBuffer buf= new StringBuffer();
		buf.append(proposal.getCompletion());
		char[] typeName= Signature.getSignatureSimpleName(proposal.getSignature());
		if (typeName.length > 0) {
			buf.append("    "); //$NON-NLS-1$
			buf.append(typeName);
		}
		char[] declaration= proposal.getDeclarationSignature();
		if (declaration != null) {
			declaration= Signature.getSignatureSimpleName(declaration);
			if (declaration.length > 0) {
				buf.append(" - "); //$NON-NLS-1$
				buf.append(declaration);
			}
		}

		return buf.toString();
	}
	
	public String createPackageProposalLabel(CompletionProposal proposal) {
		Assert.isTrue(proposal.getKind() == CompletionProposal.PACKAGE_REF);
		return String.valueOf(proposal.getDeclarationSignature());
	}
	
	public String createSimpleLabel(CompletionProposal proposal) {
		return String.valueOf(proposal.getCompletion());
	}
	
	public String createAnonymousTypeLabel(CompletionProposal proposal) {
		char[] declaringTypeSignature= proposal.getDeclarationSignature();
		
		StringBuffer buffer= new StringBuffer();
		buffer.append(Signature.getSignatureSimpleName(declaringTypeSignature));
		buffer.append('(');
		appendUnboundedParameterList(buffer, proposal);
		buffer.append(')');
		buffer.append("  "); //$NON-NLS-1$
		buffer.append(JavaTextMessages.getString("ResultCollector.anonymous_type")); //$NON-NLS-1$

		return buffer.toString();
	}
	
	/**
	 * Creates the display label for a given <code>CompletionProposal</code>.
	 *  
	 * @param proposal the completion proposal to create the display label for
	 * @return the display label for <code>proposal</code>
	 */
	public String createLabel(CompletionProposal proposal) {
		switch (proposal.getKind()) {
			case CompletionProposal.METHOD_NAME_REFERENCE:
			case CompletionProposal.METHOD_REF:
			case CompletionProposal.ANNOTATION_ATTRIBUTE_REF:
			case CompletionProposal.POTENTIAL_METHOD_DECLARATION:
				return createMethodProposalLabel(proposal);
			case CompletionProposal.METHOD_DECLARATION:
				return createOverrideMethodProposalLabel(proposal);
			case CompletionProposal.ANONYMOUS_CLASS_DECLARATION:
				return createAnonymousTypeLabel(proposal);
			case CompletionProposal.TYPE_REF:
				return createTypeProposalLabel(proposal);
			case CompletionProposal.PACKAGE_REF:
				return createPackageProposalLabel(proposal);
			case CompletionProposal.FIELD_REF:
				return createLabelWithTypeAndDeclaration(proposal);
			case CompletionProposal.LOCAL_VARIABLE_REF:
			case CompletionProposal.VARIABLE_DECLARATION:
				return createSimpleLabelWithType(proposal);
			case CompletionProposal.KEYWORD:
			case CompletionProposal.LABEL_REF:
				return createSimpleLabel(proposal);
			default:
				Assert.isTrue(false);
				return null;
		}
	}

	/**
	 * Creates and returns a decorated image descriptor for a completion proposal.
	 * 
	 * @param proposal the proposal for which to create an image descriptor
	 * @return the created image descriptor, or <code>null</code> if no image is available
	 */
	public ImageDescriptor createImageDescriptor(CompletionProposal proposal) {
		final int flags= proposal.getFlags();
		
		ImageDescriptor descriptor;
		switch (proposal.getKind()) {
			case CompletionProposal.METHOD_DECLARATION:
			case CompletionProposal.METHOD_NAME_REFERENCE:
			case CompletionProposal.METHOD_REF:
			case CompletionProposal.ANNOTATION_ATTRIBUTE_REF:
			case CompletionProposal.POTENTIAL_METHOD_DECLARATION:
				descriptor= JavaElementImageProvider.getMethodImageDescriptor(false, flags);
				break;
			case CompletionProposal.ANONYMOUS_CLASS_DECLARATION:
			case CompletionProposal.TYPE_REF:
				descriptor= JavaElementImageProvider.getTypeImageDescriptor(false, false, flags, false);
				break;
			case CompletionProposal.FIELD_REF:
				descriptor= JavaElementImageProvider.getFieldImageDescriptor(false, flags);
				break;
			case CompletionProposal.LOCAL_VARIABLE_REF:
			case CompletionProposal.VARIABLE_DECLARATION:
				descriptor= JavaPluginImages.DESC_OBJS_LOCAL_VARIABLE;
				break;
			case CompletionProposal.PACKAGE_REF:
				descriptor= JavaPluginImages.DESC_OBJS_PACKAGE;
				break;
			case CompletionProposal.KEYWORD:
			case CompletionProposal.LABEL_REF:
				descriptor= null;
				break;
			default:
				descriptor= null;
				Assert.isTrue(false);
		}
		
		if (descriptor == null)
			return null;
		return decorateImageDescriptor(descriptor, proposal);
	}
	
	public ImageDescriptor createMethodImageDescriptor(CompletionProposal proposal) {
		final int flags= proposal.getFlags();
		return decorateImageDescriptor(JavaElementImageProvider.getMethodImageDescriptor(false, flags), proposal);
	}
	
	public ImageDescriptor createTypeImageDescriptor(CompletionProposal proposal) {
		final int flags= proposal.getFlags();
		return decorateImageDescriptor(JavaElementImageProvider.getTypeImageDescriptor(false, false, flags, false), proposal);
	}
	
	public ImageDescriptor createFieldImageDescriptor(CompletionProposal proposal) {
		final int flags= proposal.getFlags();
		return decorateImageDescriptor(JavaElementImageProvider.getFieldImageDescriptor(false, flags), proposal);
	}
	
	public ImageDescriptor createLocalImageDescriptor(CompletionProposal proposal) {
		return decorateImageDescriptor(JavaPluginImages.DESC_OBJS_LOCAL_VARIABLE, proposal);
	}
	
	public ImageDescriptor createPackageImageDescriptor(CompletionProposal proposal) {
		return decorateImageDescriptor(JavaPluginImages.DESC_OBJS_PACKAGE, proposal);
	}
	
	/**
	 * Returns a version of <code>descriptor</code> decorated according to
	 * the passed <code>modifier</code> flags.
	 *  
	 * @param descriptor the image descriptor to decorate
	 * @param proposal the proposal
	 * @return an image descriptor for a method proposal
	 * @see Flags
	 */
	private ImageDescriptor decorateImageDescriptor(ImageDescriptor descriptor, CompletionProposal proposal) {
		int adornments= 0;
		int flags= proposal.getFlags();
		int kind= proposal.getKind();
		
		if (Flags.isDeprecated(flags))
			adornments |= JavaElementImageDescriptor.DEPRECATED;

		if (kind == CompletionProposal.FIELD_REF || kind == CompletionProposal.METHOD_DECLARATION || kind == CompletionProposal.METHOD_DECLARATION || kind == CompletionProposal.METHOD_NAME_REFERENCE || kind == CompletionProposal.METHOD_REF)
			if (Flags.isStatic(flags))
				adornments |= JavaElementImageDescriptor.STATIC;
	
		if (Flags.isFinal(flags))
			adornments |= JavaElementImageDescriptor.FINAL;
		
		if (kind == CompletionProposal.METHOD_DECLARATION || kind == CompletionProposal.METHOD_DECLARATION || kind == CompletionProposal.METHOD_NAME_REFERENCE || kind == CompletionProposal.METHOD_REF)
			if (Flags.isSynchronized(flags))
				adornments |= JavaElementImageDescriptor.SYNCHRONIZED;
		
		if (Flags.isAbstract(flags) && !Flags.isInterface(flags))
			adornments |= JavaElementImageDescriptor.ABSTRACT;
		
		return new JavaElementImageDescriptor(descriptor, adornments, JavaElementImageProvider.SMALL_SIZE);
	}
	
}
