/**
 * 
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

/**
 * @author Madan Kavarthapu
 *
 */
public class EGClient {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws IOException{
		
		String hostname = "10.237.48.238";
		//String hostname = "127.0.0.1";
		String port = "30010";
		
		Socket socket = new Socket(InetAddress.getByName(hostname), Integer.parseInt(port));
		// read data from server.
		InputStream input = socket.getInputStream();
		BufferedReader reader = new BufferedReader(
				new InputStreamReader(input));
		
		// send data to server
		OutputStream output = socket.getOutputStream();
		PrintWriter writer = new PrintWriter(output, true);
		
		// This is a message sent to the server
		writer.print("GET_REPORT"); // change it to print while working with real EG Server. 
		writer.flush();
		
		String line = "";
		StringBuffer sb = new StringBuffer();
		while ((line = reader.readLine()) != null) {
			//System.out.println(line);
			sb.append(line + System.lineSeparator());
			if (line.startsWith("END_OF_REPORT"))
				break;
		}
		
		String[] lines = sb.toString().split("\\n");
		System.out.println(lines.length);
		for(String s: lines){
		    System.out.print(s);
		    if (s.startsWith("LAT_99")){
		    	String metric[] = s.split("\\s+");
		    	System.out.println(metric[1]);
		    }
		}
		socket.close();
	}

}
