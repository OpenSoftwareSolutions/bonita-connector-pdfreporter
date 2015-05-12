/*******************************************************************************
 * Copyright (c) 2015 Open Software Solutions GmbH.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License v2.0
 * which accompanies this distribution, and is available at
 *
 * Contributors:
 *     Open Software Solutions GmbH
 ******************************************************************************/

package org.oss.bonitasoft.connectors.pdfreporter.test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileOutputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.bonitasoft.engine.api.APIAccessor;
import org.bonitasoft.engine.api.ProcessAPI;
import org.bonitasoft.engine.bpm.document.DocumentNotFoundException;
import org.bonitasoft.engine.bpm.document.DocumentValue;
import org.bonitasoft.engine.bpm.document.impl.DocumentImpl;
import org.bonitasoft.engine.connector.Connector;
import org.bonitasoft.engine.connector.ConnectorException;
import org.bonitasoft.engine.connector.ConnectorValidationException;
import org.bonitasoft.engine.connector.EngineExecutionContext;
import org.bonitasoft.engine.exception.BonitaException;
import org.bonitasoft.engine.io.IOUtil;
import org.hsqldb.Server;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.oss.bonitasoft.connectors.pdfreporter.CreatePDFReport;

/**
 * @author Magnus Karlsson
 */
public class CreatePDFReportTest {

    // input parameters
    final String DB_DRIVER = "dbDriver";

    final String JDBC_URL = "jdbcUrl";

    final String USER = "user";

    final String PASSWORD = "password";

    final String JRXML_DOC = "jrxmlDocument";

    final String PARAMETERS = "parameters";

    final String OUTPUT_REPORT_DOC = "outputReportDocument";

    private static final String WRONG_DB_DRIVER = "com.mysql.jdbc.DriverWRONG";

    private static final String WRONG_JRXML_DOC = "wrongJrxml";

    private static final String WRONG_JDBC_URL = "jdbc:mysql://argyweb.com/wrong_database";

    private static final String WRONG_USERNAME = "wrong_user_name";

    private static final String WRONG_PASSWORD = "wrong_password";

    private static Server hsqlServer = null;

    protected static final Logger LOG = Logger.getLogger(CreatePDFReportTest.class.getName());

    EngineExecutionContext engineExecutionContext;

    APIAccessor apiAccessor;

    ProcessAPI processAPI;

    @Rule
    public TestRule testWatcher = new TestWatcher() {

        @Override
        public void starting(final Description d) {
            LOG.warning("==== Starting test: " + this.getClass().getName() + "." + d.getMethodName() + "() ====");
        }

        @Override
        public void failed(final Throwable e, final Description d) {
            LOG.warning("==== Failed test: " + this.getClass().getName() + "." + d.getMethodName() + "() ====");
        }

        @Override
        public void succeeded(final Description d) {
            LOG.warning("==== Succeeded test: " + this.getClass().getName() + "." + d.getMethodName() + "() ====");
        }

    };

    /**
     * test good parameters that will not cause fault
     *
     * @throws BonitaException
     */
    @Test
    public void testGoodParameters() throws Exception {
        getMockedContext();
        final Connector connector = getWorkingConnector();
        connector.validateInputParameters();
    }

    /**
     * test null parameter that will cause fault
     *
     * @throws BonitaException
     */
    @Test(expected = ConnectorValidationException.class)
    public void testNullParameter() throws Exception {
        getMockedContext();
        final CreatePDFReport connector = getWorkingConnector();
        final String wrongDbDriver = null;
        final Map<String, Object> inputs = new HashMap<String, Object>();
        inputs.put(DB_DRIVER, wrongDbDriver);
        connector.setInputParameters(inputs);
        connector.validateInputParameters();
    }

    /**
     * test wrong database driver. make sure provide a wrong database driver
     *
     * @throws BonitaException
     */
    @Test(expected = ConnectorValidationException.class)
    public void testWrongDbDriver() throws Exception {
        getMockedContext();
        final CreatePDFReport connector = getWorkingConnector();
        final String wrongDbDriver = WRONG_DB_DRIVER;
        final Map<String, Object> inputs = new HashMap<String, Object>();
        inputs.put(DB_DRIVER, wrongDbDriver);
        connector.setInputParameters(inputs);
        connector.validateInputParameters();
    }

    /**
     * test wrong jrxml doc.
     *
     * @throws BonitaException
     *
     */

    @Test(expected = ConnectorValidationException.class)
    public void testWrongJrxmlDocument() throws Exception {
        getMockedContext();
        final CreatePDFReport connector = getWorkingConnector();
        final String wrongJrxmlDoc = WRONG_JRXML_DOC;
        final Map<String, Object> inputs = new HashMap<String, Object>();
        inputs.put(JRXML_DOC, wrongJrxmlDoc);
        connector.setInputParameters(inputs);
        connector.validateInputParameters();
    }

    /**
     * test wrong JDBC Url. please provide a wrong JDBC Url in config.properties
     *
     * @throws BonitaException
     */
    @Test(expected = ConnectorValidationException.class)
    public void testWrongJdbcUrl() throws Exception {
        getMockedContext();
        final CreatePDFReport connector = getWorkingConnector();
        final String wrongJdbcUrl = WRONG_JDBC_URL;
        final Map<String, Object> inputs = new HashMap<String, Object>();
        inputs.put(JDBC_URL, wrongJdbcUrl);
        connector.setInputParameters(inputs);
        connector.validateInputParameters();
    }

    /**
     * test wrong database username. please provide a wrong user name in config.properties
     *
     * @throws BonitaException
     */
    @Test(expected = ConnectorValidationException.class)
    public void testWrongDbUser() throws Exception {
        getMockedContext();
        final CreatePDFReport connector = getWorkingConnector();
        connector.validateInputParameters();
        final String wrongUserName = WRONG_USERNAME;
        final Map<String, Object> inputs = new HashMap<String, Object>();
        inputs.put(USER, wrongUserName);
        connector.setInputParameters(inputs);
        connector.validateInputParameters();
    }

    /**
     * test wrong database password. please provide a wrong password in config.properties
     *
     * @throws BonitaException
     */
    @Test(expected = ConnectorValidationException.class)
    public void testWrongDbPassword() throws Exception {
        getMockedContext();
        final CreatePDFReport connector = getWorkingConnector();
        final String wrongPassword = WRONG_PASSWORD;
        final Map<String, Object> inputs = new HashMap<String, Object>();
        inputs.put(PASSWORD, wrongPassword);
        connector.setInputParameters(inputs);
        connector.validateInputParameters();
    }

    /**
     * test create a report fail.
     */
    @Test
    public void testCreateAReportFail() throws Exception {
        getMockedContext();
        final CreatePDFReport connector = getWorkingConnector();
        final Map<String, Object> inputs = new HashMap<String, Object>();
        inputs.put(PASSWORD, "wrong_password");
        connector.setInputParameters(inputs);
        try {
            connector.execute();
            fail();
        } catch (final ConnectorException e) {
            Assert.assertTrue(true);
        }
    }

    /**
     * test create a report successfully.
     *
     * @throws Exception
     */
    @Test
    public void testCreateAReportPdf() throws Exception {

        getMockedContext();

        CreatePDFReport connector = getWorkingConnector();
        connector.validateInputParameters();
        connector.execute();
        byte[] reportContent = ((DocumentValue) connector.getResult()).getContent();
        File contentFile = File.createTempFile("pdfreporter", ".pdf");
        FileOutputStream fos = new FileOutputStream(contentFile.getCanonicalPath());
        fos.write(reportContent);
        fos.close();
        System.out.println("Rapport : " + contentFile.getCanonicalPath());
        assertTrue(contentFile.isFile());
        assertTrue(contentFile.getName().contains("pdf"));
        assertTrue(contentFile.lastModified() > System.currentTimeMillis() - 60000);
        assertTrue(contentFile.length() > 1000L);

    }

    private void getMockedContext() throws Exception {
        final File root = new File(".");
        final File file = new File(root, "src/test/resources/pdfreporter_bonita.jrxml");
        byte[] fileContent = IOUtil.getAllContentFrom(file);

        DocumentImpl document = new DocumentImpl();
        document.setCreationDate(new Date());
        document.setId(1);
        document.setProcessInstanceId(1);
        document.setName("jrxml");
        document.setFileName("report.jrxml");
        document.setContentMimeType("application/xml");
        document.setContentStorageId("1L");
        document.setHasContent(true);

        engineExecutionContext = mock(EngineExecutionContext.class);
        apiAccessor = mock(APIAccessor.class);
        processAPI = mock(ProcessAPI.class);
        when(apiAccessor.getProcessAPI()).thenReturn(processAPI);
        when(engineExecutionContext.getProcessInstanceId()).thenReturn(1L);
        when(processAPI.getLastDocument(1L, "jrxml")).thenReturn(document);
        when(processAPI.getLastDocument(1L, WRONG_JRXML_DOC)).thenThrow(
                new DocumentNotFoundException(new Throwable("Document not found : " + WRONG_JRXML_DOC)));
        when(processAPI.getDocumentContent("1L")).thenReturn(fileContent);
    }

    private CreatePDFReport getWorkingConnector() throws Exception {

        final CreatePDFReport connector = new CreatePDFReport();
        final Map<String, Object> inputs = new HashMap<String, Object>();

        // Database access information
        inputs.put(DB_DRIVER, "org.hsqldb.jdbcDriver");
        inputs.put(JDBC_URL, "jdbc:hsqldb:hsql://localhost/iva");
        inputs.put(USER, "sa");
        inputs.put(PASSWORD, "");

        // Report settings parameters
        // - A table named address with this 5 fields :id(integer) firstname(varchar), lastname(varchar), street(varchar), city(varchar)
        inputs.put(JRXML_DOC, "jrxml");
        final List<List<String>> parametersList = new ArrayList<List<String>>();
        final List<String> parameter2List = new ArrayList<String>();
        parameter2List.add("param2");
        parameter2List.add("1");
        parametersList.add(parameter2List);
        inputs.put(PARAMETERS, parametersList);

        connector.setExecutionContext(engineExecutionContext);
        connector.setAPIAccessor(apiAccessor);
        connector.setInputParameters(inputs);

        return connector;
    }

    @BeforeClass
    public static void beforeClass() {
    	hsqlServer = new Server();
		hsqlServer.setLogWriter(null);
		hsqlServer.setSilent(true);
		hsqlServer.setDatabaseName(0, "iva");
		hsqlServer.setDatabasePath(0, "file:ivadb");
		hsqlServer.start();
    }

    @AfterClass
    public static void afterClass() {
    	hsqlServer.stop();
		hsqlServer = null;
    }


    @Before
    public void createTable() throws Exception {
        Class.forName("org.hsqldb.jdbcDriver");
        Connection conn = DriverManager.getConnection(
                "jdbc:hsqldb:hsql://localhost/iva",
                "sa",
                "");
        Statement statement = conn.createStatement();
        statement.execute("create table address (" +
                "id INTEGER," +
                "firstname VARCHAR(50)," +
                "lastname VARCHAR(50)," +
                "street VARCHAR(50)," +
                "city VARCHAR(50)" +
                ");"
                );
        statement.execute("insert into address values (1, 'Sherlock', 'Holmes', '221B Baker Street ', 'London')");
        statement.execute("insert into address values (2, 'Bruce', 'Wayne', 'Wayne Manor', 'Gotham')");
        statement.execute("insert into address values (3, 'Henry Walton', 'Jones', '38 Adler Avenue', 'Fairfield')");
        conn.close();
    }

    @After
    public void deleteTable() throws ClassNotFoundException, SQLException {
        Class.forName("org.hsqldb.jdbcDriver");
        Connection conn = DriverManager.getConnection(
                "jdbc:hsqldb:hsql://localhost/iva",
                "sa",
                "");
        Statement statement = conn.createStatement();

        statement.execute("drop table address");

        conn.close();
    }
}
