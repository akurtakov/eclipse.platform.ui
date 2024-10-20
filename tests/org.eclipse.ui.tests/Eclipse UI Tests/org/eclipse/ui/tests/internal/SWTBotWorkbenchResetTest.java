/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.ui.tests.internal;

import static org.eclipse.ui.tests.harness.util.UITestCase.processEvents;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.tests.harness.util.UITestCase;
import org.junit.Test;

/**
 * Test for Bug 511729 — Improve robustness to SWTBot.resetWorkbench()
 */
public class SWTBotWorkbenchResetTest {

	/**
	 * Open a new window, switch to a different perspective such that parts are
	 * hidden. Then perform resetWorkbench(), and verify view parts are restored
	 * and appear healthy.
	 */
	@Test
	public void testResetWorkbench() throws CoreException {
		IWorkbenchWindow window = UITestCase.openTestWindow(IDE.RESOURCE_PERSPECTIVE_ID);
		window.getWorkbench().showPerspective("org.eclipse.ui.tests.api.ViewPerspective", window);
		processEvents();

		resetWorkbench(window);

		// verify parts were restored/shown and they have content
		int viewCount = 0;
		for (IViewReference ref : window.getActivePage().getViewReferences()) {
			IViewPart view = ref.getView(false);
			if (view != null) {
				Composite composite = view.getSite().getService(Composite.class);
				assertNotNull(composite);
				assertFalse(composite.isDisposed());
				viewCount++;
			}
		}
		assertTrue(viewCount > 0);
	}

	/**
	 * Mimic's SWTBBot's SWTWorkbenchBot#resetWorkbench()
	 */
	private void resetWorkbench(IWorkbenchWindow window) throws WorkbenchException {
		// closeAllShells(): close all shells except for active workbench window
		for (Shell shell : window.getShell().getDisplay().getShells()) {
			if (shell != window.getShell() && !shell.isDisposed()) {
				shell.close();
			}
		}
		processEvents();

		// saveAllEditors();
		window.getActivePage().saveAllEditors(false);
		processEvents();

		// closeAllEditors();
		window.getActivePage().closeAllEditors(false);
		processEvents();

		// resetActivePerspective();
		window.getActivePage().resetPerspective();
		processEvents();

		// defaultPerspective().activate();
		String perspectiveId = window.getWorkbench().getPerspectiveRegistry().getDefaultPerspective();
		window.getWorkbench().showPerspective(perspectiveId, window);
		processEvents();

		// resetActivePerspective();
		window.getActivePage().resetPerspective();
		processEvents();
	}
}
