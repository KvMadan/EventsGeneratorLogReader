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
			+ "  `tStamp` datetime NOT NULL,"
			+ "  `eventtype` varchar(50),"
			+ "  `TPS` decimal(7,3),"
			+ "  `TRTms` decimal(7,3),"
			+ "  `TOTAL_TIMEOUTS` decimal(12,3),"
			+ "  `TOTAL_SENT` decimal(12,3),"
			+ "  `TOTAL_FAILED` decimal(12,3),"
			+ "  `TOTAL_RESENT` decimal(12,3),"
			+ "  `TOTAL_RESENT_TO` decimal(12,3),"
			+ "  `TOTAL_BUSY` decimal(12,3),"
			+ "  `RUN_TIMEs` decimal(12,3),"
			+ "  `FATAL` decimal(12,3),"
			+ "  `ERROR` decimal(12,3),"
			+ "  `WARN` decimal(12,3))";

	private String INSERT_TRANSACTIONS_SQL = "INSERT INTO %s.eg ( %s ) VALUES ( %s )";
	
	private String TABLE_EG_SQL = "SELECT count(*) FROM information_schema.tables WHERE table_schema = '%s' AND table_name = '%s'";

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
				log.debug("Table Created Successfully");
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
			}

			// Update Schema name
			INSERT_TRANSACTIONS_SQL = String.format(INSERT_TRANSACTIONS_SQL,
					egSchema, "%s", "%s");

			connection.setCatalog(egSchema);
			log.debug("Catalog: " + connection.getCatalog());

			// move this to destroy method.
			// connection.close();

		} catch (ClassNotFoundException e) {
			log.error("Class Not found: " + JDBC_DRIVER);
			e.printStackTrace();
		} catch (SQLException e) {
			log.error(e.getMessage());
			e.printStackTrace();
		}
	}
	
	public void writeMetricsToDB(String eventType, HashMap<String, String> map, String currentTime){
		
		StringBuffer keys = new StringBuffer();
		StringBuffer values = new StringBuffer();
		
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
			e.printStackTrace();
		}
	}

	public void destroy() {
		// closing the MYSQL Connection
		try {
			connection.close();
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				connection.close();
			} catch (SQLException e) {
			}
		}
	}
}
