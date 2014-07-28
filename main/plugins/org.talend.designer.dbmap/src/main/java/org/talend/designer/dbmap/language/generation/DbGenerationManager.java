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
package org.talend.designer.dbmap.language.generation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.talend.commons.ui.runtime.exception.ExceptionHandler;
import org.talend.commons.utils.StringUtils;
import org.talend.commons.utils.data.text.StringHelper;
import org.talend.core.model.metadata.IMetadataColumn;
import org.talend.core.model.metadata.IMetadataTable;
import org.talend.core.model.metadata.MetadataToolHelper;
import org.talend.core.model.process.EConnectionType;
import org.talend.core.model.process.IConnection;
import org.talend.core.model.process.IContext;
import org.talend.core.model.process.IContextParameter;
import org.talend.core.model.process.INode;
import org.talend.core.model.process.IProcess;
import org.talend.core.model.utils.ContextParameterUtils;
import org.talend.designer.dbmap.DbMapComponent;
import org.talend.designer.dbmap.external.data.ExternalDbMapData;
import org.talend.designer.dbmap.external.data.ExternalDbMapEntry;
import org.talend.designer.dbmap.external.data.ExternalDbMapTable;
import org.talend.designer.dbmap.i18n.Messages;
import org.talend.designer.dbmap.language.AbstractDbLanguage;
import org.talend.designer.dbmap.language.IDbLanguage;
import org.talend.designer.dbmap.language.IJoinType;
import org.talend.designer.dbmap.language.operator.IDbOperator;
import org.talend.designer.dbmap.language.operator.IDbOperatorManager;
import org.talend.designer.dbmap.model.tableentry.TableEntryLocation;

/**
 * DOC amaumont class global comment. Detailled comment <br/>
 * 
 * $Id$
 * 
 */
public abstract class DbGenerationManager {

    private Map<String, ExternalDbMapTable> nameToInputTable;

    private HashMap<String, ExternalDbMapTable> nameToVarsTable;

    protected IDbLanguage language;

    private final Set<String> aliasAlreadyDeclared = new HashSet<String>();

    protected String queryColumnsName = ""; //$NON-NLS-1$

    private String tabSpaceString = ""; //$NON-NLS-1$

    /**
     * DOC amaumont GenerationManager constructor comment.
     * 
     * @param language2
     */
    public DbGenerationManager(IDbLanguage language) {
        super();
        this.language = language;
    }

    /**
     * DOC amaumont Comment method "setInputTables".
     * 
     * @param inputTables
     */
    public void setInputTables(List<ExternalDbMapTable> inputTables) {
        // this.inputTables = inputTables;
        nameToInputTable = new HashMap<String, ExternalDbMapTable>(inputTables.size());
        for (ExternalDbMapTable table : inputTables) {
            nameToInputTable.put(table.getName(), table);
        }
    }

    /**
     * DOC amaumont Comment method "setInputTables".
     * 
     * @param varsTables
     */
    public void setVarsTables(List<ExternalDbMapTable> varsTables) {
        // this.inputTables = varsTables;
        nameToVarsTable = new HashMap<String, ExternalDbMapTable>(varsTables.size());
        for (ExternalDbMapTable table : varsTables) {
            nameToVarsTable.put(table.getName(), table);
        }
    }

    public ExternalDbMapTable getInputTable(String tableName) {
        return nameToInputTable.get(tableName);
    }

    public ExternalDbMapTable getVarsTable(String tableName) {
        return nameToVarsTable.get(tableName);
    }

    public boolean isInputTable(String tableName) {
        return getInputTable(tableName) != null;
    }

    public boolean isVarsTable(String tableName) {
        return getVarsTable(tableName) != null;
    }

    public String getTableColumnVariable(String tableName, String columnName) {
        return StringHelper.replacePrms(this.language.getTemplateTableColumnVariable(), new Object[] { tableName, columnName });
    }

    public String getGeneratedCodeTableColumnVariable(String tableName, String columnName) {
        return StringHelper.replacePrms(this.language.getTemplateGeneratedCodeTableColumnVariable(), new Object[] { tableName,
                columnName });
    }

    public String getTableColumnVariable(TableEntryLocation location) {
        return this.language.getLocation(location.tableName, location.columnName);
    }

    public String getVarsColumnVariable(String columnName) {
        return StringHelper.replacePrms(this.language.getTemplateVarsColumnVariable(), new Object[] { columnName });
    }

    public String indent(final int i) {
        return StringUtils.repeat("  ", i); //$NON-NLS-1$
    }

    public String getQueryColumnsName() {
        return this.queryColumnsName;
    }

    /**
     * DOC amaumont Comment method "ckeckConstraintsAreEmpty".
     * 
     * @param ExternalDbMapTable
     * @return
     */
    public boolean checkFiltersAreEmpty(ExternalDbMapTable outputTable) {
        List<ExternalDbMapEntry> constraints = outputTable.getCustomConditionsEntries();
        int lstSize = constraints.size();
        boolean oneConstraintIsNotEmpty = false;
        for (int i = 0; i < lstSize; i++) {

            String constraintExpression = (constraints.get(i)).getExpression();
            if (constraintExpression != null && constraintExpression.trim().length() > 0) {
                oneConstraintIsNotEmpty = true;
                break;
            }
        }
        return !oneConstraintIsNotEmpty;
    }

    /**
     * Getter for language.
     * 
     * @return the language
     */
    public IDbLanguage getLanguage() {
        return this.language;
    }

    public IDbOperatorManager getOperatorsManager() {
        return this.language.getOperatorsManager();
    }

    /**
     * DOC amaumont Comment method "removeUnmatchingEntriesWithColumnsOfMetadataTable".
     * 
     * @param outputTable
     * @param metadataTable
     */
    protected ExternalDbMapTable removeUnmatchingEntriesWithColumnsOfMetadataTable(ExternalDbMapTable externalDbMapTable,
            IMetadataTable metadataTable) {
        ExternalDbMapTable clonedTable = null;
        try {
            clonedTable = (ExternalDbMapTable) externalDbMapTable.clone();
        } catch (CloneNotSupportedException e) {
            ExceptionHandler.process(e);
        }

        List<ExternalDbMapEntry> metadataTableEntries = clonedTable.getMetadataTableEntries();

        if (metadataTableEntries != null) {

            HashMap<String, IMetadataColumn> hNameToColumn = new HashMap<String, IMetadataColumn>();

            List<IMetadataColumn> listColumns = metadataTable.getListColumns();
            for (IMetadataColumn column : listColumns) {
                hNameToColumn.put(column.getLabel(), column);
            }
            List<ExternalDbMapEntry> dbMapEntriesToRemove = new ArrayList<ExternalDbMapEntry>();
            for (ExternalDbMapEntry externalTableEntry : metadataTableEntries) {
                String entryName = externalTableEntry.getName();
                IMetadataColumn column = hNameToColumn.get(entryName);
                if (column == null) {
                    dbMapEntriesToRemove.add(externalTableEntry);
                }
            }
            metadataTableEntries.removeAll(dbMapEntriesToRemove);
        }
        return clonedTable;

    }

    /**
     * 
     * ggu Comment method "buildSqlSelect".
     * 
     * @param component
     * @param outputTableName
     * @return
     */
    public String buildSqlSelect(DbMapComponent component, String outputTableName) {
        return buildSqlSelect(component, outputTableName, ""); //$NON-NLS-1$
    }

    /**
     * 
     * ggu Comment method "buildSqlSelect".
     * 
     * @param component
     * @param outputTableName
     * @param tabSpaceString
     * @return
     */
    public String buildSqlSelect(DbMapComponent component, String outputTableName, String tabString) {
        queryColumnsName = "\""; //$NON-NLS-1$
        aliasAlreadyDeclared.clear();
        this.tabSpaceString = tabString;

        List<IConnection> outputConnections = (List<IConnection>) component.getOutgoingConnections();

        Map<String, IConnection> nameToOutputConnection = new HashMap<String, IConnection>();
        for (IConnection connection : outputConnections) {
            nameToOutputConnection.put(connection.getUniqueName(), connection);
        }

        ExternalDbMapData data = (ExternalDbMapData) component.getExternalData();

        StringBuilder sb = new StringBuilder();

        List<ExternalDbMapTable> outputTables = data.getOutputTables();
        int lstOutputTablesSize = outputTables.size();
        ExternalDbMapTable outputTable = null;
        for (int i = 0; i < lstOutputTablesSize; i++) {
            ExternalDbMapTable temp = outputTables.get(i);
            if (outputTableName.equals(temp.getName())) {
                outputTable = temp;
                break;
            }
        }

        if (outputTable != null) {

            IConnection connection = nameToOutputConnection.get(outputTable.getName());
            List<IMetadataColumn> columns = new ArrayList<IMetadataColumn>();
            if (connection != null) {
                IMetadataTable metadataTable = connection.getMetadataTable();
                if (metadataTable != null) {
                    columns.addAll(metadataTable.getListColumns());
                    // call this function seems no use now but only problems
                    // outputTable = removeUnmatchingEntriesWithColumnsOfMetadataTable(outputTable, metadataTable);
                }
            }
            sb.append("\""); //$NON-NLS-1$
            sb.append(DbMapSqlConstants.SELECT);
            sb.append(DbMapSqlConstants.NEW_LINE).append(tabSpaceString);

            List<ExternalDbMapEntry> metadataTableEntries = outputTable.getMetadataTableEntries();
            if (metadataTableEntries != null) {
                int lstSizeOutTableEntries = metadataTableEntries.size();
                for (int i = 0; i < lstSizeOutTableEntries; i++) {
                    ExternalDbMapEntry dbMapEntry = metadataTableEntries.get(i);
                    String expression = dbMapEntry.getExpression();
                    expression = initExpression(component, dbMapEntry);
                    expression = addQuoteForSpecialChar(expression, component);
                    // for (IMetadataColumn column : columns) {
                    // if (expression != null && column.getLabel().equals(dbMapEntry.getName())) {
                    //                            expression = expression.replaceFirst("." + dbMapEntry.getName(), //$NON-NLS-1$
                    //                                    "." + column.getOriginalDbColumnName()); //$NON-NLS-1$
                    // break;
                    // }
                    // }
                    expression += DbMapSqlConstants.SPACE + DbMapSqlConstants.AS + DbMapSqlConstants.SPACE + dbMapEntry.getName();
                    if (i > 0) {
                        sb.append(DbMapSqlConstants.COMMA);
                        sb.append(DbMapSqlConstants.SPACE);

                        queryColumnsName += DbMapSqlConstants.COMMA + DbMapSqlConstants.SPACE;
                    }
                    if (expression != null && expression.trim().length() > 0) {
                        sb.append(expression);

                        queryColumnsName += expression;
                    } else {
                        sb.append(DbMapSqlConstants.LEFT_COMMENT);
                        String str = outputTable.getName() + DbMapSqlConstants.DOT + dbMapEntry.getName();
                        sb.append(Messages.getString("DbGenerationManager.OuputExpSetMessage", str)); //$NON-NLS-1$
                        sb.append(DbMapSqlConstants.RIGHT_COMMENT);
                    }
                }
            }

            sb.append(DbMapSqlConstants.NEW_LINE).append(tabSpaceString);
            sb.append(DbMapSqlConstants.FROM);

            List<ExternalDbMapTable> inputTables = data.getInputTables();

            // load input table in hash
            boolean explicitJoin = false;
            int lstSizeInputTables = inputTables.size();
            Map<String, ExternalDbMapTable> nameToInputTable = new HashMap<String, ExternalDbMapTable>();
            for (int i = 0; i < lstSizeInputTables; i++) {
                ExternalDbMapTable inputTable = inputTables.get(i);
                nameToInputTable.put(inputTable.getName(), inputTable);
                IJoinType joinType = language.getJoin(inputTable.getJoinType());
                if (!language.unuseWithExplicitJoin().contains(joinType) && i > 0) {
                    explicitJoin = true;
                }

            }

            StringBuilder sbWhere = new StringBuilder();
            boolean isFirstClause = true;
            for (int i = 0; i < lstSizeInputTables; i++) {
                ExternalDbMapTable inputTable = inputTables.get(i);
                if (buildConditions(component, sbWhere, inputTable, false, isFirstClause)) {
                    isFirstClause = false;
                }
            }

            sb.append(DbMapSqlConstants.NEW_LINE).append(tabSpaceString);

            IJoinType previousJoinType = null;

            for (int i = 0; i < lstSizeInputTables; i++) {
                ExternalDbMapTable inputTable = inputTables.get(i);
                IJoinType joinType = null;
                if (i == 0) {
                    joinType = AbstractDbLanguage.JOIN.NO_JOIN;
                } else {
                    joinType = language.getJoin(inputTable.getJoinType());
                }
                boolean commaCouldBeAdded = !explicitJoin && i > 0;
                boolean crCouldBeAdded = false;
                if (language.unuseWithExplicitJoin().contains(joinType) && !explicitJoin) {
                    buildTableDeclaration(component, sb, inputTable, commaCouldBeAdded, crCouldBeAdded, false);

                } else if (!language.unuseWithExplicitJoin().contains(joinType) && explicitJoin) {
                    if (i > 0) {
                        if (previousJoinType == null) {
                            buildTableDeclaration(component, sb, inputTables.get(i - 1), commaCouldBeAdded, crCouldBeAdded, true);
                            previousJoinType = joinType;
                        } else {
                            sb.append(DbMapSqlConstants.NEW_LINE).append(tabSpaceString);
                        }
                        sb.append(DbMapSqlConstants.SPACE);
                    }
                    String labelJoinType = joinType.getLabel();
                    sb.append(labelJoinType);
                    sb.append(DbMapSqlConstants.SPACE);
                    if (joinType == AbstractDbLanguage.JOIN.CROSS_JOIN) {
                        ExternalDbMapTable nextTable = null;
                        if (i < lstSizeInputTables) {
                            nextTable = inputTables.get(i);
                            buildTableDeclaration(component, sb, nextTable, false, false, true);
                        }

                    } else {

                        // ExternalDbMapTable rightTable = joinLeftToJoinRightTables.get(inputTable.getName());
                        buildTableDeclaration(component, sb, inputTable, false, false, true);
                        // if (rightTable != null) {
                        // } else {
                        // sb.append(" <!! NO JOIN CLAUSES FOR '" + inputTable.getName() + "' !!> ");
                        // }
                        sb.append(DbMapSqlConstants.SPACE);
                        sb.append(DbMapSqlConstants.ON);
                        sb.append(DbMapSqlConstants.LEFT_BRACKET);
                        sb.append(DbMapSqlConstants.SPACE);
                        if (!buildConditions(component, sb, inputTable, true, true)) {
                            sb.append(DbMapSqlConstants.LEFT_COMMENT);
                            sb.append(DbMapSqlConstants.SPACE);
                            sb.append(Messages.getString("DbGenerationManager.conditionNotSet")); //$NON-NLS-1$
                            sb.append(DbMapSqlConstants.SPACE);
                            sb.append(DbMapSqlConstants.RIGHT_COMMENT);
                        }
                        sb.append(DbMapSqlConstants.SPACE);
                        sb.append(DbMapSqlConstants.RIGHT_BRACKET);
                    }

                }
            }
            /*
             * for addition conditions
             */
            // like as input.newcolumn1>100
            List<String> whereAddition = new ArrayList<String>();
            // olny pure start with group or order, like as order/group by input.newcolumn1
            List<String> byAddition = new ArrayList<String>();
            // like as input.newcolumn1>100 group/oder by input.newcolumn1
            List<String> containWhereAddition = new ArrayList<String>();
            // like as "OR/AND input.newcolumn1", will keep original
            List<String> originalWhereAddition = new ArrayList<String>();

            if (outputTable != null) {
                List<ExternalDbMapEntry> customConditionsEntries = outputTable.getCustomConditionsEntries();
                if (customConditionsEntries != null) {
                    for (ExternalDbMapEntry entry : customConditionsEntries) {
                        String exp = initExpression(component, entry);
                        if (exp != null && !DbMapSqlConstants.EMPTY.equals(exp.trim())) {
                            if (containWith(exp, DbMapSqlConstants.GROUP_BY_PATTERN, true)
                                    || containWith(exp, DbMapSqlConstants.ORDER_BY_PATTERN, true)) {
                                byAddition.add(exp);
                            } else if (containWith(exp, DbMapSqlConstants.GROUP_BY_PATTERN, false)
                                    || containWith(exp, DbMapSqlConstants.ORDER_BY_PATTERN, false)) {
                                containWhereAddition.add(exp);
                            } else if (containWith(exp, DbMapSqlConstants.OR, true)
                                    || containWith(exp, DbMapSqlConstants.AND, true)) {
                                originalWhereAddition.add(exp);
                            } else {
                                whereAddition.add(exp);
                            }
                        }
                    }
                }
            }

            String whereClauses = sbWhere.toString();

            boolean whereFlag = whereClauses.trim().length() > 0;
            boolean whereAddFlag = !whereAddition.isEmpty();
            boolean whereConntainFlag = !containWhereAddition.isEmpty();

            boolean whereOriginalFlag = !originalWhereAddition.isEmpty();
            if (whereFlag || whereAddFlag || whereConntainFlag || whereOriginalFlag) {
                sb.append(DbMapSqlConstants.NEW_LINE).append(tabSpaceString);
                sb.append(DbMapSqlConstants.WHERE);
            }
            if (whereFlag) {
                sb.append(whereClauses);
            }
            if (whereAddFlag) {
                for (int i = 0; i < whereAddition.size(); i++) {
                    if (i == 0 && whereFlag || i > 0) {
                        sb.append(DbMapSqlConstants.NEW_LINE).append(tabSpaceString);
                        sb.append(DbMapSqlConstants.SPACE);
                        sb.append(DbMapSqlConstants.AND);
                    }
                    sb.append(DbMapSqlConstants.SPACE);
                    sb.append(whereAddition.get(i));
                }
            }
            if (whereOriginalFlag) {
                for (String s : originalWhereAddition) {
                    sb.append(DbMapSqlConstants.NEW_LINE).append(tabSpaceString);
                    sb.append(DbMapSqlConstants.SPACE);
                    sb.append(s);
                }
            }
            if (whereConntainFlag) {
                for (int i = 0; i < containWhereAddition.size(); i++) {
                    if (i == 0 && (whereFlag || whereAddFlag) || i > 0) {
                        sb.append(DbMapSqlConstants.NEW_LINE).append(tabSpaceString);
                        sb.append(DbMapSqlConstants.SPACE);
                        sb.append(DbMapSqlConstants.AND);
                    }
                    sb.append(DbMapSqlConstants.SPACE);
                    sb.append(containWhereAddition.get(i));
                }
            }

            if (!byAddition.isEmpty()) {
                sb.append(DbMapSqlConstants.NEW_LINE).append(tabSpaceString);
                for (String s : byAddition) {
                    sb.append(s);
                    sb.append(DbMapSqlConstants.NEW_LINE).append(tabSpaceString);
                }
            }
        }

        List<String> contextList = getContextList(component);
        String sqlQuery = sb.toString();
        boolean haveReplace = false;
        for (String context : contextList) {
            if (sqlQuery.contains(context)) {
                sqlQuery = sqlQuery.replace(context, "\" +" + context + "+ \""); //$NON-NLS-1$ //$NON-NLS-2$
                haveReplace = true;
            }
            if (queryColumnsName.contains(context)) {
                queryColumnsName = queryColumnsName.replace(context, "\" +" + context + "+ \""); //$NON-NLS-1$ //$NON-NLS-2$
            }
        }
        if (!haveReplace) {
            List<String> connContextList = getConnectionContextList(component);
            for (String context : connContextList) {
                if (sqlQuery.contains(context)) {
                    sqlQuery = sqlQuery.replace(context, "\" +" + context + "+ \""); //$NON-NLS-1$ //$NON-NLS-2$
                }
                if (queryColumnsName.contains(context)) {
                    queryColumnsName = queryColumnsName.replace(context, "\" +" + context + "+ \""); //$NON-NLS-1$ //$NON-NLS-2$
                }
            }
        }
        sqlQuery = handleQuery(sqlQuery);
        queryColumnsName = handleQuery(queryColumnsName);

        return sqlQuery;
    }

    private String handleQuery(String query) {
        if (query != null) {
            if (!query.trim().endsWith("\"")) { //$NON-NLS-1$
                query = query + "\""; //$NON-NLS-1$
            } else if (query.trim().endsWith("\\\"")) { //$NON-NLS-1$
                query = query + " \""; //$NON-NLS-1$
            } else {
                if (query.trim().endsWith("+ \"")) { //$NON-NLS-1$
                    query = query.substring(0, query.lastIndexOf("+ \"")); //$NON-NLS-1$
                }
            }
        }
        return query;
    }

    private boolean containWith(String expression, String pattern, boolean start) {
        if (expression != null) {
            expression = expression.trim();
            try {
                if (start) {
                    pattern = DbMapSqlConstants.EMPTY + '^' + pattern;
                }
                Pattern regex = Pattern.compile(pattern, Pattern.CANON_EQ | Pattern.CASE_INSENSITIVE);
                Matcher regexMatcher = regex.matcher(expression);
                return regexMatcher.find();
            } catch (PatternSyntaxException ex) {
                //
            }
        }
        return false;
    }

    protected List<String> getContextList(DbMapComponent component) {
        List<String> contextList = new ArrayList<String>();
        IProcess process = component.getProcess();
        IContext context = process.getContextManager().getDefaultContext();
        List<IContextParameter> paraList = context.getContextParameterList();
        for (IContextParameter para : paraList) {
            contextList.add(ContextParameterUtils.JAVA_NEW_CONTEXT_PREFIX + para.getName());
        }
        return contextList;
    }

    /**
     * DOC amaumont Comment method "buildConditions".
     * 
     * @param sb
     * @param inputTable
     * @param writeForJoin TODO
     * @param isFirstClause TODO
     */
    private boolean buildConditions(DbMapComponent component, StringBuilder sb, ExternalDbMapTable inputTable,
            boolean writeForJoin, boolean isFirstClause) {
        List<ExternalDbMapEntry> inputEntries = inputTable.getMetadataTableEntries();
        int lstSizeEntries = inputEntries.size();
        boolean atLeastOneConditionWritten = false;
        for (int j = 0; j < lstSizeEntries; j++) {
            ExternalDbMapEntry dbMapEntry = inputEntries.get(j);
            if (writeForJoin == dbMapEntry.isJoin()) {
                boolean conditionWritten = buildCondition(component, sb, inputTable, isFirstClause, dbMapEntry, !writeForJoin);
                if (conditionWritten) {
                    atLeastOneConditionWritten = true;
                }
                if (isFirstClause && conditionWritten) {
                    isFirstClause = false;
                }
            }
        }
        return atLeastOneConditionWritten;
    }

    /**
     * DOC amaumont Comment method "buildCondition".
     * 
     * @param sbWhere
     * @param table
     * @param isFirstClause
     * @param dbMapEntry
     * @param writeCr TODO
     */
    private boolean buildCondition(DbMapComponent component, StringBuilder sbWhere, ExternalDbMapTable table,
            boolean isFirstClause, ExternalDbMapEntry dbMapEntry, boolean writeCr) {
        String expression = dbMapEntry.getExpression();
        expression = initExpression(component, dbMapEntry);
        IDbOperator dbOperator = getOperatorsManager().getOperatorFromValue(dbMapEntry.getOperator());
        boolean operatorIsSet = dbOperator != null;
        boolean expressionIsSet = expression != null && expression.trim().length() > 0;

        boolean conditionWritten = false;

        if (operatorIsSet) {

            if (writeCr) {
                sbWhere.append(DbMapSqlConstants.NEW_LINE).append(tabSpaceString);
                sbWhere.append(DbMapSqlConstants.SPACE);
            }
            if (!isFirstClause) {
                sbWhere.append(DbMapSqlConstants.SPACE);
                sbWhere.append(DbMapSqlConstants.AND);
                sbWhere.append(DbMapSqlConstants.SPACE);
            }
            String entryName = dbMapEntry.getName();
            entryName = getOriginalColumnName(entryName, component, table);
            String tableName = table.getName();
            if (table.getAlias() == null) {
                tableName = getHandledTableName(component, table.getName());
            } else {
                tableName = getHandledField(table.getName());
            }
            String locationInputEntry = language.getLocation(tableName, getHandledField(entryName));
            sbWhere.append(DbMapSqlConstants.SPACE);
            sbWhere.append(locationInputEntry);
            sbWhere.append(getSpecialRightJoin(table));

            sbWhere.append(DbMapSqlConstants.SPACE);
            if (operatorIsSet) {
                sbWhere.append(dbOperator.getOperator()).append(DbMapSqlConstants.SPACE);
            } else if (!operatorIsSet && expressionIsSet) {
                sbWhere.append(DbMapSqlConstants.LEFT_COMMENT);
                sbWhere.append(DbMapSqlConstants.SPACE);
                sbWhere.append(Messages.getString("DbGenerationManager.InputOperationSetMessage", entryName)); //$NON-NLS-1$
                sbWhere.append(DbMapSqlConstants.SPACE);
                sbWhere.append(DbMapSqlConstants.RIGHT_COMMENT);
            }
            if (operatorIsSet && !expressionIsSet && !dbOperator.isMonoOperand()) {
                String str = table.getName() + DbMapSqlConstants.DOT + entryName;
                sbWhere.append(DbMapSqlConstants.LEFT_COMMENT);
                sbWhere.append(DbMapSqlConstants.SPACE);
                sbWhere.append(Messages.getString("DbGenerationManager.InputExpSetMessage", str)); //$NON-NLS-1$
                sbWhere.append(DbMapSqlConstants.SPACE);
                sbWhere.append(DbMapSqlConstants.RIGHT_COMMENT);
            } else if (expressionIsSet) {
                sbWhere.append(expression);
                sbWhere.append(getSpecialLeftJoin(table));
            }
            conditionWritten = true;

        }

        return conditionWritten;
    }

    protected String getSpecialRightJoin(ExternalDbMapTable table) {
        return DbMapSqlConstants.EMPTY;
    }

    protected String getSpecialLeftJoin(ExternalDbMapTable table) {
        return DbMapSqlConstants.EMPTY;
    }

    /**
     * DOC amaumont Comment method "buildFromTableDeclaration".
     * 
     * @param sb
     * @param inputTable
     * @param commaCouldBeAdded
     * @param crCouldBeAdded TODO
     * @param writingInJoin TODO
     */
    private void buildTableDeclaration(DbMapComponent component, StringBuilder sb, ExternalDbMapTable inputTable,
            boolean commaCouldBeAdded, boolean crCouldBeAdded, boolean writingInJoin) {
        sb.append(DbMapSqlConstants.SPACE);
        String alias = inputTable.getAlias();
        if (alias != null) {
            List<IConnection> inputConnections = (List<IConnection>) component.getIncomingConnections();
            IConnection iconn = null;
            if (inputConnections != null) {
                iconn = this.getConnectonByName(inputConnections, inputTable.getTableName());
            }
            boolean isELTDBMap = false;
            if (iconn != null) {
                INode source = iconn.getSource();
                isELTDBMap = isELTDBMap(source);
            }
            if (isELTDBMap) {
                buildTableDeclaration(sb, inputTable, iconn);
            } else if (!aliasAlreadyDeclared.contains(inputTable.getName())) {
                if (crCouldBeAdded) {
                    sb.append(DbMapSqlConstants.NEW_LINE).append(tabSpaceString);
                }
                if (commaCouldBeAdded) {
                    sb.append(DbMapSqlConstants.COMMA);
                    sb.append(DbMapSqlConstants.SPACE);
                }
                sb.append(getHandledTableName(component, inputTable.getTableName()));
                sb.append(DbMapSqlConstants.SPACE);
                sb.append(getHandledField(alias));
                aliasAlreadyDeclared.add(alias);
            } else {
                if (writingInJoin) {
                    sb.append(getHandledTableName(component, inputTable.getName()));
                }
            }
        } else {
            if (crCouldBeAdded) {
                sb.append(DbMapSqlConstants.NEW_LINE).append(tabSpaceString);
            }
            if (commaCouldBeAdded) {
                sb.append(DbMapSqlConstants.COMMA);
                sb.append(DbMapSqlConstants.SPACE);
            }
            buildTableDeclaration(component, sb, inputTable);

        }
    }

    private IConnection getConnectonByName(List<IConnection> inputConnections, String metaTableName) {
        IConnection retConnection = null;
        for (IConnection iconn : inputConnections) {
            IMetadataTable metadataTable = iconn.getMetadataTable();
            String tName = iconn.getName();
            if (tName.equals(metaTableName) && metadataTable != null) {
                retConnection = iconn;
                break;
            }
        }
        return retConnection;
    }

    protected void buildTableDeclaration(DbMapComponent component, StringBuilder sb, ExternalDbMapTable inputTable) {
        List<IConnection> inputConnections = (List<IConnection>) component.getIncomingConnections();
        if (inputConnections != null) {
            IConnection iconn = this.getConnectonByName(inputConnections, inputTable.getTableName());
            buildTableDeclaration(sb, inputTable, iconn);
        }

    }

    private static boolean isELTDBMap(INode node) {
        return node.isELTComponent()
        /**
         * The source map node using EConnectionType.TABLE wrote Database, but it wrote data to the table defined in
         * *ELTOutput component rather than the table defined in the source map node ****So****,maybe should always
         * generate subsql when input node is a map ignoring the EConnectionType
         */
        // && iconn.getLineStyle() == EConnectionType.TABLE_REF
                && node.getComponent().getName().endsWith("Map"); //$NON-NLS-1$
    }

    /**
     * DOC cmeng Comment method "buildTableDeclaration".
     * 
     * @param sb
     * @param inputTable
     * @param iconn
     * @return
     */
    private void buildTableDeclaration(StringBuilder sb, ExternalDbMapTable inputTable, IConnection iconn) {
        if (iconn == null) {
            return;
        }
        boolean replace = false;
        String inputTableName = inputTable.getName();
        IMetadataTable metadataTable = iconn.getMetadataTable();
        INode source = iconn.getSource();
        String tableName = metadataTable.getTableName();
        if (isELTDBMap(source)) {
            DbMapComponent externalNode = null;
            if (source instanceof DbMapComponent) {
                externalNode = (DbMapComponent) source;
            } else {
                externalNode = (DbMapComponent) source.getExternalNode();
            }
            DbGenerationManager genManager = externalNode.getGenerationManager();
            String deliveredTable = genManager.buildSqlSelect(externalNode, tableName, tabSpaceString + "  "); //$NON-NLS-1$
            int begin = 1;
            int end = deliveredTable.length() - 1;
            if (begin <= end) {
                sb.append("(").append(DbMapSqlConstants.NEW_LINE).append(tabSpaceString).append("  "); //$NON-NLS-1$ //$NON-NLS-2$
                sb.append(deliveredTable.substring(begin, end)).append(DbMapSqlConstants.NEW_LINE).append(tabSpaceString)
                        .append(" ) "); //$NON-NLS-1$
            }
        }
        String tableColneName = tableName;
        tableColneName = MetadataToolHelper.validateTableName(tableColneName);
        if (inputTableName.contains(".") && tableName != null) { //$NON-NLS-1$
            MapExpressionParser mapParser2 = new MapExpressionParser("\\s*(\\w+)\\s*\\.\\s*(\\w+)\\s*"); //$NON-NLS-1$
            List<Map<String, String>> tableNameList = mapParser2.parseInTableEntryLocations(inputTableName);
            for (Map<String, String> tableNameMap : tableNameList) {
                Set<Entry<String, String>> setTable = tableNameMap.entrySet();
                Iterator<Entry<String, String>> iteTable = setTable.iterator();

                while (iteTable.hasNext()) {
                    Entry<String, String> tableEntry = iteTable.next();
                    String tableLabel = tableEntry.getKey();
                    String schemaValue = tableEntry.getValue();
                    if (tableLabel.equals(metadataTable.getLabel()) && tableColneName.equals(tableLabel)) {
                        sb.append(schemaValue);
                        sb.append("."); //$NON-NLS-1$
                        sb.append(tableName);
                        replace = true;
                    }
                }

            }
        } else if (tableName != null) {
            if (inputTableName.equals(metadataTable.getLabel()) && tableColneName.equals(inputTableName)) {
                sb.append(tableName);
                replace = true;
            }
        }
        if (!replace) {
            sb.append(inputTable.getName());
        }
    }

    protected String addQuoteForSpecialChar(String expression, DbMapComponent component) {
        return expression;
    }

    protected String initExpression(DbMapComponent component, ExternalDbMapEntry dbMapEntry) {
        String expression = dbMapEntry.getExpression();
        if (expression != null) {
            List<Map<String, String>> itemNameList = null;

            // MapExpressionParser mapParser = new MapExpressionParser("((\\s*(\\w+)\\s*\\.)*)(\\w+)");
            // List<String> parseInTableEntryLocations = mapParser.parseInTableEntryLocations2(expression);
            // for (String entryLocation : parseInTableEntryLocations) {
            //
            // }

            // context.schema.context.table.column
            // context.schema.table.column
            // schema.context.table.column
            // schema.table.column
            // table.column
            MapExpressionParser mapParser1 = new MapExpressionParser("((\\s*(\\w+)\\s*\\.)*)(\\w+)"); //$NON-NLS-1$
            itemNameList = mapParser1.parseInTableEntryLocations2(expression);

            if (itemNameList == null || itemNameList.isEmpty()) {
                MapExpressionParser mapParser2 = new MapExpressionParser("\\s*(\\w+)\\s*\\.\\s*(\\w+)\\s*"); //$NON-NLS-1$
                itemNameList = mapParser2.parseInTableEntryLocations(expression);
            }

            for (Map<String, String> itemNamemap : itemNameList) {
                Set<Entry<String, String>> set = itemNamemap.entrySet();
                Iterator<Entry<String, String>> ite = set.iterator();
                while (ite.hasNext()) {
                    Entry<String, String> entry = ite.next();
                    String columnValue = entry.getKey();
                    String tableValue = entry.getValue();

                    List<IConnection> inputConnections = (List<IConnection>) component.getIncomingConnections();
                    if (inputConnections == null) {
                        return expression;
                    }
                    for (IConnection iconn : inputConnections) {
                        IMetadataTable metadataTable = iconn.getMetadataTable();
                        String tName = iconn.getName();
                        if (tableValue.equals(tName) && metadataTable != null) {
                            List<IMetadataColumn> lColumn = metadataTable.getListColumns();
                            String tableName = metadataTable.getTableName();
                            String tableColneName = tableName;
                            tableColneName = MetadataToolHelper.validateTableName(tableColneName);
                            if (tableValue.contains(".") && tableName != null) { //$NON-NLS-1$
                                MapExpressionParser mapParser2 = new MapExpressionParser("\\s*(\\w+)\\s*\\.\\s*(\\w+)\\s*"); //$NON-NLS-1$
                                List<Map<String, String>> tableNameList = mapParser2.parseInTableEntryLocations(tableValue);

                                for (Map<String, String> tableNameMap : tableNameList) {
                                    Set<Entry<String, String>> setTable = tableNameMap.entrySet();
                                    Iterator<Entry<String, String>> iteTable = setTable.iterator();

                                    while (iteTable.hasNext()) {
                                        Entry<String, String> tableEntry = iteTable.next();
                                        String tableLabel = tableEntry.getKey();
                                        String schemaValue = tableEntry.getValue();
                                        if (tableLabel.equals(metadataTable.getLabel()) && tableColneName.equals(tableLabel)) {
                                            tableName = tableName.replaceAll("\\$", "\\\\\\$"); //$NON-NLS-1$//$NON-NLS-2$
                                            expression = expression.replaceFirst(tableValue, schemaValue + "." + tableName);
                                        }
                                    }

                                }
                            } else if (tableName != null) {
                                if (tableValue.equals(metadataTable.getLabel()) && tableColneName.equals(tableValue)) {
                                    tableName = tableName.replaceAll("\\$", "\\\\\\$"); //$NON-NLS-1$ //$NON-NLS-2$
                                    expression = expression.replaceFirst(tableValue, tableName);
                                }
                            }
                            for (IMetadataColumn co : lColumn) {
                                if (columnValue.equals(co.getLabel())) {
                                    String oriName = co.getOriginalDbColumnName();
                                    // if OriginalDbColumn is empty , still use label to generate sql
                                    if (oriName == null || "".equals(oriName)) { //$NON-NLS-1$
                                        continue;
                                    }
                                    // if it is temp delived table, use label to generate sql
                                    if (iconn.getLineStyle() == EConnectionType.TABLE_REF) {
                                        continue;
                                    }
                                    oriName = oriName.replaceAll("\\$", "\\\\\\$"); //$NON-NLS-1$ //$NON-NLS-2$
                                    expression = expression.replaceFirst("\\." + co.getLabel(), //$NON-NLS-1$
                                            "\\." + oriName); //$NON-NLS-1$
                                }
                            }

                        }
                    }
                }
            }

        }
        return expression;
    }

    private String getOriginalColumnName(String entryName, DbMapComponent component, ExternalDbMapTable table) {
        List<IConnection> inputConnections = (List<IConnection>) component.getIncomingConnections();
        if (inputConnections != null) {
            for (IConnection iconn : inputConnections) {
                IMetadataTable metadataTable = iconn.getMetadataTable();
                String tName = iconn.getName();
                String tableValue = table.getTableName();
                if (tableValue != null && tableValue.equals(tName) && metadataTable != null) {
                    List<IMetadataColumn> lColumn = metadataTable.getListColumns();
                    for (IMetadataColumn colu : lColumn) {
                        if (colu.getLabel().equals(entryName)) {
                            entryName = colu.getOriginalDbColumnName();
                            return entryName;
                        }
                    }
                }
            }
        }
        return entryName;
    }

    protected List<String> getConnectionContextList(DbMapComponent component) {
        List<String> contextList = new ArrayList<String>();
        List<IConnection> inputConnections = (List<IConnection>) component.getIncomingConnections();
        List<Map<String, String>> itemNameList = null;
        for (IConnection iconn : inputConnections) {
            String connName = iconn.getName();
            if (!ContextParameterUtils.containContextVariables(connName)) {
                continue;
            }

            MapExpressionParser mapParser1 = new MapExpressionParser("\\s*(\\w+)\\s*\\.\\s*(\\w+)\\s*\\.\\s*(\\w+)\\s*"); //$NON-NLS-1$
            itemNameList = mapParser1.parseInTableEntryLocations(connName);
            if (itemNameList == null) {
                return contextList;
            }
            IMetadataTable metadataTable = iconn.getMetadataTable();
            for (Map<String, String> itemNamemap : itemNameList) {
                Set<Entry<String, String>> set = itemNamemap.entrySet();
                Iterator<Entry<String, String>> ite = set.iterator();
                while (ite.hasNext()) {
                    Entry<String, String> entry = ite.next();
                    String tableValue = entry.getKey();
                    String schemaValue = entry.getValue();
                    if (metadataTable != null && tableValue.equals(metadataTable.getLabel())) {
                        if (!contextList.contains(schemaValue)) {
                            contextList.add(schemaValue);
                        }
                    }
                }
            }

        }
        return contextList;
    }

    protected String getHandledTableName(DbMapComponent component, String name) {
        return name;
    }

    protected String getHandledField(String field) {
        return field;
    }

}
