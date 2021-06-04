import com.google.gson.Gson;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Scanner;

public class Client {
    final static int ServerPort = 59899;
    final static Gson gson = new Gson();
    static String name = "First_connection";
    private static boolean checkSetName = false;
    private static boolean checkLogOut = false;

    //Encryption variable:
    //AES only supports key sizes of 16, 24 or 32 bytes
    //private final static String SECRET_KEY = "Chat Application - Khang Vi"; // wrong
    private final static String SECRET_KEY = "Chat Application__Huynh Khang Vi";
    static SecretKeySpec skeySpec = new SecretKeySpec(SECRET_KEY.getBytes(), "AES");
    static String encrypted = "";
    static String decrypted = "";
    static Cipher cipher;

    private static String encrypt(String text) {
        try {
            cipher = Cipher.getInstance("AES/ECB/PKCS5PADDING");
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec);
            byte[] byteEncrypted = cipher.doFinal(text.getBytes());
            encrypted = Base64.getEncoder().encodeToString(byteEncrypted);
            return encrypted;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        }
        return encrypted;
    }

    public static String decrypt (String encrypted_msg) {
        try {
            cipher = Cipher.getInstance("AES/ECB/PKCS5PADDING");
            cipher.init(Cipher.DECRYPT_MODE, skeySpec);
            byte[] byteEncrypted = Base64.getDecoder().decode(encrypted_msg);
            byte[] byteDecrypted = cipher.doFinal(byteEncrypted);
            decrypted = new String(byteDecrypted);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        }
        return decrypted;
    }

    public static void main(String args[]) throws UnknownHostException, IOException {
        Scanner scn = new Scanner(System.in);

        // getting localhost ip
        InetAddress ip = InetAddress.getByName("localhost");

        // establish the connection
        Socket s = new Socket(ip, ServerPort);

        // obtaining input and out streams
        DataInputStream dis = new DataInputStream(s.getInputStream());
        DataOutputStream dos = new DataOutputStream(s.getOutputStream());

        //take a server default name (including change to JSON & encryption)
        Message msg_obj = new Message("getname", name);
        msg_obj.splitStringIntoValues();
        String msg_JSON = gson.toJson(msg_obj);
        String msg_encrypted = encrypt(msg_JSON);

        dos.writeUTF(msg_encrypted);
        String welcome = decrypt(dis.readUTF());    //First received message will be the welcome message.
        String myName = decrypt(dis.readUTF());

        name = myName.substring(14);
        System.out.println(welcome);
        System.out.println(myName);

        // sendMessage thread
        Thread sendMessage = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
//                    gui.getMsg();
//                    System.out.println(gui.getMsg());
                    // read the message to deliver.
                    String input = scn.nextLine();

                    //Change to JSON
                    Message msg_obj = new Message(input, name);
                    msg_obj.splitStringIntoValues();
                    String msg_JSON = gson.toJson(msg_obj);

                    //Encryption
                    String msg_encrypted = encrypt(msg_JSON);
                    //System.out.println(msg_encrypted);


                    try {
                        // write on the output stream
                        dos.writeUTF(msg_encrypted);
                        //Check for setName
                        if(input.length() > 7 && input.substring(0,7).equals("setname")){
                            checkSetName = true;
                        }
                        //Check for logout
                        if(input.equals("logout")){
                            checkLogOut = true;
                            break;
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        // readMessage thread
        Thread readMessage = new Thread(new Runnable() {
            @Override
            public void run() {

                while (true) {
                    try {
                        // read the message sent to this client
                        String msg = decrypt(dis.readUTF());

                        //Check server for sending new name
                        if(msg.length() > 18 && msg.substring(0, 18).equals("Your new name is: ")
                                && checkSetName == true) {
                            name = msg.substring(18);
                            checkSetName = false;
                        }
                        System.out.println(msg);

                        //Check for logout
                        if(msg.equals("Thanks for using this application!") && checkLogOut == true) {
                            break;
                        }
                    } catch (IOException e) {

                        e.printStackTrace();
                    }
                }
            }
        });

        sendMessage.start();
        readMessage.start();
    }
}