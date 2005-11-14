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
package org.eclipse.ltk.internal.core.refactoring.history;

/**
 * Interface for constants related to refactoring serialization.
 * 
 * @since 3.2
 */
public interface IRefactoringSerializationConstants {

	/** The output method */
	public static final String OUTPUT_METHOD= "xml"; //$NON-NLS-1$

	/** The output encoding */
	public static final String OUTPUT_ENCODING= "utf-8"; //$NON-NLS-1$

	/** The comment attribute */
	public static final String ATTRIBUTE_COMMENT= "comment"; //$NON-NLS-1$

	/** The description attribute */
	public static final String ATTRIBUTE_DESCRIPTION= "description"; //$NON-NLS-1$

	/** The id attribute */
	public static final String ATTRIBUTE_ID= "id"; //$NON-NLS-1$

	/** The time stamp attribute */
	public static final String ATTRIBUTE_STAMP= "stamp"; //$NON-NLS-1$

	/** The project attribute */
	public static final String ATTRIBUTE_PROJECT= "project"; //$NON-NLS-1$

	/** The version attribute */
	public static final String ATTRIBUTE_VERSION= "version"; //$NON-NLS-1$

	/** The refactoring element */
	public static final String ELEMENT_REFACTORING= "refactoring"; //$NON-NLS-1$

	/** The session element */
	public static final String ELEMENT_SESSION= "session"; //$NON-NLS-1$
}