import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import java.net.ServerSocket;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Vector;

// Server class
public class Server {
    //Encryption variable:
    private final static String SECRET_KEY = "Chat Application__Huynh Khang Vi";
    static SecretKeySpec skeySpec = new SecretKeySpec(SECRET_KEY.getBytes(), "AES");
    static String encrypted = "";
    static Cipher cipher;

    private static String encrypt(String text) {
        //SECRET_KEY sẽ cần chuyển qua dạng bytes, sau đó mới tạo ra key dùng thuật toán AES.
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

    // Vector to store active clients
    static Vector<ClientHandler> list = new Vector<>();
    static Vector<Group> grList = new Vector<>();


    // Array for optimize Vector list.
    //static Vector<WorkerThread> optimizeList = new Vector<>();

    // counter for clients
    static int i = 0;

    public static void main(String[] args) throws IOException {
        // server is listening on port 59899
        ServerSocket ss = new ServerSocket(59899);

        Socket s;

        // running infinite loop for getting
        // client request
        try {
            while (true) {
                // Accept the incoming request
                s = ss.accept();

                System.out.println("New client request received : " + s);

                // obtain input and output streams
                DataInputStream dis = new DataInputStream(s.getInputStream());
                DataOutputStream dos = new DataOutputStream(s.getOutputStream());

                System.out.println("Creating a new handler for this client...");

                // Create a new handler object for handling this request.
                ClientHandler mtch = new ClientHandler(s, "client " + i, dis, dos);

                //Send Welcome to the new User.
                dos.writeUTF(encrypt("Welcome to this chat app!"));

                //Send new user notification to all the active users.
                for (ClientHandler thread : Server.list) {
                    if(thread.isloggedin==true){
                        thread.encryptAndSend("/System: New user have been connected: " + mtch.getName());
                    }
                }

                // Create a new Thread with this object.
                Thread t = new Thread(mtch);

                System.out.println("Adding this client to active client list");

                // add this client to active clients list
                list.add(mtch);

                // start the thread.
                t.start();

                // increment i for new client (use for naming).
                i++;

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

