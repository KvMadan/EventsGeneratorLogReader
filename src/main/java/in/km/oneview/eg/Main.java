/**
 *         EG Commands: GET_MONITOR - default"; GET_FULL_MONITOR";
 *         GET_APP_STAT"; GET_REPORT"; PRINT_REPORT"; SET_LOG_LEVEL:[0,1,2]";
 *         PAUSE_LOAD:[SECONDS]"; RESUME_LOAD"; PRINT_WARN:[0,1]";
 */
package in.km.oneview.eg;


import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

/**
 * @author Madan Kavarthapu
 *
 */
@SuppressWarnings("unused")
public class Main {

	final static Logger log = Logger.getLogger(Main.class);
	
	private String hostname = "10.237.48.238";
	private String port = "30010";
	private String line;
	
	private Socket socket;
	
	private InputStream input;
	private BufferedReader reader;
	
	private OutputStream output;
	private PrintWriter writer;
	
	private StringBuffer metricsReceived;
	
	private GenericMysqlMetricsSender mysqlMetricsSender;
	
	public Main(String hostname, String port){
		this.hostname = hostname;
		this.port = port;
	}
	
	public boolean init(){
		
		try{
			log.debug("Connecting to EG Server @ " + hostname + ":" + port);
			System.out.println("Connecting to EG Server @ " + hostname + ":" + port);
			socket = new Socket(InetAddress.getByName(hostname), Integer.parseInt(port));
			// read data from server.
			input = socket.getInputStream();
			reader = new BufferedReader(
					new InputStreamReader(input));
			
			// send data to server
			output = socket.getOutputStream();
			writer = new PrintWriter(output, true);
			return true;
		}
		catch(Exception e){
			e.printStackTrace();
		}
		return false;
	}
	
	
	public void startReader(){
		
		try{
			Thread receiverThread = new Thread(new Runnable(){

				public void run() {
					
					try {
						metricsReceived = new StringBuffer();
						log.debug("Receiving data from Server:");
						while ((line = reader.readLine()) != null) {
							log.debug(line);
							metricsReceived.append(line + System.lineSeparator());
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			});
			receiverThread.start();
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}
	
	private void printMetrics(HashMap<String, String> map){

	    Iterator<Map.Entry<String, String>> it = map.entrySet().iterator();
	    while (it.hasNext()) {
	        Map.Entry<String, String> pair = it.next();
	        log.debug(pair.getKey() + " --> " + pair.getValue());
	        it.remove(); 
	    }
	}
	
	public void startWriter(){
		Thread senderThread = new Thread(new Runnable(){
			public void run() {
				while(true){
					// This is a message sent to the server
					writer.print("GET_MONITOR"); // change it to print while working with real EG Server. 
					writer.flush();
					log.debug("Sent Message: GET_MONITOR");
					try {
						Thread.sleep(5000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		});
		senderThread.start();
	}
	
	public void sentMetricsToDB(){
		Thread metricsDBSenderThread = new Thread(new Runnable(){
			public void run() {
				while(true){
					
					java.util.Date dt = new java.util.Date();
					java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
					String currentTime = sdf.format(dt);
					
					// This is a message sent to DB
					if (metricsReceived.length() != 0){
						log.debug("Received Message Length : " + metricsReceived.length());
						
						String[] lines = metricsReceived.toString().split("\\n");
						HashMap<String, String> map = new HashMap<String, String>();
						
						String currentEvent = "";
						String previousEvent = "";
						
						for(String line: lines){
					
							if (!line.startsWith("###")){
								String[] metrics = line.split("\\s+");
							    //System.out.println(metrics[0] + " = " + metrics[1]);

								map.put(metrics[0].replaceAll("[)(]", ""), metrics[1]);
							}
							else{
								previousEvent = currentEvent;
								currentEvent = line;
								
								log.debug("Sending Metrics: " + previousEvent);
								//printMetrics(map);
								//Write Metrics to DB
								if(!map.isEmpty())
									mysqlMetricsSender.writeMetricsToDB(previousEvent.replaceAll("###", "").trim(), map, currentTime);
								
							}
						}
						
						log.debug("Sending Metrics (last received): " + currentEvent);
						//printMetrics(map);
						//Write the last received Metrics to DB
						mysqlMetricsSender.writeMetricsToDB(currentEvent.replaceAll("###", "").trim(), map, currentTime);
						
						metricsReceived.setLength(0);
					}
					try {
						Thread.sleep(5000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		});
		metricsDBSenderThread.start();
	}
	
	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		
		/*
		metricsSender = new GenericMysqlMetricsSender();
		metricsSender.setup("localhost", "3306", "egdb", "root", "root");
		metricsSender.destroy();
		
		//Main eg = new Main("10.237.48.238","30010");
		//Main eg = new Main("127.0.0.1","30010");
		*/
		
		String egserver = System.getProperty("eg.server");
		String egport = System.getProperty("eg.port");
		String mysqlServer = System.getProperty("mysql.server");
		String mysqlPort = System.getProperty("mysql.port");
		String mysqlDb = System.getProperty("mysql.db");
		String mysqlUser = System.getProperty("mysql.user");
		String mysqlPwd = System.getProperty("mysql.pwd");
		
		/*
		 * (egserver.length() == 0 || egport.length()== 0 || 
				 mysqlServer.length() == 0 || mysqlPort.length() == 0 ||
				 mysqlDb.length() == 0 || mysqlUser.length() == 0 || 
				 mysqlPwd.length() == 0)
		 */
		
		if (egserver == null || egport == null || 
				mysqlServer == null || mysqlPort == null ||
				mysqlDb == null || mysqlUser == null ||
				mysqlPwd == null){
			
			System.out.println("\nMissing Required parameters!\n");
			
			String usage = "Usage: \\n java -Deg.server=10.237.48.238 -Deg.port=30010 \\n -Dmysql.server=localhost -Dmysql.port=3306 \\n"
					+ "-Dmysql.db=egdb -Dmysql.user=root -Dmysql.pwd=root \\n -Dlog4j.configuration=.\\res\\log4j.properties \\n "
					+ "-jar EGLogsReader_v1.1.jar";
			
			System.out.println("*****************************************************");
			System.out.println("Usage: ");
			System.out.println("java -Deg.server=10.237.48.238 -Deg.port=30010");
			System.out.println("-Dmysql.server=localhost -Dmysql.port=3306");
			System.out.println("-Dmysql.db=egdb -Dmysql.user=root -Dmysql.pwd=root");
			System.out.println("-jar EGLogsReader_v1.1.jar");
			System.out.println("*****************************************************");
			
			System.exit(1);
		}
		else{
			System.out.println("All available");
		}
		
		log.debug("EG Server: " + System.getProperty("eg.server"));
		Main eg = new Main(System.getProperty("eg.server"), System.getProperty("eg.port"));

		if(eg.init()){
			
			eg.mysqlMetricsSender = new GenericMysqlMetricsSender();
			eg.mysqlMetricsSender.setup(System.getProperty("mysql.server"), System.getProperty("mysql.port"), System.getProperty("mysql.db"), System.getProperty("mysql.user"), System.getProperty("mysql.pwd"));
			//eg.mysqlMetricsSender.setup("localhost", "3306", "egdb", "root", "root");
			
			eg.startWriter();
			eg.startReader();
			eg.sentMetricsToDB();
		}
	}
}