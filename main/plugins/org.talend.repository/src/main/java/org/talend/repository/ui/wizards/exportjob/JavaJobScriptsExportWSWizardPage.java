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
package org.talend.repository.ui.wizards.exportjob;

import java.io.File;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Widget;
import org.talend.commons.exception.ExceptionHandler;
import org.talend.commons.exception.PersistenceException;
import org.talend.core.GlobalServiceRegister;
import org.talend.core.model.general.ModuleNeeded;
import org.talend.core.model.general.ModuleNeeded.ELibraryInstallStatus;
import org.talend.core.model.process.IContext;
import org.talend.core.model.properties.ProcessItem;
import org.talend.core.model.repository.IRepositoryViewObject;
import org.talend.core.repository.constants.FileConstants;
import org.talend.core.repository.model.ProxyRepositoryFactory;
import org.talend.core.ui.branding.IBrandingService;
import org.talend.designer.runprocess.IProcessor;
import org.talend.librariesmanager.model.ModulesNeededProvider;
import org.talend.repository.i18n.Messages;
import org.talend.repository.ui.utils.Log4jPrefsSettingManager;
import org.talend.repository.ui.wizards.exportjob.scriptsmanager.JobScriptsManager;
import org.talend.repository.ui.wizards.exportjob.scriptsmanager.JobScriptsManager.ExportChoice;
import org.talend.repository.ui.wizards.exportjob.scriptsmanager.JobScriptsManagerFactory;
import org.talend.repository.ui.wizards.exportjob.scriptsmanager.petals.ContextExportDialog;
import org.talend.repository.ui.wizards.exportjob.scriptsmanager.petals.ContextExportType;
import org.talend.repository.ui.wizards.exportjob.scriptsmanager.petals.ContextTypeDefinition;
import org.talend.repository.ui.wizards.exportjob.scriptsmanager.petals.PetalsExportException;
import org.talend.repository.ui.wizards.exportjob.scriptsmanager.petals.PetalsTemporaryOptionsKeeper;
import org.talend.repository.ui.wizards.exportjob.scriptsmanager.petals.SaUtils;
import org.talend.repository.ui.wizards.exportjob.scriptsmanager.petals.TalendUtils;
import org.talend.resource.IExportJobResourcesService;

/**
 * DOC x class global comment. Detailled comment <br/>
 * 
 */
public class JavaJobScriptsExportWSWizardPage extends JavaJobScriptsExportWizardPage {

    /**
     * type of job exports.
     * */
    public static enum JobExportType {
        POJO(Messages.getString("JavaJobScriptsExportWSWizardPage.POJO"), false), //$NON-NLS-1$
        WSWAR(Messages.getString("JavaJobScriptsExportWSWizardPage.WSWAR"), false), //$NON-NLS-1$
        WSZIP(Messages.getString("JavaJobScriptsExportWSWizardPage.WSZIP"), false), //$NON-NLS-1$
        JBOSSESB(Messages.getString("JavaJobScriptsExportWSWizardPage.JBOSSESB"), true), //$NON-NLS-1$
        PETALSESB(Messages.getString("JavaJobScriptsExportWSWizardPage.PETALSESB"), true), //$NON-NLS-1$
        OSGI(Messages.getString("JavaJobScriptsExportWSWizardPage.OSGI"), false);//$NON-NLS-1$

        public final String label;

        public final boolean deprecate;

        private JobExportType(String label, boolean deprecate) {
            this.label = label;
            this.deprecate = deprecate;
        }

        /**
         * return the type according to the label or the POJO type if no match.
         * */
        public static JobExportType getTypeFromLabel(String label) {
            for (JobExportType type : JobExportType.values()) {
                if (type.label.equals(label)) {
                    return type;
                }
            }
            return POJO;
        }

        /**
         * return the type according to the type string, then try the label string or the POJO type if no match
         * */
        public static JobExportType getTypeFromString(String str) {
            if (str == null) {
                return POJO;
            } else {
                try {
                    return JobExportType.valueOf(str);
                } catch (IllegalArgumentException iae) {// for compatibility try the label also
                    return JobExportType.getTypeFromLabel(str);
                }
            }
        }

    }

    public static final String ESBTYPE_JBOSS_MQ = "JBoss MQ"; //$NON-NLS-1$

    public static final String ESBTYPE_JBOSS_MESSAGING = "JBoss Messaging"; //$NON-NLS-1$

    protected Combo exportTypeCombo;

    protected Combo esbTypeCombo;

    protected ScrolledComposite scrolledComposite;

    protected Composite pageComposite;

    protected Composite optionsGroupComposite;

    protected Composite destinationNameFieldComposite;

    protected Composite destinationNameFieldInnerComposite;

    protected Button webXMLButton;

    protected Button configFileButton;

    protected Button axisLibButton;

    protected Button wsddButton;

    protected Button wsdlButton;

    protected Button chkButton;

    protected Button singletonButton;

    protected Button generateEndpointButton;

    protected Button sourceButton;

    protected Button validateByWsdlButton;

    protected Text esbQueueMessageName;

    protected Text esbServiceName;

    protected Text esbCategory;

    public static final String STORE_EXPORTTYPE_ID = "JavaJobScriptsExportWizardPage.STORE_EXPORTTYPE_ID"; //$NON-NLS-1$

    public static final String STORE_WEBXML_ID = "JavaJobScriptsExportWizardPage.STORE_WEBXML_ID"; //$NON-NLS-1$

    public static final String STORE_CONFIGFILE_ID = "JavaJobScriptsExportWizardPage.STORE_CONFIGFILE_ID"; //$NON-NLS-1$

    public static final String STORE_AXISLIB_ID = "JavaJobScriptsExportWizardPage.STORE_AXISLIB_ID"; //$NON-NLS-1$

    public static final String STORE_WSDD_ID = "JavaJobScriptsExportWizardPage.STORE_WSDD_ID"; //$NON-NLS-1$

    public static final String STORE_WSDL_ID = "JavaJobScriptsExportWizardPage.STORE_WSDL_ID"; //$NON-NLS-1$

    public static final String EXTRACT_ZIP_FILE = "JavaJobScriptsExportWizardPage.EXTRACT_ZIP_FILE"; //$NON-NLS-1$

    protected JobExportType exportTypeFixed;

    private final Map<String, List<ContextTypeDefinition>> ctxToTypeDefs = new HashMap<String, List<ContextTypeDefinition>>();

    private List<ContextTypeDefinition> currentCtxTypes;

    private String saDestinationFilePath;

    public static final String PETALS_EXPORT_DESTINATIONS = "org.ow2.petals.esbexport.destinations"; //$NON-NLS-1$

    JavaJobScriptsExportWSWizardPagePresenter presenter = new JavaJobScriptsExportWSWizardPagePresenter(this);

    public JavaJobScriptsExportWSWizardPage(IStructuredSelection selection, String exportType) {
        super(selection);
        // there assign the manager again
        exportTypeFixed = exportType != null ? JobExportType.getTypeFromString(exportType) : null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.wizard.WizardPage#setWizard(org.eclipse.jface.wizard.IWizard)
     */
    @Override
    public void setWizard(IWizard newWizard) {
        super.setWizard(newWizard);
        initialiseDefaultDialogSettings();
    }

    /**
     * this set default dialog settings if none already exists.
     */
    private void initialiseDefaultDialogSettings() {
        IDialogSettings dialogSettings = getDialogSettings();
        if (dialogSettings != null) {
            // set default export type according to system properties
            String exportType = dialogSettings.get(STORE_EXPORTTYPE_ID);
            String defaultExportType = System.getProperty("talend.export.job.default.type"); //$NON-NLS-1$
            if ((exportType == null || exportType.equals("")) && defaultExportType != null && !defaultExportType.equals("")) { //$NON-NLS-1$ //$NON-NLS-2$
                dialogSettings.put(STORE_EXPORTTYPE_ID, defaultExportType);
            }
        }// else ignors it
    }

    /**
     * Extracts all types of job for exporting, about the exporting job type, please refer to {@link JobExportType}.
     * Subclasses can override this method to return the types that job requires for exporting. Added by Marvin Wang on
     * Mar 6, 2013.
     * 
     * @return
     */
    protected List<JobExportType> extractExportJobTypes() {
        // Feature TDI-29084:put the Deprecated build type at last
        List<JobExportType> deprecateTypeList = new ArrayList<JobExportType>();
        List<JobExportType> typeList = new ArrayList<JobExportType>();
        for (JobExportType type : JobExportType.values()) {
            if (!type.deprecate) {
                typeList.add(type);
            } else {
                deprecateTypeList.add(type);
            }
        }
        typeList.addAll(deprecateTypeList);
        return typeList;
    }

    @Override
    public JobExportType getCurrentExportType1() {
        if (exportTypeCombo != null && !exportTypeCombo.getText().equals("")) { //$NON-NLS-1$
            return JobExportType.getTypeFromLabel(exportTypeCombo.getText());
        } else {
            IDialogSettings settings = getDialogSettings();
            if (settings != null && settings.get(STORE_EXPORTTYPE_ID) != null) {
                JobExportType type = JobExportType.getTypeFromString(settings.get(STORE_EXPORTTYPE_ID));
                for (JobExportType exportType : extractExportJobTypes()) {
                    if (!Boolean.getBoolean("talend.export.job.2." + exportType.toString() + ".hide")) { //$NON-NLS-1$//$NON-NLS-2$
                        if (type.equals(exportType)) {
                            return type; // check if at least the type is included in the list or not, if not, just get
                                         // the default one (POJO)
                        }
                    }
                }
            }
        }
        return JobExportType.POJO;
    }

    @Override
    protected ExportTreeViewer getExportTree() {
        return new ExportTreeViewer(selection, this) {

            /*
             * (non-Javadoc)
             * 
             * @see org.talend.repository.ui.wizards.exportjob.ExportTreeViewer#checkSelection()
             */
            @Override
            protected void checkSelection() {
                checkExport();
            }
        };
    }

    @Override
    public void createControl(Composite parent) {

        initializeDialogUnits(parent);
        GridLayout layout = new GridLayout();

        if (exportTypeFixed == null || !exportTypeFixed.equals(JobExportType.JBOSSESB)) {
            SashForm sash = createExportTree(parent);
            // Added a scrolled composite by Marvin Wang on Feb. 27, 2012 for bug TDI-19198.
            scrolledComposite = new ScrolledComposite(sash, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
            pageComposite = new Group(scrolledComposite, 0);
            pageComposite.setLayout(layout);
            pageComposite.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL));
            pageComposite.setFont(parent.getFont());
            setControl(sash);
            sash.setWeights(new int[] { 0, 1, 23 });
        } else {
            scrolledComposite = new ScrolledComposite(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
            pageComposite = new Group(scrolledComposite, 0);
            pageComposite.setLayout(layout);
            pageComposite.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL));
            pageComposite.setFont(parent.getFont());
            setControl(parent);
        }
        layout = new GridLayout();
        layout.marginHeight = 0;
        layout.verticalSpacing = 0;
        destinationNameFieldComposite = new Composite(pageComposite, SWT.NONE);
        GridData gridData = new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.GRAB_HORIZONTAL);
        destinationNameFieldComposite.setLayoutData(gridData);
        destinationNameFieldComposite.setLayout(layout);

        destinationNameFieldInnerComposite = new Composite(destinationNameFieldComposite, SWT.NONE);
        gridData = new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.GRAB_HORIZONTAL);
        destinationNameFieldInnerComposite.setLayoutData(gridData);
        destinationNameFieldInnerComposite.setLayout(layout);

        createDestinationGroup(destinationNameFieldInnerComposite);
        // createExportTree(pageComposite);
        if (!isMultiNodes()) {
            IBrandingService brandingService = (IBrandingService) GlobalServiceRegister.getDefault().getService(
                    IBrandingService.class);
            boolean allowVerchange = brandingService.getBrandingConfiguration().isAllowChengeVersion();
            if (allowVerchange) {
                createJobVersionGroup(pageComposite);
            } else {
                selectedJobVersion = "0.1";
            }
        }

        createExportTypeGroup(pageComposite);

        createOptionsGroupButtons(pageComposite);

        restoreResourceSpecificationWidgetValues(); // ie.- local

        updateWidgetEnablements();
        setPageComplete(determinePageCompletion());

        giveFocusToDestination();

        pageComposite.setSize(pageComposite.computeSize(SWT.DEFAULT, SWT.DEFAULT));
        scrolledComposite.setContent(pageComposite);
    }

    protected void createExportTypeGroup(Composite parent) {
        // options group
        Group optionsGroup = new Group(parent, SWT.NONE);
        GridLayout layout = new GridLayout();
        optionsGroup.setLayout(layout);
        optionsGroup.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.GRAB_HORIZONTAL));
        optionsGroup.setText(Messages.getString("JavaJobScriptsExportWSWizardPage.BuildType")); //$NON-NLS-1$
        optionsGroup.setFont(parent.getFont());

        optionsGroup.setLayout(new GridLayout(1, true));

        Composite left = new Composite(optionsGroup, SWT.NONE);
        left.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, true, false));
        left.setLayout(new GridLayout(3, false));

        Label label = new Label(left, SWT.NONE);
        label.setText(Messages.getString("JavaJobScriptsExportWSWizardPage.BuildLabel")); //$NON-NLS-1$

        exportTypeCombo = new Combo(left, SWT.PUSH);
        GridData gd = new GridData();
        gd.horizontalSpan = 1;
        exportTypeCombo.setLayoutData(gd);

        for (JobExportType exportType : extractExportJobTypes()) {
            if (!Boolean.getBoolean("talend.export.job.2." + exportType.toString() + ".hide")) { //$NON-NLS-1$//$NON-NLS-2$
                exportTypeCombo.add(exportType.label);
            }
        }
        exportTypeCombo.setText(getCurrentExportType1().label);
        if (exportTypeFixed != null) {
            left.setVisible(false);
            optionsGroup.setVisible(false);
            exportTypeCombo.setText(exportTypeFixed.label);
        }

        chkButton = new Button(left, SWT.CHECK);
        chkButton.setText(Messages.getString("JavaJobScriptsExportWSWizardPage.extractZipFile")); //$NON-NLS-1$
        JobExportType comboType = JobExportType.getTypeFromString(exportTypeCombo.getText());
        if (comboType.equals(JobExportType.WSWAR) || comboType.equals(JobExportType.PETALSESB)
                || comboType.equals(JobExportType.OSGI)) {
            chkButton.setVisible(false);
            zipOption = null;
        } else {
            chkButton.setVisible(true);
            zipOption = String.valueOf(chkButton.getSelection());
        }
        chkButton.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                chkButton.setSelection(chkButton.getSelection());
                zipOption = String.valueOf(chkButton.getSelection());
            }
        });
        exportTypeCombo.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {

            }

            @Override
            public void widgetSelected(SelectionEvent e) {
                destinationNameFieldInnerComposite.dispose();
                GridLayout layout = new GridLayout();
                destinationNameFieldInnerComposite = new Composite(destinationNameFieldComposite, SWT.NONE);
                GridData gridData = new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.GRAB_HORIZONTAL);
                destinationNameFieldInnerComposite.setLayoutData(gridData);
                destinationNameFieldInnerComposite.setLayout(layout);
                createDestinationGroup(destinationNameFieldInnerComposite);

                destinationNameFieldComposite.layout();

                optionsGroupComposite.dispose();
                createOptionsGroupButtons(pageComposite);
                pageComposite.setSize(pageComposite.computeSize(SWT.DEFAULT, SWT.DEFAULT));
                pageComposite.layout();
                JobExportType comboType = JobExportType.getTypeFromString(exportTypeCombo.getText());
                if (comboType.equals(JobExportType.WSWAR) || comboType.equals(JobExportType.OSGI)) {
                    chkButton.setVisible(false);
                    zipOption = null;
                } else {
                    chkButton.setVisible(true);
                    zipOption = String.valueOf(chkButton.getSelection());
                }
                checkExport();
            }
        });
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.ui.wizards.exportjob.JavaJobScriptsExportWizardPage#createJobScriptsManager()
     */
    @Override
    public JobScriptsManager createJobScriptsManager() {
        Map<ExportChoice, Object> exportChoiceMap = getExportChoiceMap();
        String log4jLevel = "";
        String launcher = (getCurrentExportType1() == JobExportType.POJO) ? launcherCombo.getText() : "all";
        String context = (contextCombo == null || contextCombo.isDisposed()) ? IContext.DEFAULT : contextCombo.getText();

        JobScriptsManager manager = JobScriptsManagerFactory.createManagerInstance(exportChoiceMap, context, launcher,
                IProcessor.NO_STATISTICS, IProcessor.NO_TRACES, getCurrentExportType1());
        manager.setDestinationPath(getDestinationValue());
        if (log4jLevelCombo != null && !log4jLevelCombo.isDisposed()) {
            if (log4jLevelCombo.isEnabled()) {
                log4jLevel = log4jLevelCombo.getText();
            } else {
                log4jLevel = null;
            }
        } else {
            log4jLevel = null;
        }
        manager.setLog4jLevel(log4jLevel);
        return manager;
    }

    @Override
    protected String getOutputSuffix() {
        switch (getCurrentExportType1()) {
        case WSWAR:
            return FileConstants.WAR_FILE_SUFFIX;
        case JBOSSESB:
            return FileConstants.ESB_FILE_SUFFIX;
        case OSGI:
            return FileConstants.JAR_FILE_SUFFIX;
        default:
            return FileConstants.ZIP_FILE_SUFFIX;
        }
    }

    protected String getPetalsDefaultSaName() {
        return "sa-talend-" + getDefaultFileName().get(0) + "Service-provide.zip"; //$NON-NLS-1$ //$NON-NLS-2$
    }

    /**
     * Open an appropriate destination browser so that the user can specify a source to import from.
     */
    @Override
    protected void handleDestinationBrowseButtonPressed() {
        FileDialog dialog = new FileDialog(getContainer().getShell(), SWT.SAVE);
        JobExportType jobExportType = getCurrentExportType1();
        switch (jobExportType) {
        case WSWAR:
            dialog.setFilterExtensions(new String[] { "*" + FileConstants.WAR_FILE_SUFFIX, "*.*" }); //$NON-NLS-1$ //$NON-NLS-2$
            break;
        case JBOSSESB:
            dialog.setFilterExtensions(new String[] { "*" + FileConstants.ESB_FILE_SUFFIX, "*.*" }); //$NON-NLS-1$ //$NON-NLS-2$
            break;
        case OSGI:
            if (isAddMavenScript()) {
                dialog.setFilterExtensions(new String[] { "*" + FileConstants.ZIP_FILE_SUFFIX, "*.*" }); //$NON-NLS-1$ //$NON-NLS-2$
            } else {
                dialog.setFilterExtensions(new String[] { "*" + FileConstants.JAR_FILE_SUFFIX, "*.*" }); //$NON-NLS-1$ //$NON-NLS-2$
            }
            break;
        case PETALSESB:
            dialog.setFilterExtensions(new String[] { "*" + FileConstants.ZIP_FILE_SUFFIX, "*.*" }); //$NON-NLS-1$ //$NON-NLS-2$
            break;
        default:
            dialog.setFilterExtensions(new String[] { "*" + FileConstants.ZIP_FILE_SUFFIX, "*.*" }); //$NON-NLS-1$ //$NON-NLS-2$
        }

        if (jobExportType.equals(JobExportType.PETALSESB)) {
            IPath destPath = new Path(saDestinationFilePath);
            String fileName, directory;
            if (destPath.toFile().isDirectory()) {
                fileName = getPetalsDefaultSaName();
                directory = destPath.toOSString();
            } else {
                fileName = destPath.lastSegment();
                directory = destPath.removeLastSegments(1).toOSString();
            }
            dialog.setFileName(fileName);
            dialog.setFilterPath(directory);
        } else {
            dialog.setText(""); //$NON-NLS-1$
            // this is changed by me shenhaize
            dialog.setFileName(getDefaultFileName().get(0));
            String currentSourceString = getDestinationValue();
            int lastSeparatorIndex = currentSourceString.lastIndexOf(File.separator);
            if (lastSeparatorIndex != -1) {
                dialog.setFilterPath(currentSourceString.substring(0, lastSeparatorIndex));
            }
        }

        String selectedFileName = dialog.open();
        if (selectedFileName == null) {
            return;
        }
        String idealSuffix;
        if (isAddMavenScript()) {
            idealSuffix = FileConstants.ZIP_FILE_SUFFIX;
        } else {
            idealSuffix = getOutputSuffix();
        }
        if (!selectedFileName.endsWith(idealSuffix)) {
            selectedFileName += idealSuffix;
        }
        // when user change the name of job,will add the version auto
        if (selectedFileName != null && !selectedFileName.endsWith(getSelectedJobVersion() + idealSuffix)) {
            String b = selectedFileName.substring(0, (selectedFileName.length() - 4));
            File file = new File(b);

            String str = file.getName();

            String s = getDefaultFileName().get(0);

            if (str.equals(s)) {
                if (getDefaultFileName().get(1) != null && !"".equals(getDefaultFileName().get(1))) {
                    selectedFileName = b + ((JobExportType.OSGI.equals(jobExportType)) ? "-" : "_") + getDefaultFileName().get(1)
                            + idealSuffix;
                } else {
                    selectedFileName = b + idealSuffix;
                }
            } else {
                selectedFileName = b + idealSuffix;
            }

        }
        if (selectedFileName != null) {
            setErrorMessage(null);
            saDestinationFilePath = selectedFileName;
            setDestinationValue(selectedFileName);

            if (getDialogSettings() != null) {
                IDialogSettings section = getDialogSettings().getSection(DESTINATION_FILE);
                if (section == null) {
                    section = getDialogSettings().addNewSection(DESTINATION_FILE);
                }
                section.put(DESTINATION_FILE, selectedFileName);
            }

        }
    }

    @Override
    public void handleEvent(Event e) {
        super.handleEvent(e);
        Widget source = e.widget;
        if (source instanceof Combo) {
            String destination = ((Combo) source).getText();
            if (getDialogSettings() != null) {
                IDialogSettings section = getDialogSettings().getSection(DESTINATION_FILE);
                if (section == null) {
                    section = getDialogSettings().addNewSection(DESTINATION_FILE);
                }
                section.put(DESTINATION_FILE, destination);
            }
            if (destination != null) {
                if (!destination.endsWith(getOutputSuffix())) {
                    destination += getOutputSuffix();
                }
                saDestinationFilePath = destination;
            }
        }
    }

    protected void restoreWidgetValuesForPetalsESB() {

        IDialogSettings settings = getDialogSettings();
        if (settings != null) {
            String[] directoryNames = settings.getArray(STORE_DESTINATION_NAMES_ID);
            String saName = getPetalsDefaultSaName();
            if (directoryNames != null && directoryNames.length > 0) {
                // destination
                for (int i = 0; i < directoryNames.length; i++) {
                    if (directoryNames[i].toLowerCase().endsWith(FileConstants.ZIP_FILE_SUFFIX)) {
                        directoryNames[i] = (directoryNames[i].charAt(0) + "").toUpperCase() + directoryNames[i].substring(1);//$NON-NLS-1$
                        addDestinationItem(directoryNames[i]);
                    }
                }
                File file = new File(directoryNames[0]);
                File dest = new File(file.getParentFile(), saName);
                setDestinationValue(dest.getAbsolutePath());
            } else {
                String userDir = System.getProperty("user.dir"); //$NON-NLS-1$
                IPath path = new Path(userDir).append(saName);
                setDestinationValue(path.toOSString());
            }
            saDestinationFilePath = this.getDestinationValue();
            sourceButton.setSelection(settings.getBoolean(STORE_SOURCE_ID));
            zipOption = "false"; // Do not extract the ZIP //$NON-NLS-1$
        }

        if (getProcessItem() != null) {
            List<String> contextNames = getJobContexts(getProcessItem());
            contextCombo.setItems(contextNames.toArray(new String[contextNames.size()]));
            contextCombo.setVisibleItemCount(contextNames.size());
            if (contextNames.size() > 0) {
                contextCombo.select(0);
            }
        }
    }

    protected void restoreWidgetValuesForESB() {
        IDialogSettings settings = getDialogSettings();
        if (settings != null) {
            String[] directoryNames = settings.getArray(STORE_DESTINATION_NAMES_ID);
            if (directoryNames != null && directoryNames.length > 0) {
                String fileName = getDefaultFileNameWithType();
                // destination
                for (int i = 0; i < directoryNames.length; i++) {
                    if (directoryNames[i].toLowerCase().endsWith(FileConstants.ESB_FILE_SUFFIX)) {
                        directoryNames[i] = (directoryNames[i].charAt(0) + "").toUpperCase() + directoryNames[i].substring(1);//$NON-NLS-1$
                        addDestinationItem(directoryNames[i]);
                    }
                }
                File dest = new File(new File(directoryNames[0]).getParentFile(), fileName);
                setDestinationValue(dest.getAbsolutePath());
            } else {
                setDefaultDestination();
            }

            IDialogSettings section = getDialogSettings().getSection(DESTINATION_FILE);
            if (section == null) {
                section = getDialogSettings().addNewSection(DESTINATION_FILE);
            }
            if (jobScriptButton != null && !jobScriptButton.isDisposed()) {
                jobScriptButton.setSelection(settings.getBoolean(STORE_SOURCE_ID));
            }
            if (contextButton != null && !contextButton.isDisposed()) {
                contextButton.setSelection(settings.getBoolean(STORE_CONTEXT_ID));
            }
            if (applyToChildrenButton != null && !applyToChildrenButton.isDisposed()) {
                applyToChildrenButton.setSelection(settings.getBoolean(APPLY_TO_CHILDREN_ID));
            }
            if (jobItemButton != null && !jobItemButton.isDisposed()) {
                jobItemButton.setSelection(settings.getBoolean(STORE_JOB_ID));
            }

            if (section.get(ESB_EXPORT_TYPE) != null) {
                esbTypeCombo.setText(section.get(ESB_EXPORT_TYPE));
                if (section.get(ESB_SERVICE_NAME) != null) {
                    esbServiceName.setText(section.get(ESB_SERVICE_NAME));
                }
                if (section.get(ESB_CATEGORY) != null) {
                    esbCategory.setText(section.get(ESB_CATEGORY));
                }
                if (section.get(QUERY_MESSAGE_NAME) != null) {
                    esbQueueMessageName.setText(section.get(QUERY_MESSAGE_NAME));
                }
            }
        }

        if (getProcessItem() != null && contextCombo != null) {
            try {
                setProcessItem((ProcessItem) ProxyRepositoryFactory.getInstance()
                        .getUptodateProperty(getProcessItem().getProperty()).getItem());
            } catch (PersistenceException e) {
                e.printStackTrace();
            }
            List<String> contextNames = getJobContexts(getProcessItem());
            contextCombo.setItems(contextNames.toArray(new String[contextNames.size()]));
            if (contextNames.size() > 0) {
                contextCombo.select(0);
            }
        }
    }

    protected void restoreWidgetValuesForOSGI() {
        IDialogSettings settings = getDialogSettings();
        if (settings != null) {
            String[] directoryNames = settings.getArray(STORE_DESTINATION_NAMES_ID);
            if (directoryNames != null && directoryNames.length > 0) {
                String fileName = getDefaultFileNameWithType();
                for (int i = 0; i < directoryNames.length; i++) {
                    if (directoryNames[i].toLowerCase().endsWith(FileConstants.JAR_FILE_SUFFIX)) {
                        directoryNames[i] = (directoryNames[i].charAt(0) + "").toUpperCase() + directoryNames[i].substring(1);//$NON-NLS-1$
                        addDestinationItem(directoryNames[i]);
                    }
                }
                File dest = new File(new File(directoryNames[0]).getParentFile(), fileName);
                setDestinationValue(dest.getAbsolutePath());
            } else {
                setDefaultDestinationForOSGI();
            }
        } else {
            setDefaultDestinationForOSGI();
        }
    }

    protected void restoreWidgetValuesForWS() {
        IDialogSettings settings = getDialogSettings();
        if (settings != null) {
            String[] directoryNames = settings.getArray(STORE_DESTINATION_NAMES_ID);
            if (directoryNames != null && directoryNames.length > 0) {
                String fileName = getDefaultFileNameWithType();
                // destination
                for (int i = 0; i < directoryNames.length; i++) {
                    if (directoryNames[i].toLowerCase().endsWith(FileConstants.WAR_FILE_SUFFIX)
                            || directoryNames[i].toLowerCase().endsWith(FileConstants.ZIP_FILE_SUFFIX)) {
                        directoryNames[i] = (directoryNames[i].charAt(0) + "").toUpperCase() + directoryNames[i].substring(1);//$NON-NLS-1$
                        addDestinationItem(directoryNames[i]);
                    }
                }
                File dest = new File(new File(directoryNames[0]).getParentFile(), fileName);
                setDestinationValue(dest.getAbsolutePath());
            } else {
                setDefaultDestination();
            }

            webXMLButton.setSelection(settings.getBoolean(STORE_WEBXML_ID));
            configFileButton.setSelection(settings.getBoolean(STORE_CONFIGFILE_ID));
            axisLibButton.setSelection(settings.getBoolean(STORE_AXISLIB_ID));
            wsddButton.setSelection(settings.getBoolean(STORE_WSDD_ID));
            wsdlButton.setSelection(settings.getBoolean(STORE_WSDL_ID));
            jobScriptButton.setSelection(settings.getBoolean(STORE_SOURCE_ID));
            contextButton.setSelection(settings.getBoolean(STORE_CONTEXT_ID));
            applyToChildrenButton.setSelection(settings.getBoolean(APPLY_TO_CHILDREN_ID));
            chkButton.setSelection(settings.getBoolean(EXTRACT_ZIP_FILE));
            // TDI-26294:should use getVisible here since the isVisible need the parent's isVisible()
            if (chkButton.getVisible()) {
                zipOption = String.valueOf(chkButton.getSelection());
            } else {
                zipOption = "false"; //$NON-NLS-1$
            }

        }

        if (getProcessItem() != null && contextCombo != null) {
            try {
                setProcessItem((ProcessItem) ProxyRepositoryFactory.getInstance()
                        .getUptodateProperty(getProcessItem().getProperty()).getItem());
            } catch (PersistenceException e) {
                e.printStackTrace();
            }
            List<String> contextNames = getJobContexts(getProcessItem());
            contextCombo.setItems(contextNames.toArray(new String[contextNames.size()]));
            if (contextNames.size() > 0) {
                contextCombo.select(0);
            }
        }
        if (log4jLevelCombo != null) {
            log4jLevelCombo.setItems(Log4jPrefsSettingManager.getLevel());
            if (Log4jPrefsSettingManager.getLevel().length > 0) {
                log4jLevelCombo.select(2);
            }
        }
    }

    protected void restoreWidgetValuesForPOJO() {
        IDialogSettings settings = getDialogSettings();

        if (settings != null) {
            String[] directoryNames = settings.getArray(STORE_DESTINATION_NAMES_ID);
            String fileName = getDefaultFileNameWithType();
            if (!fileName.endsWith(FileConstants.ZIP_FILE_SUFFIX)) {
                fileName = fileName + FileConstants.ZIP_FILE_SUFFIX;
            }
            if (directoryNames != null && directoryNames.length > 0) {
                // destination
                for (int i = 0; i < directoryNames.length; i++) {
                    // String destination;
                    // Path dirPath = new Path(directoryNames[i]);
                    // if (dirPath.segmentCount() == 0) {
                    // destination = dirPath.toOSString() + File.separator + fileName;
                    // } else {
                    // destination = dirPath.append(fileName).toOSString();
                    // }
                    if (directoryNames[i].toLowerCase().endsWith(FileConstants.ZIP_FILE_SUFFIX)) {
                        directoryNames[i] = (directoryNames[i].charAt(0) + "").toUpperCase() + directoryNames[i].substring(1);//$NON-NLS-1$
                        addDestinationItem(directoryNames[i]);
                    }
                }
                setDestinationValue(directoryNames[0].substring(0, (directoryNames[0].lastIndexOf("\\") + 1)) + fileName);//$NON-NLS-1$
            } else {
                setDefaultDestination();
            }
            shellLauncherButton.setSelection(settings.getBoolean(STORE_SHELL_LAUNCHER_ID));
            jobItemButton.setSelection(settings.getBoolean(STORE_JOB_ID));

            jobScriptButton.setSelection(settings.getBoolean(STORE_SOURCE_ID));
            contextButton.setSelection(settings.getBoolean(STORE_CONTEXT_ID));
            applyToChildrenButton.setSelection(settings.getBoolean(APPLY_TO_CHILDREN_ID));
            chkButton.setSelection(settings.getBoolean(EXTRACT_ZIP_FILE));
            zipOption = String.valueOf(chkButton.getSelection());
            // genCodeButton.setSelection(settings.getBoolean(STORE_GENERATECODE_ID));
        }

        launcherCombo.setItems(JobScriptsManager.getLauncher());
        if (JobScriptsManager.getLauncher().length > 0) {
            launcherCombo.select(0);
        }
        if (getProcessItem() != null && contextCombo != null) {
            // don't update the property, this one will be automatically updated if needed when call the getItem()

            // try {
            // process[0].setProcess((ProcessItem) ProxyRepositoryFactory.getInstance().getUptodateProperty(
            // getProcessItem().getProperty()).getItem());
            // } catch (PersistenceException e) {
            // ExceptionHandler.process(e);
            // }
            ProcessItem item = getProcessItem();
            try {
                String id = item.getProperty().getId();
                IRepositoryViewObject lastVersion = ProxyRepositoryFactory.getInstance().getLastVersion(id);
                item = (ProcessItem) lastVersion.getProperty().getItem();
            } catch (PersistenceException e) {
                throw new RuntimeException(e);
            }
            List<String> contextNames;
            contextNames = getJobContexts(item);

            contextCombo.setItems(contextNames.toArray(new String[contextNames.size()]));
            if (contextNames.size() > 0) {
                contextCombo.select(0);
            }
        }

        if (log4jLevelCombo != null) {
            log4jLevelCombo.setItems(Log4jPrefsSettingManager.getLevel());
            if (Log4jPrefsSettingManager.getLevel().length > 0) {
                log4jLevelCombo.select(2);
            }
        }
    }

    @Override
    protected void internalSaveWidgetValues() {
        // update directory names history
        IDialogSettings settings = getDialogSettings();
        if (settings != null) {
            if (getCurrentExportType1().equals(JobExportType.PETALSESB)) {
                String[] directoryNames = settings.getArray(PETALS_EXPORT_DESTINATIONS);
                if (directoryNames == null) {
                    directoryNames = new String[0];
                }

                directoryNames = addToHistory(directoryNames, saDestinationFilePath);
                settings.put(PETALS_EXPORT_DESTINATIONS, directoryNames);
                return;
            }
            String[] directoryNames = settings.getArray(STORE_DESTINATION_NAMES_ID);
            if (directoryNames == null) {
                directoryNames = new String[0];
            }
            String destinationValue = getDestinationValue();
            directoryNames = addToHistory(directoryNames, destinationValue);
            // String[] directoryNames = new String[1];
            // String destinationValue = getDestinationValue();
            // if (destinationValue != null) {
            // destinationValue = destinationValue.substring(0, destinationValue.lastIndexOf(File.separator));
            // }
            // directoryNames[0] = destinationValue;

            settings.put(STORE_EXPORTTYPE_ID, getCurrentExportType1().toString());
            settings.put(STORE_DESTINATION_NAMES_ID, directoryNames);
            if (getCurrentExportType1().equals(JobExportType.OSGI)) {
                return;
            }
            if (contextButton != null) {
                settings.put(STORE_CONTEXT_ID, contextButton.getSelection());
            }
            if (jobScriptButton != null && !jobScriptButton.isDisposed()) {
                settings.put(STORE_SOURCE_ID, jobScriptButton.getSelection());
            }
            if (applyToChildrenButton != null) {
                settings.put(APPLY_TO_CHILDREN_ID, applyToChildrenButton.getSelection());
            }
            if (jobItemButton != null && !jobItemButton.isDisposed()) {
                settings.put(STORE_JOB_ID, jobItemButton.getSelection());
            }

            if (log4jLevelCombo != null && !log4jLevelCombo.isDisposed()) {
                settings.put(LOG4J_LEVEL_ID, log4jLevelCombo.getText());
            }

            if (getCurrentExportType1().equals(JobExportType.POJO)) {
                settings.put(STORE_SHELL_LAUNCHER_ID, shellLauncherButton.getSelection());
                settings.put(EXTRACT_ZIP_FILE, chkButton.getSelection());
                return;
            } else if (getCurrentExportType1().equals(JobExportType.WSZIP)) {
                settings.put(STORE_WEBXML_ID, webXMLButton.getSelection());
                settings.put(STORE_CONFIGFILE_ID, configFileButton.getSelection());
                settings.put(STORE_AXISLIB_ID, axisLibButton.getSelection());
                settings.put(STORE_WSDD_ID, wsddButton.getSelection());
                settings.put(STORE_WSDL_ID, wsdlButton.getSelection());
                settings.put(EXTRACT_ZIP_FILE, chkButton.getSelection());
            }

        }
    }

    @Override
    protected Map<ExportChoice, Object> getExportChoiceMap() {
        JobExportType comboType = JobExportType.getTypeFromString(exportTypeCombo.getText());
        if (comboType.equals(JobExportType.POJO)) {
            return JavaJobScriptsExportWSWizardPage.super.getExportChoiceMap();
        }
        Map<ExportChoice, Object> exportChoiceMap = new EnumMap<ExportChoice, Object>(ExportChoice.class);
        if (comboType.equals(JobExportType.PETALSESB)) {
            exportChoiceMap.put(ExportChoice.needSourceCode, sourceButton.getSelection());
            exportChoiceMap.put(ExportChoice.needDependencies, Boolean.TRUE);
            exportChoiceMap.put(ExportChoice.needUserRoutine, Boolean.TRUE);
            return exportChoiceMap;
        }
        exportChoiceMap.put(ExportChoice.needJobItem, false);
        exportChoiceMap.put(ExportChoice.needSourceCode, false);

        if (comboType.equals(JobExportType.JBOSSESB)) {
            exportChoiceMap.put(ExportChoice.needMetaInfo, true);
            exportChoiceMap.put(ExportChoice.needContext, contextButton.getSelection());
            exportChoiceMap.put(ExportChoice.esbQueueMessageName, esbQueueMessageName.getText());
            exportChoiceMap.put(ExportChoice.esbServiceName, esbServiceName.getText());
            exportChoiceMap.put(ExportChoice.esbCategory, esbCategory.getText());
            exportChoiceMap.put(ExportChoice.esbExportType, esbTypeCombo.getText());
            exportChoiceMap.put(ExportChoice.needDependencies, jobItemButton.getSelection());
            exportChoiceMap.put(ExportChoice.needJobItem, jobItemButton.getSelection());
            exportChoiceMap.put(ExportChoice.needSourceCode, jobItemButton.getSelection()); // take source code also
            // when take item
            return exportChoiceMap;
        }

        if (comboType.equals(JobExportType.OSGI)) {
            exportChoiceMap.put(ExportChoice.needMetaInfo, true);
            exportChoiceMap.put(ExportChoice.needContext, true);
            exportChoiceMap.put(ExportChoice.needJobItem, false);
            exportChoiceMap.put(ExportChoice.needSourceCode, false);
            if (addBSButton != null) {
                exportChoiceMap.put(ExportChoice.needMavenScript, addBSButton.getSelection());
            }
            return exportChoiceMap;
        }

        // fix bug 9150, export items and code source, added by nma
        exportChoiceMap.put(ExportChoice.needJobItem, jobScriptButton.getSelection());
        exportChoiceMap.put(ExportChoice.needSourceCode, jobScriptButton.getSelection());

        exportChoiceMap.put(ExportChoice.needWEBXML, webXMLButton.getSelection());
        exportChoiceMap.put(ExportChoice.needCONFIGFILE, configFileButton.getSelection());
        exportChoiceMap.put(ExportChoice.needAXISLIB, axisLibButton.getSelection());
        exportChoiceMap.put(ExportChoice.needWSDD, wsddButton.getSelection());
        exportChoiceMap.put(ExportChoice.needWSDL, wsdlButton.getSelection());
        exportChoiceMap.put(ExportChoice.needJobScript, Boolean.TRUE);
        exportChoiceMap.put(ExportChoice.needContext, contextButton.getSelection());
        exportChoiceMap.put(ExportChoice.applyToChildren, applyToChildrenButton.getSelection());

        if (comboType.equals(JobExportType.WSWAR)) {
            exportChoiceMap.put(ExportChoice.needMetaInfo, true);
            exportChoiceMap.put(ExportChoice.needJobItem, true);
            exportChoiceMap.put(ExportChoice.needDependencies, true);
            exportChoiceMap.put(ExportChoice.needSourceCode, true);
        } else {
            exportChoiceMap.put(ExportChoice.needMetaInfo, false);
        }

        return exportChoiceMap;
    }

    protected void createOptionsGroupButtons(Composite parent) {
        // Commented by Marvin Wang on Feb.27, 2012 for bug TDI-19198, directly create components on Group.
        GridLayout layout = new GridLayout();
        optionsGroupComposite = new Composite(parent, SWT.NONE);
        GridData gridData = new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.GRAB_HORIZONTAL);
        // fix the setParametersValue button can not see sometimes.
        // gridData.minimumHeight = 200;
        optionsGroupComposite.setLayoutData(gridData);
        optionsGroupComposite.setLayout(layout);
        // options group
        Group optionsGroup = new Group(optionsGroupComposite, SWT.NONE);

        optionsGroup.setLayout(layout);

        optionsGroup.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.GRAB_HORIZONTAL));

        // optionsGroup.setText(IDEWorkbenchMessages.WizardExportPage_options);
        optionsGroup.setText(Messages.getString("IDEWorkbenchMessages.WizardExportPage_options")); //$NON-NLS-1$
        optionsGroup.setFont(parent.getFont());

        Font font = optionsGroup.getFont();
        optionsGroup.setLayout(new GridLayout(1, true));

        Composite left = new Composite(optionsGroup, SWT.NONE);
        gridData = new GridData(SWT.LEFT, SWT.TOP, true, false);
        left.setLayoutData(gridData);
        left.setLayout(new GridLayout(3, true));

        switch (getCurrentExportType1()) {
        case POJO:
            layout = new GridLayout();
            layout.verticalSpacing = 1;
            layout.marginHeight = 0;
            optionsGroup.setLayout(layout);
            createOptions(optionsGroup, font);
            restoreWidgetValuesForPOJO();
            break;
        case JBOSSESB:
            createOptionsForJbossESB(left, font);
            restoreWidgetValuesForESB();
            break;
        case PETALSESB:
            createOptionsforPetalsESB(left, font);
            restoreWidgetValuesForPetalsESB();
            restoreWidgetValues();
            break;
        case OSGI:
            createOptionsForOSGIESB(left, font);
            restoreWidgetValuesForOSGI();
            break;
        default:
            createOptionsForWS(left, font);
            break;
        }
    }

    @Override
    protected void restoreWidgetValues() {

        IDialogSettings settings = getDialogSettings();
        if (settings != null) {
            String[] directoryNames = settings.getArray(PETALS_EXPORT_DESTINATIONS);
            if (directoryNames == null || directoryNames.length == 0) {
                return;
            }

            if (directoryNames[0].endsWith(getPetalsDefaultSaName())) {
                setDestinationValue(directoryNames[0]);
                saDestinationFilePath = directoryNames[0];
            }

            for (String directoryName : directoryNames) {
                addDestinationItem(directoryName);
            }
        }
    }

    private void createOptionsforPetalsESB(Composite left, Font font) {
        GridLayout layout;
        // Buttons
        singletonButton = new Button(left, SWT.CHECK | SWT.LEFT);
        singletonButton.setText(Messages.getString("PetalsJobScriptsExportWizardPage.SingletonJob")); //$NON-NLS-1$
        singletonButton.setFont(font);
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 1;
        singletonButton.setLayoutData(gd);
        singletonButton.setSelection(PetalsTemporaryOptionsKeeper.INSTANCE.isSingleton());
        singletonButton.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                boolean selection = singletonButton.getSelection();
                PetalsTemporaryOptionsKeeper.INSTANCE.setSingleton(selection);
            }
        });

        generateEndpointButton = new Button(left, SWT.CHECK | SWT.LEFT);
        generateEndpointButton.setText(Messages.getString("PetalsJobScriptsExportWizardPage.GenerateEndpoint")); //$NON-NLS-1$
        generateEndpointButton.setFont(font);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 1;
        generateEndpointButton.setLayoutData(gd);
        generateEndpointButton.setSelection(PetalsTemporaryOptionsKeeper.INSTANCE.isGenerateEndpoint());
        generateEndpointButton.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                boolean selection = generateEndpointButton.getSelection();
                PetalsTemporaryOptionsKeeper.INSTANCE.setGenerateEndpoint(selection);
            }
        });

        sourceButton = new Button(left, SWT.CHECK | SWT.LEFT);
        sourceButton.setText(Messages.getString("JobScriptsExportWizardPage.sourceFiles")); //$NON-NLS-1$
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 2;
        sourceButton.setLayoutData(gd);
        sourceButton.setSelection(true);
        sourceButton.setFont(font);

        validateByWsdlButton = new Button(left, SWT.CHECK | SWT.LEFT);
        validateByWsdlButton.setText("Validate Petals messages"); //$NON-NLS-1$
        validateByWsdlButton.setFont(font);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 1;
        validateByWsdlButton.setLayoutData(gd);
        validateByWsdlButton.setSelection(PetalsTemporaryOptionsKeeper.INSTANCE.isValidateByWsdl());
        validateByWsdlButton.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                boolean selection = validateByWsdlButton.getSelection();
                PetalsTemporaryOptionsKeeper.INSTANCE.setValidateByWsdl(selection);
                validateOptionsGroup();
            }
        });

        // Default context
        left = new Composite(optionsGroupComposite, SWT.NONE);
        left.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        layout = new GridLayout(2, false);
        layout.marginHeight = 0;
        layout.marginBottom = 7;
        layout.horizontalSpacing = 10;
        left.setLayout(layout);

        new Label(left, SWT.NONE).setText(Messages.getString("PetalsJobScriptsExportWizardPage.JobContext")); //$NON-NLS-1$
        contextCombo = new Combo(left, SWT.DROP_DOWN | SWT.READ_ONLY);
        gd = new GridData();
        gd.widthHint = 180;
        contextCombo.setLayoutData(gd);

        if (getProcessItem() != null) {
            List<String> contextNames = getJobContexts(getProcessItem());
            contextCombo.setItems(contextNames.toArray(new String[contextNames.size()]));
        }

        // Exposed contexts
        left = new Composite(optionsGroupComposite, SWT.NONE);
        left.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        layout = new GridLayout(2, false);
        layout.marginHeight = 0;
        layout.marginBottom = 7;
        layout.horizontalSpacing = 10;
        left.setLayout(layout);

        final Link exposedContextsLink = new Link(left, SWT.NONE);
        exposedContextsLink.setText(Messages.getString("PetalsJobScriptsExportWizardPage.EditTheExposedContexts")); //$NON-NLS-1$
        exposedContextsLink.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {

                ContextExportDialog dlg = new ContextExportDialog(getShell(), currentCtxTypes);
                if (dlg.open() == Window.OK) {
                    currentCtxTypes = dlg.getContexts();
                    String contextName = contextCombo.getItem(contextCombo.getSelectionIndex());
                    ctxToTypeDefs.put(contextName, currentCtxTypes);
                    contextCombo.notifyListeners(SWT.Selection, new Event());
                    validateOptionsGroup();
                }
            }
        });

        // Additional listeners
        contextCombo.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                widgetSelected(e);
            }

            @Override
            public void widgetSelected(SelectionEvent e) {

                int index = contextCombo.getSelectionIndex();
                if (index < 0) {
                    return;
                }

                // Get the context types
                String value = contextCombo.getItem(index);
                currentCtxTypes = ctxToTypeDefs.get(value);
                if (currentCtxTypes == null) {
                    try {
                        currentCtxTypes = TalendUtils.getWsdlSchemaForContexts(getProcessItem(), value);

                    } catch (PetalsExportException e1) {

                        currentCtxTypes = new ArrayList<ContextTypeDefinition>(0);
                        MessageDialog.openError(getShell(), Messages.getString("PetalsJobScriptsExportWizardPage.ContextError"), //$NON-NLS-1$
                                Messages.getString("PetalsJobScriptsExportWizardPage.3")); //$NON-NLS-1$

                    } finally {
                        ctxToTypeDefs.put(value, currentCtxTypes);
                    }
                }

                PetalsTemporaryOptionsKeeper.INSTANCE.setContexts(currentCtxTypes);

                // Update the link label
                int exportedCtxCount = 0;
                for (ContextTypeDefinition ctx : currentCtxTypes) {
                    if (ctx.getExportType() != ContextExportType.NOT_EXPORTED) {
                        exportedCtxCount++;
                    }
                }

                exposedContextsLink.setText(Messages.getString("PetalsJobScriptsExportWizardPage.EditTheExposedContexts_") + exportedCtxCount + ")</a>"); //$NON-NLS-1$ //$NON-NLS-2$
                exposedContextsLink.setEnabled(currentCtxTypes.size() != 0);
            }
        });

        if (contextCombo.getItemCount() > 0) {
            contextCombo.select(0);
            contextCombo.notifyListeners(SWT.Selection, new Event());
        }
    }

    @Override
    protected boolean validateOptionsGroup() {

        boolean isValid = false;
        if (super.validateOptionsGroup()) {

            // WSDL-based validation can only be checked if there is no attachment
            boolean hasAttachment = false;
            for (int i = 0; !hasAttachment && currentCtxTypes != null && i < currentCtxTypes.size(); i++) {
                ContextTypeDefinition def = currentCtxTypes.get(i);
                hasAttachment = def.getExportType() != ContextExportType.NOT_EXPORTED
                        || def.getExportType() != ContextExportType.PARAMETER;
            }

            if (hasAttachment && PetalsTemporaryOptionsKeeper.INSTANCE.isValidateByWsdl()) {
                setErrorMessage(Messages.getString("PetalsJobScriptsExportWizardPage.WsdlBasedValidationNotSupported")); //$NON-NLS-1$
                isValid = false;
            } else {
                setErrorMessage(null);
                isValid = true;
            }

            if (getCurrentExportType1().equals(JobExportType.PETALSESB)) {
                if (isMultiNodes()) {
                    setErrorMessage(Messages.getString("JavaJobScriptsExportWSWizardPage.singleJobExport"));
                    isValid = false;
                }
            }
        }

        setPageComplete(isValid);
        return isValid;
    }

    private void createOptionsForJbossESB(Composite left, Font font) {
        contextButton = new Button(left, SWT.CHECK | SWT.LEFT);
        contextButton.setText(Messages.getString("JobScriptsExportWizardPage.contextPerlScripts")); //$NON-NLS-1$
        contextButton.setSelection(true);
        contextButton.setFont(font);

        String jobLabel = ""; //$NON-NLS-1$
        contextCombo = new Combo(left, SWT.PUSH);
        if (getProcessItem() != null) {
            try {
                setProcessItem((ProcessItem) ProxyRepositoryFactory.getInstance()
                        .getUptodateProperty(getProcessItem().getProperty()).getItem());
            } catch (PersistenceException e) {
                e.printStackTrace();
            }
            jobLabel = (getProcessItem()).getProperty().getLabel();
            List<String> contextNames = getJobContexts(getProcessItem());
            contextCombo.setItems(contextNames.toArray(new String[contextNames.size()]));
            if (contextNames.size() > 0) {
                contextCombo.select(0);
            }
        }

        applyToChildrenButton = new Button(left, SWT.CHECK | SWT.LEFT);
        applyToChildrenButton.setText(Messages.getString("JavaJobScriptsExportWSWizardPage.ApplyToChildren")); //$NON-NLS-1$
        applyToChildrenButton.setSelection(true);

        jobItemButton = new Button(left, SWT.CHECK | SWT.LEFT);
        jobItemButton.setText(Messages.getString("JobScriptsExportWizardPage.sourceFiles")); //$NON-NLS-1$
        jobItemButton.setSelection(true);
        jobItemButton.setFont(font);
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 1;
        jobItemButton.setLayoutData(gd);

        Label esbTypeLabel = new Label(left, SWT.None);
        esbTypeLabel.setText(Messages.getString("JavaJobScriptsExportWSWizardPage.esbExportTypeLabel")); //$NON-NLS-1$

        esbTypeCombo = new Combo(left, SWT.PUSH);
        // commented by fwang on Jan 6,2013 for bug TDI-23827
        // gd = new GridData();
        // gd.horizontalSpan = 2;
        // esbTypeCombo.setLayoutData(gd);

        esbTypeCombo.add(ESBTYPE_JBOSS_MQ);
        esbTypeCombo.add(ESBTYPE_JBOSS_MESSAGING);
        esbTypeCombo.select(0);

        Label esbServiceNameLabel = new Label(left, SWT.RIGHT);
        esbServiceNameLabel.setText(Messages.getString("JavaJobScriptsExportWSWizardPage.esbServiceNameLabel")); //$NON-NLS-1$

        esbServiceName = new Text(left, SWT.BORDER);
        esbServiceName.setText("DefaultServiceName"); //$NON-NLS-1$
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 2;
        esbServiceName.setLayoutData(gd);

        Label esbCategoryLabel = new Label(left, SWT.None);
        esbCategoryLabel.setText(Messages.getString("JavaJobScriptsExportWSWizardPage.esbCategoryLabel")); //$NON-NLS-1$

        esbCategory = new Text(left, SWT.BORDER);
        esbCategory.setText("DefaultCategory"); //$NON-NLS-1$
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 2;
        esbCategory.setLayoutData(gd);

        Label queueLabel = new Label(left, SWT.None);
        queueLabel.setText(Messages.getString("JavaJobScriptsExportWSWizardPage.queueName")); //$NON-NLS-1$

        esbQueueMessageName = new Text(left, SWT.BORDER);
        esbQueueMessageName.setText(Messages.getString("JavaJobScriptsExportWSWizardPage.actionRequest", jobLabel)); //$NON-NLS-1$
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 2;
        esbQueueMessageName.setLayoutData(gd);
    }

    private void createOptionsForOSGIESB(Composite optionsComposite, Font font) {
        IExportJobResourcesService resourcesService = null;
        if (GlobalServiceRegister.getDefault().isServiceRegistered(IExportJobResourcesService.class)) {
            resourcesService = (IExportJobResourcesService) GlobalServiceRegister.getDefault().getService(
                    IExportJobResourcesService.class);
        }
        if (resourcesService == null) {
            return;
        }

        addBSButton = new Button(optionsComposite, SWT.CHECK | SWT.LEFT);
        addBSButton.setText("Add maven script"); //$NON-NLS-1$
        addBSButton.setFont(font);

        addBSButton.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                boolean show = addBSButton.getSelection();
                String destinationValue = getDestinationValue();
                if (destinationValue.endsWith(getOutputSuffix())) {
                    if (show) {
                        destinationValue = destinationValue.substring(0, destinationValue.indexOf(getOutputSuffix()))
                                + OUTPUT_FILE_SUFFIX;
                    }
                } else if (destinationValue.endsWith(OUTPUT_FILE_SUFFIX)) {
                    if (!show) {
                        destinationValue = destinationValue.substring(0, destinationValue.indexOf(OUTPUT_FILE_SUFFIX))
                                + getOutputSuffix();
                    }
                }
                setDestinationValue(destinationValue);
            }
        });

    }

    protected void createOptionsForWS(Composite optionsGroup, Font font) {

        webXMLButton = new Button(optionsGroup, SWT.CHECK | SWT.LEFT);
        webXMLButton.setText(Messages.getString("JavaJobScriptsExportWSWizardPage.WEBXML")); //$NON-NLS-1$
        webXMLButton.setFont(font);
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 1;
        webXMLButton.setLayoutData(gd);
        webXMLButton.setSelection(true);

        configFileButton = new Button(optionsGroup, SWT.CHECK | SWT.LEFT);
        configFileButton.setText(Messages.getString("JavaJobScriptsExportWSWizardPage.ServerConfigFile")); //$NON-NLS-1$
        configFileButton.setFont(font);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 2;
        configFileButton.setLayoutData(gd);
        configFileButton.setSelection(true);

        wsddButton = new Button(optionsGroup, SWT.CHECK | SWT.LEFT);
        wsddButton.setText(Messages.getString("JavaJobScriptsExportWSWizardPage.WSDDFile")); //$NON-NLS-1$
        wsddButton.setFont(font);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 1;
        wsddButton.setLayoutData(gd);
        wsddButton.setSelection(true);

        wsdlButton = new Button(optionsGroup, SWT.CHECK | SWT.LEFT);
        wsdlButton.setText(Messages.getString("JavaJobScriptsExportWSWizardPage.WSDLFile")); //$NON-NLS-1$
        wsdlButton.setFont(font);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 2;
        wsdlButton.setLayoutData(gd);
        wsdlButton.setSelection(true);

        jobScriptButton = new Button(optionsGroup, SWT.CHECK | SWT.LEFT);
        jobScriptButton.setText(Messages.getString("JobScriptsExportWizardPage.sourceFiles")); //$NON-NLS-1$
        jobScriptButton.setSelection(true);
        jobScriptButton.setFont(font);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 1;
        jobScriptButton.setLayoutData(gd);

        axisLibButton = new Button(optionsGroup, SWT.CHECK | SWT.LEFT);
        axisLibButton.setText(Messages.getString("JavaJobScriptsExportWSWizardPage.AxisLib")); //$NON-NLS-1$
        axisLibButton.setFont(font);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 2;
        axisLibButton.setLayoutData(gd);
        axisLibButton.setSelection(true);

        contextButton = new Button(optionsGroup, SWT.CHECK | SWT.LEFT);
        contextButton.setText(Messages.getString("JobScriptsExportWizardPage.contextPerlScripts")); //$NON-NLS-1$
        contextButton.setSelection(true);
        contextButton.setFont(font);

        contextCombo = new Combo(optionsGroup, SWT.PUSH);

        applyToChildrenButton = new Button(optionsGroup, SWT.CHECK | SWT.LEFT);
        applyToChildrenButton.setText(Messages.getString("JavaJobScriptsExportWSWizardPage.ApplyToChildren")); //$NON-NLS-1$
        applyToChildrenButton.setSelection(true);

        if (Log4jPrefsSettingManager.getInstance().isLog4jEnable()) {
            log4jButton = new Button(optionsGroup, SWT.CHECK | SWT.LEFT);
            log4jButton.setText(Messages.getString("JavaJobScriptsExportWSWizardPage.LOG4jLEVEL")); //$NON-NLS-1$
            log4jButton.setSelection(true);
            log4jButton.setFont(font);
            log4jButton.setSelection(false);
            log4jButton.addSelectionListener(new SelectionAdapter() {

                @Override
                public void widgetSelected(SelectionEvent e) {
                    if (log4jButton.getSelection()) {
                        log4jLevelCombo.setEnabled(true);
                    } else {
                        log4jLevelCombo.setEnabled(false);
                    }
                }
            });

            log4jLevelCombo = new Combo(optionsGroup, SWT.PUSH);
            gd = new GridData();
            gd.horizontalSpan = 2;
            log4jLevelCombo.setLayoutData(gd);
            log4jLevelCombo.setEnabled(false);
        }

        restoreWidgetValuesForWS();

        if (JobExportType.getTypeFromString(exportTypeCombo.getText()).equals(JobExportType.WSWAR)) {
            webXMLButton.setEnabled(false);
            webXMLButton.setSelection(true);
            configFileButton.setEnabled(false);
            configFileButton.setSelection(true);
            wsddButton.setEnabled(false);
            wsddButton.setSelection(true);
            wsdlButton.setEnabled(false);
            wsdlButton.setSelection(true);
            jobScriptButton.setEnabled(false);
            jobScriptButton.setSelection(true);
            axisLibButton.setEnabled(false);
            axisLibButton.setSelection(true);
            contextButton.setEnabled(false);
            contextButton.setSelection(true);
        }
    }

    public String getExtractOption() {
        if (chkButton != null && !chkButton.isDisposed()) {
            return String.valueOf(chkButton.getSelection());
        } else {
            return null;
        }
    }

    @Override
    public boolean isAddMavenScript() {
        if (addBSButton != null && !addBSButton.isDisposed()) {
            return addBSButton.getSelection();
        }

        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.ui.wizards.exportjob.JobScriptsExportWizardPage#checkExport()
     */
    @Override
    public boolean checkExport() {
        setErrorMessage(null);
        if (!super.checkExport()) {
            return false;
        }
        if (getCurrentExportType1().equals(JobExportType.PETALSESB)) {
            chkButton.setVisible(false);
            zipOption = null;
            if (isMultiNodes()) {
                setErrorMessage(Messages.getString("JavaJobScriptsExportWSWizardPage.singleJobExport"));
            }
            validateOptionsGroup();
        }
        if (getCurrentExportType1().equals(JobExportType.PETALSESB)) {
            if (isMultiNodes()) {
                setErrorMessage(Messages.getString("JavaJobScriptsExportWSWizardPage.singleJobExport"));
            }
        }
        if (getCurrentExportType1().equals(JobExportType.JBOSSESB)) {
            if (isMultiNodes()) {
                setErrorMessage(Messages.getString("JavaJobScriptsExportWSWizardPage.singleJobExport"));
            }
            // check if the needed librairy is installed.
            String requiredJar = "jbossesb-rosetta.jar"; //$NON-NLS-1$

            List<ModuleNeeded> toCheck = ModulesNeededProvider.getModulesNeeded();
            for (ModuleNeeded current : toCheck) {
                if (requiredJar.equals(current.getModuleName())) {
                    if (current.getStatus() == ELibraryInstallStatus.NOT_INSTALLED) {
                        StringBuilder buff = new StringBuilder();
                        buff.append(Messages.getString("JavaJobScriptsExportWSWizardPage.exportForJBoss")); //$NON-NLS-1$
                        buff.append(Messages.getString("JavaJobScriptsExportWSWizardPage.checkVersion")); //$NON-NLS-1$
                        setErrorMessage(buff.toString());
                        break;
                    }
                }
            }
        }
        if (getCurrentExportType1().equals(JobExportType.OSGI)) {
            if (isMultiNodes()) {
                setErrorMessage("This type of export support actually only a single job export.");
            }
        }
        if (getCheckNodes().length == 0) {
            setErrorMessage(Messages.getString("JavaJobScriptsExportWSWizardPage.needOneJobSelected"));
        }
        boolean noError = getErrorMessage() == null;

		// TESB-13867 Export limitations for ESB 'Jobs'
		// add extra checks.
		if (noError) {
			String errorMsg = presenter.extraCheck(getCurrentExportType1(), getCheckNodes());
			if (errorMsg != null) {
				setErrorMessage(errorMsg);
				return false;
			}
		}

        setPageComplete(noError);
        return noError;
    }

    @Override
    public boolean finish() {
        if (exportTypeCombo != null && JobExportType.getTypeFromString(exportTypeCombo.getText()).equals(JobExportType.PETALSESB)) {
            if (!ensureTargetFileIsValid(new File(saDestinationFilePath))) {
                return true;
            }
            File suFile = null;
            suFile = new File(getDestinationValue());
            // suFile = new File(new File(directory, suName).getAbsolutePath());
            suFile.exists();
            boolean ok = true;
            try {
                // Get the job description
                String desc = (getProcessItem()).getProperty().getDescription();

                // The super class packages the job in the SU file
                if ((ok = super.finish()) == true) {
                    if (desc == null) {
                        desc = ""; //$NON-NLS-1$
                    } else {
                        // Replace XML mark-up characters
                        desc = desc.replaceAll("<", "&lt;"); //$NON-NLS-1$ //$NON-NLS-2$
                        desc = desc.replaceAll(">", "&gt;"); //$NON-NLS-1$ //$NON-NLS-2$
                    }
                    suFile.exists();

                    // We now have to package it in the SA
                    File saFile = SaUtils.createSaForTalend(suFile, saDestinationFilePath, desc);
                    if (saFile == null || !saFile.exists()) {
                        ok = false;
                        MessageDialog.openError(getShell(), Messages.getString("PetalsJobScriptsExportWizardPage.SaExportError"), //$NON-NLS-1$
                                Messages.getString("PetalsJobScriptsExportWizardPage.SaExportErrorDetails")); //$NON-NLS-1$
                    }
                }

            } catch (Exception e) {
                ExceptionHandler.process(e);

            }

            return ok;
        }

        if (exportTypeCombo != null && JobExportType.getTypeFromString(exportTypeCombo.getText()).equals(JobExportType.JBOSSESB)) {
            if (getDialogSettings() != null) {
                IDialogSettings section = getDialogSettings().getSection(DESTINATION_FILE);
                if (section == null) {
                    section = getDialogSettings().addNewSection(DESTINATION_FILE);
                }
                section.put(ESB_EXPORT_TYPE, esbTypeCombo.getText());
                section.put(ESB_SERVICE_NAME, esbServiceName.getText());
                section.put(ESB_CATEGORY, esbCategory.getText());
                section.put(QUERY_MESSAGE_NAME, esbQueueMessageName.getText());
            }

        }
        return super.finish();
    }

}
