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
                	
                	String line ="";
                	System.out.println("Entering Inside While");
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
