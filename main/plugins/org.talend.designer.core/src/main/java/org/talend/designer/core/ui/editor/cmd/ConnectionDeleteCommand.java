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
package org.talend.designer.core.ui.editor.cmd;

import java.util.List;

import org.eclipse.gef.commands.Command;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.talend.core.model.process.INode;
import org.talend.core.model.process.INodeConnector;
import org.talend.designer.core.i18n.Messages;
import org.talend.designer.core.ui.editor.connections.Connection;
import org.talend.designer.core.ui.editor.jobletcontainer.JobletContainer;
import org.talend.designer.core.ui.editor.nodes.Node;
import org.talend.designer.core.ui.editor.process.Process;

/**
 * Command that will delete a given connection. <br/>
 * 
 * $Id$
 * 
 */
public class ConnectionDeleteCommand extends Command {

    private List<Connection> connectionList;

    /**
     * Initialisation of the command that will delete the given connection.
     * 
     * @param connection connection to delete
     */
    public ConnectionDeleteCommand(List<Connection> connectionList) {
        setLabel(Messages.getString("ConnectionDeleteCommand.Label")); //$NON-NLS-1$
        this.connectionList = connectionList;
    }

    @Override
    public void execute() {
        Process process = (Process) connectionList.get(0).getSource().getProcess();
        for (Connection connection : connectionList) {
            boolean re = deleteExpendNode(connection);
            if (re) {
                return;
            }
            connection.disconnect();
            {
                /**
                 * Have been moved those codes to Connection.disconnect()/target.removeInput()
                 */
                // final INode target = connection.getTarget();
                // if (target.getExternalNode() instanceof AbstractNode) {
                // ((AbstractNode) target.getExternalNode()).removeInput(connection);
                // }
            }
            INodeConnector nodeConnectorSource, nodeConnectorTarget;
            nodeConnectorSource = connection.getSourceNodeConnector();
            if (nodeConnectorSource != null) {
                nodeConnectorSource.setCurLinkNbOutput(nodeConnectorSource.getCurLinkNbOutput() - 1);
            }
            nodeConnectorTarget = connection.getTargetNodeConnector();
            nodeConnectorTarget.setCurLinkNbInput(nodeConnectorTarget.getCurLinkNbInput() - 1);
        }
        process.checkStartNodes();
        process.checkProcess();
    }

    @Override
    public void undo() {
        Process process = (Process) connectionList.get(0).getSource().getProcess();
        for (Connection connection : connectionList) {
            collpseJoblet(connection);
            connection.reconnect();
            {
                /**
                 * Have been moved those codes to Connection.reconnect()
                 */
                // INode target = connection.getTarget();
                // if (target.getExternalNode() instanceof AbstractNode) {
                // ((AbstractNode) target.getExternalNode()).addInput(connection);
                // }
            }
            INodeConnector nodeConnectorSource, nodeConnectorTarget;
            nodeConnectorSource = connection.getSourceNodeConnector();
            if (nodeConnectorSource != null) {
                nodeConnectorSource.setCurLinkNbOutput(nodeConnectorSource.getCurLinkNbOutput() + 1);
            }

            nodeConnectorTarget = connection.getTargetNodeConnector();
            if (nodeConnectorTarget != null) {
                nodeConnectorTarget.setCurLinkNbInput(nodeConnectorTarget.getCurLinkNbInput() + 1);
            }

        }
        process.checkStartNodes();
        process.checkProcess();
    }

    private boolean deleteExpendNode(Connection connection) {
        final INode target = connection.getTarget();
        if (target instanceof Node && ((Node) target).getNodeContainer() != null) {
            if (((Node) target).getJobletNode() != null) {
                MessageDialog.openWarning(Display.getCurrent().getActiveShell(),
                        Messages.getString("ConnectionDeleteCommand.COLLAPSE"), //$NON-NLS-1$
                        Messages.getString("ConnectionDeleteCommand.COLLAPSEJOBLET")); //$NON-NLS-1$
                return true;
            }
            ((Node) target).getNodeContainer().getInputs().remove(connection);
        }
        final INode source = connection.getSource();
        if (source instanceof Node && ((Node) source).getNodeContainer() != null) {
            if (((Node) source).getJobletNode() != null) {
                MessageDialog.openWarning(Display.getCurrent().getActiveShell(),
                        Messages.getString("ConnectionDeleteCommand.COLLAPSE"), //$NON-NLS-1$
                        Messages.getString("ConnectionDeleteCommand.COLLAPSEJOBLET")); //$NON-NLS-1$
                return true;
            }
            ((Node) source).getNodeContainer().getOutputs().remove(connection);
        }
        return false;
    }

    private void collpseJoblet(Connection conn) {
        INode source = conn.getSource();
        if ((source instanceof Node) && ((Node) source).isJoblet()) {
            ((JobletContainer) ((Node) source).getNodeContainer()).setCollapsed(true);
        }
        INode target = conn.getTarget();
        if ((target instanceof Node) && ((Node) target).isJoblet()) {
            ((JobletContainer) ((Node) target).getNodeContainer()).setCollapsed(true);
        }
    }
}
