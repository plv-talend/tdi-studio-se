package org.talend.designer.xmlmap.figures.anchors;

import org.eclipse.draw2d.ChopboxAnchor;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.PolylineConnection;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.ConnectionEditPart;
import org.eclipse.swt.SWT;
import org.talend.designer.xmlmap.figures.treeNode.TreeNodeFigure;
import org.talend.designer.xmlmap.model.emf.xmlmap.AbstractInOutTree;
import org.talend.designer.xmlmap.parts.AbstractInOutTreeEditPart;
import org.talend.designer.xmlmap.parts.BaseConnectionEditPart;
import org.talend.designer.xmlmap.parts.InputXmlTreeEditPart;
import org.talend.designer.xmlmap.parts.LookupConnectionEditPart;
import org.talend.designer.xmlmap.parts.TreeNodeEditPart;

public class ConnectionColumnAnchor extends ChopboxAnchor {

    private ConnectionEditPart connectionPart;

    private TreeNodeEditPart treeNodePart;

    private boolean forceDarshDot;

    public ConnectionColumnAnchor(IFigure owner, ConnectionEditPart connectionPart, TreeNodeEditPart treeNodePart,
            boolean forceDarshDot) {
        super(owner);
        this.connectionPart = connectionPart;
        this.treeNodePart = treeNodePart;
        this.forceDarshDot = forceDarshDot;
    }

    public ConnectionColumnAnchor(IFigure owner, ConnectionEditPart connectionPart, TreeNodeEditPart treeNodePart) {
        this(owner, connectionPart, treeNodePart, false);
    }

    public IFigure getOwner() {
        return super.getOwner();
    }

    public Point getReferencePoint() {
        if (connectionPart == null) {
            return getOwner().getBounds().getLeft();
        }

        AbstractInOutTreeEditPart inOutTreeEditPart = treeNodePart.getInOutTreeEditPart(treeNodePart);

        boolean loctionRight = false;
        if (inOutTreeEditPart instanceof InputXmlTreeEditPart) {
            loctionRight = true;
        }

        Point ref = null;

        if (inOutTreeEditPart != null && ((AbstractInOutTree) inOutTreeEditPart.getModel()).isMinimized()) {
            if (loctionRight) {
                ref = inOutTreeEditPart.getFigure().getBounds().getRight();
            } else {
                ref = inOutTreeEditPart.getFigure().getBounds().getLeft();
            }

        } else {
            if (getOwner() == null) {
                return null;
            } else if (getOwner() instanceof TreeNodeFigure) {
                TreeNodeFigure nodeFigure = (TreeNodeFigure) getOwner();

                if (nodeFigure.getElement() != null) {
                    if (loctionRight) {
                        ref = nodeFigure.getElement().getBounds().getRight();
                    } else {
                        ref = nodeFigure.getElement().getBounds().getLeft();

                    }
                    getOwner().translateToAbsolute(ref);

                }
            } else {
                ref = getOwner().getBounds().getCenter();
                getOwner().translateToAbsolute(ref);
            }

        }
        if (inOutTreeEditPart != null && ref != null) {
            IFigure treeFigure = inOutTreeEditPart.getFigure();
            if (loctionRight) {
                int avialableX = treeFigure.getBounds().getRight().x;
                if (ref.x > avialableX) {
                    ref.x = avialableX;
                }
            } else {
                int avialableX = treeFigure.getBounds().x;
                if (ref.x < avialableX) {
                    ref.x = avialableX;
                }
            }
        }

        if (connectionPart instanceof BaseConnectionEditPart && connectionPart.getFigure() instanceof PolylineConnection) {
            BaseConnectionEditPart baseConnectionPart = (BaseConnectionEditPart) connectionPart;
            PolylineConnection connFigure = (PolylineConnection) connectionPart.getFigure();

            org.eclipse.swt.graphics.Point avilableSize = treeNodePart.getViewer().getControl().getSize();

            if (forceDarshDot) {
                connFigure.setLineStyle(SWT.LINE_DASHDOTDOT);
            } else if (ref != null) {
                if (ref.y < 0) {
                    if (!(baseConnectionPart instanceof LookupConnectionEditPart)) {
                        ref.y = 0;
                    }
                    if (loctionRight) {
                        baseConnectionPart.setSourceConcained(false);
                    } else {
                        baseConnectionPart.setTargetContained(false);
                    }
                    if (baseConnectionPart.isDOTStyle()) {
                        connFigure.setLineStyle(SWT.LINE_DASHDOTDOT);
                    } else {
                        connFigure.setLineStyle(SWT.LINE_SOLID);
                    }
                } else if (ref.y > avilableSize.y) {
                    if (!(baseConnectionPart instanceof LookupConnectionEditPart)) {
                        ref.y = avilableSize.y;
                    }
                    if (loctionRight) {
                        baseConnectionPart.setSourceConcained(false);
                    } else {
                        baseConnectionPart.setTargetContained(false);
                    }
                    if (baseConnectionPart.isDOTStyle()) {
                        connFigure.setLineStyle(SWT.LINE_DASHDOTDOT);
                    } else {
                        connFigure.setLineStyle(SWT.LINE_SOLID);
                    }
                } else {
                    if (loctionRight) {
                        baseConnectionPart.setSourceConcained(true);
                    } else {
                        baseConnectionPart.setTargetContained(true);
                    }
                    if (!baseConnectionPart.isDOTStyle()) {
                        connFigure.setLineStyle(SWT.LINE_SOLID);
                    }
                }
            }
        }

        return ref;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.draw2d.ChopboxAnchor#getLocation(org.eclipse.draw2d.geometry.Point)
     */
    @Override
    public Point getLocation(Point reference) {
        Point referencePoint = getReferencePoint();
        if (referencePoint != null) {
            return new Point(referencePoint.x, referencePoint.y);
        } else {
            return new Point(0, 0);
        }

    }

    @Override
    protected Rectangle getBox() {
        return super.getBox();
    }

}