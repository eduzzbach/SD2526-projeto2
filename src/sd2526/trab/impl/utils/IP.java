package sd2526.trab.impl.utils;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class IP {

	public static String hostAddress() {
		try {
			return InetAddress.getLocalHost().getHostAddress();
		} catch (UnknownHostException e) {
			return "?.?.?.?";
		}
	}
	
	public static String hostname() {
		try {
			return InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			return "?.?.?.?";
		}
	}
	
	public static String domain() {		
		var h = hostname();
		int i = h.indexOf('.');
		return i < 0 ? h : h.substring(i+1);
	}
}