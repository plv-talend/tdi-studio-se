// ============================================================================
//
// Talend Community Edition
//
// Copyright (C) 2006-2007 Talend - www.talend.com
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
//
// ============================================================================
package org.talend.repository.registeruser;

import java.rmi.RemoteException;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.PlatformUI;
import org.talend.commons.exception.BusinessException;
import org.talend.core.CorePlugin;
import org.talend.core.GlobalServiceRegister;
import org.talend.core.model.general.ConnectionBean;
import org.talend.core.prefs.CorePreferenceInitializer;
import org.talend.core.prefs.PreferenceManipulator;
import org.talend.core.ui.branding.IBrandingService;
import org.talend.repository.registeruser.proxy.RegisterUserPortTypeProxy;

/**
 * DOC mhirt class global comment. Detailled comment <br/>
 * 
 * $Id$
 * 
 */
public class RegisterManagement {

    private static final int REGISTRATION_MAX_TRIES = 6;

    // REGISTRATION_DONE = 1 : registration OK
    private static final double REGISTRATION_DONE = 2;

    public static boolean register(String email, String country, boolean isProxyEnabled, String proxyHost,
            String proxyPort, String designerVersion, String projectLanguage, String osName, String osVersion,
            String javaVersion, long totalMemory, Long memRAM, int nbProc) throws BusinessException {
        boolean result = false;

        // if proxy is enabled
        if (isProxyEnabled) {
            // get parameter and put them in System.properties.
            System.setProperty("http.proxyHost", proxyHost); //$NON-NLS-1$
            System.setProperty("http.proxyPort", proxyPort); //$NON-NLS-1$

            // override automatic update parameters
            if (proxyPort != null && proxyPort.trim().equals("")) {
                proxyPort = null;
            }
            CorePreferenceInitializer.setProxy(proxyHost, proxyPort);
        }

        RegisterUserPortTypeProxy proxy = new RegisterUserPortTypeProxy();
        proxy.setEndpoint("http://www.talend.com/TalendRegisterWS/registerws.php"); //$NON-NLS-1$
        try {
            IBrandingService brandingService = (IBrandingService) GlobalServiceRegister.getDefault().getService(
                    IBrandingService.class);
            result = proxy.registerUserWithAllUserInformations(email, country, designerVersion, brandingService
                    .getShortProductName(), projectLanguage, osName, osVersion, javaVersion, totalMemory + "", memRAM
                    + "", nbProc + "");
            if (result) {
                PlatformUI.getPreferenceStore().setValue("REGISTRATION_DONE", 1); //$NON-NLS-1$
                PreferenceManipulator prefManipulator = new PreferenceManipulator(CorePlugin.getDefault()
                        .getPreferenceStore());
                // prefManipulator.addUser(email);
                // prefManipulator.setLastUser(email);

                // Create a default connection:
                if (prefManipulator.readConnections().isEmpty()) {
                    ConnectionBean recup = ConnectionBean.getDefaultConnectionBean();
                    recup.setUser(email);
                    recup.setComplete(true);
                    prefManipulator.addConnection(recup);
                }

            }
        } catch (RemoteException e) {
            decrementTry();
            throw new BusinessException(e);
        }
        return result;
    }

    /**
     * DOC mhirt Comment method "isProductRegistered".
     * 
     * @return
     */
    public static boolean isProductRegistered() {
        initPreferenceStore();
        IPreferenceStore prefStore = PlatformUI.getPreferenceStore();
        if ((prefStore.getInt("REGISTRATION_TRIES") > 1) && ((prefStore.getInt("REGISTRATION_DONE") != 1))) { //$NON-NLS-1$ //$NON-NLS-2$
            return false;
        }
        return true;
    }

    /**
     * DOC mhirt Comment method "init".
     * 
     * @return
     */
    private static void initPreferenceStore() {
        IPreferenceStore prefStore = PlatformUI.getPreferenceStore();
        if (prefStore.getDefaultInt("REGISTRATION_TRIES") == 0) { //$NON-NLS-1$
            prefStore.setDefault("REGISTRATION_TRIES", REGISTRATION_MAX_TRIES); //$NON-NLS-1$
        }
        if (prefStore.getDefaultInt("REGISTRATION_DONE") == 0) { //$NON-NLS-1$
            prefStore.setDefault("REGISTRATION_DONE", REGISTRATION_DONE); //$NON-NLS-1$
        }
    }

    /**
     * DOC mhirt Comment method "incrementTryNumber".
     */
    public static void decrementTry() {
        IPreferenceStore prefStore = PlatformUI.getPreferenceStore();
        prefStore.setValue("REGISTRATION_TRIES", prefStore.getInt("REGISTRATION_TRIES") - 1); //$NON-NLS-1$ //$NON-NLS-2$
    }

    // public static void main(String[] args) {
    // try {
    // boolean result = RegisterManagement.register("a@a.fr", "fr", "Beta2");
    // System.out.println(result);
    // } catch (BusinessException e) {
    // // TODO Auto-generated catch block
    // e.printStackTrace();
    // }
    // }
}
