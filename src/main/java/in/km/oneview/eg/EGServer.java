/**
 * 
 */
package in.km.oneview.eg;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * @author Madan Kavarthapu
 *
 */
public class EGServer {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws IOException {
		ServerSocket listener = new ServerSocket(30010);
        try {
            while (true) {
                Socket socket = listener.accept();
                try {
                	
                	InputStream input = socket.getInputStream();
                	BufferedReader reader = new BufferedReader(
        					new InputStreamReader(input));
                	
                	//Sending
                	PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                	
                	String line = "";
                	//System.out.println("Entering Inside While");
                	while((line = reader.readLine()) != null){
                		System.out.println(line);
                		if (line.equalsIgnoreCase("GET_MONITOR")){
                			out.println("### CUSTOM_DIAMETER.Total-CCR_N_INIT");
                			out.println("TPS	18.00");
                			out.println("TRT(ms)	10.91");
                			out.println("TOTAL_TIMEOUTS	142.00");
                			out.println("TOTAL_SENT	189.00");
                			out.println("TOTAL_FAILED	194.00");
                			out.println("### CUSTOM_DIAMETER.abpwrk1-TOTAL");
                			out.println("TPS	288.00");
                			out.println("TRT(ms)	210.91");
                			out.println("TOTAL_TIMEOUTS	242.00");
                			out.println("TOTAL_SENT	289.00");
                			out.println("TOTAL_FAILED	294.00");
                			out.println("TOTAL_RESENT	20.00");
                			out.println("TOTAL_RESENT_TO	20.00");
                			out.println("TOTAL_BUSY	20.00");
                			out.println("RUN_TIME(s)	211.00");
                			out.flush();
                		}
                		else if (line.equalsIgnoreCase("GET_REPORT")){
                			out.println("EG0 Report");
                			out.println("=====================================================================");
                			out.println("    Load Summary");
                			out.println();
                			out.println("LAT_MAX                        52");
                			out.println("LAT_50                         51");
                			out.println("LAT_90                       1001");
                			out.println("LAT_95                       1001");
                			out.println("LAT_99                       1001");
                			out.println("LAT_995                      1001");
                			out.println("LAT_999                      1001");
                			out.println("LAT_9999                     1001");
                			out.println("LAT_99999                    1001");
                			out.println("TOTAL_SENT                 125756");
                			out.println("RESENT                          0");
                			out.println("DUPLICATED                      0");
                			out.println("NET_OK                      62878");
                			out.println("NET_TIMEOUT                 62821");
                			out.println("RESENT_TIMEOUT                  0");
                			out.println("NET_CONNECTIONS                 0");
                			out.println("NET_APP05012                62878");
                			out.println();
                			out.println();
                			out.println("----------------------------------------------------------------------");
                			out.println("    Total Summary");
                			out.println("");
                			out.println("TOTAL_SENT                 125756");
                			out.println("RESENT                          0");
                			out.println("DUPLICATED                      0");
                			out.println("NET_OK                      62878");
                			out.println("NET_TIMEOUT                 62821");
                			out.println("RESENT_TIMEOUT                  0");
                			out.println("APP_SUCCESS                     0");
                			out.println("APP_ERROR                   62878");
                			out.println("APP_BUSY                        0");
                			out.println("");
                			out.println("RAR_TRIGGER                     0");
                			out.println("RAR_EVENT                       0");
                			out.println("RAA_REPLY                       0");
                			out.println("ReAUTH_UPDATE                   0");
                			out.println("");
                			out.println("ASR_TRIGGER                     0");
                			out.println("ASR_EVENT                       0");
                			out.println("ASA_REPLY                       0");
                			out.println("ABORT_SESS                      0");
                			out.println("");
                			out.println("");
                			out.println("    Charges Summary");
							out.println();
                			out.println("OPEN_SESS                       0");
                			out.println("TOTAL_SENT                      0");
                			out.println("RESENT                          0");
                			out.println("DUPLICATED                      0");
                			out.println("NET_OK                          0");
                			out.println("NET_TIMEOUT                     0");
                			out.println("RESENT_TIMEOUT                  0");
                			out.println("APP_SUCCESS                     0");
                			out.println("APP_ERROR                       0");
                			out.println("APP_BUSY                        0");
                			out.println("");
                			out.println("");
                			out.println("----------------------------------------------------------------------");
                			out.println("NET_TOTALSEND              125756");
                			out.println("NET_TOTALRECV               62878");
                			out.println("NET_TOTALSESS              125756");
                			out.println("TOTAL_RUN_TIME           00:59:33");
                			out.println("");
                			out.println("");
                			out.println("=====================================================================");
                			out.println("");
                			out.println("END_OF_REPORT");
                			}
                	}
                } finally {
                    socket.close();
                }
            }
        }
        finally {
            listener.close();
        }
	}
}
