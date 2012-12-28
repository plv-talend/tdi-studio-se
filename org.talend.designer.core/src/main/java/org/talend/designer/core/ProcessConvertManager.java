// ============================================================================
//
// Copyright (C) 2006-2012 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.designer.core;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.talend.commons.ui.runtime.exception.ExceptionHandler;
import org.talend.core.model.process.IProcess;

/**
 * DOC zwzhao class global comment. Detailled comment
 */
public class ProcessConvertManager {

    public static List<IProcessConvertService> getProcessConvertService() {
        List<IProcessConvertService> processConvertServices = new ArrayList<IProcessConvertService>();
        IExtensionRegistry extensionRegistry = Platform.getExtensionRegistry();
        IExtensionPoint extensionPoint = extensionRegistry.getExtensionPoint("org.talend.designer.core.process_convert"); //$NON-NLS-1$
        if (extensionPoint != null) {
            IExtension[] extensions = extensionPoint.getExtensions();
            for (IExtension extension : extensions) {
                IConfigurationElement[] configurationElements = extension.getConfigurationElements();
                for (IConfigurationElement configurationElement : configurationElements) {
                    try {
                        Object service = configurationElement.createExecutableExtension("class"); //$NON-NLS-1$
                        if (service instanceof IProcessConvertService) {
                            processConvertServices.add((IProcessConvertService) service);
                        }
                    } catch (CoreException e) {
                        ExceptionHandler.process(e);
                    }
                }
            }
        }
        return processConvertServices;
    }
}
