/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ltk.internal.core.refactoring;

import org.eclipse.core.commands.operations.IContextOperationApprover;
import org.eclipse.core.commands.operations.IUndoContext;

public class RefactoringUndoContext implements IUndoContext {

	public String getLabel() {
		return RefactoringCoreMessages.getString("RefactoringUndoContext.label"); //$NON-NLS-1$
	}

	public IContextOperationApprover getOperationApprover() {
		return null;
	}

	public boolean matches(IUndoContext context) {
		return true;
	}
}
