package sd2526.trab.impl.utils;

import java.util.concurrent.ConcurrentHashMap;

public class SyncPoint {
	
	private final ConcurrentHashMap<Long, String> result;
	private long version;
	
	private SyncPoint() {
		this.result = new ConcurrentHashMap<Long, String>();
		this.version = 0;
	}
	
	private static SyncPoint instance = null;
	
	public static synchronized SyncPoint getSyncPoint() {
		if(SyncPoint.instance == null)
			SyncPoint.instance = new SyncPoint();
		
		return SyncPoint.instance;
	}
	
	public synchronized String waitForResult( long n ) {
		while( version < n ) {
			try {
				wait();
			} catch (InterruptedException e) {
				// nothing to be done here
			}
		}
		
		return result.remove(n);
	}
	
	public synchronized void waitForVersion( long n ) {
		while( version < n ) {
			try {
				wait();
			} catch (InterruptedException e) {
				// nothing to be done here
			}
		}
	}
	
	public synchronized void setResult( long n, String res ) {
		if( n < version)
			throw new RuntimeException("Version " + n + " is already set");
		if ( res != null ) {
			result.put(n, res);
		}
		version = n;
		notifyAll();
	}
}
