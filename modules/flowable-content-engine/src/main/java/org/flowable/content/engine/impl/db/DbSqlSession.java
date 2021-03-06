/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.flowable.content.engine.impl.db;

import java.sql.Connection;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.session.SqlSession;
import org.flowable.content.engine.ContentEngineConfiguration;
import org.flowable.engine.common.api.FlowableException;
import org.flowable.engine.common.api.FlowableOptimisticLockingException;
import org.flowable.engine.common.impl.Page;
import org.flowable.engine.common.impl.db.ListQueryParameterObject;
import org.flowable.engine.common.impl.interceptor.Session;
import org.flowable.engine.common.impl.persistence.entity.Entity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseConnection;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;

/**
 * @author Tijs Rademakers
 * @author Joram Barrez
 */
public class DbSqlSession implements Session {

    private static final Logger log = LoggerFactory.getLogger(DbSqlSession.class);

    public static String[] JDBC_METADATA_TABLE_TYPES = { "TABLE" };

    protected SqlSession sqlSession;
    protected DbSqlSessionFactory dbSqlSessionFactory;
    protected String connectionMetadataDefaultCatalog;
    protected String connectionMetadataDefaultSchema;

    public DbSqlSession(DbSqlSessionFactory dbSqlSessionFactory) {
        this.dbSqlSessionFactory = dbSqlSessionFactory;
        this.sqlSession = dbSqlSessionFactory.getSqlSessionFactory().openSession();
    }

    public DbSqlSession(DbSqlSessionFactory dbSqlSessionFactory, Connection connection, String catalog, String schema) {
        this.dbSqlSessionFactory = dbSqlSessionFactory;
        this.sqlSession = dbSqlSessionFactory.getSqlSessionFactory().openSession(connection); // Note the use of connection param here, different from other constructor
        this.connectionMetadataDefaultCatalog = catalog;
        this.connectionMetadataDefaultSchema = schema;
    }

    // insert ///////////////////////////////////////////////////////////////////

    public void insert(Entity entity) {
        if (entity.getId() == null) {
            String id = dbSqlSessionFactory.getIdGenerator().getNextId();
            entity.setId(id);
        }

        String insertStatement = dbSqlSessionFactory.getInsertStatement(entity);
        insertStatement = dbSqlSessionFactory.mapStatement(insertStatement);

        if (insertStatement == null) {
            throw new FlowableException("no insert statement for " + entity.getClass() + " in the ibatis mapping files");
        }

        log.debug("inserting: {}", entity);
        sqlSession.insert(insertStatement, entity);
    }

    // update
    // ///////////////////////////////////////////////////////////////////

    public void update(Entity entity) {
        String updateStatement = dbSqlSessionFactory.getUpdateStatement(entity);
        updateStatement = dbSqlSessionFactory.mapStatement(updateStatement);

        if (updateStatement == null) {
            throw new FlowableException("no update statement for " + entity.getClass() + " in the ibatis mapping files");
        }

        log.debug("updating: {}", entity);
        int updatedRecords = sqlSession.update(updateStatement, entity);
        if (updatedRecords == 0) {
            throw new FlowableOptimisticLockingException(entity + " was updated by another transaction concurrently");
        }
    }

    public int update(String statement, Object parameters) {
        String updateStatement = dbSqlSessionFactory.mapStatement(statement);
        return sqlSession.update(updateStatement, parameters);
    }

    // delete
    // ///////////////////////////////////////////////////////////////////

    public void delete(String statement, Object parameter) {
        sqlSession.delete(statement, parameter);
    }

    public void delete(Entity entity) {
        String deleteStatement = dbSqlSessionFactory.getDeleteStatement(entity.getClass());
        deleteStatement = dbSqlSessionFactory.mapStatement(deleteStatement);
        if (deleteStatement == null) {
            throw new FlowableException("no delete statement for " + entity.getClass() + " in the ibatis mapping files");
        }

        sqlSession.delete(deleteStatement, entity);
    }

    // select
    // ///////////////////////////////////////////////////////////////////

    @SuppressWarnings({ "rawtypes" })
    public List selectList(String statement) {
        return selectList(statement, null, 0, Integer.MAX_VALUE);
    }

    @SuppressWarnings("rawtypes")
    public List selectList(String statement, Object parameter) {
        return selectList(statement, parameter, 0, Integer.MAX_VALUE);
    }

    @SuppressWarnings("rawtypes")
    public List selectList(String statement, Object parameter, Page page) {
        if (page != null) {
            return selectList(statement, parameter, page.getFirstResult(), page.getMaxResults());
        } else {
            return selectList(statement, parameter, 0, Integer.MAX_VALUE);
        }
    }

    @SuppressWarnings("rawtypes")
    public List selectList(String statement, ListQueryParameterObject parameter, Page page) {
        if (page != null) {
            parameter.setFirstResult(page.getFirstResult());
            parameter.setMaxResults(page.getMaxResults());
        }
        return selectList(statement, parameter);
    }

    @SuppressWarnings("rawtypes")
    public List selectList(String statement, Object parameter, int firstResult, int maxResults) {
        return selectList(statement, new ListQueryParameterObject(parameter, firstResult, maxResults));
    }

    @SuppressWarnings("rawtypes")
    public List selectList(String statement, ListQueryParameterObject parameter) {
        return selectListWithRawParameter(statement, parameter, parameter.getFirstResult(), parameter.getMaxResults());
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public List selectListWithRawParameter(String statement, Object parameter, int firstResult, int maxResults) {
        statement = dbSqlSessionFactory.mapStatement(statement);
        if (firstResult == -1 || maxResults == -1) {
            return Collections.EMPTY_LIST;
        }
        List loadedObjects = sqlSession.selectList(statement, parameter);
        return loadedObjects;
    }

    @SuppressWarnings({ "rawtypes" })
    public List selectListWithRawParameterWithoutFilter(String statement, Object parameter, int firstResult, int maxResults) {
        statement = dbSqlSessionFactory.mapStatement(statement);
        if (firstResult == -1 || maxResults == -1) {
            return Collections.EMPTY_LIST;
        }
        return sqlSession.selectList(statement, parameter);
    }

    public Object selectOne(String statement, Object parameter) {
        statement = dbSqlSessionFactory.mapStatement(statement);
        Object result = sqlSession.selectOne(statement, parameter);
        return result;
    }

    @SuppressWarnings("unchecked")
    public <T extends Entity> T selectById(Class<T> entityClass, String id) {
        T entity = null;

        String selectStatement = dbSqlSessionFactory.getSelectStatement(entityClass);
        selectStatement = dbSqlSessionFactory.mapStatement(selectStatement);
        entity = (T) sqlSession.selectOne(selectStatement, id);
        if (entity == null) {
            return null;
        }

        return entity;
    }

    public void flush() {
        sqlSession.flushStatements();
    }

    public void close() {
        sqlSession.close();
    }

    public void commit() {
        sqlSession.commit();
    }

    public void rollback() {
        sqlSession.rollback();
    }

    // schema operations
    // ////////////////////////////////////////////////////////

    public void dbSchemaCheckVersion() {
        log.debug("flowable content db schema check successful");
    }

    public void dbSchemaCreate() {
        Liquibase liquibase = createLiquibaseInstance();
        try {
            liquibase.update("form");
        } catch (Exception e) {
            throw new FlowableException("Error creating form engine tables", e);
        }
    }

    public void dbSchemaDrop() {
        Liquibase liquibase = createLiquibaseInstance();
        try {
            liquibase.dropAll();
        } catch (Exception e) {
            throw new FlowableException("Error dropping form engine tables", e);
        }
    }

    public <T> T getCustomMapper(Class<T> type) {
        return sqlSession.getMapper(type);
    }

    public boolean isMysql() {
        return dbSqlSessionFactory.getDatabaseType().equals("mysql");
    }

    public boolean isOracle() {
        return dbSqlSessionFactory.getDatabaseType().equals("oracle");
    }

    protected Liquibase createLiquibaseInstance() {
        try {
            DatabaseConnection connection = new JdbcConnection(sqlSession.getConnection());
            Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(connection);
            database.setDatabaseChangeLogTableName(ContentEngineConfiguration.LIQUIBASE_CHANGELOG_PREFIX + database.getDatabaseChangeLogTableName());
            database.setDatabaseChangeLogLockTableName(ContentEngineConfiguration.LIQUIBASE_CHANGELOG_PREFIX + database.getDatabaseChangeLogLockTableName());

            if (StringUtils.isNotEmpty(sqlSession.getConnection().getSchema())) {
                database.setDefaultSchemaName(sqlSession.getConnection().getSchema());
                database.setLiquibaseSchemaName(sqlSession.getConnection().getSchema());
            }

            if (StringUtils.isNotEmpty(sqlSession.getConnection().getCatalog())) {
                database.setDefaultCatalogName(sqlSession.getConnection().getCatalog());
                database.setLiquibaseCatalogName(sqlSession.getConnection().getCatalog());
            }

            Liquibase liquibase = new Liquibase("org/flowable/form/db/liquibase/flowable-form-db-changelog.xml", new ClassLoaderResourceAccessor(), database);
            return liquibase;

        } catch (Exception e) {
            throw new FlowableException("Error dropping form engine tables", e);
        }
    }

    // getters and setters
    // //////////////////////////////////////////////////////

    public SqlSession getSqlSession() {
        return sqlSession;
    }

    public DbSqlSessionFactory getDbSqlSessionFactory() {
        return dbSqlSessionFactory;
    }

}
