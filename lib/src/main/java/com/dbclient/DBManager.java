package com.dbclient;

import sqlcomponentizer.dbserializer.DBDeserializer;
import sqlcomponentizer.dbserializer.DBSerializer;
import sqlcomponentizer.dbserializer.DBSerializerException;
import sqlcomponentizer.dbserializer.DBSerializerPrimaryKeyMissingException;
import sqlcomponentizer.preparedstatement.ComponentizedPreparedStatement;
import sqlcomponentizer.preparedstatement.SQLTokens;
import sqlcomponentizer.preparedstatement.component.OrderByComponent;
import sqlcomponentizer.preparedstatement.component.PSComponent;
import sqlcomponentizer.preparedstatement.component.columns.As;
import sqlcomponentizer.preparedstatement.component.columns.aggregate.Count;
import sqlcomponentizer.preparedstatement.component.condition.SQLOperatorCondition;
import sqlcomponentizer.preparedstatement.component.condition.SQLOperators;
import sqlcomponentizer.preparedstatement.statement.DeleteComponentizedPreparedStatementBuilder;
import sqlcomponentizer.preparedstatement.statement.InsertIntoComponentizedPreparedStatementBuilder;
import sqlcomponentizer.preparedstatement.statement.SelectComponentizedPreparedStatementBuilder;
import sqlcomponentizer.preparedstatement.statement.UpdateComponentizedPreparedStatementBuilder;

import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class DBManager {

    /* Insert and Deep Insert */

    public static void insert(Connection conn, Object object) throws DBSerializerException, IllegalAccessException, SQLException, InterruptedException, DBSerializerPrimaryKeyMissingException, InvocationTargetException {
        // Get table name and table map
        String tableName = DBSerializer.getTableName(object.getClass());
        Map<String, Object> tableMap = DBSerializer.getTableMap(object);

        // Get if should generate primary key
        boolean shouldGeneratePrimaryKey = false;
        String primaryKeyName = null;
        try {
            // Set the primary key table name
            primaryKeyName = DBSerializer.getPrimaryKeyName(object.getClass());

            // If the primary key is null in the tableMap, set shouldGeneratePrimaryKey to true
            if (tableMap.get(primaryKeyName) == null)
                shouldGeneratePrimaryKey = true;

            // If the primary key has a value, return false
        } catch (DBSerializerPrimaryKeyMissingException e) {
            // If there is no primary key, then leave as false
        }

        // Insert into table with tableMap and build with shouldGeneratePrimaryKey
        ComponentizedPreparedStatement cps = InsertIntoComponentizedPreparedStatementBuilder.forTable(tableName).addColAndVals(tableMap).build(shouldGeneratePrimaryKey);

        // Get connection pool and insert
        List<Map<String, Object>> generatedKeyMaps = DBClient.updateReturnGeneratedKeys(conn, cps);

        // If shouldGeneratePrimaryKey, use the first element of the keySet of the first element of generatedKeyMaps to set the primary key TODO: Should this throw an exception
        if (shouldGeneratePrimaryKey) {
            for (String key: generatedKeyMaps.get(0).keySet()) {
                // Check if the generatedKey is a BigInteger and cast to Long
                Object primaryKeyValue = generatedKeyMaps.get(0).get(key);

                if (primaryKeyValue instanceof BigInteger)
                    primaryKeyValue = ((BigInteger)primaryKeyValue).intValue();

                DBDeserializer.setPrimaryKey(object, primaryKeyValue);
                break;
            }
        }

    }

    public static Object deepInsert(Connection conn, Object object, boolean setWithDeepestSubObjectID) throws DBSerializerException, IllegalAccessException, DBSerializerPrimaryKeyMissingException, SQLException, InterruptedException, InvocationTargetException {
        // Create ID to be given by subobject
        Object id = null;

        // Insert sub objects first
        List<Object> subObjects = DBSerializer.getSubObjects(object);
        for (Object subObject: subObjects) {
            // Deep insert subObject
            id = deepInsert(conn, subObject, setWithDeepestSubObjectID);
        }

        // Set primary key as subobject's id if recursive
        if (id != null && setWithDeepestSubObjectID)
            DBDeserializer.setPrimaryKey(object, id);

        // Insert object
        insert(conn, object);

        // If shouldGetID, set the ID as the inserted object's primary key
        if (setWithDeepestSubObjectID) {
            id = DBSerializer.getPrimaryKey(object);
        }

        return id;
    }

    public static void deepInsert(Connection conn, Object object) throws DBSerializerException, IllegalAccessException, DBSerializerPrimaryKeyMissingException, SQLException, InterruptedException, InvocationTargetException {
        // Deep insert recursive with shouldGetID as false
        deepInsert(conn, object, false);
    }

    /* Delete Objects Where */

    public static void deleteWhere(Connection conn, Class dbClass, String whereCol, SQLOperators operator, Object whereVal) throws DBSerializerException, SQLException, InterruptedException {
        deleteWhere(
                conn,
                dbClass,
                Map.of(
                        whereCol, whereVal
                ),
                operator
        );
    }

    public static void deleteWhere(Connection conn, Class dbClass, Map<String, Object> whereColMap, SQLOperators commonOperator) throws DBSerializerException, SQLException, InterruptedException {
        List<PSComponent> sqlConditions = new ArrayList<>();

        whereColMap.forEach((k,v) -> sqlConditions.add(new SQLOperatorCondition(k, commonOperator, v)));

        deleteWhere(conn, dbClass, sqlConditions);
    }

    public static void deleteWhere(Connection conn, Class dbClass, List<PSComponent> sqlConditions) throws DBSerializerException, SQLException, InterruptedException {
        // Get tableName
        String tableName = DBSerializer.getTableName(dbClass);

        // Create CPS
        ComponentizedPreparedStatement cps = DeleteComponentizedPreparedStatementBuilder
                .forTable(tableName)
                .where(sqlConditions)
                .build();

        delete(conn, cps);
    }

    public static void delete(Connection conn, ComponentizedPreparedStatement cps) throws InterruptedException, SQLException {
        // Get connection
//        Connection conn = SQLConnectionPoolInstance.getConnection();

        // Do update
        DBClient.update(conn, cps);
    }

    /* Get All Objects Where */

    public static <T> List<T> selectAllByPrimaryKey(Connection conn, Class<T> dbClass, Object primaryKey) throws DBSerializerPrimaryKeyMissingException, DBSerializerException, IllegalAccessException, SQLException, InterruptedException, InvocationTargetException, NoSuchMethodException, InstantiationException {
        String primaryKeyName = DBSerializer.getPrimaryKeyName(dbClass);

        return selectAllWhere(conn, dbClass, primaryKeyName, SQLOperators.EQUAL, primaryKey);
    }

    public static <T> List<T> selectAllWhere(Connection conn, Class<T> dbClass, String whereCol, SQLOperators operator, Object whereVal) throws DBSerializerException, SQLException, InterruptedException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        return selectAllWhere(conn, dbClass, Map.of(whereCol, whereVal), operator);
    }

    public static <T> List<T> selectAllWhere(Connection conn, Class<T> dbClass, Map<String, Object> colValMap, SQLOperators commonOperator) throws DBSerializerException, SQLException, InterruptedException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        // Create PS Component List
        List<PSComponent> sqlConditions = new ArrayList<>();

        // Add SQLOperatorConditions for each colValMap with the commonOperator
        colValMap.forEach((k, v) -> sqlConditions.add(new SQLOperatorCondition(k, commonOperator, v)));

        return selectAllWhere(conn, dbClass, sqlConditions);
    }

    public static <T> List<T> selectAllWhere(Connection conn, Class<T> dbClass, List<PSComponent> sqlConditions) throws DBSerializerException, InterruptedException, SQLException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        // Get tableName
        String tableName = DBSerializer.getTableName(dbClass);

        // Create CPS
        ComponentizedPreparedStatement cps = SelectComponentizedPreparedStatementBuilder.forTable(tableName)
                .select(SQLTokens.ALL)
                .where(sqlConditions)
                .build();

        return select(conn, dbClass, cps);
    }

    public static <T> List<T> selectAllWhereOrderByLimit(Connection conn, Class<T> dbClass, String whereCol, SQLOperators operator, Object whereVal, List<String> orderByColumns, OrderByComponent.Direction direction, Integer limit) throws DBSerializerException, SQLException, InterruptedException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        return selectAllWhereOrderByLimit(conn, dbClass, Map.of(whereCol, whereVal), operator, orderByColumns, direction, limit);
    }

    public static <T> List<T> selectAllWhereOrderByLimit(Connection conn, Class<T> dbClass, Map<String, Object> whereColValMap, SQLOperators commonOperator, List<String> orderByColumns, OrderByComponent.Direction direction, Integer limit) throws DBSerializerException, SQLException, InterruptedException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        // Create PS Component List
        List<PSComponent> sqlConditions = new ArrayList<>();

        // Add SQLOperatorConditions for each colValMap with the commonOperator
        whereColValMap.forEach((k, v) -> sqlConditions.add(new SQLOperatorCondition(k, commonOperator, v)));

        return selectAllWhereOrderByLimit(conn, dbClass, sqlConditions, orderByColumns, direction, limit);
    }

    public static <T> List<T> selectAllWhereOrderByLimit(Connection conn, Class<T> dbClass, List<PSComponent> sqlConditions, List<String> orderBoColumns, OrderByComponent.Direction direction, Integer limit) throws DBSerializerException, SQLException, InterruptedException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        // Get tableName
        String tableName = DBSerializer.getTableName(dbClass);

        //Create Select CPS
        ComponentizedPreparedStatement cps = SelectComponentizedPreparedStatementBuilder.forTable(tableName)
                .select(SQLTokens.ALL)
                .where(sqlConditions)
                .orderBy(direction, orderBoColumns)
                .limit(limit)
                .build();

        return select(conn, dbClass, cps);
    }

    public static <T> List<T> select(Connection conn, Class<T> dbClass, ComponentizedPreparedStatement cps) throws InterruptedException, DBSerializerException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException, SQLException {
        // Get resultMap
        List<Map<String, Object>> resultMaps = DBClient.query(conn, cps);

        // Create resultArray
        List<T> resultArray = new ArrayList<>();

        // Deserialize to array of objects of the class dbClass
        for (Map<String, Object> resultMap: resultMaps) {
            // Get objects of the class dbClass
            resultArray.add(DBDeserializer.createObjectFromMap(dbClass, resultMap));
        }

        // Return resultArray
        return resultArray;
    }

    protected static <T> T asdf() {
        return (T)"asdf";
    }

    /* Count Object Where */

    public static Long countObjectWhere(Connection conn, Class dbClass, String whereCol, SQLOperators operator, Object whereVal) throws DBSerializerException, SQLException, InterruptedException {
        return countObjectWhere(conn, dbClass, Map.of(whereCol, whereVal), operator);
    }

    public static Long countObjectWhere(Connection conn, Class dbClass, Map<String, Object> whereColValMap, SQLOperators commonOperator) throws DBSerializerException, SQLException, InterruptedException {
        // Create SQL condition list
        List<PSComponent> sqlConditions = new ArrayList<>();

        // Append whereColValMaps with commonOperator SQL condition list
        whereColValMap.forEach((k, v) -> sqlConditions.add(new SQLOperatorCondition(k, commonOperator, v)));

        return countObjectWhere(conn, dbClass, sqlConditions);
    }

    public static Long countObjectWhere(Connection conn, Class dbClass, List<PSComponent> sqlConditions) throws DBSerializerException, InterruptedException, SQLException {
        // Get tableName
        String tableName = DBSerializer.getTableName(dbClass);

        // Set tempID
        String tempID = "c";

        // Build componentized prepared statement
        ComponentizedPreparedStatement cps = SelectComponentizedPreparedStatementBuilder.forTable(tableName).select(new As(new Count(SQLTokens.ALL).toString(), tempID).toString()).where(sqlConditions).build();

        // Return result of countWithComponentizedPreparedStatement
        return countWithComponentizedPreparedStatement(conn, cps, tempID);
    }

    /* Count Object Where Inner Join */

    public static Long countObjectWhereInnerJoin(Connection conn, Class dbClass, String whereCol, SQLOperators operator, Object whereVal, Class joinClass, String... joinColumns) throws DBSerializerException, InterruptedException, SQLException {
        return countObjectWhereInnerJoin(conn, dbClass, Map.of(whereCol, whereVal), operator, joinClass, joinColumns);
    }

    public static Long countObjectWhereInnerJoin(Connection conn, Class dbClass, Map<String, Object> whereColValMap, SQLOperators commonOperator, Class joinClass, String... joinColumns) throws DBSerializerException, InterruptedException, SQLException {
        // Create SQL condition list
        List<PSComponent> sqlConditions = new ArrayList<>();

        // Add each from whereColValMap to sqlConditions using the common operator
        whereColValMap.forEach((k, v) -> sqlConditions.add(new SQLOperatorCondition(k, commonOperator, v)));

        return countObjectWhereInnerJoin(conn, dbClass, sqlConditions, joinClass, joinColumns);
    }

    public static Long countObjectWhereInnerJoin(Connection conn, Class dbClass, List<PSComponent> sqlConditions, Class joinClass, String... joinColumns) throws DBSerializerException, InterruptedException, SQLException {
        return countObjectWhereInnerJoin(conn, dbClass, sqlConditions, joinClass, Arrays.asList(joinColumns));
    }

    public static Long countObjectWhereInnerJoin(Connection conn, Class dbClass, List<PSComponent> sqlConditions, Class joinClass, List<String> joinColumns) throws DBSerializerException, InterruptedException, SQLException {
        // Get tableName
        String tableName = DBSerializer.getTableName(dbClass);
        String joinTableName = DBSerializer.getTableName(joinClass);

        // Set tempID
        String tempID = "c";

        // Build componentized prepared statement
        ComponentizedPreparedStatement cps = SelectComponentizedPreparedStatementBuilder.forTable(tableName).select(new As(new Count(SQLTokens.ALL).toString(), tempID).toString()).where(sqlConditions).innerJoin(joinTableName, joinColumns).build();

        // Return result of countWithComponentizedPreparedStatement
        return countWithComponentizedPreparedStatement(conn, cps, tempID);
    }

    public static Long countWithComponentizedPreparedStatement(Connection conn, ComponentizedPreparedStatement cps, String generatedKeyIdentifier) throws InterruptedException, SQLException {

        // Get result map
        List<Map<String, Object>> resultMapList = DBClient.query(conn, cps);

        // Should be the first object in the list and the only object in the map
        if (resultMapList.size() > 0) {
            if (resultMapList.get(0).containsKey(generatedKeyIdentifier)) {
                if (resultMapList.get(0).get(generatedKeyIdentifier) instanceof Long) {
                    return (Long)resultMapList.get(0).get(generatedKeyIdentifier);
                }
            }
        }

        // Return null if the object did not exist
        return null;
    }

    /* Count Object By Column Where */

    public static Integer countObjectByColumnWhere(Connection conn, Class dbClass, String byColumn, String whereCol, SQLOperators operator, Object whereVal) throws DBSerializerException, SQLException, InterruptedException {
        return countObjectByColumnWhere(conn, dbClass, byColumn, Map.of(whereCol, whereVal), operator);
    }

    public static Integer countObjectByColumnWhere(Connection conn, Class dbClass, String byColumn, Map<String, Object> whereColValMap, SQLOperators commonOperator) throws DBSerializerException, SQLException, InterruptedException {
        List<PSComponent> sqlConditions = new ArrayList<>();
        whereColValMap.forEach((k, v) -> sqlConditions.add(new SQLOperatorCondition(k, commonOperator, v)));
        return countObjectByColumnWhere(conn, dbClass, byColumn, sqlConditions);
    }

    public static Integer countObjectByColumnWhere(Connection conn, Class dbClass, String byColumn, List<PSComponent> sqlConditions) throws DBSerializerException, SQLException, InterruptedException {
        // Table, row=v
        String tableName = DBSerializer.getTableName(dbClass);

        ComponentizedPreparedStatement cps = SelectComponentizedPreparedStatementBuilder.forTable(tableName).select(byColumn).where(sqlConditions).build();

        List<Map<String, Object>> resultMap = DBClient.query(conn, cps);

        return resultMap.size();
    }

    /* Update Where */

    public static void updateWhere(Connection conn, Class dbClass, String toUpdateCol, Object newVal, String whereCol, SQLOperators operator, Object whereVal) throws DBSerializerException, SQLException, InterruptedException {
        updateWhere(
                conn,
                dbClass,
                Map.of(
                        toUpdateCol, newVal
                ),
                Map.of(
                        whereCol, whereVal
                ),
                operator
        );
    }

    public static void updateWhere(Connection conn, Class dbClass, Map<String, Object> toUpdateColValMap, Map<String, Object> whereColValMap, SQLOperators commonOperator) throws InterruptedException, DBSerializerException, SQLException {
        String tableName = DBSerializer.getTableName(dbClass);

        ComponentizedPreparedStatement cps = UpdateComponentizedPreparedStatementBuilder.forTable(tableName)
                .set(toUpdateColValMap)
                .where(whereColValMap, commonOperator)
                .build();

        update(conn, dbClass, cps);
    }

    public static void update(Connection conn, Class dbClass, ComponentizedPreparedStatement cps) throws InterruptedException, SQLException {
        DBClient.update(conn, cps);
    }

}
