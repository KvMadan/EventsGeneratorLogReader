/**
 *  EG Commands: GET_MONITOR - default"; GET_FULL_MONITOR";
 *  GET_APP_STAT"; GET_REPORT"; PRINT_REPORT"; SET_LOG_LEVEL:[0,1,2]";
 *  PAUSE_LOAD:[SECONDS]"; RESUME_LOAD"; PRINT_WARN:[0,1]";
 */
package in.km.oneview.eg;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

/**
 * @author Madan Kavarthapu
 *
 */

public class Main {

	final static Logger log = Logger.getLogger(Main.class);
	
	private String hostname = "10.237.48.238";
	private String port = "30010";
	private int frequency;
	private String testName;
	//private String line;
	
	private Socket socket, reportSocket;
	
	private InputStream input, reportInput;
	private BufferedReader reader, reportReader;
	
	private OutputStream output, reportOutput;
	private PrintWriter writer, reportWriter;
	
	private StringBuffer metricsReceived, reportReceived;
	
	private GenericMysqlMetricsSender mysqlMetricsSender;
	
	public Main(String hostname, String port, String frequency, String testName){
		this.hostname = hostname;
		this.port = port;
		
		if (frequency != null && !frequency.isEmpty())
			this.frequency = Integer.parseInt(frequency);
		else
			this.frequency = 5;
		
		if (testName != null && !testName.isEmpty())
			this.testName = testName + "_" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date());
		else
			this.testName = "Test_" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date());
	}
	
	public boolean init(){
		
		try{
			log.debug("Connecting to EG Server @ " + hostname + ":" + port);
			System.out.println("Connecting to EG Server @ " + hostname + ":" + port);
			socket = new Socket(InetAddress.getByName(hostname), Integer.parseInt(port));
			reportSocket = new Socket(InetAddress.getByName(hostname), Integer.parseInt(port));
			// read data from server.
			input = socket.getInputStream();
			reader = new BufferedReader(
					new InputStreamReader(input));
			// read report data from server
			reportInput = reportSocket.getInputStream();
			reportReader = new BufferedReader(
					new InputStreamReader(reportInput));
			
			// send data to server
			output = socket.getOutputStream();
			writer = new PrintWriter(output, true);
			//send report data to server
			reportOutput = reportSocket.getOutputStream();
			reportWriter = new PrintWriter(reportOutput, true);
			log.debug("Connected to EG and corresonding streams are opened");
			
			return true;
		}
		catch(Exception e){
			log.error("Exception", e);
		}
		return false;
	}
	
	
	public void startReader(){
		
		try{
			Thread receiverThread = new Thread(new Runnable(){

				public void run() {
					Thread.currentThread().setName("EG Reader");
					try {
						metricsReceived = new StringBuffer();
						log.debug("Receiving data from Server:");
						String line;
						while ((line = reader.readLine()) != null) {
							log.debug(line);
							metricsReceived.append(line + System.lineSeparator());
						}
					} catch (IOException e) {
						log.error("IOException", e);
					}
				}
			});
			receiverThread.start();
		}
		catch(Exception e){
			log.error("Exception", e);
		}
	}

	public void startReportReader(){
		
		try{
			Thread receiverThread = new Thread(new Runnable(){

				public void run() {
					Thread.currentThread().setName("EG Report Reader");
					try {
						reportReceived = new StringBuffer();
						log.debug("Receiving data from Server:");
						String line;
						while ((line = reportReader.readLine()) != null) {
							log.debug(line);
							reportReceived.append(line + System.lineSeparator());
						}
					} catch (IOException e) {
						log.error("IOException", e);
					}
				}
			});
			receiverThread.start();
		}
		catch(Exception e){
			log.error("Exception", e);
		}
	}	
	
	@SuppressWarnings("unused")
	private void printMetrics(HashMap<String, String> map){
	    Iterator<Map.Entry<String, String>> it = map.entrySet().iterator();
	    while (it.hasNext()) {
	        Map.Entry<String, String> pair = it.next();
	        log.debug(pair.getKey() + " = " + pair.getValue());
	        //it.remove(); 
	    }
	}
	
	public void startWriter(){
		Thread senderThread = new Thread(new Runnable(){
			public void run() {
				Thread.currentThread().setName("EG Writer");
				while(true){
					// This is a message sent to the server
					writer.print("GET_MONITOR"); // change it to print while working with real EG Server. 
					writer.flush();
					
					//Checking the server side socket availability. 
					if (writer.checkError()){
						try {
							socket.close();
							log.debug("EG is closed, exiting...");
						} catch (IOException e) {
							e.printStackTrace();
						}
						System.exit(1);
					}
					
					log.debug("Sent Message: GET_MONITOR");
					try {
						Thread.sleep(frequency * 1000);
					} catch (InterruptedException e) {
						log.error("Exception", e);
					}
				}
			}
		});
		senderThread.start();
	}
	
	public void startReportWriter(){
		Thread senderThread = new Thread(new Runnable(){
			public void run() {
				Thread.currentThread().setName("EG Report Writer");
				while(true){
					// This is a message sent to the server
					reportWriter.print("GET_REPORT"); // change it to print while working with real EG Server. 
					reportWriter.flush();
					log.debug("Sent Message: GET_REPORT");
					try {
						Thread.sleep(60000);
					} catch (InterruptedException e) {
						log.error("Exception", e);
					}
				}
			}
		});
		senderThread.start();
	}
	
	public void sendMetricsToDB(){
		Thread metricsDBSenderThread = new Thread(new Runnable(){
			public void run() {
				Thread.currentThread().setName("EG Metrics DB Sender");
				while(true){
					
					java.util.Date dt = new java.util.Date();
					java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
					String currentTime = sdf.format(dt);
					
					// Sending metrics to DB
					if (metricsReceived.length() != 0){
						log.debug("Received Message Length : " + metricsReceived.length() + "\\n Message Received: " + metricsReceived.toString());
						
						String[] lines = metricsReceived.toString().split("\\n");
						HashMap<String, String> map = new HashMap<String, String>();
						
						String currentEvent = "";
						String previousEvent = "";
						
						for(String line: lines){
					
							if (!line.startsWith("###")){
								String[] metrics = line.split("\\s+");
								log.debug("Metrics Lines: " + metrics.length);
							    //System.out.println(metrics[0] + " = " + metrics[1]);
								if (metrics.length >= 2){
									//log.debug(metrics[0] + " = " + metrics[1]);
									if (metrics[1].equalsIgnoreCase("-nan"))
										metrics[1] = "";
									map.put(metrics[0].replaceAll("[)(]", ""), metrics[1]);
								}
							}
							else{
								previousEvent = currentEvent;
								currentEvent = line;
								
								log.debug("Sending Metrics: " + previousEvent);
								//printMetrics(map);
								//Write Metrics to DB
								if(!map.isEmpty())
									mysqlMetricsSender.writeMetricsToDB(testName, previousEvent.replaceAll("###", "").trim(), map, currentTime);
								
							}
						}
						
						log.debug("Sending Metrics (last received): " + currentEvent);
						//printMetrics(map);
						//Write the last received Metrics to DB
						mysqlMetricsSender.writeMetricsToDB(testName, currentEvent.replaceAll("###", "").trim(), map, currentTime);
						
						metricsReceived.setLength(0);
					}
					try {
						Thread.sleep(5000);
					} catch (InterruptedException e) {
						log.error("Exception", e);
					}
				}
			}
		});
		metricsDBSenderThread.start();
	}
	
	public void sendReportToDB(){
		
		//Thread reportSenderThread = new Thread(
				
			Runnable reportSenderThread =	new Runnable(){
			public void run() {
				Thread.currentThread().setName("EG Report DB Sender");
				//while(true){
					
					// Sending Report to DB
					if (reportReceived.length() != 0){
						log.debug("Received Report Length : " + reportReceived.length());
						log.debug("Received Report: " + reportReceived.toString());
						
						String[] reportLines = reportReceived.toString().split("\\r?\\n");
						log.debug("Received Report Lines: " + reportLines.length);
						HashMap<String, String> reportMap = new HashMap<String, String>();
						String tag="";
						//String reportX="";
						
						String currentReportX = "";
						String previousReportX = "";
						
						for(String line: reportLines){
							if (line.trim().matches("EG\\d+\\s+Report")){
								
								String[] reports = line.split("\\s+");
								//reportX = reports[0];
								previousReportX = currentReportX;
								currentReportX = reports[0];
								
								log.debug("EG Report ID# " + currentReportX);
								
								//Send Report to DB if multiple reports returned by EG
								if(!reportMap.isEmpty()){
									log.debug("Processing Report: " + previousReportX);
									java.util.Date dt = new java.util.Date();
									java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
									String currentTime = sdf.format(dt);
									mysqlMetricsSender.writeReportToDB(testName, previousReportX, reportMap, currentTime);
									reportReceived.setLength(0);
								}
								
							}
							else if (line.trim().equalsIgnoreCase("Load Summary"))
								tag = "LS_";
							else if (line.trim().equalsIgnoreCase("Total Summary"))
								tag = "TS_";
							else if (line.trim().equalsIgnoreCase("Charges Summary"))
								tag = "CS_";
							else if (line.startsWith("NET_TOTALSEND") || line.startsWith("NET_TOTALRECV") || 
									line.startsWith("NET_TOTALSESS") || line.startsWith("TOTAL_RUN_TIME"))
								tag = "X_";
							
							if (isItMetric(line)){
								String[] metrics = line.split("\\s+");
							    log.debug(tag + metrics[0] + " = " + metrics[1]);
							    
							    if (metrics[1].startsWith(">"))
							    {
							    	log.debug("Trunctacting Timeout value : " + metrics[1]);
							    	metrics[1] = new StringBuffer(metrics[1]).deleteCharAt(0).toString();
							    }

								reportMap.put(tag + metrics[0], metrics[1]);
							}
						}
						//Printing Map Details
						//log.debug("Printing Map Details: ");
						//printMetrics(reportMap);
						
						//Send Report to DB
						if(!reportMap.isEmpty()){
							log.debug("Processing Report: (Last Report)" + currentReportX);
							java.util.Date dt = new java.util.Date();
							java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
							String currentTime = sdf.format(dt);
							mysqlMetricsSender.writeReportToDB(testName, currentReportX, reportMap, currentTime);
							reportReceived.setLength(0);
						}
					}
					else{
						log.debug("No data to send");
					}
					/*try {
						Thread.sleep(10000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}*/
				//}
			}
		};
				//);
		//reportSenderThread.start();
		 ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
		 service.scheduleAtFixedRate(reportSenderThread, 60, 60, TimeUnit.SECONDS);
	}
	
	private boolean isItMetric(String line){
		//System.out.println("Is it Metric: " + line);
		if (line.trim().matches("EG\\d+\\s+Report") || line.startsWith("=") || line.startsWith("-") ||
				line.trim().equalsIgnoreCase("Load Summary") ||  
				line.trim().equalsIgnoreCase("Total Summary") || 
				line.trim().equalsIgnoreCase("Charges Summary") ||
				line.equalsIgnoreCase("END_OF_REPORT") || 
				line.length() == 0){
			
			return false;
		}
		return true;
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
		//String egFrequency = System.getProperty("eg.frequency");
		
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
			
			/*String usage = "Usage: \\n java -Deg.server=10.237.48.238 -Deg.port=30010 \\n -Dmysql.server=localhost -Dmysql.port=3306 \\n"
					+ "-Dmysql.db=egdb -Dmysql.user=root -Dmysql.pwd=root \\n -Dlog4j.configuration=.\\log4j.properties \\n "
					+ "-jar EGLogsReader_v1.1.jar";*/
			
			System.out.println("*****************************************************");
			System.out.println("Usage: ");
			System.out.println("java -Deg.server=10.237.48.238 -Deg.port=30010");
			System.out.println("-Dmysql.server=localhost -Dmysql.port=3306");
			System.out.println("-Dmysql.db=egdb -Dmysql.user=root -Dmysql.pwd=root");
			System.out.println("-Deg.frequency=5 -Deg.testname=XYZ");
			System.out.println("-jar EGLogsReader_v1.1.jar");
			System.out.println("*****************************************************");
			
			System.exit(1);
		}
		else{
			log.debug("EG Server: " + System.getProperty("eg.server"));
		}
		
		
		Main eg = new Main(System.getProperty("eg.server"), System.getProperty("eg.port"), System.getProperty("eg.frequency"), System.getProperty("eg.testname"));

		if(eg.init()){
			
			eg.mysqlMetricsSender = new GenericMysqlMetricsSender();
			eg.mysqlMetricsSender.setup(System.getProperty("mysql.server"), System.getProperty("mysql.port"), System.getProperty("mysql.db"), System.getProperty("mysql.user"), System.getProperty("mysql.pwd"));
			//eg.mysqlMetricsSender.setup("localhost", "3306", "egdb", "root", "root");
			
			eg.startWriter();
			eg.startReader();
			
			
			//For Report
			eg.startReportWriter();
			eg.startReportReader();
			
			eg.sendMetricsToDB();
			eg.sendReportToDB();
		}
	}
}