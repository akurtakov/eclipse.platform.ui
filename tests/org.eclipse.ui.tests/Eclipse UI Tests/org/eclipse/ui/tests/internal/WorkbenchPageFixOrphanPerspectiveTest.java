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
 * Tests for {@code WorkbenchPage.getPerspective(MPerspective)} and its
 * interaction with {@code WorkbenchPage.fixOrphanPerspective}.
 *
 * <p>
 * An 'orphan' perspective is one that was originally created through a
 * contribution but whose contributing bundle is no longer available.
 * {@code fixOrphanPerspective} converts such a perspective into a local custom
 * copy. E4 perspectives contributed via the application model (with a non-null
 * {@code contributorURI}) must <em>not</em> be treated as orphans.
 */
@ExtendWith(CloseTestWindowsExtension.class)
public class WorkbenchPageFixOrphanPerspectiveTest {

	/**
	 * Verifies that {@code WorkbenchPage.fixOrphanPerspective} is triggered when
	 * switching to a perspective whose element ID is not registered in the
	 * perspective registry and that has no {@code contributorURI} (i.e. a legacy
	 * orphan). After the fix the perspective is expected to have a new element ID
	 * and a corresponding custom perspective descriptor in the registry.
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
		// No contributorURI set – simulates a legacy orphan

		// Add the orphan perspective to the stack and switch to it.
		// This triggers WorkbenchPage.selectionHandler -> getPerspective(MPerspective)
		// -> fixOrphanPerspective because the element ID is not in the registry and
		// contributorURI is null.
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
	 * Verifies that an E4 perspective contributed via the application model (i.e.
	 * one whose {@code contributorURI} is non-null) is <em>not</em> converted to a
	 * local copy when it is activated. The perspective's element ID must remain
	 * unchanged and the registered descriptor must not be marked as a custom
	 * definition.
	 */
	@Test
	public void testE4PerspectiveNotConvertedToLocalCopy() {
		IWorkbenchWindow window = openTestWindow();
		WorkbenchWindow workbenchWindow = (WorkbenchWindow) window;

		EModelService modelService = workbenchWindow.getService(EModelService.class);

		List<MPerspectiveStack> stacks = modelService.findElements(workbenchWindow.getModel(), null,
				MPerspectiveStack.class);
		assertFalse(stacks.isEmpty(), "Perspective stack should exist in the window model");
		MPerspectiveStack perspectiveStack = stacks.get(0);

		String e4Id = "org.eclipse.ui.tests.internal.e4.perspective.model";
		String e4Label = "E4 Model Perspective";

		assertNull(PlatformUI.getWorkbench().getPerspectiveRegistry().findPerspectiveWithId(e4Id),
				"Pre-condition: e4 perspective ID must not be registered in the perspective registry");

		MPerspective e4Perspective = modelService.createModelElement(MPerspective.class);
		e4Perspective.setElementId(e4Id);
		e4Perspective.setLabel(e4Label);
		// Set a contributorURI to simulate an E4 model-contributed perspective
		e4Perspective.setContributorURI("platform:/plugin/org.eclipse.ui.tests");

		perspectiveStack.getChildren().add(e4Perspective);
		perspectiveStack.setSelectedElement(e4Perspective);
		processEvents();

		// 1. The element ID must NOT have been changed.
		assertEquals(e4Id, e4Perspective.getElementId(),
				"An E4 model perspective must not have its element ID changed");

		// 2. A descriptor must have been registered with the original ID.
		IPerspectiveDescriptor descriptor = PlatformUI.getWorkbench().getPerspectiveRegistry()
				.findPerspectiveWithId(e4Id);
		assertNotNull(descriptor, "A descriptor should have been registered for the E4 perspective");

		// 3. The descriptor must NOT be marked as a custom (local copy) definition.
		assertFalse(((PerspectiveDescriptor) descriptor).hasCustomDefinition(),
				"An E4 model perspective must not be marked as a custom definition (local copy)");

		// Cleanup
		PlatformUI.getWorkbench().getPerspectiveRegistry().deletePerspective(descriptor);
	}
}
