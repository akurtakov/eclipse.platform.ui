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
package org.eclipse.ui.tests.internal;

import static org.eclipse.ui.tests.harness.util.UITestUtil.openTestWindow;
import static org.eclipse.ui.tests.harness.util.UITestUtil.processEvents;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.ILogListener;
import org.eclipse.core.runtime.Platform;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.ui.advanced.MPerspective;
import org.eclipse.e4.ui.model.application.ui.advanced.MPerspectiveStack;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.WorkbenchWindow;
import org.eclipse.ui.internal.registry.PerspectiveDescriptor;
import org.eclipse.ui.tests.harness.util.CloseTestWindowsExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tests for WorkbenchPage.fixOrphanPerspective.
 *
 * <p>
 * An 'orphan' perspective is one that was originally created through a
 * contribution but whose contributing bundle is no longer available.
 * {@code fixOrphanPerspective} converts such a perspective into a local custom
 * copy so that it continues to function correctly within the workbench.
 */
@ExtendWith(CloseTestWindowsExtension.class)
public class WorkbenchPageFixOrphanPerspectiveTest {

	/**
	 * Verifies that {@code WorkbenchPage.fixOrphanPerspective} is triggered when
	 * switching to a perspective whose element ID is not registered in the
	 * perspective registry. After the fix the perspective is expected to have a new
	 * element ID and a corresponding custom perspective descriptor in the registry.
	 */
	@Test
	public void testFixOrphanPerspective() {
		IWorkbenchWindow window = openTestWindow();
		WorkbenchWindow workbenchWindow = (WorkbenchWindow) window;

		EModelService modelService = workbenchWindow.getService(EModelService.class);

		List<MPerspectiveStack> stacks = modelService.findElements(workbenchWindow.getModel(), null,
				MPerspectiveStack.class);
		assertFalse(stacks.isEmpty(), "Perspective stack should exist in the window model");
		MPerspectiveStack perspectiveStack = stacks.get(0);

		// Use an ID that is guaranteed not to be in the perspective registry.
		String orphanId = "org.eclipse.ui.tests.internal.orphan.perspective.nonexistent";
		String orphanLabel = "OrphanTestPerspective";

		assertNull(PlatformUI.getWorkbench().getPerspectiveRegistry().findPerspectiveWithId(orphanId),
				"Pre-condition: orphan ID must not be registered in the perspective registry");

		MPerspective orphanPerspective = modelService.createModelElement(MPerspective.class);
		orphanPerspective.setElementId(orphanId);
		orphanPerspective.setLabel(orphanLabel);

		// Add the orphan perspective to the stack and switch to it.
		// This triggers WorkbenchPage.selectionHandler -> getPerspective(MPerspective)
		// -> fixOrphanPerspective because the element ID is not in the registry.
		perspectiveStack.getChildren().add(orphanPerspective);
		perspectiveStack.setSelectedElement(orphanPerspective);
		processEvents();

		// Verify that fixOrphanPerspective was called:
		// 1. The element ID must have been changed from the original orphan ID.
		String newId = orphanPerspective.getElementId();
		assertNotEquals(orphanId, newId,
				"fixOrphanPerspective should have updated the orphan perspective's element ID");

		// 2. A new perspective descriptor for the fixed perspective must be registered.
		IPerspectiveDescriptor newDescriptor = PlatformUI.getWorkbench().getPerspectiveRegistry()
				.findPerspectiveWithId(newId);
		assertNotNull(newDescriptor,
				"fixOrphanPerspective should have created a new perspective descriptor in the registry");

		// 3. The new descriptor must be marked as a custom (local copy) definition.
		assertTrue(((PerspectiveDescriptor) newDescriptor).hasCustomDefinition(),
				"The descriptor created by fixOrphanPerspective should be marked as a custom definition");

		// Cleanup: remove the perspective descriptor that was added to the registry.
		PlatformUI.getWorkbench().getPerspectiveRegistry().deletePerspective(newDescriptor);
	}

	/**
	 * Verifies that a perspective whose element ID exists as a snippet in the
	 * application model (simulating a perspective contributed via
	 * {@code fragment.e4xmi} whose descriptor was not yet registered by
	 * {@code PerspectiveRegistry.postConstruct}) is <em>not</em> treated as an
	 * orphan when switched to.
	 *
	 * <p>
	 * Before the fix, {@code WorkbenchPage.fixOrphanPerspective} was erroneously
	 * called for such perspectives because {@code getPerspectiveDesc} returned
	 * {@code null} — the descriptor map was populated only from extension-point
	 * registrations, missing snippet-only perspectives. After the fix,
	 * {@code PerspectiveRegistry.findPerspectiveWithId} lazily registers a
	 * descriptor from the matching snippet, so {@code fixOrphanPerspective} is
	 * never invoked and no "local copy" log message is produced.
	 */
	@Test
	public void testSnippetPerspectiveIsNotTreatedAsOrphan() {
		IWorkbenchWindow window = openTestWindow();
		WorkbenchWindow workbenchWindow = (WorkbenchWindow) window;

		EModelService modelService = workbenchWindow.getService(EModelService.class);
		MApplication application = PlatformUI.getWorkbench().getService(MApplication.class);

		List<MPerspectiveStack> stacks = modelService.findElements(workbenchWindow.getModel(), null,
				MPerspectiveStack.class);
		assertFalse(stacks.isEmpty(), "Perspective stack should exist in the window model");
		MPerspectiveStack perspectiveStack = stacks.get(0);

		// Use an ID that is not registered via any extension point, but that is present
		// as a snippet (simulating a perspective added via fragment.e4xmi after
		// PerspectiveRegistry.postConstruct has already run).
		String snippetId = "org.eclipse.ui.tests.internal.snippet.perspective.test";
		String snippetLabel = "SnippetTestPerspective";

		assertNull(PlatformUI.getWorkbench().getPerspectiveRegistry().findPerspectiveWithId(snippetId),
				"Pre-condition: snippet ID must not be registered in the perspective registry");

		// Add the perspective to the application snippets (as fragment.e4xmi would).
		MPerspective snippetPerspective = modelService.createModelElement(MPerspective.class);
		snippetPerspective.setElementId(snippetId);
		snippetPerspective.setLabel(snippetLabel);
		application.getSnippets().add(snippetPerspective);

		// Simulate the clone that busySetPerspective -> cloneSnippet places in the
		// stack: a separate model element with the same element ID.
		MPerspective stackPerspective = modelService.createModelElement(MPerspective.class);
		stackPerspective.setElementId(snippetId);
		stackPerspective.setLabel(snippetLabel);

		List<String> logMessages = new ArrayList<>();
		ILogListener listener = (status, plugin) -> logMessages.add(status.getMessage());
		Platform.addLogListener(listener);
		try {
			perspectiveStack.getChildren().add(stackPerspective);
			perspectiveStack.setSelectedElement(stackPerspective);
			processEvents();
		} finally {
			Platform.removeLogListener(listener);
		}

		// Verify that fixOrphanPerspective was NOT called:
		// 1. The element ID must be unchanged (not converted to a local copy).
		assertEquals(snippetId, stackPerspective.getElementId(),
				"A snippet perspective's element ID must not be changed by fixOrphanPerspective");

		// 2. No "local copy" log message must have been produced.
		assertFalse(logMessages.stream().anyMatch(msg -> msg != null && msg.contains("local copy")),
				"A snippet perspective must not produce a 'local copy' log message");

		// Cleanup: remove the lazily created descriptor (also removes the snippet via
		// PerspectiveRegistry.deletePerspective -> removeSnippet).
		IPerspectiveDescriptor descriptor = PlatformUI.getWorkbench().getPerspectiveRegistry()
				.findPerspectiveWithId(snippetId);
		if (descriptor != null) {
			PlatformUI.getWorkbench().getPerspectiveRegistry().deletePerspective(descriptor);
		}
		// Also remove the original snippet we added (deletePerspective calls
		// removeSnippet with the same ID, so this is a safe no-op if already removed).
		application.getSnippets().remove(snippetPerspective);
	}
}