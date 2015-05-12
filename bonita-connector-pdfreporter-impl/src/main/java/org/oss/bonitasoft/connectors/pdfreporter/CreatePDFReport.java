/*******************************************************************************
 * Copyright (c) 2015 Open Software Solutions GmbH.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License v2.0
 * which accompanies this distribution, and is available at
 *
 * Contributors:
 *     Open Software Solutions GmbH
 ******************************************************************************/
package org.oss.bonitasoft.connectors.pdfreporter;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.bonitasoft.engine.bpm.document.Document;
import org.bonitasoft.engine.bpm.document.DocumentNotFoundException;
import org.bonitasoft.engine.bpm.document.DocumentValue;
import org.bonitasoft.engine.connector.AbstractConnector;
import org.bonitasoft.engine.connector.ConnectorException;
import org.bonitasoft.engine.connector.ConnectorValidationException;
import org.bonitasoft.engine.session.InvalidSessionException;
import org.oss.pdfreporter.engine.JRParameter;
import org.oss.pdfreporter.engine.JasperCompileManager;
import org.oss.pdfreporter.engine.JasperExportManager;
import org.oss.pdfreporter.engine.JasperFillManager;
import org.oss.pdfreporter.engine.JasperPrint;
import org.oss.pdfreporter.engine.JasperReport;
import org.oss.pdfreporter.registry.ApiRegistry;
import org.oss.pdfreporter.repo.RepositoryManager;
import org.oss.pdfreporter.sql.IConnection;
import org.oss.pdfreporter.sql.SQLException;
import org.oss.pdfreporter.sql.SqlFactory;

/**
 * @author Magnus Karlsson
 */
public class CreatePDFReport extends AbstractConnector {

	// input parameters
	private static final String DB_DRIVER = "dbDriver";

	private static final String JDBC_URL = "jdbcUrl";

	private static final String USER = "user";

	private static final String PASSWORD = "password";

	private static final String JRXML_DOC = "jrxmlDocument";

	private static final String PARAMETERS = "parameters";

	// output
	private static final String REPORT_DOC_VALUE = "reportDocValue";

	// Data base configuration
	private String dbDriver;

	private String jdbcUrl;

	private String user;

	private String password;

	// Report settings
	private String jrxmlDocument;

	private byte[] jrxmlContent;

	private Map<String, String> parameters = null;

	private Logger LOGGER = Logger.getLogger(this.getClass().getName());

	private static IConnection conn = null;

	private static final String RESOURCES = "src/main/java";

	private static final String PDF_TEMP_FILE = "temp.pdf";

	public Object getResult() {
		return getOutputParameters().get(REPORT_DOC_VALUE);
	}

	@SuppressWarnings("unchecked")
	private void initInputs() {
		dbDriver = (String) getInputParameter(DB_DRIVER);
		LOGGER.info(DB_DRIVER + " " + dbDriver);
		jdbcUrl = (String) getInputParameter(JDBC_URL);
		LOGGER.info(JDBC_URL + " " + jdbcUrl);

		user = (String) getInputParameter(USER);
		LOGGER.info(USER + " " + user);

		password = (String) getInputParameter(PASSWORD);
		LOGGER.info(PASSWORD + " ******");

		jrxmlDocument = (String) getInputParameter(JRXML_DOC);
		LOGGER.info(JRXML_DOC + " " + jrxmlDocument);

		final List<List<Object>> parametersList = (List<List<Object>>) getInputParameter(PARAMETERS);
		parameters = new HashMap<String, String>();
		if (parametersList != null) {
			// System.out.println("initInputs - parameters list :" + parametersList.toString());
			for (List<Object> rows : parametersList) {
				if (rows.size() == 2) {
					Object keyContent = rows.get(0);
					Object valueContent = rows.get(1);
					LOGGER.info("Parameter " + keyContent + " " + valueContent);
					if (keyContent != null && valueContent != null) {
						final String key = keyContent.toString();
						final String value = valueContent.toString();
						parameters.put(key, value);
					}
				}
			}
		}
	}

	@Override
	public void validateInputParameters() throws ConnectorValidationException {
		initInputs();
		final List<String> errors = new ArrayList<String>();
		if (jrxmlDocument == null || jrxmlDocument.trim().length() == 0) {
			errors.add("jrxmlDocument cannot be empty!");
		}


		Long processInstanceId = getExecutionContext().getProcessInstanceId();
		try {
			Document document = getAPIAccessor().getProcessAPI().getLastDocument(processInstanceId, jrxmlDocument);
			if (!document.hasContent() || !document.getContentFileName().matches(".*\\.jrxml")) {
				errors.add("the jrxmlDocument " + document.getName() + " must have for content a jrxml file compatible with jasper v5");
			}
			else {
				jrxmlContent = getAPIAccessor().getProcessAPI().getDocumentContent(document.getContentStorageId());
			}
		} catch (Exception e) {
			errors.add(jrxmlDocument + " is not the name of a document defined in the process");
		}

		if (!errors.isEmpty()) {
			throw new ConnectorValidationException(this, errors);
		}

		// Load JDBC driver
		// Check that jrxmlFile exists
		// Test database connection
		if(dbDriver != null && !dbDriver.isEmpty()){
			if(jdbcUrl != null && !jdbcUrl.isEmpty()){
				try {
					databaseValidations(dbDriver, jrxmlDocument, jdbcUrl, user, password);
				} catch (final ClassNotFoundException e) {
					errors.add("dbDriver JDBC Driver not found!");
				} catch (final DocumentNotFoundException dnfe) {
					errors.add("jrxmlDocument '" + jrxmlDocument + "' not found!");
				} catch (final SQLException e) {
					errors.add("jdbcUrlCannot connect to database. Check 'jdbcUrl', 'user' and 'password' parameters. Message: " + e.getMessage());
				} catch (InvalidSessionException ise) {
					errors.add("InvalidSessionException" + ise.getMessage());
				} catch (IOException ioe) {
					errors.add("IOException" + ioe.getMessage());
				}
			} else {
				errors.add("jdbc URl has not been specified.");
			}
		} else {
			errors.add("db Driver has not been specified.");
		}
		if (!errors.isEmpty()) {
			throw new ConnectorValidationException(this, errors);
		}
	}

	@Override
	protected void executeBusinessLogic() throws ConnectorException {
		try {
			createJasperReportFromDataBase(dbDriver, jdbcUrl, user, password, jrxmlDocument, parameters);
		} catch (final Exception e) {
			throw new ConnectorException(e);
		}
	}

	/**
	 * validate the database
	 *
	 * @throws InvalidSessionException
	 * @throws IOException
	 * @throws ConnectorValidationException
	 */
	public void databaseValidations(final String dbDriver, final String jrxmlDocument, final String jdbcUrl, final String user, final String password)
			throws ClassNotFoundException, DocumentNotFoundException, SQLException, InvalidSessionException, IOException, ConnectorValidationException {

		// Load JDBC driver
		try {
			SqlFactory.registerFactory(dbDriver);
			conn = ApiRegistry.getSqlFactory().createConnection(jdbcUrl, user, password);
		} catch (final SQLException e) {
			if (LOGGER.isLoggable(Level.WARNING)) {
				LOGGER.warning("Connection error: " + e.getMessage());
			}
			try {
				if (conn != null) {
					conn.close();
				}
			} catch (final Exception e1) {
				if (LOGGER.isLoggable(Level.WARNING)) {
					LOGGER.warning("Exception during finally. Message: " + e1.getMessage());
				}
			}
			throw e;
		}

	}

	public void createJasperReportFromDataBase(final String dbDriver, final String jdbcUrl, final String user, final String password,
			final String jrxmlDocument, final Map<String, String> parameters) throws Exception {
		if (LOGGER.isLoggable(Level.INFO)) {
			LOGGER.info("Creating a new PDFReporter from database");
		}
		ApiRegistry.initSession();
		// load resources
		RepositoryManager repo = RepositoryManager.getInstance();
		repo.setDefaultResourceFolder(RESOURCES);
		try {
			final JasperReport report = JasperCompileManager.compileReport(new ByteArrayInputStream(jrxmlContent));
			final Map<String, Object> typedParameters = getTypedParameters(report, parameters);
 			final JasperPrint print = JasperFillManager.fillReport(report, typedParameters, conn);

			byte[] content;
			String suffix = "." + "pdf";
			JasperExportManager.exportReportToPdfFile(print, PDF_TEMP_FILE);

			File tempFile = new File(PDF_TEMP_FILE);
			content = FileUtils.readFileToByteArray(tempFile);
			String mimeType = "application/pdf";

			DocumentValue docValue = new DocumentValue(content, mimeType, "pdfreporter_report" + suffix);
			setOutputParameter(REPORT_DOC_VALUE, docValue);
		} catch (final Exception e) {
			if (LOGGER.isLoggable(Level.WARNING)) {
				LOGGER.warning(e.toString());
			}
			throw e;
		} finally {
			// delete temp file.
			new File(PDF_TEMP_FILE).delete();
			try {
				if (conn != null) {
					conn.close();
					ApiRegistry.dispose();
				}
			} catch (final Exception e) {
				if (LOGGER.isLoggable(Level.WARNING)) {
					LOGGER.warning("Exception during finally. Message: " + e.getMessage());
				}
				throw e;
			}
		}
	}

	private Map<String, Object> getTypedParameters(final JasperReport report, final Map<String, String> parameters) {
		final Map<String, Object> typedParameters = new HashMap<String, Object>();
		for (final JRParameter param : report.getParameters()) {
			final String paramName = param.getName();
			final String paramType = param.getValueClassName();
			final String value = parameters.get(paramName);
			if (value != null && paramType != null) {
				if (paramType.equals(String.class.getName())) {
					typedParameters.put(paramName, value);
				} else if (paramType.equals(Integer.class.getName())) {
					try {
						final Integer typedValue = Integer.parseInt(value);
						typedParameters.put(paramName, typedValue);
					} catch (final NumberFormatException e) {
						throw new IllegalArgumentException("Invalid parameter type for " + paramName + ": " + Integer.class.getName()
								+ " value is expected, current is " + value);
					}
				} else if (paramType.equals(Short.class.getName())) {
					try {
						final Short typedValue = Short.parseShort(value);
						typedParameters.put(paramName, typedValue);
					} catch (final NumberFormatException e) {
						throw new IllegalArgumentException("Invalid parameter type for " + paramName + ": " + Short.class.getName()
								+ " value is expected, current is " + value);
					}
				} else if (paramType.equals(Long.class.getName())) {
					try {
						final Long typedValue = Long.parseLong(value);
						typedParameters.put(paramName, typedValue);
					} catch (final NumberFormatException e) {
						throw new IllegalArgumentException("Invalid parameter type for " + paramName + ": " + Long.class.getName()
								+ " value is expected, current is " + value);
					}
				} else if (paramType.equals(Double.class.getName())) {
					try {
						final Double typedValue = Double.parseDouble(value);
						typedParameters.put(paramName, typedValue);
					} catch (final NumberFormatException e) {
						throw new IllegalArgumentException("Invalid parameter type for " + paramName + ": " + Double.class.getName()
								+ " value is expected, current is " + value);
					}
				} else if (paramType.equals(Float.class.getName())) {
					try {
						final Float typedValue = Float.parseFloat(value);
						typedParameters.put(paramName, typedValue);
					} catch (final NumberFormatException e) {
						throw new IllegalArgumentException("Invalid parameter type for " + paramName + ": " + Float.class.getName()
								+ " value is expected, current is " + value);
					}
				} else if (paramType.equals(BigDecimal.class.getName())) {
					try {
						final BigDecimal typedValue = new BigDecimal(value);
						typedParameters.put(paramName, typedValue);
					} catch (final NumberFormatException e) {
						throw new IllegalArgumentException("Invalid parameter type for " + paramName + ": " + BigDecimal.class.getName()
								+ " value is expected, current is " + value);
					}
				} else if (paramType.equals(Date.class.getName())) {
					try {
						final Date typedValue = new SimpleDateFormat().parse(value);
						typedParameters.put(paramName, typedValue);
					} catch (final ParseException e) {
						throw new IllegalArgumentException("Invalid parameter type for " + paramName + ": " + Date.class.getName()
								+ " value is expected, current is " + value);
					}
				} else if (paramType.equals(Boolean.class.getName())) {
					final Boolean typedValue = Boolean.parseBoolean(value);
					typedParameters.put(paramName, typedValue);
				}

			}
		}
		return typedParameters;
	}

}
