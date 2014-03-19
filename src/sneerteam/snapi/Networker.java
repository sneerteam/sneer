package sneerteam.snapi;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

import android.util.Log;

public class Networker implements Runnable {
	
	public interface NetworkerListener {
		void receivedPacket(ByteBuffer data) throws IOException;
	}
	
	// Cache of resolved IP address for "dynamic.sneer.me"
	SocketAddress serveraddr; 

	// who is going to get our callbacks 
	NetworkerListener listener;
	
	// our UDP socket
	DatagramChannel channel;
	
	// last time we sent a refresh 
	long lastRefreshTime = 0;

	volatile boolean running = true;

	final String LOG_TAG = getClass().getCanonicalName();
	
	public Networker(NetworkerListener listener) throws IOException, UnknownHostException {
		this.listener = listener;		
		new Thread(this).start();
	}

	private void init() throws UnknownHostException, IOException,
			SocketException {
		
		// dynamic.sneer.me:55555
		serveraddr = new InetSocketAddress(InetAddress.getByName("dynamic.sneer.me"), 55555);
		
		// open the local nonblocking UDP socket
		channel = DatagramChannel.open();
		channel.configureBlocking(true);
		channel.socket().bind(null); // is this needed? test taking it out
	}
	
	public void send(String edn) {
		byte[] sendData;
		try {
			sendData = edn.getBytes("UTF-8");
			channel.send(ByteBuffer.wrap(sendData), serveraddr);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void log(String message) {
		Log.d(LOG_TAG , message);
	}
	
	public void run() {
		
		log("starting networker thread");
		
		try {
			init();
		} catch (IOException e) {
			e.printStackTrace();
			Log.e(LOG_TAG, "error initializing networker", e);
			return;
		}

		while (running) {
			ByteBuffer data = ByteBuffer.allocate(4096);
			try {
				SocketAddress sender = null;
				sender = channel.receive(data);
				if (sender == null) {
					break; // we're done -- no packets
				} else {
					data.flip();
					listener.receivedPacket(data);
				}
			} catch (IOException e) {
				e.printStackTrace();
				break;
			}
		}
	}

	public void close() {
		running = false;
	}
}
