/*******************************************************************************
 * Copyright (c) 2015 Open Software Solutions GmbH.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-3.0.html
 *
 * Contributors:
 *     Open Software Solutions GmbH - initial API and implementation
 ******************************************************************************/
package org.bonitasoft.connectors.pdfreporter.test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.hsqldb.Server;

public class HSQLServer {

	private static Server hsqlServer = null;

	static void start(){
		hsqlServer = new Server();
		hsqlServer.setLogWriter(null);
		hsqlServer.setSilent(true);
		hsqlServer.setDatabaseName(0, "iva");
		hsqlServer.setDatabasePath(0, "file:ivadb");
		hsqlServer.start();
	}


	static void stop(){
		hsqlServer.stop();
		hsqlServer = null;
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {

		start();
		Connection connection = null;
		ResultSet rs = null;

		// making a connection
		try {
			Class.forName("org.hsqldb.jdbcDriver");
			connection = DriverManager.getConnection("jdbc:hsqldb:hsql://localhost/iva", "sa", ""); // can through sql exception
			connection.prepareStatement("drop table barcodes if exists;").execute();
			connection.prepareStatement("create table barcodes (id integer, barcode varchar(20) not null);").execute();
			connection.prepareStatement("insert into barcodes (id, barcode)"
					+ "values (1, 'SWE12345566');").execute();


			// query from the db
			rs = connection.prepareStatement("select id, barcode  from barcodes;").executeQuery();
			rs.next();
			System.out.println(String.format("ID: %1d, Name: %1s", rs.getInt(1), rs.getString(2)));

		} catch (SQLException e1) {
			e1.printStackTrace();
		} catch (ClassNotFoundException e2) {
			e2.printStackTrace();
		}
		stop();
	}

}
