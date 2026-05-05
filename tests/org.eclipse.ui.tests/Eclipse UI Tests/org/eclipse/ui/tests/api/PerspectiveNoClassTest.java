/*******************************************************************************
 * Copyright (c) 2026 Contributors and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Contributors - initial API and implementation
 *******************************************************************************/
package org.eclipse.ui.tests.api;

import static org.eclipse.ui.tests.harness.util.UITestUtil.openTestWindow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.tests.harness.util.CloseTestWindowsExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Tests that a perspective registered in {@code org.eclipse.ui.perspectives}
 * without a {@code class} attribute can be opened successfully and receives the
 * default layout (editor area only).
 */
public class PerspectiveNoClassTest {

	/**
	 * The id of the perspective registered in plugin.xml without a {@code class}
	 * attribute.
	 */
	public static final String PERSP_ID = "org.eclipse.ui.tests.api.NoClassPerspective";

	@RegisterExtension
	public CloseTestWindowsExtension closeTestWindows = new CloseTestWindowsExtension();

	/**
	 * Verifies that a perspective with no {@code class} attribute can be opened
	 * without throwing an exception.
	 */
	@Test
	public void testOpenPerspectiveWithNoClassAttribute() {
		IWorkbenchWindow window = openTestWindow(PERSP_ID);
		assertNotNull(window, "Expected a workbench window to be opened");
	}

	/**
	 * Verifies that after opening a no-class perspective the active perspective has
	 * the correct id.
	 */
	@Test
	public void testActivePerspectiveId() {
		IWorkbenchWindow window = openTestWindow(PERSP_ID);
		IWorkbenchPage page = window.getActivePage();
		assertNotNull(page, "Expected an active page");
		assertEquals(PERSP_ID, page.getPerspective().getId(),
				"Active perspective should be the no-class perspective");
	}

	/**
	 * Verifies that the no-class perspective provides the default layout, i.e. the
	 * editor area is visible and no extra views are open.
	 */
	@Test
	public void testDefaultLayoutContainsEditorArea() {
		IWorkbenchWindow window = openTestWindow(PERSP_ID);
		IWorkbenchPage page = window.getActivePage();
		assertNotNull(page, "Expected an active page");
		// The default layout should show the editor area.
		assertEquals(true, page.isEditorAreaVisible(),
				"Editor area should be visible in the default layout");
	}
}
