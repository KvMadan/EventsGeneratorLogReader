/**
 * 
 */
package in.km.oneview.eg;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.Logger;

/**
 * @author Madan Kavarthapu
 *
 */
@SuppressWarnings("unused")
public class GenericMysqlMetricsSender {

	final static Logger log = Logger.getLogger(GenericMysqlMetricsSender.class);

	private String JDBC_DRIVER = "com.mysql.jdbc.Driver";
	private String DB_URL = "jdbc:mysql://%s:%s/";
	private Connection connection = null;
	private Statement statement;
	private String DB_CREATE_SQL = "CREATE DATABASE %s";

	private String TABLE_CREATE_EG_SQL = "CREATE TABLE IF NOT EXISTS %s.eg ("
			+ "  `tStamp` datetime(6) NOT NULL,"
			+ "  `testName` varchar(50),"
			+ "  `eventtype` varchar(50),"
			+ "  `TPS` decimal(12,3),"
			+ "  `TRTms` decimal(12,3),"
			+ "  `TOTAL_TIMEOUTS` decimal(12,3),"
			+ "  `TOTAL_SENT` decimal(12,3),"
			+ "  `TOTAL_FAILED` decimal(12,3),"
			+ "  `TOTAL_RESENT` decimal(12,3),"
			+ "  `TOTAL_LAST_RESENT_TO` decimal(12,3),"
			+ "  `TOTAL_RESENT_TO` decimal(12,3),"
			+ "  `TOTAL_BUSY` decimal(12,3),"
			+ "  `RUN_TIMEs` decimal(12,3),"
			+ "  `TOTAL_NOTIFICATIONS` decimal(12,3),"
			+ "  `FATAL` decimal(12,3),"
			+ "  `ERROR` decimal(12,3),"
			+ "  `WARN` decimal(12,3))";

	private String INSERT_TRANSACTIONS_SQL = "INSERT INTO %s.eg ( %s ) VALUES ( %s )";
	
	private String TABLE_EG_SQL = "SELECT count(*) FROM information_schema.tables WHERE table_schema = '%s' AND table_name = '%s'";

	//For Report
	private String TABLE_CREATE_EG_REPORT_SQL = "CREATE TABLE IF NOT EXISTS %s.egreport ("
			+ "tStamp datetime(6) NOT NULL,"
			+ "testName varchar(50),"
			+ "ReportName varchar(50),"
			+ "LS_LAT_MAX decimal(12,3),"
			+ "LS_LAT_50 decimal(12,3),"
			+ "LS_LAT_90 decimal(12,3),"
			+ "LS_LAT_95 decimal(12,3),"
			+ "LS_LAT_99 decimal(12,3),"
			+ "LS_LAT_995 decimal(12,3),"
			+ "LS_LAT_999 decimal(12,3),"
			+ "LS_LAT_9999 decimal(12,3),"
			+ "LS_LAT_99999 decimal(12,3),"
			+ "LS_TOTAL_SENT decimal(12,3),"
			+ "LS_RESENT decimal(12,3),"
			+ "LS_DUPLICATED decimal(12,3),"
			+ "LS_NET_OK decimal(12,3),"
			+ "LS_NET_TIMEOUT decimal(12,3),"
			+ "LS_RESENT_TIMEOUT decimal(12,3),"
			+ "LS_NET_CONNECTIONS decimal(12,3),"
			//+ "LS_NET_APP05012 decimal(12,3),"
			+ "TS_TOTAL_SENT decimal(12,3),"
			+ "TS_RESENT decimal(12,3),"
			+ "TS_DUPLICATED decimal(12,3),"
			+ "TS_NET_OK decimal(12,3),"
			+ "TS_NET_TIMEOUT decimal(12,3),"
			+ "TS_RESENT_TIMEOUT decimal(12,3),"
			+ "TS_APP_SUCCESS decimal(12,3),"
			+ "TS_APP_ERROR decimal(12,3),"
			+ "TS_APP_BUSY decimal(12,3),"
			+ "TS_RAR_TRIGGER decimal(12,3),"
			+ "TS_RAR_EVENT decimal(12,3),"
			+ "TS_RAA_REPLY decimal(12,3),"
			+ "TS_ReAUTH_UPDATE decimal(12,3),"
			+ "TS_ASR_TRIGGER decimal(12,3),"
			+ "TS_ASR_EVENT decimal(12,3),"
			+ "TS_ASA_REPLY decimal(12,3),"
			+ "TS_ABORT_SESS decimal(12,3),"
			+ "CS_OPEN_SESS decimal(12,3),"
			+ "CS_TOTAL_SENT decimal(12,3),"
			+ "CS_RESENT decimal(12,3),"
			+ "CS_DUPLICATED decimal(12,3),"
			+ "CS_NET_OK decimal(12,3),"
			+ "CS_NET_TIMEOUT decimal(12,3),"
			+ "CS_RESENT_TIMEOUT decimal(12,3),"
			+ "CS_APP_SUCCESS decimal(12,3),"
			+ "CS_APP_ERROR decimal(12,3),"
			+ "CS_APP_BUSY decimal(12,3),"
			+ "X_NET_TOTALSEND decimal(12,3),"
			+ "X_NET_TOTALRECV decimal(12,3),"
			+ "X_NET_TOTALSESS decimal(12,3),"
			+ "X_TOTAL_RUN_TIME varchar(15),"
			+ "PRIMARY KEY (`tStamp`)"
			+ ")";
	
	private String INSERT_EG_REPORT_SQL = "INSERT INTO %s.egreport ( %s ) VALUES ( %s )";
	
	private String TABLE_EG_REPORT_SQL = "SELECT count(*) FROM information_schema.tables WHERE table_schema = '%s' AND table_name = '%s'";
	
	// For Report Status codes. 
	private String TABLE_CREATE_EG_REPORT_STATUS_SQL = "CREATE TABLE IF NOT EXISTS %s.egstatuscodes ("
			+ "	`tStamp` DATETIME(6) NOT NULL,"
			+ " `tStampReport` DATETIME(6) NOT NULL," 
			+ " `statuscode` VARCHAR(45) NULL,"
			+ " `value` DECIMAL(12,2) NULL,"
			+ " PRIMARY KEY (`tStamp`),"
			+ "FOREIGN KEY FK(`tStampReport`) REFERENCES %s.egreport(`tStamp`)"
			+ ")"; 
	
	private String INSERT_EG_REPORT_STATUS_SQL = "INSERT INTO %s.egstatuscodes (tStamp, tStampReport, statuscode, value) VALUES %s ";
	
	private String TABLE_EG_REPORT_STATUS_SQL = "SELECT count(*) FROM information_schema.tables WHERE table_schema = '%s' AND table_name = '%s'";
  

	public void setup(String mysqlHost, String mysqlPort, String egSchema,
			String userName, String password) {
		boolean schemaExists = false;
		try {
			Class.forName(JDBC_DRIVER);

			connection = DriverManager.getConnection(
					String.format(DB_URL, mysqlHost, mysqlPort), userName,
					password);

			ResultSet resultSet = connection.getMetaData().getCatalogs();

			// iterate each schema in the ResultSet
			log.debug("**List of available Schemas**");
			while (resultSet.next()) {
				// Get the database name, which is at position 1
				String databaseName = resultSet.getString(1);
				log.debug(databaseName);
				if (databaseName.equalsIgnoreCase(egSchema)) {
					log.debug("Database already exists");
					schemaExists = true;
					break;
				}
			}
			resultSet.close();

			statement = connection.createStatement();
			
			if (!schemaExists) {
				// Create a new Schema with the given name.
				
				statement.executeUpdate(String
						.format(DB_CREATE_SQL, egSchema));
				log.debug("Database created successfully..");
				// Create Required Tables in DB.
				statement.executeUpdate(String.format(TABLE_CREATE_EG_SQL,
						egSchema));
				statement.executeUpdate(String.format(TABLE_CREATE_EG_REPORT_SQL, egSchema));
				statement.executeUpdate(String.format(TABLE_CREATE_EG_REPORT_STATUS_SQL, egSchema, egSchema));
				log.debug("Table(s) Created Successfully");
			}
			else{
				TABLE_EG_SQL = String.format(TABLE_EG_SQL, egSchema, "eg");
				log.debug(TABLE_EG_SQL);
				resultSet = statement.executeQuery(TABLE_EG_SQL);
				
				if (resultSet.first()) {
					// Get the table count with name eg, which is at position 1
					int tableCount = resultSet.getInt(1);
					log.debug("Is eg table Exists?? - " + (tableCount == 1 ? "Yes" : "No"));
					if (tableCount == 0){
						// Create Required Tables in DB.
						statement.executeUpdate(String.format(TABLE_CREATE_EG_SQL, egSchema));
						log.debug("Table Created Successfully");
					}
				}
				resultSet.close();
				
				TABLE_EG_REPORT_SQL = String.format(TABLE_EG_REPORT_SQL, egSchema, "egreport");
				log.debug(TABLE_EG_REPORT_SQL);
				resultSet = statement.executeQuery(TABLE_EG_REPORT_SQL);
				
				if (resultSet.first()) {
					// Get the table count with name egreport, which is at position 1
					int tableCount = resultSet.getInt(1);
					log.debug("Is egreport table Exists?? - " + (tableCount == 1 ? "Yes" : "No"));
					if (tableCount == 0){
						// Create Required Tables in DB.
						statement.executeUpdate(String.format(TABLE_CREATE_EG_REPORT_SQL, egSchema));
						log.debug("Table Created Successfully");
					}
				}
				resultSet.close();
				
				// For status codes.
				TABLE_EG_REPORT_STATUS_SQL = String.format(TABLE_EG_REPORT_STATUS_SQL,  egSchema, "egstatuscodes");
				log.debug(TABLE_EG_REPORT_STATUS_SQL);
				resultSet = statement.executeQuery(TABLE_EG_REPORT_STATUS_SQL);
				
				if (resultSet.first()){
					// Get the table count with name egreport, which is at position 1
					int tableCount = resultSet.getInt(1);
					log.debug("Is egstatuscodes table Exists?? - " + (tableCount == 1 ? "Yes" : "No"));
					if (tableCount == 0){
						// Create Required Tables in DB.
						statement.executeUpdate(String.format(TABLE_CREATE_EG_REPORT_STATUS_SQL, egSchema, egSchema));
						log.debug("Table Created Successfully");
					}
				}
			}

			// Update Schema name
			INSERT_TRANSACTIONS_SQL = String.format(INSERT_TRANSACTIONS_SQL,
					egSchema, "%s", "%s");
			INSERT_EG_REPORT_SQL = String.format(INSERT_EG_REPORT_SQL, egSchema, "%s", "%s");
			
			INSERT_EG_REPORT_STATUS_SQL = String.format(INSERT_EG_REPORT_STATUS_SQL, egSchema, "%s");

			connection.setCatalog(egSchema);
			log.debug("Catalog: " + connection.getCatalog());

			// move this to destroy method.
			// connection.close();

		} catch (ClassNotFoundException e) {
			log.error("ClassNotFound ", e);
			e.printStackTrace();
		} catch (SQLException e) {
			log.error("SQLException ", e);
			e.printStackTrace();
		}
	}
	
	public void writeMetricsToDB(String testName, String eventType, HashMap<String, String> map, String currentTime){
		
		StringBuffer keys = new StringBuffer();
		StringBuffer values = new StringBuffer();
		
		//Adding TestName
		keys.append("testName" + ",");
        values.append("'" + testName + "',");
		
		//Adding Event Type
        keys.append("eventtype" + ",");
        values.append("'" + eventType + "',");
        
        //Adding DatTime
        keys.append("tStamp" + ",");
        values.append("'" + currentTime + "',");
		
	    Iterator<Map.Entry<String, String>> it = map.entrySet().iterator();
	    while (it.hasNext()) {
	        Map.Entry<String, String> pair = it.next();
	        //System.out.println(pair.getKey() + " --> " + pair.getValue());

	        keys.append(pair.getKey() + ",");
	        values.append("'" + pair.getValue() + "',");
	        
	        it.remove(); 
	    }
	    
	    keys.deleteCharAt(keys.length()-1);
	    values.deleteCharAt(values.length()-1);
	    
	    String sql = String.format(INSERT_TRANSACTIONS_SQL, keys.toString(), values.toString());
	    log.debug(sql);
	    try {
	    	statement = connection.createStatement();
			int count = statement.executeUpdate(sql);
			log.debug("Rows Inserted in DB: " + count);
		} catch (SQLException e) {
			log.error("SQLException", e);
		}
	}
	
	public void writeReportToDB(String testName, String reportName, HashMap<String, String> map, String reportTime){
		StringBuffer keys = new StringBuffer();
		StringBuffer values = new StringBuffer();
		
		java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
		
		// For NET_APP*** LATE_APP*** values
		StringBuffer statusFields = new StringBuffer();
		
		//Adding TestName
		keys.append("testName" + ",");
        values.append("'" + testName + "',");
		
		//Adding Event Type
        keys.append("ReportName" + ",");
        values.append("'" + reportName + "',");
        
        //Adding DatTime
        keys.append("tStamp" + ",");
        values.append("'" + reportTime + "',");
		
	    Iterator<Map.Entry<String, String>> it = map.entrySet().iterator();
	    while (it.hasNext()) {
	        Map.Entry<String, String> pair = it.next();
	        
	        //Logic to send the NET_APP*** LATE_APP*** values to separate table. 
	        if (pair.getKey().startsWith("LS_NET_APP") || pair.getKey().startsWith("LS_LATE_APP")){
	        	java.util.Date dt = new java.util.Date();
	        	String currentTime = sdf.format(dt);
	        	statusFields.append("('"+ currentTime + "','" + reportTime + "','" + pair.getKey() + "'," +  pair.getValue() + ")," );
	        	// Next Steps. 
	        }
	        else{
		        keys.append(pair.getKey() + ",");
		        values.append("'" + pair.getValue() + "',");
	        }
	        // end
	        log.debug(pair.getKey() + " --> " + pair.getValue());
	        it.remove(); 
	    }
	    keys.deleteCharAt(keys.length()-1);
	    values.deleteCharAt(values.length()-1);

	    statusFields.deleteCharAt(statusFields.length()-1);
	    String sql = String.format(INSERT_EG_REPORT_SQL, keys.toString(), values.toString());
	    log.debug(sql);
	    String statusSql = String.format(INSERT_EG_REPORT_STATUS_SQL, statusFields.toString());
	    log.debug(statusSql);
	    
	    try {
	    	statement = connection.createStatement();
			int count = statement.executeUpdate(sql);
			log.debug("Rows Inserted in DB: (EG Report) " + count);
			
			count = statement.executeUpdate(statusSql);
			log.debug("Rows Inserted in DB:(EG Status Codes) " + count);
			
		} catch (SQLException e) {
			log.error("SQLException", e);
		}
	}

	public void destroy() {
		// closing the MYSQL Connection
		try {
			connection.close();
		} catch (SQLException e) {
			log.error("SQLException", e);
		} finally {
			try {
				connection.close();
			} catch (SQLException e) {
			}
		}
	}
}
