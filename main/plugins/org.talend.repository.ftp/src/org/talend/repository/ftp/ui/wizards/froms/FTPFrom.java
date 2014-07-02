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
package org.talend.repository.ftp.ui.wizards.froms;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;
import org.talend.commons.ui.swt.formtools.LabelledCombo;
import org.talend.commons.ui.swt.formtools.LabelledText;
import org.talend.core.model.metadata.builder.connection.FTPConnection;
import org.talend.core.model.properties.ConnectionItem;
import org.talend.repository.ftp.i18n.Messages;
import org.talend.repository.ftp.ui.wizards.pags.FTPPage;
import org.talend.repository.ui.swt.utils.AbstractForm;

/**
 * DOC hwang class global comment. Detailled comment
 */
public class FTPFrom extends AbstractForm {

    private static final String ENCODING = "ISO-8859-15"; //$NON-NLS-1$

    private static final String PUBLIC_KEY = "Public key"; //$NON-NLS-1$

    private static final String CUSTOM = "CUSTOM";//$NON-NLS-1$

    private static final String PASSWORD = "Password"; //$NON-NLS-1$

    private FTPPage page;

    private LabelledText ftpUsernameText;

    private LabelledText ftpPasswordText;

    private LabelledText ftpPortText;

    private LabelledText ftpHostText;

    private LabelledText proxyUsernameText;

    private LabelledText proxyPasswordText;

    private LabelledText proxyPortText;

    private LabelledText proxyHostText;

    private LabelledText privatekeyText;

    private LabelledText passphraseText;

    private LabelledText keyFileText;

    private LabelledText keyPasswordText;

    private LabelledCombo connModelCombo;

    private LabelledCombo encodeCombo;

    private LabelledCombo methodCombo;

    private Text customText;

    private Button sftpSuppBut;

    private Button ftpsSuppBut;

    private Button useSocksBut;

    private Composite sftpCom;

    private Composite ftpsCom;

    private Composite proxyCom;

    /**
     * DOC Administrator FTPFrom constructor comment.
     * 
     * @param parent
     * @param style
     */
    public FTPFrom(Composite parent, ConnectionItem connectionItem, String[] existingNames, FTPPage page) {
        super(parent, SWT.NONE, existingNames);
        this.connectionItem = connectionItem;
        this.page = page;
        setConnectionItem(connectionItem); // must be first.
        setupForm(false);
        layoutForm();
    }

    private void layoutForm() {
        GridLayout layout = (GridLayout) getLayout();
        layout.marginHeight = 0;
        setLayout(layout);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.ui.swt.utils.AbstractForm#adaptFormToReadOnly()
     */

    @Override
    protected void adaptFormToReadOnly() {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.ui.swt.utils.AbstractForm#addFields()
     */

    @Override
    protected void addFields() {
        Group ftpParameterGroup = new Group(this, SWT.NULL);
        ftpParameterGroup.setText("Server"); //$NON-NLS-1$
        GridLayout layoutGroup = new GridLayout();
        layoutGroup.numColumns = 2;
        ftpParameterGroup.setLayout(layoutGroup);

        GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
        ftpParameterGroup.setLayoutData(gridData);

        ftpUsernameText = new LabelledText(ftpParameterGroup, "Username", true); //$NON-NLS-1$

        ftpPasswordText = new LabelledText(ftpParameterGroup, "Password", 1, SWT.BORDER | SWT.PASSWORD); //$NON-NLS-1$

        ftpHostText = new LabelledText(ftpParameterGroup, "Host", true); //$NON-NLS-1$

        ftpPortText = new LabelledText(ftpParameterGroup, "Port", true); //$NON-NLS-1$

        Composite com = new Composite(ftpParameterGroup, SWT.NONE);
        layoutGroup = new GridLayout();
        layoutGroup.numColumns = 2;
        com.setLayout(layoutGroup);
        List<String> codeList = new ArrayList<String>();
        codeList.add(ENCODING);
        codeList.add("UTF-8"); //$NON-NLS-1$
        codeList.add(CUSTOM);
        encodeCombo = new LabelledCombo(com, "Encoding", "", codeList); //$NON-NLS-1$ //$NON-NLS-2$
        if (getConnection().getEcoding() == null || "".equals(getConnection().getEcoding())) { //$NON-NLS-1$
            encodeCombo.setText(ENCODING);
            getConnection().setEcoding(encodeCombo.getText());
        }

        customText = new Text(ftpParameterGroup, SWT.BORDER | SWT.SINGLE);
        GridData gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
        gridData.horizontalSpan = 1;
        customText.setLayoutData(gd);

        List<String> connList = new ArrayList<String>();
        connList.add("Passive"); //$NON-NLS-1$
        connList.add("Active"); //$NON-NLS-1$
        connModelCombo = new LabelledCombo(ftpParameterGroup, Messages.getString("FTPFrom_conn_model"), "", connList); //$NON-NLS-1$ //$NON-NLS-2$
        if (getConnection().getMode() == null || "".equals(getConnection().getMode())) { //$NON-NLS-1$
            connModelCombo.setText(Messages.getString("FTPFrom_passive")); //$NON-NLS-1$
            getConnection().setMode(connModelCombo.getText());
        }
        // Composite com = new Composite(mdmParameterGroup, SWT.NONE);

        Group buildGroup = new Group(this, SWT.NULL);
        buildGroup.setText("Parameter"); //$NON-NLS-1$
        layoutGroup = new GridLayout();
        layoutGroup.numColumns = 1;
        buildGroup.setLayout(layoutGroup);

        gridData = new GridData(GridData.FILL_BOTH);
        buildGroup.setLayoutData(gridData);

        sftpCom = new Composite(buildGroup, SWT.NONE);
        layoutGroup = new GridLayout();
        layoutGroup.numColumns = 2;
        sftpCom.setLayout(layoutGroup);

        gridData = new GridData(GridData.FILL_BOTH);
        sftpCom.setLayoutData(gridData);

        sftpSuppBut = new Button(sftpCom, SWT.CHECK);
        sftpSuppBut.setText(Messages.getString("FTPFrom_sftp_suport")); //$NON-NLS-1$
        ftpsSuppBut = new Button(sftpCom, SWT.CHECK);
        ftpsSuppBut.setText(Messages.getString("FTPFrom_ftps_support")); //$NON-NLS-1$
        List<String> list = new ArrayList<String>();
        list.add(PUBLIC_KEY);
        list.add(PASSWORD);

        methodCombo = new LabelledCombo(sftpCom, Messages.getString("FTPFrom_authen_method"), "", list); //$NON-NLS-1$ //$NON-NLS-2$
        methodCombo.setVisible(false);

        privatekeyText = new LabelledText(sftpCom, "Private key", true); //$NON-NLS-1$
        passphraseText = new LabelledText(sftpCom, "Key Passphrase", true); //$NON-NLS-1$
        privatekeyText.setVisible(false);
        passphraseText.setVisible(false);

        keyFileText = new LabelledText(sftpCom, "Keystore File", true); //$NON-NLS-1$
        keyPasswordText = new LabelledText(sftpCom, "Keystore Password", true); //$NON-NLS-1$
        keyFileText.setVisible(false);
        keyPasswordText.setVisible(false);

        proxyCom = new Composite(buildGroup, SWT.NONE);
        proxyCom.setLayout(layoutGroup);
        proxyCom.setLayoutData(gridData);

        useSocksBut = new Button(proxyCom, SWT.CHECK);
        useSocksBut.setText(Messages.getString("FTPFrom_sccks_proxy")); //$NON-NLS-1$
        Composite c = new Composite(proxyCom, SWT.NONE);
        proxyHostText = new LabelledText(proxyCom, "Proxy Host", true); //$NON-NLS-1$
        proxyPortText = new LabelledText(proxyCom, "Proxy Prot", true); //$NON-NLS-1$
        proxyUsernameText = new LabelledText(proxyCom, "Proxy User", true); //$NON-NLS-1$
        proxyPasswordText = new LabelledText(proxyCom, "Proxy Password", true); //$NON-NLS-1$
        proxyHostText.setVisible(false);
        proxyPortText.setVisible(false);
        proxyUsernameText.setVisible(false);
        proxyPasswordText.setVisible(false);

        // addCheckButton(mdmParameterGroup);
        checkFieldsValue();
        // if (!verified) {
        // page.setPageComplete(false);
        // }

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.ui.swt.utils.AbstractForm#addFieldsListeners()
     */

    @Override
    protected void addFieldsListeners() {
        ftpUsernameText.addModifyListener(new ModifyListener() {

            @Override
            public void modifyText(ModifyEvent e) {
                getConnection().setUsername(ftpUsernameText.getText());
                checkFieldsValue();
            }
        });

        ftpPasswordText.addModifyListener(new ModifyListener() {

            @Override
            public void modifyText(ModifyEvent e) {
                getConnection().setPassword(ftpPasswordText.getText());
                checkFieldsValue();
            }
        });
        ftpPortText.addModifyListener(new ModifyListener() {

            @Override
            public void modifyText(ModifyEvent e) {
                getConnection().setPort(ftpPortText.getText());
                checkFieldsValue();
            }
        });
        ftpHostText.addModifyListener(new ModifyListener() {

            @Override
            public void modifyText(ModifyEvent e) {
                getConnection().setHost(ftpHostText.getText());
                checkFieldsValue();
            }
        });
        proxyUsernameText.addModifyListener(new ModifyListener() {

            @Override
            public void modifyText(ModifyEvent e) {
                getConnection().setProxyuser(proxyUsernameText.getText());
            }
        });
        proxyPasswordText.addModifyListener(new ModifyListener() {

            @Override
            public void modifyText(ModifyEvent e) {
                getConnection().setProxypassword(proxyPasswordText.getText());
            }
        });
        proxyPortText.addModifyListener(new ModifyListener() {

            @Override
            public void modifyText(ModifyEvent e) {
                getConnection().setProxyport(proxyPortText.getText());
            }
        });
        proxyHostText.addModifyListener(new ModifyListener() {

            @Override
            public void modifyText(ModifyEvent e) {
                getConnection().setProxyhost(proxyHostText.getText());
            }
        });
        keyFileText.addModifyListener(new ModifyListener() {

            @Override
            public void modifyText(ModifyEvent e) {
                getConnection().setKeystoreFile(keyFileText.getText());
            }
        });
        keyPasswordText.addModifyListener(new ModifyListener() {

            @Override
            public void modifyText(ModifyEvent e) {
                getConnection().setKeystorePassword(keyPasswordText.getText());
            }
        });
        connModelCombo.addModifyListener(new ModifyListener() {

            @Override
            public void modifyText(ModifyEvent e) {
                getConnection().setMode(connModelCombo.getText());
            }
        });
        encodeCombo.addModifyListener(new ModifyListener() {

            @Override
            public void modifyText(ModifyEvent e) {
                getConnection().setEcoding(encodeCombo.getText());
                if (CUSTOM.equals(getConnection().getEcoding())) {
                    customText.setVisible(true);
                    customText.setText(ENCODING);
                    getConnection().setCustomEncode(customText.getText());
                } else {
                    getConnection().setCustomEncode(getConnection().getEcoding());
                    customText.setVisible(false);
                }
            }
        });

        customText.addModifyListener(new ModifyListener() {

            @Override
            public void modifyText(ModifyEvent e) {
                getConnection().setCustomEncode(customText.getText());
            }
        });

        methodCombo.addModifyListener(new ModifyListener() {

            @Override
            public void modifyText(ModifyEvent e) {
                getConnection().setMethod(methodCombo.getText());
                if (PUBLIC_KEY.equals(getConnection().getMethod())) {
                    privatekeyText.setVisible(true);
                    passphraseText.setVisible(true);
                    privatekeyText.setText(getConnection().getPrivatekey() != null ? getConnection().getPrivatekey() : "");
                    passphraseText.setText(getConnection().getPassphrase() != null ? getConnection().getPassphrase() : ""); //$NON-NLS-1$
                    getConnection().setPrivatekey(privatekeyText.getText());
                    getConnection().setPassphrase(passphraseText.getText());
                } else {
                    getConnection().setPrivatekey(getConnection().getPrivatekey());
                    getConnection().setPassphrase(getConnection().getPassphrase());
                    privatekeyText.setVisible(false);
                    passphraseText.setVisible(false);
                }
                checkFieldsValue();
            }
        });

        privatekeyText.addModifyListener(new ModifyListener() {

            @Override
            public void modifyText(ModifyEvent e) {
                getConnection().setPrivatekey(privatekeyText.getText());
            }
        });

        passphraseText.addModifyListener(new ModifyListener() {

            @Override
            public void modifyText(ModifyEvent e) {
                getConnection().setPassphrase(passphraseText.getText());
            }
        });
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.ui.swt.utils.AbstractForm#addUtilsButtonListeners()
     */

    @Override
    protected void addUtilsButtonListeners() {
        sftpSuppBut.addSelectionListener(new SelectionAdapter() {

            /*
             * (non-Javadoc)
             * 
             * @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events.SelectionEvent)
             */

            @Override
            public void widgetSelected(SelectionEvent e) {
                boolean show = sftpSuppBut.getSelection();
                ftpsSuppBut.setVisible(!show);
                if (PUBLIC_KEY.equals(methodCombo.getText())) {
                    privatekeyText.setVisible(show);
                    passphraseText.setVisible(show);
                }
                methodCombo.setVisible(show);
                connModelCombo.setVisible(!show);
                getConnection().setSFTP(show);
                checkFieldsValue();
            }

        });

        ftpsSuppBut.addSelectionListener(new SelectionAdapter() {

            /*
             * (non-Javadoc)
             * 
             * @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events.SelectionEvent)
             */

            @Override
            public void widgetSelected(SelectionEvent e) {
                boolean show = ftpsSuppBut.getSelection();
                sftpSuppBut.setVisible(!show);
                keyFileText.setVisible(show);
                keyPasswordText.setVisible(show);
                connModelCombo.setVisible(!show);
                getConnection().setFTPS(show);
            }

        });

        useSocksBut.addSelectionListener(new SelectionAdapter() {

            /*
             * (non-Javadoc)
             * 
             * @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events.SelectionEvent)
             */

            @Override
            public void widgetSelected(SelectionEvent e) {
                boolean show = useSocksBut.getSelection();
                proxyHostText.setVisible(show);
                proxyPortText.setVisible(show);
                proxyUsernameText.setVisible(show);
                proxyPasswordText.setVisible(show);
                getConnection().setUsesocks(show);
            }

        });
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.ui.swt.utils.AbstractForm#checkFieldsValue()
     */

    @Override
    public boolean checkFieldsValue() {
        if (ftpHostText.getCharCount() == 0) {
            updateStatus(IStatus.ERROR, "Host can not be null!"); //$NON-NLS-1$
            return false;
        }

        if (ftpPortText.getCharCount() == 0) {
            updateStatus(IStatus.ERROR, "Port can not be null!"); //$NON-NLS-1$
            return false;
        }

        if (ftpUsernameText.getCharCount() == 0) {
            updateStatus(IStatus.ERROR, "Username can not be null!"); //$NON-NLS-1$
            return false;
        }

        // only check the Password model
        if (sftpSuppBut.getSelection() && methodCombo != null && PASSWORD.equals(methodCombo.getText())) {
            if (ftpPasswordText.getCharCount() == 0) {
                updateStatus(IStatus.ERROR, "Password can not be null!"); //$NON-NLS-1$
                return false;
            }
        }
        updateStatus(IStatus.OK, null);
        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.ui.swt.utils.AbstractForm#initialize()
     */

    @Override
    protected void initialize() {
        ftpUsernameText.setText(getConnection().getUsername());
        ftpPasswordText.setText(getConnection().getPassword());
        ftpPortText.setText(getConnection().getPort());
        ftpHostText.setText(getConnection().getHost());
        encodeCombo.setText(getConnection().getEcoding());
        if (CUSTOM.equals(encodeCombo.getText())) {
            customText.setVisible(true);
        } else {
            customText.setVisible(false);
        }
        if (getConnection().getCustomEncode() == null) {
            customText.setText(ENCODING);
        } else {
            customText.setText(getConnection().getCustomEncode());
        }
        connModelCombo.setText(getConnection().getMode());
        if (getConnection().isSFTP()) {
            ftpsSuppBut.setVisible(false);
            methodCombo.setVisible(true);
            connModelCombo.setVisible(false);
            sftpSuppBut.setSelection(getConnection().isSFTP());
            methodCombo.setText(getConnection().getMethod());
            if (PUBLIC_KEY.equals(getConnection().getMethod())) {
                privatekeyText.setVisible(true);
                passphraseText.setVisible(true);
                privatekeyText.setText(getConnection().getPrivatekey() != null ? getConnection().getPrivatekey() : "");
                passphraseText.setText(getConnection().getPassphrase() != null ? getConnection().getPassphrase() : ""); //$NON-NLS-1$
            }
        }
        if (getConnection().isFTPS()) {
            keyFileText.setVisible(true);
            sftpSuppBut.setVisible(false);
            keyPasswordText.setVisible(true);
            connModelCombo.setVisible(false);
            ftpsSuppBut.setSelection(getConnection().isFTPS());
            keyFileText.setText(getConnection().getKeystoreFile());
            keyPasswordText.setText(getConnection().getKeystorePassword());
        }
        if (getConnection().isUsesocks()) {
            useSocksBut.setSelection(getConnection().isUsesocks());
            proxyHostText.setVisible(true);
            proxyPortText.setVisible(true);
            proxyUsernameText.setVisible(true);
            proxyPasswordText.setVisible(true);
            proxyHostText.setText(getConnection().getProxyhost());
            proxyPortText.setText(getConnection().getProxyport());
            proxyUsernameText.setText(getConnection().getProxyuser());
            proxyPasswordText.setText(getConnection().getProxypassword());
        }
    }

    public void removeHideValue() {
        if (!connModelCombo.getCombo().isVisible()) {
            getConnection().setMode(""); //$NON-NLS-1$
        }
        if (!sftpSuppBut.isVisible()) {
            getConnection().setSFTP(false);
        }
        if (!ftpsSuppBut.isVisible()) {
            getConnection().setFTPS(false);
        }
        if (!useSocksBut.isVisible()) {
            getConnection().setUsesocks(false);
        }
        if (!methodCombo.getCombo().isVisible()) {
            getConnection().setMethod(""); //$NON-NLS-1$
        }
        if (!keyPasswordText.getTextControl().isVisible()) {
            getConnection().setKeystorePassword(""); //$NON-NLS-1$
        }
        if (!keyFileText.getTextControl().isVisible()) {
            getConnection().setKeystoreFile(""); //$NON-NLS-1$
        }
        if (!proxyHostText.getTextControl().isVisible()) {
            getConnection().setProxyhost(""); //$NON-NLS-1$
        }
        if (!proxyPortText.getTextControl().isVisible()) {
            getConnection().setProxyport(""); //$NON-NLS-1$
        }
        if (!proxyPasswordText.getTextControl().isVisible()) {
            getConnection().setProxypassword(""); //$NON-NLS-1$
        }
        if (!proxyUsernameText.getTextControl().isVisible()) {
            getConnection().setProxyuser(""); //$NON-NLS-1$
        }
        if (!customText.isVisible()) {
            getConnection().setCustomEncode(ENCODING);
        }
        if (getConnection().getCustomEncode() == null || "".equals(getConnection().getCustomEncode())) { //$NON-NLS-1$
            getConnection().setCustomEncode(ENCODING);
        }
    }

    protected FTPConnection getConnection() {
        return (FTPConnection) connectionItem.getConnection();
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        checkFieldsValue();
    }

}
