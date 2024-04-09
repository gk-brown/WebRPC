/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.httprpc.kilo.sql;

import org.httprpc.kilo.Required;
import org.httprpc.kilo.beans.BeanAdapter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static org.httprpc.kilo.util.Collections.mapOf;

/**
 * Provides support for programmatically constructing and executing SQL
 * queries.
 */
public class QueryBuilder {
    private Class<?> type;
    private StringBuilder sqlBuilder;
    private List<String> parameters;

    private List<Object> generatedKeys = null;

    private static final int INITIAL_CAPACITY = 1024;

    /**
     * Constructs a new query builder.
     */
    public QueryBuilder() {
        this(INITIAL_CAPACITY);
    }

    /**
     * Constructs a new query builder.
     *
     * @param capacity
     * The initial capacity.
     */
    public QueryBuilder(int capacity) {
        this(null, new StringBuilder(capacity), new LinkedList<>());
    }

    private QueryBuilder(Class<?> type, StringBuilder sqlBuilder, List<String> parameters) {
        this.type = type;
        this.sqlBuilder = sqlBuilder;
        this.parameters = parameters;
    }

    /**
     * Creates a "select" query.
     *
     * @param type
     * An annotated entity type.
     *
     * @return
     * A "select" query.
     */
    public static QueryBuilder select(Class<?> type) {
        if (type == null) {
            throw new IllegalArgumentException();
        }

        var tableName = getTableName(type);

        var sqlBuilder = new StringBuilder("select ");

        var i = 0;

        for (var entry : BeanAdapter.getProperties(type).entrySet()) {
            var accessor = entry.getValue().getAccessor();

            var column = accessor.getAnnotation(Column.class);

            if (column == null) {
                continue;
            }

            if (i > 0) {
                sqlBuilder.append(", ");
            }

            sqlBuilder.append(tableName);
            sqlBuilder.append(".");

            var columnName = column.value();

            sqlBuilder.append(columnName);

            var propertyName = entry.getKey();

            if (!columnName.equals(propertyName)) {
                sqlBuilder.append(" as ");
                sqlBuilder.append(propertyName);
            }

            i++;
        }

        if (i == 0) {
            throw new UnsupportedOperationException();
        }

        sqlBuilder.append(" from ");
        sqlBuilder.append(tableName);
        sqlBuilder.append("\n");

        return new QueryBuilder(type, sqlBuilder, new LinkedList<>());
    }

    /**
     * Creates an "insert" query.
     *
     * @param type
     * An annotated entity type.
     *
     * @return
     * An "insert" query.
     */
    public static QueryBuilder insert(Class<?> type) {
        if (type == null) {
            throw new IllegalArgumentException();
        }

        var tableName = getTableName(type);

        var sqlBuilder = new StringBuilder("insert into ");

        sqlBuilder.append(tableName);

        var columnNames = new LinkedList<String>();
        var parameters = new LinkedList<String>();

        for (var entry : BeanAdapter.getProperties(type).entrySet()) {
            var accessor = entry.getValue().getAccessor();

            var column = accessor.getAnnotation(Column.class);

            if (column == null) {
                continue;
            }

            var primaryKey = accessor.getAnnotation(PrimaryKey.class);

            if (primaryKey != null && primaryKey.generated()) {
                continue;
            }

            columnNames.add(column.value());
            parameters.add(entry.getKey());
        }

        if (columnNames.isEmpty()) {
            throw new UnsupportedOperationException();
        }

        sqlBuilder.append(" (");

        var i = 0;

        for (var columnName : columnNames) {
            if (i > 0) {
                sqlBuilder.append(", ");
            }

            sqlBuilder.append(columnName);

            i++;
        }

        sqlBuilder.append(") values (");

        for (var j = 0; j < i; j++) {
            if (j > 0) {
                sqlBuilder.append(", ");
            }

            sqlBuilder.append("?");
        }

        sqlBuilder.append(")\n");

        return new QueryBuilder(type, sqlBuilder, parameters);
    }

    /**
     * Creates an "update" query.
     *
     * @param type
     * An annotated entity type.
     *
     * @return
     * An "update" query.
     */
    public static QueryBuilder update(Class<?> type) {
        if (type == null) {
            throw new IllegalArgumentException();
        }

        var tableName = getTableName(type);

        var sqlBuilder = new StringBuilder("update ");

        sqlBuilder.append(tableName);
        sqlBuilder.append(" set ");

        var i = 0;

        var parameters = new LinkedList<String>();

        for (var entry : BeanAdapter.getProperties(type).entrySet()) {
            var accessor = entry.getValue().getAccessor();

            var column = accessor.getAnnotation(Column.class);

            if (column == null) {
                continue;
            }

            var primaryKey = accessor.getAnnotation(PrimaryKey.class);

            if (primaryKey != null && primaryKey.generated()) {
                continue;
            }

            if (i > 0) {
                sqlBuilder.append(", ");
            }

            var columnName = column.value();

            sqlBuilder.append(columnName);
            sqlBuilder.append(" = ");

            if (accessor.getAnnotation(Required.class) == null) {
                sqlBuilder.append("coalesce(?, ");
                sqlBuilder.append(columnName);
                sqlBuilder.append(")");
            } else {
                sqlBuilder.append("?");
            }

            parameters.add(entry.getKey());

            i++;
        }

        if (i == 0) {
            throw new UnsupportedOperationException();
        }

        sqlBuilder.append("\n");

        return new QueryBuilder(type, sqlBuilder, parameters);
    }

    /**
     * Creates a "delete" query.
     *
     * @param type
     * An annotated entity type.
     *
     * @return
     * A "delete" query.
     */
    public static QueryBuilder delete(Class<?> type) {
        if (type == null) {
            throw new IllegalArgumentException();
        }

        var tableName = getTableName(type);

        var sqlBuilder = new StringBuilder("delete from ");

        sqlBuilder.append(tableName);
        sqlBuilder.append("\n");

        return new QueryBuilder(type, sqlBuilder, new LinkedList<>());
    }

    private static String getTableName(Class<?> type) {
        var table = type.getAnnotation(Table.class);

        if (table == null) {
            throw new UnsupportedOperationException();
        }

        return table.value();
    }

    /**
     * Appends a "where" clause that filters on the primary key.
     *
     * @param key
     * The key of the argument representing the primary key value.
     *
     * @return
     * The {@link QueryBuilder} instance.
     */
    public QueryBuilder wherePrimaryKeyEquals(String key) {
        if (key == null) {
            throw new IllegalArgumentException();
        }

        if (type == null) {
            throw new IllegalStateException();
        }

        sqlBuilder.append("where ");

        for (var property : BeanAdapter.getProperties(type).values()) {
            var accessor = property.getAccessor();

            var column = accessor.getAnnotation(Column.class);

            if (column != null) {
                var primaryKey = accessor.getAnnotation(PrimaryKey.class);

                if (primaryKey != null) {
                    sqlBuilder.append(column.value());
                    sqlBuilder.append(" = ?\n");

                    parameters.add(key);

                    return this;
                }
            }
        }

        throw new UnsupportedOperationException();
    }

    /**
     * Appends arbitrary SQL text to a query. Named parameters can be declared
     * by prepending a colon to an argument name.
     *
     * @param text
     * The SQL text to append.
     *
     * @return
     * The {@link QueryBuilder} instance.
     */
    public QueryBuilder append(String text) {
        if (text == null) {
            throw new IllegalArgumentException();
        }

        var quoted = false;

        var n = text.length();
        var i = 0;

        while (i < n) {
            var c = text.charAt(i++);

            if (c == ':' && !quoted) {
                var parameterBuilder = new StringBuilder(32);

                while (i < n) {
                    c = text.charAt(i);

                    if (!Character.isJavaIdentifierPart(c)) {
                        break;
                    }

                    parameterBuilder.append(c);

                    i++;
                }

                if (parameterBuilder.isEmpty()) {
                    throw new IllegalArgumentException("Missing parameter name.");
                }

                parameters.add(parameterBuilder.toString());

                sqlBuilder.append("?");
            } else if (c == '?' && !quoted) {
                parameters.add(null);

                sqlBuilder.append(c);
            } else {
                if (c == '\'') {
                    quoted = !quoted;
                }

                sqlBuilder.append(c);
            }
        }

        return this;
    }

    /**
     * Appends arbitrary SQL text to a query, terminated by a newline character.
     *
     * @param text
     * The SQL text to append.
     *
     * @return
     * The {@link QueryBuilder} instance.
     */
    public QueryBuilder appendLine(String text) {
        append(text);

        sqlBuilder.append("\n");

        return this;
    }

    /**
     * Returns the query builder's parameters.
     *
     * @return
     * The query builder's parameters.
     */
    public List<String> getParameters() {
        return Collections.unmodifiableList(parameters);
    }

    /**
     * Prepares a query for execution.
     *
     * @param connection
     * The connection on which the query will be executed.
     *
     * @return
     * A prepared statement that can be used to execute the query.
     *
     * @throws SQLException
     * If an error occurs while preparing the query.
     */
    public PreparedStatement prepare(Connection connection) throws SQLException {
        if (connection == null) {
            throw new IllegalArgumentException();
        }

        return connection.prepareStatement(toString(), Statement.RETURN_GENERATED_KEYS);
    }

    /**
     * Executes a query.
     *
     * @param statement
     * The statement that will be used to execute the query.
     *
     * @return
     * The query results.
     *
     * @throws SQLException
     * If an error occurs while executing the query.
     */
    public ResultSet executeQuery(PreparedStatement statement) throws SQLException {
        return executeQuery(statement, mapOf());
    }

    /**
     * Executes a query.
     *
     * @param statement
     * The statement that will be used to execute the query.
     *
     * @param arguments
     * The query arguments.
     *
     * @return
     * The query results.
     *
     * @throws SQLException
     * If an error occurs while executing the query.
     */
    public ResultSet executeQuery(PreparedStatement statement, Map<String, ?> arguments) throws SQLException {
        if (statement == null || arguments == null) {
            throw new IllegalArgumentException();
        }

        apply(statement, arguments);

        return statement.executeQuery();
    }

    /**
     * Executes a query.
     *
     * @param statement
     * The statement that will be used to execute the query.
     *
     * @return
     * The number of rows that were affected by the query.
     *
     * @throws SQLException
     * If an error occurs while executing the query.
     */
    public int executeUpdate(PreparedStatement statement) throws SQLException {
        return executeUpdate(statement, mapOf());
    }

    /**
     * Executes a query.
     *
     * @param statement
     * The statement that will be used to execute the query.
     *
     * @param arguments
     * The query arguments.
     *
     * @return
     * The number of rows that were affected by the query.
     *
     * @throws SQLException
     * If an error occurs while executing the query.
     */
    public int executeUpdate(PreparedStatement statement, Map<String, ?> arguments) throws SQLException {
        if (statement == null || arguments == null) {
            throw new IllegalArgumentException();
        }

        apply(statement, arguments);

        var updateCount = statement.executeUpdate();

        try (var generatedKeys = statement.getGeneratedKeys()) {
            if (generatedKeys.next()) {
                var generatedKeysMetaData = generatedKeys.getMetaData();

                var n = generatedKeysMetaData.getColumnCount();

                this.generatedKeys = new ArrayList<>(n);

                for (var i = 0; i < n; i++) {
                    this.generatedKeys.add(generatedKeys.getObject(i + 1));
                }
            } else {
                this.generatedKeys = null;
            }
        }

        return updateCount;
    }

    /**
     * Returns the keys that were generated by the query.
     *
     * @return
     * The list of generated keys, or {@code null} if the query did not produce
     * any keys.
     */
    public List<Object> getGeneratedKeys() {
        if (generatedKeys != null) {
            return Collections.unmodifiableList(generatedKeys);
        } else {
            return null;
        }
    }

    /**
     * Appends a set of arguments to a prepared statement.
     *
     * @param statement
     * The prepared statement.
     *
     * @param arguments
     * The batch arguments.
     *
     * @throws SQLException
     * If an error occurs while adding the batch.
     */
    public void addBatch(PreparedStatement statement, Map<String, ?> arguments) throws SQLException {
        if (statement == null || arguments == null) {
            throw new IllegalArgumentException();
        }

        apply(statement, arguments);

        statement.addBatch();
    }

    private void apply(PreparedStatement statement, Map<String, ?> arguments) throws SQLException {
        var i = 1;

        for (var parameter : parameters) {
            if (parameter == null) {
                continue;
            }

            var value = arguments.get(parameter);

            if (value instanceof Enum<?>) {
                value = value.toString();
            }

            statement.setObject(i++, value);
        }
    }

    /**
     * Returns the generated query text.
     *
     * @return
     * The generated query text.
     */
    @Override
    public String toString() {
        return sqlBuilder.toString();
    }
}
