package org.eclipse.ui.internal.dialogs;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
import java.io.File;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.util.Date;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.dialogs.PropertyPage;
import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.ui.internal.IHelpContextIds;
import org.eclipse.ui.internal.WorkbenchMessages;

/**
 * This is a dummy page that is added to the property dialog
 * when multiple elements are selected. At the moment
 * we don't handle multiple selection in a meaningful way.
 */
public class ResourceInfoPage extends PropertyPage {
	
	private Button editableBox;
	private Button derivedBox;
	private boolean readOnlyValue;
	private boolean derivedValue;
	private static String READ_ONLY = WorkbenchMessages.getString("ResourceInfo.readOnly"); //$NON-NLS-1$
	private static String DERIVED = WorkbenchMessages.getString("ResourceInfo.derived"); //$NON-NLS-1$
	private static String NAME_TITLE = WorkbenchMessages.getString("ResourceInfo.name"); //$NON-NLS-1$
	private static String TYPE_TITLE = WorkbenchMessages.getString("ResourceInfo.type"); //$NON-NLS-1$
	private static String LOCATION_TITLE = WorkbenchMessages.getString("ResourceInfo.location"); //$NON-NLS-1$
	private static String SIZE_TITLE = WorkbenchMessages.getString("ResourceInfo.size"); //$NON-NLS-1$
	private static String BYTES_LABEL = WorkbenchMessages.getString("ResourceInfo.bytes"); //$NON-NLS-1$
	private static String FILE_LABEL = WorkbenchMessages.getString("ResourceInfo.file"); //$NON-NLS-1$
	private static String FOLDER_LABEL = WorkbenchMessages.getString("ResourceInfo.folder"); //$NON-NLS-1$
	private static String PROJECT_LABEL = WorkbenchMessages.getString("ResourceInfo.project"); //$NON-NLS-1$
	private static String UNKNOWN_LABEL = WorkbenchMessages.getString("ResourceInfo.unknown"); //$NON-NLS-1$
	private static String NOT_LOCAL_TEXT = WorkbenchMessages.getString("ResourceInfo.notLocal"); //$NON-NLS-1$
	private static String PATH_TITLE = WorkbenchMessages.getString("ResourceInfo.path"); //$NON-NLS-1$
	private static String TIMESTAMP_TITLE = WorkbenchMessages.getString("ResourceInfo.lastModified"); //$NON-NLS-1$

	//Max value width in characters before wrapping
	private static final int MAX_VALUE_WIDTH = 80;

/**
 * Create the group that shows the name, location, size and type.
 *
 * @param parent the composite the group will be created in
 * @param resource the resource the information is being taken from.
 * @return the composite for the group
 */
private Composite createBasicInfoGroup(Composite parent, IResource resource) {
	
	Font font = parent.getFont();

	Composite basicInfoComposite = new Composite(parent, SWT.NULL);
	GridLayout layout = new GridLayout();
	layout.numColumns = 2;
	layout.marginWidth = 0;
	layout.marginHeight = 0;
	basicInfoComposite.setLayout(layout);
	GridData data = new GridData();
	data.verticalAlignment = GridData.FILL;
	data.horizontalAlignment = GridData.FILL;
	basicInfoComposite.setLayoutData(data);
	basicInfoComposite.setFont(font);

	//The group for path
	Label pathLabel = new Label(basicInfoComposite, SWT.NONE);
	pathLabel.setText(PATH_TITLE);
	GridData gd = new GridData();
	gd.verticalAlignment = SWT.TOP;
	pathLabel.setLayoutData(gd);
	pathLabel.setFont(font);
	
	// path value label
	Text pathValueText = new Text(basicInfoComposite, SWT.WRAP | SWT.READ_ONLY);
	pathValueText.setText(resource.getFullPath().toString());
	gd = new GridData();
	gd.widthHint = convertWidthInCharsToPixels(MAX_VALUE_WIDTH);
	gd.grabExcessHorizontalSpace = true;
	gd.horizontalAlignment = gd.FILL;
	pathValueText.setLayoutData(gd);
	pathValueText.setFont(font);
	
	//The group for types
	Label typeTitle = new Label(basicInfoComposite, SWT.LEFT);
	typeTitle.setText(TYPE_TITLE);
	typeTitle.setFont(font);
	
	Text typeValue = new Text(basicInfoComposite, SWT.LEFT | SWT.READ_ONLY);
	typeValue.setText(getTypeString(resource));
	typeValue.setFont(font);

	//The group for location
	Label locationTitle = new Label(basicInfoComposite, SWT.LEFT);
	locationTitle.setText(LOCATION_TITLE);
	gd = new GridData();
	gd.verticalAlignment = SWT.TOP;
	locationTitle.setLayoutData(gd);
	locationTitle.setFont(font);
	
	
	Text locationValue = new Text(basicInfoComposite, SWT.WRAP | SWT.READ_ONLY);
	locationValue.setText(getLocationText(resource));
	gd = new GridData();
	gd.widthHint = convertWidthInCharsToPixels(MAX_VALUE_WIDTH);
	gd.grabExcessHorizontalSpace = true;
	gd.horizontalAlignment = gd.FILL;
	locationValue.setLayoutData(gd);
	locationValue.setFont(font);
	
	if (resource.getType() == IResource.FILE) {
		//The group for size
		Label sizeTitle = new Label(basicInfoComposite, SWT.LEFT);
		sizeTitle.setText(SIZE_TITLE);
		sizeTitle.setFont(font);
		
		Text sizeValue = new Text(basicInfoComposite, SWT.LEFT | SWT.READ_ONLY);
		sizeValue.setText(MessageFormat.format(BYTES_LABEL, new Object[] {getSizeString((IFile) resource)}));
		sizeValue.setFont(font);
	}

	return basicInfoComposite;
}
protected Control createContents(Composite parent) {

	WorkbenchHelp.setHelp(parent, IHelpContextIds.RESOURCE_INFO_PROPERTY_PAGE);

	// layout the page
	IResource resource = (IResource) getElement();
	this.readOnlyValue = resource.isReadOnly();
	if(resource.getType()!= IResource.PROJECT)
		this.derivedValue = resource.isDerived();

	// top level group
	Composite composite = new Composite(parent, SWT.NONE);
	GridLayout layout = new GridLayout();
	layout.marginWidth = 0;
	layout.marginHeight = 0;
	composite.setLayout(layout);
	GridData data = new GridData(GridData.FILL);
	data.grabExcessHorizontalSpace = true;
	composite.setLayoutData(data);
	composite.setFont(parent.getFont());

	createBasicInfoGroup(composite, resource);
	createSeparator(composite);
	createStateGroup(composite,resource);

	return composite;
}
/**
 * Create the isEditable button and it's associated label as a child of parent
 * using the editableValue of the receiver. The Composite will be the parent of
 * the button.
 */
private void createEditableButton(Composite composite) {
	
	this.editableBox = new Button(composite, SWT.CHECK | SWT.RIGHT);
	this.editableBox.setAlignment(SWT.LEFT);
	this.editableBox.setText(READ_ONLY);
	this.editableBox.setSelection(this.readOnlyValue);
	this.editableBox.addSelectionListener(new SelectionAdapter() {
		public void widgetSelected(SelectionEvent e) {
			readOnlyValue = editableBox.getSelection();
		}
	});
	this.editableBox.setFont(composite.getFont());
	GridData data = new GridData();
	data.horizontalSpan = 2;
	this.editableBox.setLayoutData(data);
}

/**
 * Create the derived button and it's associated label as a child of parent
 * using the derived of the receiver. The Composite will be the parent of
 * the button.
 */
private void createDerivedButton(Composite composite) {
	
	this.derivedBox = new Button(composite, SWT.CHECK | SWT.RIGHT);
	this.derivedBox.setAlignment(SWT.LEFT);
	this.derivedBox.setText(DERIVED);
	this.derivedBox.setSelection(this.derivedValue);
	this.derivedBox.addSelectionListener(new SelectionAdapter() {
		public void widgetSelected(SelectionEvent e) {
			derivedValue = editableBox.getSelection();
		}
	});
	
	this.derivedBox.setFont(composite.getFont());
	GridData data = new GridData();
	data.horizontalSpan = 2;
	this.derivedBox.setLayoutData(data);
}

/**
 * Create a separator that goes across the entire page
 */
private void createSeparator(Composite composite) {

	Label separator =
		new Label(composite, SWT.SEPARATOR | SWT.HORIZONTAL);
	GridData gridData = new GridData();
	gridData.horizontalAlignment = GridData.FILL;
	gridData.grabExcessHorizontalSpace = true;
	separator.setLayoutData(gridData);
}
/**
 * Create the group that shows the read only state and the timestamp.
 *
 * @return the composite for the group
 * @param parent the composite the group will be created in
 * @param resource the resource the information is being taken from.
 */
private void createStateGroup(Composite parent, IResource resource) {
	
	Font font = parent.getFont();

	Composite composite = new Composite(parent, SWT.NULL);
	GridLayout layout = new GridLayout();
	layout.numColumns = 2;
	layout.marginWidth = 0;
	layout.marginHeight = 0;
	composite.setLayout(layout);
	GridData data = new GridData();
	data.horizontalAlignment = GridData.FILL;
	composite.setLayoutData(data);
	composite.setFont(font);

	Label timeStampLabel = new Label(composite, SWT.NONE);
	timeStampLabel.setText(TIMESTAMP_TITLE);
	timeStampLabel.setFont(font);

	// path value label
	Text timeStampValue = new Text(composite, SWT.WRAP | SWT.READ_ONLY);
	timeStampValue.setText(getDateStringValue(resource));
	timeStampValue.setFont(font);

	createEditableButton(composite);
	
	//Not relevant to projects
	if(resource.getType() != IResource.PROJECT)
		createDerivedButton(composite);
}
/**
 * Return the value for the date String for the timestamp of the supplied resource.
 * @return String
 * @param IResource - the resource to query
 */
private String getDateStringValue(IResource resource) {

	if (!resource.isLocal(IResource.DEPTH_ZERO))
		return NOT_LOCAL_TEXT;
	else {
		File localFile = resource.getLocation().toFile();
		DateFormat format = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.MEDIUM);
		return format.format(new Date(localFile.lastModified()));
	}
}
/**
 * Get the location of a resource
 */
private String getLocationText(IResource resource) {
	//Location can be null if the resource is not local
	IPath location = resource.getLocation();
	if (location != null) {
		return location.toOSString();
	}
	else {
		return NOT_LOCAL_TEXT;
	}
}
/**
 * Return a String that indicates the size of the supplied file.
 */
private String getSizeString(IFile file) {
	if (!file.isLocal(IResource.DEPTH_ZERO))
		return NOT_LOCAL_TEXT;
	else {
		File localFile = file.getLocation().toFile();
		return Long.toString(localFile.length());
	}
}
/**
 * Get the string that identifies the type of this resource.
 */
private String getTypeString(IResource resource) {
	
	if(resource.getType() == resource.FILE)
		return FILE_LABEL;

	if(resource.getType() == resource.FOLDER)
		return FOLDER_LABEL;

	if(resource.getType() == resource.PROJECT)
		return PROJECT_LABEL;

	//Should not be possible
	return UNKNOWN_LABEL;
}
/**
 * Reset the editableBox to the false.
 */
protected void performDefaults() {

	this.readOnlyValue = false;
	this.editableBox.setSelection(this.readOnlyValue);
	
	//Nothing to update if we never made the box
	if(this.derivedBox != null){
		this.derivedValue = false;
		this.derivedBox.setSelection(this.derivedValue);
	}
}
/** 
 * Apply the read only state to the resource.
 */
public boolean performOk() {
	IResource resource = (IResource) getElement();
	resource.setReadOnly(editableBox.getSelection());
	
	//Nothing to update if we never made the box
	if(this.derivedBox != null){
		try{
			resource.setDerived(derivedBox.getSelection());
		}
		catch (CoreException exception){
			ErrorDialog.openError(
				getShell(), 
				WorkbenchMessages.getString("InternalError"), //$NON-NLS-1$
				exception.getLocalizedMessage(),
				exception.getStatus()); 
			return false;
		}
	}		
	return true;
}
}
