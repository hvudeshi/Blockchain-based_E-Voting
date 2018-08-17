/**
 * 
 */
package com.network;

import java.io.Serializable;

/**
 * MessageStruct
 * 
 * A structure for communicating between server and client. Two fields indicate 
 * the message type(_code) and message body(_content).
 * 
 * Message types and description: 
 * 		type	description						direction
 * 		0		server broadcasts block			server -> client
 * 		1		client sends block  			client -> server
 *		2		server sends clientid			server -> client
 */
public class MessageStruct extends Object implements Serializable {
	private static final long serialVersionUID = 3532734764930998421L;
	public int _code;
	public Object _content;
	
	public MessageStruct() {
		this._code = 0;
		this._content = null;
	}
	
	public MessageStruct(int code, Object content) {
		this._code = code;
		this._content = content;
	}
}
