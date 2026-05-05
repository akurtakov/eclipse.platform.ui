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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

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
		String orphanLabel = "Orphan Test Perspective";

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
}
