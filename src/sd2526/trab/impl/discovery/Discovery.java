package sd2526.trab.impl.discovery;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import sd2526.trab.impl.utils.Sleep;


/**
 * <p>A class interface to perform service discovery based on periodic 
 * announcements over multicast communication.</p>
 * 
 */

public interface Discovery {

	/**
	 * Used to announce the URI of the given service name.
	 * @param serviceName - the name of the service
	 * @param serviceURI - the uri of the service
	 */
	public void announce(String serviceName, String serviceURI);

	/**
	 * Get discovered URIs for a given service name
	 * @param serviceName - name of the service
	 * @param minReplies - minimum number of requested URIs. Blocks until the number is satisfied.
	 * @return array with the discovered URIs for the given service name.
	 */
	public URI[] knownUrisOf(String serviceName, int minReplies);

	/**
	 * Get the instance of the Discovery service
	 * @return the singleton instance of the Discovery service
	 */
	public static Discovery getInstance() {
		return DiscoveryImpl.getInstance();
	}
}

/**
 * Implementation of the multicast discovery service
 */
class DiscoveryImpl implements Discovery {
	
	private static Logger Log = Logger.getLogger(Discovery.class.getName());

	static final int DISCOVERY_RETRY_TIMEOUT = 5000;
	static final int DISCOVERY_ANNOUNCE_PERIOD = 1000;
	static final int STALE_ANNOUNCEMENT_TIMEOUT = 4 * DISCOVERY_ANNOUNCE_PERIOD;
	static final InetSocketAddress DISCOVERY_ADDR = new InetSocketAddress("226.226.226.226", 2266);

	// Used separate the two fields that make up a service announcement.
	private static final String DELIMITER = "\t";

	private static final int MAX_DATAGRAM_SIZE = 65536;

	private static Discovery singleton;

	private Map<String, Map<URI, Long>> uris = new ConcurrentHashMap<>();
	
	synchronized static Discovery getInstance() {
		if (singleton == null) {
			singleton = new DiscoveryImpl();
		}
		return singleton;
	}
		
	private DiscoveryImpl() {
		this.startListener();
	}

	@Override
	public void announce(String serviceName, String serviceURI) {
		Log.fine(String.format("Starting Discovery announcements on: %s for: %s -> %s\n", DISCOVERY_ADDR, serviceName, serviceURI));

		var pktBytes = String.format("%s%s%s", serviceName, DELIMITER, serviceURI).getBytes();
		var pkt = new DatagramPacket(pktBytes, pktBytes.length, DISCOVERY_ADDR);

		// start thread to send periodic announcements
		new Thread(() -> {
			try (var ds = new DatagramSocket()) {
				while (true) {
					try {
						ds.send(pkt);
						Sleep.ms(DISCOVERY_ANNOUNCE_PERIOD);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}).start();
	}


	@Override
	public URI[] knownUrisOf(String serviceName, int minEntries) {
		boolean waitingLogged = false;
		while(true) {
			var knownMap = uris.getOrDefault(serviceName, Collections.emptyMap());
			var now = System.currentTimeMillis();
			var knownList = knownMap.entrySet().stream()
					.filter(e -> now - e.getValue() <= STALE_ANNOUNCEMENT_TIMEOUT)
					.map(Map.Entry::getKey)
					.toList();

			if (knownList.size() >= minEntries) {
				var known = knownList.toArray(new URI[knownList.size()]);
				Log.fine(() -> "Discovery lookup service=%s min=%d found=%d uris=%s"
						.formatted(serviceName, minEntries, knownList.size(), Arrays.toString(known)));
				return known;
			} else {
				if (!waitingLogged) {
					int currentSize = knownList.size();
					Log.fine(() -> "Discovery waiting for service=%s min=%d current=%d"
							.formatted(serviceName, minEntries, currentSize));
					waitingLogged = true;
				}
				pruneStale(now);
				Sleep.ms(DISCOVERY_ANNOUNCE_PERIOD);
			}
		}
	}

	private void pruneStale(long now) {
		for (var serviceEntry : uris.entrySet()) {
			var endpoints = serviceEntry.getValue();
			if (endpoints == null || endpoints.isEmpty())
				continue;
			var stale = endpoints.entrySet().stream()
					.filter(e -> now - e.getValue() > STALE_ANNOUNCEMENT_TIMEOUT)
					.map(Map.Entry::getKey)
					.toList();
			for (var uri : stale)
				endpoints.remove(uri);
		}
	}

	private void startListener() {
		Log.fine(String.format("Starting discovery on multicast group: %s, port: %d\n", DISCOVERY_ADDR.getAddress(), DISCOVERY_ADDR.getPort()));

		new Thread(() -> {
			try (var ms = new MulticastSocket(DISCOVERY_ADDR.getPort())) {				
				ms.joinGroup(DISCOVERY_ADDR, pickMulticastInterface(DISCOVERY_ADDR));
				for (;;) {
					try {
						var pkt = new DatagramPacket(new byte[MAX_DATAGRAM_SIZE], MAX_DATAGRAM_SIZE);
						ms.receive(pkt);
						var msg = new String(pkt.getData(), 0, pkt.getLength());
						Log.finest(String.format("Received: %s", msg));
						var parts = msg.split(DELIMITER);
						if (parts.length == 2) {
							var serviceName = parts[0];
							var uri = URI.create(parts[1]);
							var known = uris.computeIfAbsent(serviceName, (k) -> new ConcurrentHashMap<>());
							known.put(uri, System.currentTimeMillis());
						}

					} catch (Exception x) {
						x.printStackTrace();
					}
				}
			} catch (Exception x) {
				x.printStackTrace();
			}
		}).start();
	}
	
	private NetworkInterface pickMulticastInterface(InetSocketAddress group) throws IOException {		
		try(var tmp = new DatagramSocket()){
			tmp.connect(group);
			return NetworkInterface.getByInetAddress(tmp.getLocalAddress());
		}
	}

}