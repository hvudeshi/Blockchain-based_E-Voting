package com.main;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;

import com.blockchain.Block;
import com.network.ClientManager;
import com.network.ServerManager;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;

import static java.lang.System.exit;
import static java.lang.System.lineSeparator;

/**
 * PMMain
 *
 * Main class of a tiny framework to simulate voting using BlockChain via P2P network.
 *
 * A bi-directional migration between server and client is supported using JAVA Serialization/Reflection and Socket.
 * Detailed system design, user case and limitations are elaborated in report.
 */
public class Main {

    private static final String DEFAULT_SERVER_ADDR = "localhost";
    private static final int DEFAULT_PORT = 6777;

    /*
     * Everything starts from here!
     */
    public static void main(String[] args) {
//        int clientId=0;
        System.out.println(" ----- MAIN MENU ----- \n");
        System.out.println("1. Cast Votes");
        System.out.println("2. View Votes on Blockchain");
        System.out.println("3. Count Votes");
        System.out.println("0. Exit\n");

        Scanner scanner = new Scanner(System.in);

        System.out.println("Enter your choice: ");
        int ch = scanner.nextInt();

        if(ch == 1)
        {
            System.out.println("\n ----- Casting Votes ----- \n");
            System.out.println("Please choose a role you want to be: server or client.");
            System.out.println("server PORT - The port to listen to; \"6777\" is default port.");
            System.out.println("client SERVER_ADDRESS PORT - The server address and port to connect to; \"localhost:6777\" is default address-prt combination.");
            System.out.println("Make sure run the server first and then run client to connect to it.");
            System.out.println("> ---------- ");

            Scanner in = new Scanner(System.in);
            String line = in.nextLine();
            String[] cmd = line.split("\\s+");

            if (cmd[0].contains("s"))
            {   // server selected

                /* work as server */
                int port = DEFAULT_PORT;
                if (cmd.length > 1) {
                    try {
                        port = Integer.parseInt(cmd[1]);
                    } catch(NumberFormatException e) {
                        System.out.println("Error: port is not a number!");
                        in.close();
                        return;
                    }
                }

                ServerManager _svrMgr =new ServerManager(port);
                new Thread(_svrMgr).start();


            }
            else if (cmd[0].contains("c"))
            {
                //client selected

                /* work as client */
                String svrAddr = DEFAULT_SERVER_ADDR;
                int port = DEFAULT_PORT;
                if (cmd.length > 2) {
                    try {
                        svrAddr = cmd[1];
                        port = Integer.parseInt(cmd[2]);
                    } catch(NumberFormatException e) {
                        System.out.println("Error: port is not a number!");
                        in.close();
                        return;
                    }
                }

                ClientManager _cltMgr = new ClientManager(svrAddr, port);

                /* new thread to receive msg */
                new Thread(_cltMgr).start();

                _cltMgr.startClient();
            }
            else {
                showHelp();
                in.close();
                return;
            }
            in.close();
        }

        // VIEW VOTES
        else if(ch == 2)
        {
            System.out.println("\n ----- Displaying Votes ----- \n");

            String userHomePath = System.getProperty("user.home");
            String fileName;
            fileName=userHomePath+"/Desktop/blockchain_data";
            File f=new File(fileName);

            try
            {
                if(!f.exists())
                    System.out.println("Blockchain file not found");

                ObjectInputStream in=new ObjectInputStream(new FileInputStream(fileName));

                ArrayList<SealedObject> arr=(ArrayList<SealedObject>) in.readObject();
                for(int i=1;i<arr.size();i++) {
                    System.out.println(decrypt(arr.get(i)));
                }
                in.close();

                System.out.println("-------------------------\n");

            } catch (IOException e1) {
                e1.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (NoSuchPaddingException e) {
                e.printStackTrace();
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            } catch (InvalidKeyException e) {
                e.printStackTrace();
            }
        }

        // COUNT VOTES
        else if(ch == 3)
        {
            String userHomePath = System.getProperty("user.home");
            String fileName;
            fileName=userHomePath+"/Desktop/blockchain_data";
            File f=new File(fileName);

            try
            {
                if(!f.exists())
                    System.out.println("Please cast your votes first !");

                else
                {
                    System.out.println();
                    System.out.println("-------------------------");
                    System.out.println("Vote count: ");
                    ObjectInputStream in=new ObjectInputStream(new FileInputStream(fileName));

                    ArrayList<SealedObject> arr=(ArrayList<SealedObject>) in.readObject();
                    HashMap<String,Integer> voteMap = new HashMap<>();

                    for(int i=1; i<arr.size(); i++)
                    {
                        Block blk = (Block) decrypt(arr.get(i));
                        String key = blk.getVoteObj().getVoteParty();

                        voteMap.put(key,0);
                    }

                    for(int i=1;i<arr.size();i++) {
                        Block blk = (Block) decrypt(arr.get(i));
                        String key = blk.getVoteObj().getVoteParty();

                        voteMap.put(key, voteMap.get(key)+1);
                    }
                    in.close();

                    for(Map.Entry<String, Integer> entry : voteMap.entrySet()) {
                        System.out.println(entry.getKey() + " : " + entry.getValue());
                    }

                    System.out.println("-------------------------\n");
                }

            } catch (IOException e1) {
                e1.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (NoSuchPaddingException e) {
                e.printStackTrace();
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            } catch (InvalidKeyException e) {
                e.printStackTrace();
            }
        }

        else if(ch == 0)
            exit(0);
    }

    public static void showHelp() {
        System.out.println("Restart and select role as server or client.");
        exit(0);
    }

    public static Object decrypt(SealedObject sealedObject) throws IOException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException
    {
        SecretKeySpec sks = new SecretKeySpec("MyDifficultPassw".getBytes(), "AES");

        // Create cipher
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, sks);

        try {
//    		System.out.println(sealedObject.getObject(cipher));
            return sealedObject.getObject(cipher);
        } catch (ClassNotFoundException | IllegalBlockSizeException | BadPaddingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }
    }
}
