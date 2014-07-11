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
package org.talend.repository.json.ui.dnd;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.Path;
import org.eclipse.emf.common.util.EList;
import org.talend.commons.ui.runtime.exception.ExceptionHandler;
import org.talend.core.GlobalServiceRegister;
import org.talend.core.model.components.IComponent;
import org.talend.core.model.components.IComponentsService;
import org.talend.core.model.metadata.EMetadataEncoding;
import org.talend.core.model.metadata.IMetadataTable;
import org.talend.core.model.metadata.MetadataToolHelper;
import org.talend.core.model.metadata.builder.connection.Connection;
import org.talend.core.model.process.IElement;
import org.talend.core.model.process.IElementParameter;
import org.talend.core.model.process.INode;
import org.talend.core.model.properties.Item;
import org.talend.core.model.repository.ERepositoryObjectType;
import org.talend.core.model.utils.AbstractDragAndDropServiceHandler;
import org.talend.core.model.utils.ContextParameterUtils;
import org.talend.core.model.utils.IComponentName;
import org.talend.core.repository.RepositoryComponentSetting;
import org.talend.core.utils.TalendQuoteUtils;
import org.talend.repository.json.node.JSONRepositoryNodeType;
import org.talend.repository.json.util.ConvertJSONString;
import org.talend.repository.json.util.EJSONRepositoryToComponent;
import org.talend.repository.model.RepositoryNode;
import org.talend.repository.model.json.JSONFileConnection;
import org.talend.repository.model.json.JSONFileConnectionItem;
import org.talend.repository.model.json.JSONFileNode;
import org.talend.repository.model.json.JSONXPathLoopDescriptor;
import org.talend.repository.model.json.SchemaTarget;

/**
 * DOC wanghong class global comment. Detailled comment
 */
public class JSONDragAndDropHandler extends AbstractDragAndDropServiceHandler {

    private static final String JSON = "JSON"; //$NON-NLS-1$

    private static final String MAP = "MAP"; //$NON-NLS-1$

    private static final String INPUT = "tFileInputJSON"; //$NON-NLS-1$

    private static final String OUTPUT = "tExtractJSONFields"; //$NON-NLS-1$

    @Override
    public boolean canHandle(Connection connection) {
        return connection instanceof JSONFileConnection;
    }

    @Override
    public Object getComponentValue(Connection connection, String value, IMetadataTable table, String targetComponent) {
        if (value != null && canHandle(connection)) {
            return getJSONRepositoryValue((JSONFileConnection) connection, value, table);
        }
        return null;
    }

    private Object getJSONRepositoryValue(JSONFileConnection connection, String value, IMetadataTable table) {
        boolean isInputModel = connection.isInputModel();
        EList list;
        JSONXPathLoopDescriptor xmlDesc = null;
        if (isInputModel) {
            list = connection.getSchema();
            xmlDesc = (JSONXPathLoopDescriptor) list.get(0);
        }
        if (value.equals("FILE_PATH")) { //$NON-NLS-1$//USE_URL  JSON_MAPPING JSON   URL_PATH VALIDATIONRULES
            if (isContextMode(connection, connection.getJSONFilePath())) {
                return connection.getJSONFilePath();
            } else {
                Path p = new Path(connection.getJSONFilePath());
                return TalendQuoteUtils.addQuotes(p.toPortableString());
            }
        }
        if (value.equals("OUT_FILE_PATH")) {
            if (connection.getOutputFilePath() == null) {
                return "";
            }
            if (isContextMode(connection, connection.getOutputFilePath())) {
                return connection.getOutputFilePath();
            } else {
                Path p = new Path(connection.getOutputFilePath());
                return TalendQuoteUtils.addQuotes(p.toPortableString());
            }
        }
        if (value.equals("LIMIT")) { //$NON-NLS-1$
            if ((xmlDesc == null) || (xmlDesc.getLimitBoucle() == null)) {
                return ""; //$NON-NLS-1$
            } else {
                return xmlDesc.getLimitBoucle().toString();
            }
        }
        if (value.equals("XPATH_QUERY")) { //$NON-NLS-1$
            if (xmlDesc == null) {
                return ""; //$NON-NLS-1$
            } else {
                if (isContextMode(connection, xmlDesc.getAbsoluteXPathQuery())) {
                    String xpath = getJsonXpath(xmlDesc);
                    return xpath;
                } else {
                    String xpath = getJsonXpath(xmlDesc);
                    return TalendQuoteUtils.addQuotes(xpath);
                }
            }
        }
        if (value.equals("ENCODING")) { //$NON-NLS-1$
            if (isContextMode(connection, connection.getEncoding())) {
                return connection.getEncoding();
            } else {
                if (connection.getEncoding() == null) {
                    // get the default encoding
                    return TalendQuoteUtils.addQuotes(EMetadataEncoding.getMetadataEncoding("").getName()); //$NON-NLS-1$
                } else {
                    return TalendQuoteUtils.addQuotes(connection.getEncoding());
                }
            }
        }
        if (value.equals("ROOT")) {
            return getOutputJSONValue(connection.getRoot());
        }
        if (value.equals("GROUP")) {
            return getOutputJSONValue(connection.getGroup());
        }
        if (value.equals("LOOP")) {
            return getOutputJSONValue(connection.getLoop());
        }

        if (value.equals("JSON_MAPPING")) {
            return getTableJSONMappingValue(connection);
        }

        return null;
    }

    private String getJsonXpath(JSONXPathLoopDescriptor xmlDesc) {
        String flag = xmlDesc.getFlag();
        String xpath = xmlDesc.getAbsoluteXPathQuery();
        if (flag != null && flag.equals(ConvertJSONString.ROOT)) {
            if (xpath.startsWith("/root")) {
                xpath = xpath.replaceFirst("/root", "");
            }
            if (xpath.length() == 0) {
                xpath = "/";
            }

        } else if (flag != null && flag.equals(ConvertJSONString.ROOT_OBJECT)) {
            if (xpath.startsWith("/root/object")) {
                xpath = xpath.replaceFirst("/root/object", "");
            } else if (xpath.startsWith("/root")) {
                xpath = xpath.replaceFirst("/root", "");
            }
            if (xpath.length() == 0) {
                xpath = "/";
            }
        }
        return xpath;
    }

    private List<Map<String, String>> getOutputJSONValue(EList list) {
        List<Map<String, String>> newList = new ArrayList<Map<String, String>>();
        for (int i = 0; i < list.size(); i++) {
            Map<String, String> map = new HashMap<String, String>();
            JSONFileNode node = (JSONFileNode) list.get(i);
            String defaultValue = node.getDefaultValue();
            if (defaultValue == null) {
                defaultValue = ""; //$NON-NLS-1$
            }
            map.put("VALUE", defaultValue);
            map.put("ORDER", String.valueOf(node.getOrder()));
            map.put("PATH", node.getJSONPath());
            map.put("ATTRIBUTE", node.getAttribute());
            map.put("COLUMN", node.getRelatedColumn());
            newList.add(map);
        }
        return newList;

    }

    private List<Map<String, Object>> getTableJSONMappingValue(Connection connection) {
        List<Map<String, Object>> tableInfo = new ArrayList<Map<String, Object>>();
        if (connection instanceof JSONFileConnection) {
            JSONFileConnection jsonConnection = (JSONFileConnection) connection;
            if (jsonConnection.isInputModel()) {
                EList objectList = jsonConnection.getSchema();
                JSONXPathLoopDescriptor jsonDesc = (JSONXPathLoopDescriptor) objectList.get(0);
                List<SchemaTarget> schemaTargets = jsonDesc.getSchemaTargets();
                tableInfo.clear();

                String tagName;
                for (int j = 0; j < schemaTargets.size(); j++) {
                    SchemaTarget schemaTarget = schemaTargets.get(j);
                    if (schemaTarget.getTagName() != null && !schemaTarget.getTagName().equals("")) { //$NON-NLS-1$
                        tagName = "" + schemaTarget.getTagName().trim().replaceAll(" ", "_"); //$NON-NLS-1$ //$NON-NLS-2$
                        tagName = MetadataToolHelper.validateColumnName(tagName, j);
                        Map<String, Object> map = new HashMap<String, Object>();
                        map.put("SCHEMA_COLUMN", tagName); //$NON-NLS-1$
                        String xpath = schemaTarget.getRelativeXPathQuery();

                        String flag = jsonDesc.getFlag();
                        if (flag != null && flag.equals(ConvertJSONString.ROOT)) {
                            if (xpath.startsWith("root/")) {
                                xpath = xpath.replaceFirst("root/", "");
                            }

                        } else if (flag != null && flag.equals(ConvertJSONString.ROOT_OBJECT)) {
                            if (xpath.startsWith("object/")) {
                                xpath = xpath.replaceFirst("object/", "");
                            }
                        }

                        map.put("QUERY", TalendQuoteUtils.addQuotes(xpath)); //$NON-NLS-1$
                        tableInfo.add(map);
                    }
                }
            }
        }
        return tableInfo;
    }

    @Override
    public List<IComponent> filterNeededComponents(Item item, RepositoryNode seletetedNode, ERepositoryObjectType type) {
        List<IComponent> neededComponents = new ArrayList<IComponent>();
        if (!(item instanceof JSONFileConnectionItem)) {
            return neededComponents;
        }
        IComponentsService service = (IComponentsService) GlobalServiceRegister.getDefault().getService(IComponentsService.class);
        Set<IComponent> components = service.getComponentsFactory().getComponents();
        JSONFileConnection connection = (JSONFileConnection) ((JSONFileConnectionItem) item).getConnection();
        for (IComponent component : components) {
            if (!connection.isInputModel()) {
                if (isValid(item, type, seletetedNode, component, "JSONOUTPUT") && !neededComponents.contains(component)) {
                    neededComponents.add(component);
                }
            } else {
                if (isValid(item, type, seletetedNode, component, JSON) && !neededComponents.contains(component)) {
                    neededComponents.add(component);
                }
            }

        }
        // List<IComponent> neededComponents = RepositoryComponentManager.filterNeededComponents(item, seletetedNode,
        // type);
        return neededComponents;
    }

    private boolean isValid(Item item, ERepositoryObjectType type, RepositoryNode seletetedNode, IComponent component,
            String repositoryType) {
        if (component == null || repositoryType == null) {
            return false;
        }
        String componentProductname = component.getRepositoryType();
        if (componentProductname != null && repositoryType.endsWith(componentProductname)
                && isSubValid(item, type, seletetedNode, component, repositoryType)) {
            return true;
        }

        return false;
    }

    protected boolean isSubValid(Item item, ERepositoryObjectType type, RepositoryNode seletetedNode, IComponent component,
            String repositoryType) {
        boolean tableWithMap = true;
        if (type == ERepositoryObjectType.METADATA_CON_TABLE) {
            if (component.getName().toUpperCase().endsWith(MAP)) {
                tableWithMap = false;

            }
        }
        return tableWithMap;
    }

    @Override
    public IComponentName getCorrespondingComponentName(Item item, ERepositoryObjectType type) {
        RepositoryComponentSetting setting = null;
        if (item instanceof JSONFileConnectionItem) {
            setting = new RepositoryComponentSetting();
            setting.setName(JSON);
            setting.setRepositoryType(JSON);
            setting.setWithSchema(true);
            setting.setInputComponent(INPUT);
            setting.setOutputComponent(OUTPUT);
            List<Class<Item>> list = new ArrayList<Class<Item>>();
            Class clazz = null;
            try {
                clazz = Class.forName(JSONFileConnectionItem.class.getName());
            } catch (ClassNotFoundException e) {
                ExceptionHandler.process(e);
            }
            list.add(clazz);
            setting.setClasses(list.toArray(new Class[0]));
        }

        return setting;
    }

    private void setJSONRepositoryValue(JSONFileConnection connection, INode node, String repositoryValue) {
    }

    @Override
    public ERepositoryObjectType getType(String repositoryType) {
        if (JSON.equals(repositoryType)) {
            return JSONRepositoryNodeType.JSON;
        }
        return null;
    }

    @Override
    public void handleTableRelevantParameters(Connection connection, IElement ele, IMetadataTable metadataTable) {
        if (ele == null || metadataTable == null) {
            return;
        }
        IElementParameter fileNameParameter = ele.getElementParameter(EJSONRepositoryToComponent.FILENAME.getParameterName());
        if (fileNameParameter != null) {
            String JSONPath = "";
            // metadataTable.getAdditionalProperties().get(JSONConstants.JSON_PATH);
            if (JSONPath != null) {
                fileNameParameter.setValue(TalendQuoteUtils.addQuotesIfNotExist(JSONPath));
            }
        }
    }

    @Override
    public void setComponentValue(Connection connection, INode node, String repositoryValue) {
        if (node != null && canHandle(connection)) {
            setJSONRepositoryValue((JSONFileConnection) connection, node, repositoryValue);
        }
    }

    private boolean isContextMode(Connection connection, String value) {
        if (connection == null || value == null) {
            return false;
        }

        if (connection.isContextMode() && ContextParameterUtils.isContainContextParam(value)) {
            return true;
        }
        return false;
    }
}
