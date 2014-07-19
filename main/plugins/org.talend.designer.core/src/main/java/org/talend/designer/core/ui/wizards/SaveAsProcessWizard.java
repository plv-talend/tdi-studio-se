// ============================================================================
//
// Copyright (C) 2006-2014 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.designer.core.ui.wizards;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.part.EditorPart;
import org.osgi.framework.FrameworkUtil;
import org.talend.commons.exception.LoginException;
import org.talend.commons.exception.PersistenceException;
import org.talend.commons.ui.runtime.exception.ExceptionHandler;
import org.talend.commons.ui.runtime.exception.MessageBoxExceptionHandler;
import org.talend.commons.ui.runtime.image.ECoreImage;
import org.talend.commons.ui.runtime.image.ImageProvider;
import org.talend.core.CorePlugin;
import org.talend.core.model.process.IProcess2;
import org.talend.core.model.properties.ProcessItem;
import org.talend.core.model.properties.PropertiesFactory;
import org.talend.core.model.properties.Property;
import org.talend.core.model.relationship.RelationshipItemBuilder;
import org.talend.designer.core.DesignerPlugin;
import org.talend.designer.core.model.utils.emf.talendfile.ProcessType;
import org.talend.repository.RepositoryWorkUnit;
import org.talend.repository.editor.JobEditorInput;
import org.talend.repository.model.IProxyRepositoryFactory;
import org.talend.repository.model.IRepositoryService;
import org.talend.repository.model.RepositoryNode;

/**
 * Wizard for the creation of a new project. <br/>
 *
 * $Id: NewProcessWizard.java 46332 2010-08-05 06:48:56Z cli $
 *
 */
public class SaveAsProcessWizard extends Wizard {

    /** Main page. */
    private NewProcessWizardPage mainPage;

    /** Created project. */
    private ProcessItem processItem;

    private Property property;

    private IPath path;

    private IProxyRepositoryFactory repositoryFactory;

    private JobEditorInput jobEditorInput;

    private ProcessItem oldProcessItem;

    private Property oldProperty;

    private boolean isUpdate;

    public SaveAsProcessWizard(EditorPart editorPart) {

        this.jobEditorInput = (JobEditorInput) editorPart.getEditorInput();

        RepositoryNode repositoryNode = jobEditorInput.getRepositoryNode();
        // see: RepositoryEditorInput.setRepositoryNode(IRepositoryNode repositoryNode)
        if (repositoryNode == null) {
            repositoryNode = (RepositoryNode) CorePlugin.getDefault().getRepositoryService()
                    .getRepositoryNode(jobEditorInput.getItem().getProperty().getId(), false);
        }

        IRepositoryService service = DesignerPlugin.getDefault().getRepositoryService();
        this.path = service.getRepositoryPath(repositoryNode);

        this.oldProcessItem = (ProcessItem) jobEditorInput.getItem();
        oldProperty = this.oldProcessItem.getProperty();

        this.property = PropertiesFactory.eINSTANCE.createProperty();

        assginValues(this.property, oldProperty);

        processItem = PropertiesFactory.eINSTANCE.createProcessItem();

        processItem.setProperty(property);

        repositoryFactory = service.getProxyRepositoryFactory();

        setDefaultPageImageDescriptor(ImageProvider.getImageDesc(ECoreImage.PROCESS_WIZ));
    }

    @Override
    public void addPages() {
        mainPage = new NewProcessWizardPage(property, path);
        mainPage.initializeSaveAs(oldProperty.getLabel(), oldProperty.getVersion(), true);

        // overwrite it.
        mainPage.setTitle("Save As");
        mainPage.setDescription("Save as another new job.");

        addPage(mainPage);
        setWindowTitle("Save As");
    }

    @Override
    public boolean performFinish() {

        boolean ok = false;
        try {

            IProcess2 loadedProcess = jobEditorInput.getLoadedProcess();
            ProcessType processType = loadedProcess.saveXmlFile();

            isUpdate = isUpdate();

            if (isUpdate) {
                update(processType);
            } else {
                processItem.setProcess(processType);
                property.setId(repositoryFactory.getNextId());
                // don't need to add depended routines.

                repositoryFactory.create(processItem, mainPage.getDestinationPath());
                RelationshipItemBuilder.getInstance().addOrUpdateItem(processItem);
            }
            ok = true;

        } catch (Exception e) {
            MessageDialog.openError(getShell(), "Error", "Job could not be saved" + " : " + e.getMessage());
            ExceptionHandler.process(e);
        }

        return ok;
    }

    private void update(final ProcessType processType) {
        RepositoryWorkUnit<Object> rwu = new RepositoryWorkUnit<Object>("Save job") {

            @Override
            protected void run() throws LoginException, PersistenceException {
                IWorkspaceRunnable runnable = new IWorkspaceRunnable() {

                    @Override
                    public void run(final IProgressMonitor monitor) throws CoreException {
                        try {

                            oldProcessItem.setProcess(processType);

                            assginValues(oldProperty, property);
                            RelationshipItemBuilder.getInstance().addOrUpdateItem(oldProcessItem);

                            repositoryFactory.save(oldProcessItem);

                            // assign value
                            processItem = oldProcessItem;
                        } catch (PersistenceException pe) {
                            throw new CoreException(new Status(IStatus.ERROR, FrameworkUtil.getBundle(this.getClass())
                                    .getSymbolicName(), "persistance error", pe)); //$NON-NLS-1$
                        }
                    }
                };
                IWorkspace workspace = ResourcesPlugin.getWorkspace();
                try {
                    ISchedulingRule schedulingRule = workspace.getRoot();
                    // the update the project files need to be done in the workspace runnable to avoid all notification
                    // of changes before the end of the modifications.
                    workspace.run(runnable, schedulingRule, IWorkspace.AVOID_UPDATE, null);
                } catch (CoreException e) {
                    MessageBoxExceptionHandler.process(e.getCause());
                }
            }
        };
        rwu.setAvoidUnloadResources(true);
        rwu.setAvoidSvnUpdate(true);
        repositoryFactory.executeRepositoryWorkUnit(rwu);
    }

    public ProcessItem getProcess() {
        return this.processItem;
    }

    public boolean isUpdateOperation() {
        return this.isUpdate;
    }

    // if name is different, it will create a new job, if name is the same, means to update the job(version or
    // description...)
    private boolean isUpdate() {
        if (oldProperty.getLabel().trim().equalsIgnoreCase(property.getLabel().trim())) {
            return true;
        } else {
            return false;
        }
    }

    // left = right
    private void assginValues(Property leftProperty, Property rightProperty) {
        // 6 fields, don't contains the "locker" and "path". and author , they are the same.
        leftProperty.setLabel(rightProperty.getLabel());
        leftProperty.setPurpose(rightProperty.getPurpose());
        leftProperty.setDescription(rightProperty.getDescription());
        // same author as old one.
        leftProperty.setAuthor(rightProperty.getAuthor());
        leftProperty.setVersion(rightProperty.getVersion());
        leftProperty.setStatusCode(rightProperty.getStatusCode());
    }
}
