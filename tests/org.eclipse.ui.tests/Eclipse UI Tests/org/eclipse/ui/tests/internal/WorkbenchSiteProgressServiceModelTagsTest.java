/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
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
 ******************************************************************************/

package org.eclipse.ui.tests.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.ui.internal.workbench.swt.CSSConstants;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.internal.ModelUtils;
import org.eclipse.e4.ui.workbench.UIEvents;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.internal.PartSite;
import org.eclipse.ui.internal.progress.WorkbenchSiteProgressService;
import org.eclipse.ui.tests.api.workbenchpart.EmptyView;
import org.eclipse.ui.tests.harness.util.UITestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

public class WorkbenchSiteProgressServiceModelTagsTest {
	private IWorkbenchWindow window;

	private IWorkbenchPage page;

	private EmptyView view;

	private Event receivedEvent;

	private EventHandler eventHandler;

	private IEventBroker eventBroker;

	private PartSite site;

	private WorkbenchSiteProgressServiceTestable progressService;

	@Before
	public void doSetUp() throws Exception {
		window = UITestCase.openTestWindow();
		page = window.getActivePage();
		view = (EmptyView) page.showView(EmptyView.ID);

		assertTrue(page.getActivePart().getSite() instanceof PartSite);
		site = (PartSite) page.getActivePart().getSite();

		progressService = new WorkbenchSiteProgressServiceTestable(site);

		IEclipseContext context = ModelUtils.getContainingContext(site.getModel());
		assertNotNull(context);

		eventHandler = event -> receivedEvent = event;

		eventBroker = context.get(IEventBroker.class);
		eventBroker.subscribe(UIEvents.ApplicationElement.TOPIC_TAGS, eventHandler);
	}


	@After
	public void doTearDown() throws Exception {
		eventBroker.unsubscribe(eventHandler);
		eventBroker = null;
		page.hideView(view);
	}

	@Test
	public void testShowBusyWhenCurrentlyIdle() throws Exception {
		site.getModel().getTags().remove(CSSConstants.CSS_BUSY_CLASS); /* state idle */

		progressService.showBusy(true);

		assertTrue(site.getModel().getTags().contains(CSSConstants.CSS_BUSY_CLASS));
		assertAddBusyTagEvent(receivedEvent);
	}

	@Test
	public void testShowBusyWhenCurrentlyBusy() throws Exception {
		site.getModel().getTags().add(CSSConstants.CSS_BUSY_CLASS); /* state busy */

		progressService.showBusy(false);

		assertFalse(site.getModel().getTags().contains(CSSConstants.CSS_BUSY_CLASS));
		assertRemoveBusyTagEvent(receivedEvent);
	}

	@Test
	public void testWarnOfContentChange() throws Exception {
		progressService.warnOfContentChange();

		assertContentChangeTagEvent(receivedEvent);
	}

	//helper functions
	private static class WorkbenchSiteProgressServiceTestable extends WorkbenchSiteProgressService {
		public WorkbenchSiteProgressServiceTestable(PartSite partSite) {
			super(partSite);
		}

		@Override
		public void showBusy(boolean busy) {
			super.showBusy(busy);
		}
	}

	private void assertModelTagChangedEvent(Event event) {
		assertNotNull(event);
		assertTrue(event.getProperty(UIEvents.EventTags.ELEMENT) instanceof MPart);
		assertEquals(UIEvents.ApplicationElement.TAGS, event.getProperty(UIEvents.EventTags.ATTNAME));
	}

	private void assertAddBusyTagEvent(Event event) {
		assertModelTagChangedEvent(event);
		assertNull(event.getProperty(UIEvents.EventTags.OLD_VALUE));
		assertEquals(CSSConstants.CSS_BUSY_CLASS, event.getProperty(UIEvents.EventTags.NEW_VALUE));
	}

	private void assertRemoveBusyTagEvent(Event event) {
		assertModelTagChangedEvent(event);
		assertEquals(CSSConstants.CSS_BUSY_CLASS, event.getProperty(UIEvents.EventTags.OLD_VALUE));
		assertNull(event.getProperty(UIEvents.EventTags.NEW_VALUE));
	}

	private void assertContentChangeTagEvent(Event event) {
		assertModelTagChangedEvent(event);

		// we check if any event for the CSS_CONTENT_CHANGE_CLASS tag was propagated.
		// It happens when the warmOfContentChange method was executed
		assertTrue(CSSConstants.CSS_CONTENT_CHANGE_CLASS.equals(event.getProperty(UIEvents.EventTags.OLD_VALUE)) ||
				CSSConstants.CSS_CONTENT_CHANGE_CLASS.equals(event.getProperty(UIEvents.EventTags.NEW_VALUE)));
	}
}
