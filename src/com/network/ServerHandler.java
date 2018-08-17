/**
 * 
 */
package com.network;

import java.io.IOException;
import java.net.Socket;

/**
 * ServerManager
 * 
 * Server side class to prepare and wait for messages from a client specified
 * by _socket.
 * 
 */
public class ServerHandler extends Thread{
	/* the socket to receive messages from */
	private Socket _socket = null;
	/* used for callback */
	public ServerManager _svrMgr = null;
	
	public ServerHandler(ServerManager svrMgr, Socket socket) {
		_svrMgr = svrMgr;
		_socket = socket;
	}
	
	/*
	 * Keep running a loop to receive messages from a client specified by 
	 * _socket. Once the connection is broken, call ServerManager to 
	 * remove this client.
	 */
	@Override
	public void run() {
		while (true) {
			try {
				_svrMgr.receiveMsg(_socket);
			} catch (IOException | ClassNotFoundException e) {
				_svrMgr.clientDisconnected(_socket);
				break;
			}
		}
		
	}
}

