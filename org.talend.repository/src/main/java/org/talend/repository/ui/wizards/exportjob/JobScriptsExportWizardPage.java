// ============================================================================
//
// Copyright (C) 2006-2009 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.repository.ui.wizards.exportjob;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.dialogs.EventLoopProgressMonitor;
import org.eclipse.ui.internal.wizards.datatransfer.DataTransferMessages;
import org.eclipse.ui.internal.wizards.datatransfer.WizardFileSystemResourceExportPage1;
import org.eclipse.ui.progress.IProgressService;
import org.talend.commons.exception.ExceptionHandler;
import org.talend.commons.exception.PersistenceException;
import org.talend.core.CorePlugin;
import org.talend.core.context.Context;
import org.talend.core.context.RepositoryContext;
import org.talend.core.language.ECodeLanguage;
import org.talend.core.language.LanguageManager;
import org.talend.core.model.properties.ProcessItem;
import org.talend.core.model.repository.ERepositoryObjectType;
import org.talend.core.model.repository.IRepositoryObject;
import org.talend.core.model.repository.RepositoryManager;
import org.talend.designer.core.model.utils.emf.talendfile.impl.ProcessTypeImpl;
import org.talend.designer.runprocess.IProcessor;
import org.talend.designer.runprocess.ItemCacheManager;
import org.talend.designer.runprocess.JobInfo;
import org.talend.designer.runprocess.ProcessorException;
import org.talend.designer.runprocess.ProcessorUtilities;
import org.talend.repository.documentation.ArchiveFileExportOperationFullPath;
import org.talend.repository.documentation.ExportFileResource;
import org.talend.repository.documentation.FileSystemExporterFullPath;
import org.talend.repository.i18n.Messages;
import org.talend.repository.job.deletion.JobResource;
import org.talend.repository.job.deletion.JobResourceManager;
import org.talend.repository.model.ProxyRepositoryFactory;
import org.talend.repository.model.RepositoryNode;
import org.talend.repository.model.RepositoryNode.ENodeType;
import org.talend.repository.model.RepositoryNode.EProperties;
import org.talend.repository.ui.utils.ZipToFile;
import org.talend.repository.ui.wizards.exportjob.scriptsmanager.JobScriptsManager;
import org.talend.repository.ui.wizards.exportjob.scriptsmanager.JobScriptsManager.ExportChoice;
import org.talend.repository.utils.JobVersionUtils;

/**
 * Page of the Job Scripts Export Wizard. <br/>
 * 
 * @referto WizardArchiveFileResourceExportPage1 $Id: JobScriptsExportWizardPage.java 1 2006-12-13 下午03:09:07 bqian
 * 
 */
public abstract class JobScriptsExportWizardPage extends WizardFileSystemResourceExportPage1 {

    protected static final String DESTINATION_FILE = "destinationFile";//$NON-NLS-1$

    protected static final String ESB_EXPORT_TYPE = "esbExportType";//$NON-NLS-1$

    protected static final String ESB_SERVICE_NAME = "serviceName";//$NON-NLS-1$

    protected static final String ESB_CATEGORY = "category";//$NON-NLS-1$

    protected static final String QUERY_MESSAGE_NAME = "queryMessageName";//$NON-NLS-1$

    // widgets
    protected Button shellLauncherButton;

    protected Button systemRoutineButton;

    protected Button userRoutineButton;

    protected Button modelButton;

    protected Button jobItemButton;

    protected Button contextButton;

    protected Button jobScriptButton;

    protected ExportFileResource[] process;

    protected Combo contextCombo;

    protected Combo launcherCombo;

    protected JobScriptsManager manager;

    private IWorkspace workspace;

    protected Button applyToChildrenButton;

    private RepositoryNode[] nodes;

    protected String zipOption;

    protected Button chkButton;

    private String allVersions = "all"; //$NON-NLS-1$

    private String outputFileSuffix = ".zip"; //$NON-NLS-1$

    private String selectedJobVersion;

    private String originalRootFolderName;

    protected Button exportDependencies;

    boolean ok;

    private IStructuredSelection selection;

    private ExportTreeViewer treeViewer;

    /**
     * Create an instance of this class.
     * 
     * @param name java.lang.String
     */
    public JobScriptsExportWizardPage(String name, IStructuredSelection selection) {
        super(name, null);
        ProcessTypeImpl.lazyBool = true;
        this.selection = selection;
        manager = createJobScriptsManager();
        nodes = (RepositoryNode[]) selection.toList().toArray(new RepositoryNode[selection.size()]);
        setNodes(nodes);
    }

    protected RepositoryNode[] getCheckNodes() {
        return treeViewer.getCheckNodes();
    }

    private void setNodes(RepositoryNode[] nodes) {

        List<ExportFileResource> list = new ArrayList<ExportFileResource>();
        int nodeSize = nodes.length;
        if (nodeSize > 1) {
            manager.setMultiNodes(true);
        }
        for (int i = 0; i < nodeSize; i++) {
            RepositoryNode node = nodes[i];
            if (node.getType() == ENodeType.SYSTEM_FOLDER || node.getType() == ENodeType.SIMPLE_FOLDER) {
                addTreeNode(node, node.getProperties(EProperties.LABEL).toString(), list);
            }
            if (node.getType() == ENodeType.REPOSITORY_ELEMENT) {
                IRepositoryObject repositoryObject = node.getObject();
                if (repositoryObject.getProperty().getItem() instanceof ProcessItem) {
                    ProcessItem processItem = (ProcessItem) repositoryObject.getProperty().getItem();
                    ExportFileResource resource = new ExportFileResource(processItem, processItem.getProperty().getLabel());
                    resource.setNode(node);
                    list.add(resource);
                }
            }
        }
        process = list.toArray(new ExportFileResource[list.size()]);
    }

    private void addTreeNode(RepositoryNode node, String path, List<ExportFileResource> list) {
        if (node != null && node.getType() == ENodeType.REPOSITORY_ELEMENT) {
            IRepositoryObject repositoryObject = node.getObject();
            if (repositoryObject.getProperty().getItem() instanceof ProcessItem) {
                ProcessItem processItem = (ProcessItem) repositoryObject.getProperty().getItem();
                ExportFileResource resource = new ExportFileResource(processItem, path);
                resource.setNode(node);
                list.add(resource);
            }
        }
        Object[] nodes = node.getChildren().toArray();
        if (nodes.length <= 0) {
            return;
        }
        for (int i = 0; i < nodes.length; i++) {
            addTreeNode((RepositoryNode) nodes[i], path + "/" //$NON-NLS-1$
                    + ((RepositoryNode) nodes[i]).getProperties(EProperties.LABEL).toString(), list);
        }
    }

    public abstract JobScriptsManager createJobScriptsManager();

    /**
     * Create an instance of this class.
     * 
     * @param selection the selection
     */
    public JobScriptsExportWizardPage(IStructuredSelection selection) {
        this("jobscriptsExportPage1", selection); //$NON-NLS-1$
        setDescription(Messages.getString("JobScriptsExportWizardPage.ExportJob")); //$NON-NLS-1$
        setTitle(DataTransferMessages.ArchiveExport_exportTitle);
    }

    /**
     * yzhang Comment method "setDefaultDestination".
     */
    protected void setDefaultDestination() {

        String userDir = System.getProperty("user.dir"); //$NON-NLS-1$
        IPath path = new Path(userDir);
        int length = nodes.length;
        String destinationFile = "";
        if (getDialogSettings() != null) {
            IDialogSettings section = getDialogSettings().getSection(DESTINATION_FILE);
            if (section != null) {
                destinationFile = section.get(DESTINATION_FILE);
            }
        }
        if (destinationFile == null || "".equals(destinationFile)) {
            if (length == 1) {
                // TODOthis is changed by shenhaize first open ,it show contains in the combo
                path = path.append(this.getDefaultFileName().get(0) + "_" + this.getDefaultFileName().get(1) + getOutputSuffix()); //$NON-NLS-1$
            } else if (length > 1) {
                // i changed here ..
                path = path.append(this.getDefaultFileName().get(0)
                        + "_" + this.getDefaultFileName().get(1) + this.outputFileSuffix); //$NON-NLS-1$
            }
        } else {
            path = new Path(destinationFile);
        }

        setDestinationValue(path.toOSString());
    }

    /**
     * yzhang Comment method "getDefaultFileName".
     */
    // protected String getDefaultFileVersion() {
    // if (nodes.length >= 1) {
    // String label = null;
    // String version = null;
    // RepositoryNode node = nodes[0];
    // if (node.getType() == ENodeType.SYSTEM_FOLDER || node.getType() == ENodeType.SIMPLE_FOLDER) {
    // label = node.getProperties(EProperties.LABEL).toString();
    // } else if (node.getType() == ENodeType.REPOSITORY_ELEMENT) {
    // IRepositoryObject repositoryObject = node.getObject();
    // if (repositoryObject.getProperty().getItem() instanceof ProcessItem) {
    // ProcessItem processItem = (ProcessItem) repositoryObject.getProperty().getItem();
    // label = processItem.getProperty().getLabel();
    // System.out.println(label);
    // version = processItem.getProperty().getVersion();
    // }
    // }
    //
    // return label;
    // }
    // return "";
    //
    // }
    protected List getDefaultFileName() {
        List list = null;
        if (nodes.length >= 1) {
            String label = null;
            String version = null;
            RepositoryNode node = nodes[0];
            if (node.getType() == ENodeType.SYSTEM_FOLDER || node.getType() == ENodeType.SIMPLE_FOLDER) {
                label = node.getProperties(EProperties.LABEL).toString();
            } else if (node.getType() == ENodeType.REPOSITORY_ELEMENT) {
                IRepositoryObject repositoryObject = node.getObject();
                if (repositoryObject.getProperty().getItem() instanceof ProcessItem) {
                    ProcessItem processItem = (ProcessItem) repositoryObject.getProperty().getItem();
                    label = processItem.getProperty().getLabel();
                    version = processItem.getProperty().getVersion();
                    list = new ArrayList();
                    list.add(label);
                    list.add(version);
                }
            }

            // return label;
            return list;
        }
        return null;

    }

    /**
     * (non-Javadoc) Method declared on IDialogPage.
     */
    /**
     * (non-Javadoc) Method declared on IDialogPage.
     */
    public void createControl(Composite parent) {

        initializeDialogUnits(parent);
        SashForm sash = createExportTree(parent);

        GridLayout layout = new GridLayout();
        layout.verticalSpacing = 0;
        layout.marginHeight = 5;
        layout.marginBottom = 0;
        Composite composite = new Composite(sash, SWT.BORDER);
        composite.setLayout(layout);
        composite.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL));
        composite.setFont(parent.getFont());

        createDestinationGroup(composite);
        if (!isMultiNodes()) {
            createJobVersionGroup(composite);
        }

        createUnzipOptionGroup(composite);
        createOptionsGroup(composite);

        restoreResourceSpecificationWidgetValues(); // ie.- local
        restoreWidgetValues(); // ie.- subclass hook

        updateWidgetEnablements();
        setPageComplete(determinePageCompletion());
        setErrorMessage(null); // should not initially have error message

        setControl(sash);
        sash.setWeights(new int[] { 0, 2, 23 });
        giveFocusToDestination();

    }

    ICheckStateListener checkStateListener = new ICheckStateListener() {

        public void checkStateChanged(CheckStateChangedEvent event) {
            checkExport();
        }

    };

    public void checkExport() {

    }

    protected SashForm createExportTree(Composite parent) {
        treeViewer = new ExportTreeViewer(selection);
        SashForm sashForm = treeViewer.createContents(parent);
        treeViewer.addCheckStateListener(checkStateListener);
        return sashForm;
    }

    /**
     * ftang Comment method "createJobVersionGroup".
     * 
     * @param composite
     */
    protected void createJobVersionGroup(Composite parent) {
        Group versionGroup = new Group(parent, SWT.NONE);
        GridLayout layout = new GridLayout();
        versionGroup.setLayout(layout);
        versionGroup.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.GRAB_HORIZONTAL));
        versionGroup.setText(Messages.getString("JobScriptsExportWSWizardPage.JobVersion")); //$NON-NLS-1$
        versionGroup.setFont(parent.getFont());

        versionGroup.setLayout(new GridLayout(1, true));

        Composite left = new Composite(versionGroup, SWT.NONE);
        left.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, true, false));
        left.setLayout(new GridLayout(3, false));

        Label label = new Label(left, SWT.NONE);
        label.setText(Messages.getString("JobScriptsExportWSWizardPage.JobVersion.Label")); //$NON-NLS-1$

        final Combo versionCombo = new Combo(left, SWT.PUSH);
        GridData gd = new GridData();
        gd.horizontalSpan = 1;
        versionCombo.setLayoutData(gd);

        String[] allVersions = JobVersionUtils.getAllVersions(nodes[0]);
        String currentVersion = JobVersionUtils.getCurrentVersion(nodes[0]);
        versionCombo.setItems(allVersions);
        if (allVersions.length > 1) {
            versionCombo.add(this.allVersions);
        }
        versionCombo.setText(currentVersion);
        selectedJobVersion = currentVersion;
        versionCombo.addSelectionListener(new SelectionListener() {

            public void widgetSelected(SelectionEvent e) {
                selectedJobVersion = versionCombo.getText();
            }

            public void widgetDefaultSelected(SelectionEvent e) {
                widgetSelected(e);
            }

        });
    }

    protected void createUnzipOptionGroup(Composite parent) {
        // options group
        Group optionsGroup = new Group(parent, SWT.NONE);
        GridLayout layout = new GridLayout();
        optionsGroup.setLayout(layout);
        optionsGroup.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.GRAB_HORIZONTAL));
        optionsGroup.setText("Extract zip file"); //$NON-NLS-1$
        optionsGroup.setFont(parent.getFont());
        optionsGroup.setLayout(new GridLayout(1, true));
        Composite left = new Composite(optionsGroup, SWT.NONE);
        left.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, true, false));
        left.setLayout(new GridLayout(3, false));
        chkButton = new Button(left, SWT.CHECK);
        chkButton.setText(Messages.getString("JobScriptsExportWizardPage.extractZipFile")); //$NON-NLS-1$
        chkButton.setSelection(false);
        chkButton.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent e) {
                chkButton.setSelection(chkButton.getSelection());
                zipOption = String.valueOf(chkButton.getSelection());
            }
        });
    }

    /*
     * It's not a good method to resovle the problem of null pointer, which is led by commenting the //
     * createResourcesGroup(composite); and createButtonsGroup(composite); (non-Javadoc)
     * 
     * @see org.eclipse.ui.internal.wizards.datatransfer.WizardFileSystemResourceExportPage1#validateSourceGroup()
     */
    public boolean validateSourceGroup() {
        return true;
    }

    /**
     * Create the export options specification widgets.
     * 
     */
    public void createOptionsGroupButtons(Group optionsGroup) {
        Font font = optionsGroup.getFont();
        optionsGroup.setLayout(new GridLayout(1, true));

        Composite left = new Composite(optionsGroup, SWT.NONE);
        left.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, true, false));
        left.setLayout(new GridLayout(3, true));

        createOptions(left, font);

        // Composite right = new Composite(optionsGroup, SWT.NONE);
        // right.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, true, false));
        // right.setLayout(new GridLayout(1, true));
    }

    /**
     * Create the buttons for the group that determine if the entire or selected directory structure should be created.
     * 
     * @param optionsGroup
     * @param font
     */
    public void createOptions(final Composite optionsGroup, Font font) {
        // create directory structure radios
        shellLauncherButton = new Button(optionsGroup, SWT.CHECK | SWT.LEFT);
        shellLauncherButton.setText(Messages.getString("JobScriptsExportWizardPage.shellLauncher")); //$NON-NLS-1$
        shellLauncherButton.setSelection(true);
        shellLauncherButton.setFont(font);

        launcherCombo = new Combo(optionsGroup, SWT.PUSH);
        GridData gd = new GridData();
        gd.horizontalSpan = 2;
        launcherCombo.setLayoutData(gd);
        // laucherText = new Text(optionsGroup, SWT.BORDER);
        // laucherText.setEditable(false);

        // create directory structure radios
        systemRoutineButton = new Button(optionsGroup, SWT.CHECK | SWT.LEFT);
        systemRoutineButton.setText(Messages.getString("JobScriptsExportWizardPage.systemRoutines")); //$NON-NLS-1$
        systemRoutineButton.setSelection(true);
        systemRoutineButton.setFont(font);

        userRoutineButton = new Button(optionsGroup, SWT.CHECK | SWT.LEFT);
        userRoutineButton.setText(Messages.getString("JobScriptsExportWizardPage.userRoutines")); //$NON-NLS-1$
        userRoutineButton.setSelection(true);
        userRoutineButton.setFont(font);
        gd = new GridData();
        gd.horizontalSpan = 2;
        userRoutineButton.setLayoutData(gd);

        modelButton = new Button(optionsGroup, SWT.CHECK | SWT.LEFT);
        modelButton.setText(Messages.getString("JobScriptsExportWizardPage.requiredTalendPerlModules")); //$NON-NLS-1$
        modelButton.setSelection(true);
        modelButton.setFont(font);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 3;
        modelButton.setLayoutData(gd);

        jobScriptButton = new Button(optionsGroup, SWT.CHECK | SWT.LEFT);
        jobScriptButton.setText(Messages.getString("JobScriptsExportWizardPage.jobPerlScripts")); //$NON-NLS-1$
        jobScriptButton.setSelection(true);
        jobScriptButton.setFont(font);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 3;
        jobScriptButton.setLayoutData(gd);

        jobItemButton = new Button(optionsGroup, SWT.CHECK | SWT.LEFT);
        jobItemButton.setText(Messages.getString("JobScriptsExportWizardPage.sourceFiles")); //$NON-NLS-1$
        jobItemButton.setSelection(true);
        jobItemButton.setFont(font);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 1;
        jobItemButton.setLayoutData(gd);

        exportDependencies = new Button(optionsGroup, SWT.CHECK);
        exportDependencies.setText("Export Dependencies"); //$NON-NLS-1$
        exportDependencies.setFont(font);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 2;
        jobItemButton.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent e) {

                exportDependencies.setEnabled(jobItemButton.getSelection());
                if (!jobItemButton.getSelection()) {
                    exportDependencies.setSelection(false);
                }
            }
        });
        exportDependencies.setLayoutData(gd);

        contextButton = new Button(optionsGroup, SWT.CHECK | SWT.LEFT);
        contextButton.setText(Messages.getString("JobScriptsExportWizardPage.contextPerlScripts")); //$NON-NLS-1$
        contextButton.setSelection(true);
        contextButton.setFont(font);

        contextCombo = new Combo(optionsGroup, SWT.PUSH);

        applyToChildrenButton = new Button(optionsGroup, SWT.CHECK | SWT.LEFT);
        applyToChildrenButton.setText(Messages.getString("JobScriptsExportWizardPage.ApplyToChildren")); //$NON-NLS-1$
        // genCodeButton = new Button(optionsGroup, SWT.CHECK | SWT.LEFT);
        // genCodeButton.setText(Messages.getString("JobScriptsExportWizardPage.generatePerlFiles")); //$NON-NLS-1$
        // genCodeButton.setSelection(true);
        // genCodeButton.setFont(font);
    }

    /**
     * Returns a boolean indicating whether the directory portion of the passed pathname is valid and available for use.
     */
    protected boolean ensureTargetDirectoryIsValid(String fullPathname) {
        int separatorIndex = fullPathname.lastIndexOf(File.separator);

        if (separatorIndex == -1) {
            return true;
        }

        return ensureTargetIsValid(new File(fullPathname.substring(0, separatorIndex)));
    }

    /**
     * Returns a boolean indicating whether the passed File handle is is valid and available for use.
     */
    protected boolean ensureTargetFileIsValid(File targetFile) {
        if (targetFile.exists() && targetFile.isDirectory()) {
            displayErrorDialog(DataTransferMessages.ZipExport_mustBeFile);
            giveFocusToDestination();
            return false;
        }

        if (targetFile.exists()) {
            if (targetFile.canWrite()) {
                if (!queryYesNoQuestion(DataTransferMessages.ZipExport_alreadyExists)) {
                    return false;
                }
            } else {
                displayErrorDialog(DataTransferMessages.ZipExport_alreadyExistsError);
                giveFocusToDestination();
                return false;
            }
        }

        return true;
    }

    /**
     * Ensures that the target output file and its containing directory are both valid and able to be used. Answer a
     * boolean indicating validity.
     */
    protected boolean ensureTargetIsValid() {
        String targetPath = getDestinationValue();
        if (this.selectedJobVersion != null && this.selectedJobVersion.equals(this.allVersions)) {

            if (this.originalRootFolderName == null) {
                this.originalRootFolderName = getRootFolderName();
            }
            String newFileName = this.originalRootFolderName + manager.getSelectedJobVersion() + outputFileSuffix;
            targetPath = targetPath.substring(0, targetPath.lastIndexOf(File.separator) + 1) + newFileName;
            setDestinationValue(targetPath);
        }

        if (!ensureTargetDirectoryIsValid(targetPath)) {
            return false;
        }

        if (!ensureTargetFileIsValid(new File(targetPath))) {
            return false;
        }

        return true;
    }

    /**
     * Export the passed resource and recursively export all of its child resources (iff it's a container). Answer a
     * boolean indicating success.
     */
    protected boolean executeExportOperation(ArchiveFileExportOperationFullPath op) {
        op.setCreateLeadupStructure(true);
        op.setUseCompression(true);

        try {
            getContainer().run(true, true, op);
        } catch (InvocationTargetException e) {
            ExceptionHandler.process(e);
        } catch (InterruptedException e) {
            ExceptionHandler.process(e);

        }

        IStatus status = op.getStatus();
        if (!status.isOK()) {
            ErrorDialog.openError(getContainer().getShell(), "", null, // no //$NON-NLS-1$
                    // special
                    // message
                    status);
            return false;
        }

        return true;
    }

    /**
     * The Finish button was pressed. Try to do the required work now and answer a boolean indicating success. If false
     * is returned then the wizard will not close.
     * 
     * @returns boolean
     */
    public boolean finish() {
        if (treeViewer != null) {
            treeViewer.removeCheckStateListener(checkStateListener);
            // achen added
            if (getCheckNodes() != null) {
                setNodes(getCheckNodes());
            }
        }

        manager = createJobScriptsManager();
        manager.setMultiNodes(isMultiNodes());
        // achen modify to fix bug 0006222
        IRunnableWithProgress worker = new IRunnableWithProgress() {

            public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                final EventLoopProgressMonitor progressMonitor = new EventLoopProgressMonitor(monitor);

                progressMonitor.beginTask(
                        Messages.getString("JobScriptsExportWizardPage.exportJobScript"), IProgressMonitor.UNKNOWN); //$NON-NLS-1$
                if (selectedJobVersion != null && selectedJobVersion.equals(allVersions)) {
                    String[] allVersions = JobVersionUtils.getAllVersions(nodes[0]);
                    for (String version : allVersions) {
                        monitor
                                .subTask(Messages
                                        .getString("JobScriptsExportWizardPage.exportJob0", nodes[0].getLabel(), version)); //$NON-NLS-1$
                        ok = exportJobScript(version, progressMonitor);
                        if (!ok) {
                            return;
                        }
                    }
                } else {
                    monitor.subTask(Messages.getString(
                            "JobScriptsExportWizardPage.exportJob1", nodes[0].getLabel(), selectedJobVersion)); //$NON-NLS-1$
                    ok = exportJobScript(selectedJobVersion, progressMonitor);
                }
                monitor.subTask(Messages.getString(
                        "JobScriptsExportWizardPage.exportJobSucessful", nodes[0].getLabel(), selectedJobVersion)); //$NON-NLS-1$
                progressMonitor.done();
            }
        };
        IProgressService progressService = PlatformUI.getWorkbench().getProgressService();
        try {
            progressService.run(false, true, worker);
        } catch (InvocationTargetException e) {
            ExceptionHandler.process(e);
        } catch (InterruptedException e) {
            ExceptionHandler.process(e);
        }
        // end
        try {
            ProxyRepositoryFactory.getInstance().initialize();
            RepositoryManager.refresh(ERepositoryObjectType.PROCESS);
        } catch (PersistenceException e) {
            e.printStackTrace();
        }
        return ok;
    }

    /**
     * ftang Comment method "exportJobScript".
     * 
     * @return
     */
    private boolean exportJobScript(String version, IProgressMonitor monitor) {
        manager.setJobVersion(version);
        // monitor.subTask("Init export choices...");
        Map<ExportChoice, Object> exportChoiceMap = getExportChoiceMap();
        boolean canExport = false;
        for (ExportChoice choice : ExportChoice.values()) {
            // if (choice.equals(ExportChoice.needGenerateCode)) {
            // continue;
            // }
            if (exportChoiceMap.get(choice) != null && exportChoiceMap.get(choice) instanceof Boolean
                    && (Boolean) exportChoiceMap.get(choice)) {
                canExport = true;
                break;
            }
        }
        if (!canExport) {
            MessageDialog.openInformation(getContainer().getShell(), Messages
                    .getString("JobScriptsExportWizardPage.exportResourceError"), //$NON-NLS-1$
                    Messages.getString("JobScriptsExportWizardPage.chooseResource")); //$NON-NLS-1$
            return false;
        }

        if (!ensureTargetIsValid()) {
            return false;
        }
        // String topFolder = getRootFolderName();

        boolean isNotFirstTime = this.originalRootFolderName != null;
        if (isNotFirstTime && process[0] != null) {
            process[0].setDirectoryName(this.originalRootFolderName);

        }

        if (!isMultiNodes()) {
            for (int i = 0; i <= process.length - 1; i++) {
                process[i].removeAllMap();
                ProcessItem processItem = (ProcessItem) process[i].getItem();
                processItem = ItemCacheManager.getProcessItem(processItem.getProperty().getId(), version);
                // update with the correct version.
                process[i].setProcess(processItem);
            }

        }

        manager.setProgressMonitor(monitor);
        List<ExportFileResource> resourcesToExport = getExportResources();

        if (isNotFirstTime) {
            setTopFolder(resourcesToExport, this.originalRootFolderName);
        } else {
            setTopFolder(resourcesToExport, this.getRootFolderName());
        }

        // Save dirty editors if possible but do not stop if not all are saved
        saveDirtyEditors();
        // about to invoke the operation so save our state
        saveWidgetValues();
        // boolean ok =executeExportOperation(new ArchiveFileExportOperationFullPath(process));
        ArchiveFileExportOperationFullPath exporterOperation = getExporterOperation(resourcesToExport);

        ok = executeExportOperation(exporterOperation);

        // path can like name/name
        manager.deleteTempFiles();
        ProcessorUtilities.resetExportConfig();

        String projectName = ((RepositoryContext) CorePlugin.getContext().getProperty(Context.REPOSITORY_CONTEXT_KEY))
                .getProject().getLabel();

        List<JobResource> jobResources = new ArrayList<JobResource>();

        for (int i = 0; i < process.length; i++) {
            ProcessItem processItem = (ProcessItem) process[i].getItem();
            JobInfo jobInfo = new JobInfo(processItem, processItem.getProcess().getDefaultContext(), version);
            jobResources.add(new JobResource(projectName, jobInfo));

            Set<JobInfo> jobInfos = ProcessorUtilities.getChildrenJobInfo(processItem);
            for (JobInfo subjobInfo : jobInfos) {
                jobResources.add(new JobResource(projectName, subjobInfo));
            }
        }

        JobResourceManager reManager = JobResourceManager.getInstance();
        for (JobResource r : jobResources) {
            if (reManager.isProtected(r)) {
                try {
                    ProcessorUtilities.generateCode(r.getJobInfo().getJobId(), r.getJobInfo().getContextName(), r.getJobInfo()
                            .getJobVersion(), false, false, monitor);
                } catch (ProcessorException e) {
                    ExceptionHandler.process(e);
                }
            }
            // else {
            // try {
            // reManager.deleteResource(r);
            // } catch (Exception e) {
            // ExceptionHandler.process(e);
            // }
            // }
        }
        monitor.subTask(Messages.getString("JobScriptsExportWizardPage.exportSuccess")); //$NON-NLS-1$
        // achen modify to fix bug 0006108
        // rearchieve the jobscript zip file
        ECodeLanguage curLanguage = LanguageManager.getCurrentLanguage();
        if (curLanguage == ECodeLanguage.JAVA) {
            reBuildJobZipFile();
        }
        // see bug 7181
        if (zipOption != null && zipOption.equals("true")) { //$NON-NLS-1$
            // unzip
            try {
                String zipFile = getDestinationValue();
                ZipToFile.unZipFile(getDestinationValue(), new File(zipFile).getParentFile().getAbsolutePath());
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        return ok;
    }

    /**
     * 
     * DOC aiming Comment method "reBuildJobZipFile".
     */
    private void reBuildJobZipFile() {
        JavaJobExportReArchieveCreator creator = null;
        String zipFile = getDestinationValue();

        String tmpFolder = JavaJobExportReArchieveCreator.getTmpFolder();
        try {
            // unzip to tmpFolder
            ZipToFile.unZipFile(zipFile, tmpFolder);
            // build new jar
            for (int i = 0; i < process.length; i++) {
                if (process[i] != null) {
                    String jobFolderName = process[i].getDirectoryName();
                    int pos = jobFolderName.indexOf("/"); //$NON-NLS-1$
                    if (pos != -1) {
                        jobFolderName = jobFolderName.substring(pos + 1);
                    }
                    if (creator == null) {
                        creator = new JavaJobExportReArchieveCreator(zipFile, jobFolderName);
                    } else {
                        creator.setJobFolerName(jobFolderName);
                    }
                    creator.buildNewJar();
                }
            }
            // rezip the tmpFolder to zipFile
            ZipToFile.zipFile(tmpFolder, zipFile);
        } catch (Exception e) {
            ExceptionHandler.process(e);
        } finally {
            JavaJobExportReArchieveCreator.deleteTempFiles();
        }
    }

    /**
     * Get the export operation.
     * 
     * @param resourcesToExport
     * @return
     */
    public ArchiveFileExportOperationFullPath getExporterOperation(List<ExportFileResource> resourcesToExport) {
        ArchiveFileExportOperationFullPath exporterOperation = new ArchiveFileExportOperationFullPath(resourcesToExport,
                getDestinationValue());

        return exporterOperation;
    }

    /**
     * Get the export operation.
     * 
     * @param resourcesToExport
     * @return
     */
    public FileSystemExporterFullPath getUnzipExporterOperation(List<ExportFileResource> resourcesToExport) {
        String currentUnzipFile = getDestinationValue().replace("/", File.separator); //$NON-NLS-1$ //$NON-NLS-2$
        currentUnzipFile = currentUnzipFile.substring(0, currentUnzipFile.lastIndexOf(File.separator)); //$NON-NLS-1$
        FileSystemExporterFullPath exporterOperation = null;
        try {
            exporterOperation = new FileSystemExporterFullPath(resourcesToExport, currentUnzipFile);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            // e.printStackTrace();
            ExceptionHandler.process(e);
        }
        return exporterOperation;
    }

    /**
     * Returns the root folder name.
     * 
     * @return
     */
    private String getRootFolderName() {
        IPath path = new Path(this.getDestinationValue());
        String subjectString = path.lastSegment();
        Pattern regex = Pattern.compile("(.*)(?=(\\.(tar|zip))\\b)", Pattern.CANON_EQ | Pattern.CASE_INSENSITIVE //$NON-NLS-1$
                | Pattern.UNICODE_CASE);
        Matcher regexMatcher = regex.matcher(subjectString);
        if (regexMatcher.find()) {
            subjectString = regexMatcher.group(0);
        }
        return subjectString.trim();
    }

    /**
     * Comment method "setTopFolder".
     * 
     * @param resourcesToExport
     * @param topFolder
     */
    public void setTopFolder(List<ExportFileResource> resourcesToExport, String topFolder) {
        for (ExportFileResource fileResource : resourcesToExport) {
            String directory = fileResource.getDirectoryName();
            fileResource.setDirectoryName(topFolder + "/" + directory); //$NON-NLS-1$
        }
    }

    /**
     * Answer the string to display in self as the destination type.
     * 
     * @return java.lang.String
     */
    protected String getDestinationLabel() {
        return DataTransferMessages.ArchiveExport_destinationLabel;
    }

    /**
     * Returns resources to be exported. This returns file - for just the files use getSelectedResources.
     * 
     * @return a collection of resources currently selected for export (element type: <code>IResource</code>)
     */
    public List<ExportFileResource> getExportResources() {
        Map<ExportChoice, Object> exportChoiceMap = getExportChoiceMap();
        return manager.getExportResources(process, exportChoiceMap, contextCombo.getText(), launcherCombo.getText(),
                IProcessor.NO_STATISTICS, IProcessor.NO_TRACES);
    }

    protected Map<ExportChoice, Object> getExportChoiceMap() {
        Map<ExportChoice, Object> exportChoiceMap = new EnumMap<ExportChoice, Object>(ExportChoice.class);
        exportChoiceMap.put(ExportChoice.needLauncher, shellLauncherButton.getSelection());
        exportChoiceMap.put(ExportChoice.needSystemRoutine, systemRoutineButton.getSelection());
        exportChoiceMap.put(ExportChoice.needUserRoutine, userRoutineButton.getSelection());
        exportChoiceMap.put(ExportChoice.needTalendLibraries, modelButton.getSelection());
        exportChoiceMap.put(ExportChoice.needJobItem, jobItemButton.getSelection());
        exportChoiceMap.put(ExportChoice.needSourceCode, jobItemButton.getSelection());
        exportChoiceMap.put(ExportChoice.needDependencies, exportDependencies.getSelection());
        exportChoiceMap.put(ExportChoice.needJobScript, jobScriptButton.getSelection());
        exportChoiceMap.put(ExportChoice.needContext, contextButton.getSelection());
        exportChoiceMap.put(ExportChoice.applyToChildren, applyToChildrenButton.getSelection());
        exportChoiceMap.put(ExportChoice.needDependencies, exportDependencies.getSelection());
        // exportChoiceMap.put(ExportChoice.needGenerateCode, genCodeButton.getSelection());
        return exportChoiceMap;
    }

    /**
     * Answer the contents of self's destination specification widget. If this value does not have a suffix then add it
     * first.
     */
    protected String getDestinationValue() {
        String idealSuffix = getOutputSuffix();
        String destinationText = super.getDestinationValue();
        // only append a suffix if the destination doesn't already have a . in
        // its last path segment.
        // Also prevent the user from selecting a directory. Allowing this will
        // create a ".zip" file in the directory
        if (destinationText.length() != 0 && !destinationText.endsWith(File.separator)) {
            int dotIndex = destinationText.lastIndexOf('.');
            if (dotIndex != -1) {
                // the last path seperator index
                int pathSepIndex = destinationText.lastIndexOf(File.separator);
                if (pathSepIndex != -1 && dotIndex < pathSepIndex) {
                    destinationText += idealSuffix;
                }
            } else {
                destinationText += idealSuffix;
            }
        }
        // this is the entrance to the answer .. shenhaize.
        // System.out.println(destinationText);
        // String b = destinationText.substring(0, (destinationText.length() - 4));
        // return (b + destinationText.subSequence((destinationText.length() - 4), destinationText.length()));
        // System.out.println(destinationText + "  " + idealSuffix);
        if (destinationText.endsWith(this.getSelectedJobVersion() + this.getOutputSuffix())) {
            return destinationText;
        }
        return destinationText;

    }

    /**
     * Answer the suffix that files exported from this wizard should have. If this suffix is a file extension (which is
     * typically the case) then it must include the leading period character.
     * 
     */
    protected String getOutputSuffix() {
        return outputFileSuffix; //$NON-NLS-1$
    }

    /**
     * Open an appropriate destination browser so that the user can specify a source to import from.
     */
    protected void handleDestinationBrowseButtonPressed() {
        FileDialog dialog = new FileDialog(getContainer().getShell(), SWT.SAVE);
        dialog.setFilterExtensions(new String[] { "*.zip", "*.*" }); //$NON-NLS-1$ //$NON-NLS-2$
        dialog.setText(""); //$NON-NLS-1$
        dialog.setFileName((String) this.getDefaultFileName().get(0));
        String currentSourceString = getDestinationValue();
        int lastSeparatorIndex = currentSourceString.lastIndexOf(File.separator);
        if (lastSeparatorIndex != -1) {
            dialog.setFilterPath(currentSourceString.substring(0, lastSeparatorIndex));
        }
        String selectedFileName = dialog.open();
        if (!selectedFileName.endsWith(this.getOutputSuffix()))
            selectedFileName += this.getOutputSuffix();

        // when user change the name of job,will add the version auto
        if (selectedFileName != null && !selectedFileName.endsWith(this.getSelectedJobVersion() + this.getOutputSuffix())) {
            String b = selectedFileName.substring(0, (selectedFileName.length() - 4));
            File file = new File(b);

            String str = file.getName();

            String s = (String) this.getDefaultFileName().get(0);

            if (str.equals(s)) {
                selectedFileName = b + "_" + this.getDefaultFileName().get(1) + this.getOutputSuffix(); //$NON-NLS-1$
            } else {
                selectedFileName = b + this.getOutputSuffix();
            }

        }
        if (selectedFileName != null) {
            setErrorMessage(null);
            setDestinationValue(selectedFileName);

        }
    }

    /**
     * Hook method for saving widget values for restoration by the next instance of this class.
     */
    protected void internalSaveWidgetValues() {
    }

    /**
     * Hook method for restoring widget values to the values that they held last time this wizard was used to
     * completion.
     */
    protected void restoreWidgetValues() {
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.ui.wizards.datatransfer.WizardFileSystemResourceExportPage1#destinationEmptyMessage()
     */
    protected String destinationEmptyMessage() {
        return ""; //$NON-NLS-1$
    }

    /**
     * ftang Comment method "isMultiNodes".
     * 
     * @return
     */
    public boolean isMultiNodes() {
        if (treeViewer == null) {
            return false;
        }
        return this.getCheckNodes().length > 1;
    }

    /**
     * ftang Comment method "getSelectedJobVersion".
     * 
     * @return
     */
    public String getSelectedJobVersion() {
        return this.selectedJobVersion;
    }
}
