package com.network;

import java.io.BufferedReader;
import java.io.File;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.nio.file.Files;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SealedObject;
import javax.crypto.spec.SecretKeySpec;

import com.blockchain.Block;

import static com.main.Main.decrypt;
import static java.nio.file.attribute.PosixFilePermission.*;


/**
 * ClientManager
 *
 * Responsible for all the network communications in client side.
 *
 * In a new thread, it will run a loop receiving messages sent from server and
 * dispatch it to the main thread to handle.
 *
 * In the main thread, it provides a message handler handling all the incoming
 * messages. Also, it has interfaces serving ProcessManager.
 *
 */
public class ClientManager extends NetworkManager {

	/* the socket communicating with server */
	private Socket _socket = null;
	private Block genesisBlock;
	private ArrayList<SealedObject> blockList;
	private ArrayList<String> parties;
	private HashSet<String> hashVotes;
	private int prevHash=0;

	private int clientId;

	public ClientManager(String addr, int port) {
		try {
			_socket = new Socket(addr, port);
			System.out.println("Connected to server: " + addr + ":" + port);
			genesisBlock=new Block(0, "", "", "");
			hashVotes=new HashSet<>();
			parties = new ArrayList<>();
			parties.add("BJP");
			parties.add("INC");
			parties.add("BSP");

			blockList=new ArrayList<>();
			blockList.add(encrypt(genesisBlock));
		} catch (IOException e) {
			System.out.println("Cannot connect to server " + addr + ":" + port);
			e.setStackTrace(e.getStackTrace());
			System.exit(0);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void startClient() {

		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		System.out.println("Welcome to Voting Machine ! ");
		String choice ="y";
		do{
			Block blockObj=null;

			String voterId= null;
			String voterName =null;
			String voteParty=null;

			try {
				System.out.print("Enter Voter ID : ");
				voterId = br.readLine();
				System.out.print("Enter Voter Name : ");
				voterName = br.readLine();

				System.out.println("Vote for parties:");
				int voteChoice;

				do {
					for (int i=0 ;i<parties.size() ;i++) {
						System.out.println((i+1)+". "+ parties.get(i));
					}

					System.out.println("Enter your Vote : ");
					voteParty=br.readLine();
					voteChoice=Integer.parseInt(voteParty);
//	                System.out.println("vote choice : "+ voteChoice);
					if(voteChoice>parties.size()||voteChoice<1)
						System.out.println("Please enter correct index .");
					else
						break;
				}while(true);

				voteParty = parties.get(voteChoice-1);
				blockObj=new Block(prevHash, voterId, voterName, voteParty);

				if(checkValidity(blockObj)) {
					hashVotes.add(voterId);
					sendMsg(new MessageStruct( 1,encrypt(blockObj) ));

					prevHash=blockObj.getBlockHash();
					blockList.add(encrypt(blockObj));
					//add
				}
				else
				{
					System.out.println("Vote Invalid.");
				}
				System.out.println("Cast Another Vote (y/n) ? ");
				choice=br.readLine();

			} catch (IOException e) {
				System.out.println("ERROR: read line failed!");
				return;
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}while(choice.equals("y")||choice.equals("Y"));
		close();
	}

	public SealedObject encrypt(Block b) throws Exception
	{
		SecretKeySpec sks = new SecretKeySpec("MyDifficultPassw".getBytes(), "AES");

		// Create cipher
		Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");

		//Code to write your object to file
		cipher.init( Cipher.ENCRYPT_MODE, sks );

		return new SealedObject( b, cipher);
	}

	private boolean checkValidity(Block blockObj) {
		// TODO Auto-generated method stub
		if( hashVotes.contains((String)blockObj.getVoteObj().getVoterId() ))
			return false;
		else
			return true;
	}

	public void sendMsg(MessageStruct msg) throws IOException {
		sendMsg(_socket, msg);
	}

	// Close the socket to exit.
	public void close() {

		String userHomePath = System.getProperty("user.home");
		String fileName;
		fileName=userHomePath+"/Desktop/blockchain_data";
		File f=new File(fileName);

		try
		{
			if(!f.exists())
				f.createNewFile();
			else {
				f.delete();
				f.createNewFile();
			}

			Files.setPosixFilePermissions(f.toPath(),
					EnumSet.of(OWNER_READ, OWNER_WRITE, OWNER_EXECUTE, GROUP_READ, GROUP_EXECUTE));
			System.out.println(fileName);

			ObjectOutputStream o=new ObjectOutputStream(new FileOutputStream(fileName,true));
			o.writeObject(blockList);

			o.close();

			_socket.close();

		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		System.exit(0);
	}

	@Override
	public void msgHandler(MessageStruct msg, Socket src) {
		switch (msg._code) {
			case 0:
				/* message type sent from server to client */
//				System.out.println((String)msg._content.toString()) ;
				try {

					blockList.add((SealedObject)msg._content);

					Block decryptedBlock=(Block) decrypt((SealedObject)msg._content);
					hashVotes.add(decryptedBlock.getVoteObj().getVoterId());

				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				break;
			case 1:
				/* message type sent from broadcast to all clients */
				//server manages this
				break;
			case 2:
				clientId=(int)(msg._content);
			default:
				break;
		}
	}

	/*
	 * Running a loop to receive messages from server. If it fails when receiving, the
	 * connections is broken. Close the socket and exit with -1.
	 */
	@Override
	public void run() {
		while(true) {
			try {
				receiveMsg(_socket);

			} catch (ClassNotFoundException | IOException e) {
				if(_socket.isClosed())
				{
					System.out.println("Bye.");
					System.exit(0);
				}

				System.out.println("Connection to server is broken. Please restart client.");
				close(_socket);
				System.exit(-1);
			}
		}
	}
}