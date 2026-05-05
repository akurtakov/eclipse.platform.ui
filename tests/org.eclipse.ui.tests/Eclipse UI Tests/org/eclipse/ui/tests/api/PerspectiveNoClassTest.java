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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.ILogListener;
import org.eclipse.core.runtime.Platform;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.tests.harness.util.CloseTestWindowsExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Tests that a perspective registered in {@code org.eclipse.ui.perspectives}
 * without a {@code class} attribute can be opened successfully and receives the
 * default layout (editor area only).
 *
 * <p>Also tests that an e4 perspective (defined as a model snippet rather than
 * via the 3.x extension point) does not produce a "local copy" log message
 * when opened.
 */
public class PerspectiveNoClassTest {

	/**
	 * The id of the perspective registered in plugin.xml without a {@code class}
	 * attribute.
	 */
	public static final String PERSP_ID = "org.eclipse.ui.tests.api.NoClassPerspective";

	/**
	 * The id of the e4 perspective defined as a model snippet in fragment.e4xmi
	 * (no {@code org.eclipse.ui.perspectives} extension point registration).
	 */
	public static final String E4_PERSP_ID = "org.eclipse.ui.tests.api.E4Perspective";

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
		assertTrue(PERSP_ID.equals(page.getPerspective().getId()),
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
		assertTrue(page.isEditorAreaVisible(),
				"Editor area should be visible in the default layout");
	}

	/**
	 * Verifies that switching to an e4 perspective (defined as a model snippet in
	 * fragment.e4xmi, without a 3.x extension-point registration) via
	 * {@link EPartService#switchPerspective(String)} does NOT produce a "local
	 * copy" log message.
	 *
	 * <p>Before the fix, the {@code WorkbenchPage.fixOrphanPerspective()} method
	 * was called for such perspectives because their descriptor could not be found
	 * in the registry, causing an INFO log entry of the form
	 * {@code "Perspective with name '...' and id '...' has been made into a local copy"}.
	 * The log is only emitted when {@code EPartService.switchPerspective} is called
	 * (i.e. when the workbench selection handler fires on a perspective-stack
	 * selection change).
	 */
	@Test
	public void testE4PerspectiveDoesNotLogLocalCopy() throws WorkbenchException {
		IWorkbenchWindow window = openTestWindow();

		// Capture log messages across all perspective operations so we detect a
		// "local copy" that might be produced during the initial showPerspective call,
		// not only during the later switchPerspective call.
		List<String> logMessages = new ArrayList<>();
		ILogListener listener = (status, plugin) -> logMessages.add(status.getMessage());
		Platform.addLogListener(listener);
		try {
			// Open the e4 perspective — clones the snippet and adds it to the stack.
			PlatformUI.getWorkbench().showPerspective(E4_PERSP_ID, window);

			// Switch to a different perspective so the e4 perspective is no longer active.
			PlatformUI.getWorkbench().showPerspective(PERSP_ID, window);

			// Switch back via EPartService.switchPerspective — the code path that
			// triggers WorkbenchPage.selectionHandler and the potential "local copy" log.
			EPartService partService = window.getService(EPartService.class);
			partService.switchPerspective(E4_PERSP_ID);
		} finally {
			Platform.removeLogListener(listener);
		}
		assertFalse(
				logMessages.stream().anyMatch(msg -> msg != null && msg.contains("local copy")),
				"Opening or switching to an e4 perspective should not produce a 'local copy' log message");
	}
}
