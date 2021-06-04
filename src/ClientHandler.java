import com.google.gson.Gson;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;


// ClientHandler class
class ClientHandler implements Runnable {
    //For constructor
    private String name;
    final DataInputStream dis;
    final DataOutputStream dos;
    Socket s;
    boolean isloggedin;

    //For group function
    ArrayList<String> inGroups = new ArrayList<>();

    Gson gson = new Gson();     //JSON

    //Encryption variable:
    private final static String SECRET_KEY = "Chat Application__Huynh Khang Vi";
    SecretKeySpec skeySpec = new SecretKeySpec(SECRET_KEY.getBytes(), "AES");
    static String encrypted = "";
    String decrypted = "";
    Cipher cipher;

    public String encrypt(String text) {
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

    public String decrypt(String encrypted_msg) {

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

    public void encryptAndSend(String text) {
        try {
            dos.writeUTF(encrypt(text));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean checkInGroups(String thatGroup) {
        for(Group group : Server.grList){
            if (group.getGrName().equals(thatGroup)) {
                return true;
            }
        }
        return false;
    }

    // constructor
    public ClientHandler(Socket s, String name, DataInputStream dis, DataOutputStream dos) {
        this.dis = dis;
        this.dos = dos;
        this.name = name;
        this.s = s;
        this.isloggedin=true;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public boolean checkName(String newName) {
        for(int i = 0; i < Server.list.size(); i++) {
            if(newName.equals(Server.list.get(i).getName())) {
                return false;
            }
            else {
                switch(newName) {
                    case "":
                    case "First_connection":
                        return false;
                }
            }
        }
        return true;
    }

    public boolean checkGroupName(String newGroupName) {
        for(int i = 0; i < Server.grList.size(); i++) {
            if(newGroupName.equals(Server.grList.get(i).getGrName())) {
                return false;
            }
            else {
                switch(newGroupName) {
                    case "":
                        return false;
                }
            }
        }
        return true;
    }

    @Override
    public void run() {

        String encrypted_msg;
        String decrypted_msg;
        Message message_Obj;
        while (isloggedin == true)
        {
            try
            {
                // receive the encrypted JSON string,
                // decrypt it and parse it to Message object.
                encrypted_msg = dis.readUTF();
                decrypted_msg = decrypt(encrypted_msg);
                message_Obj = gson.fromJson(decrypted_msg, Message.class);

                //System.out.println(encrypted_msg);
                System.out.println(decrypted_msg);

                //Check for logout
                if(message_Obj.getAction().equals("logout")){
                    encryptAndSend("Thanks for using this application!");
                    this.isloggedin=false;
                    this.s.close();
                    for(ClientHandler thread :Server.list) {
                        if(name == thread.name) {
                            Server.list.remove(thread);
                            break;
                        }
                    }
                    break;
                }

                boolean checkGroup = false;
                boolean checkClient = false;
                //action commands
                switch(message_Obj.getAction()){
                    case "":
                        System.out.println("No action");
                        break;
                    //p for private
                    case "p":
                        for (ClientHandler thread : Server.list) {
                            if(thread.isloggedin==true && thread.name.equals(message_Obj.getReceiver())) {
                                thread.encryptAndSend("/private " + this.name + " : " + message_Obj.getContent());
                                checkClient = true;
                                break;
                            }
                        }
                        if(checkClient == false) {
                            encryptAndSend("Your receiver was not found! " +
                                    "Please check the list of users and try again!");
                        }
                        break;
                    //b for broadcast
                    case "b":
                        for (ClientHandler thread : Server.list) {
                            if(thread.isloggedin==true){
                                thread.encryptAndSend("/all " + this.name + " : " + message_Obj.getContent());
                            }
                        }
                        break;
                    case "getname":
                        encryptAndSend("Your name is: " + getName());
                        break;
                    case "setname":
                        if(!checkName(message_Obj.getContent())) {
                            encryptAndSend("Your name is invalid, please use another name!");
                        }
                        else {
                            setName(message_Obj.getContent());
                            encryptAndSend("Your new name is: " + getName());
                        }
                        break;
                    //Listing all clients
                    case "ls":
                        encryptAndSend("Total connected users:" + Server.list.size());
                        for(int i = 0; i < Server.list.size(); i++) {
                            encryptAndSend(Server.list.get(i).getName());
                        }
                        break;

                        //NOW FOR THE GROUP COMMANDS
                    //Create a group
                    case "cg":
                        if(!checkGroupName(message_Obj.getContent())) {
                            encryptAndSend("Your group name is invalid, please use another name!");
                        }
                        else {
                            Group newGr = new Group(message_Obj.getContent(), this);
                            Server.grList.add(newGr);
                            newGr.addMember(this);
                            inGroups.add(newGr.getGrName());
                            encryptAndSend("Your new group: " + newGr.getGrName()
                                    + ", has been created.");
                            newGr.updateStates("Group " + newGr.getGrName()
                                    + " has been created. By " + this.getName());
                        }
                        break;
                    //Join that group
                    case "join":
                        if(inGroups.contains(message_Obj.getContent())) {
                            encryptAndSend("You are already in this group!");
                            break;
                        }
                        for(Group group : Server.grList) {
                            if (group.getGrName().equals((message_Obj.getContent()))) {
                                checkGroup = true;
                                if(group.checkBannedList(this)) {
                                    encryptAndSend("You have been banned from group "
                                            + group.getGrName()
                                    + ". Please contact the group's administrator for more detail!");
                                }
                                else {
                                    group.addMember(this);
                                    inGroups.add(group.getGrName());
                                    encryptAndSend("You have joined group " + group.getGrName());
                                    group.updateStates(this.getName() + " has joined group "
                                            + group.getGrName());
                                }
                                break;
                            }
                        }
                        if(!checkGroup){
                            encryptAndSend("Group " + message_Obj.getContent() + " does not exist");
                        }
                        break;
                    //Broadcast in that group
                    case "bg":
                        if(inGroups.contains(message_Obj.getReceiver())) {
                            for(Group group : Server.grList){
                                if(group.getGrName().equals((message_Obj.getReceiver()))) {
                                    for(ClientHandler thread : group.getList()) {
                                        thread.encryptAndSend("/Group " + group.getGrName() + "("
                                                + this.name + "): " + message_Obj.getContent());
                                    }
                                    group.updateMessagesList("/Group " + group.getGrName() + "("
                                            + this.name + "): " + message_Obj.getContent());
                                    break;
                                }
                            }
                        }
                        else {
                            if(!checkInGroups(message_Obj.getReceiver())) {
                                encryptAndSend("Group " + message_Obj.getReceiver()
                                        + " does not exist");
                            }
                            else{
                                encryptAndSend("You don't have enough permission on group "
                                        + message_Obj.getReceiver());
                            }
                        }
                        break;
                    //List all members of that group
                    case "members":
                        if(inGroups.contains(message_Obj.getContent())) {
                            for(Group group : Server.grList){
                                if(group.getGrName().equals((message_Obj.getContent()))) {
                                    encryptAndSend("Total member in group " + group.getGrName()
                                            + " is: " + group.getList().size());
                                    for(int i = 0; i < group.getList().size(); i++) {
                                        encryptAndSend(group.getList().get(i).getName());
                                    }
                                    break;
                                }
                            }
                        }
                        else {
                            if(!checkInGroups(message_Obj.getContent())) {
                                encryptAndSend("Group " + message_Obj.getContent() + " does not exist");
                            }
                            else{
                                encryptAndSend("You don't have enough permission on group " + message_Obj.getContent());
                            }
                        }
                        break;
                    //List 30 newest messages of that group
                    case "gmessages":
                        if(inGroups.contains(message_Obj.getContent())) {
                            for(Group group : Server.grList){
                                if(group.getGrName().equals((message_Obj.getContent()))) {
                                    for (int i = 0; i < group.getMessagesList().size(); i++){
                                        encryptAndSend(group.getMessagesList().get(i));
                                    }
                                    break;
                                }
                            }
                        }
                        else {
                            if(!checkInGroups(message_Obj.getContent())) {
                                encryptAndSend("Group " + message_Obj.getContent() + " does not exist");
                            }
                            else{
                                encryptAndSend("You don't have enough permission on group " + message_Obj.getContent());
                            }
                        }
                        break;
                    //List all groups, and groups you are the member.
                    case "groups":
                        encryptAndSend("Total group is: " + Server.grList.size());
                        for(int i = 0; i < Server.grList.size(); i++) {
                            encryptAndSend(Server.grList.get(i).getGrName());
                        }
                        encryptAndSend("Group you are in is: " + inGroups.size());
                        for(String group : inGroups) {
                            encryptAndSend(group);
                        }
                        break;
                    //Leave that group
                    case "leave":
                        if(inGroups.contains(message_Obj.getContent())) {
                            for(Group group : Server.grList){
                                if(group.getGrName().equals((message_Obj.getContent()))) {
                                    if(group.getAdmins().contains(this)) {
                                        if(group.getAdmins().size() == 1 && group.getList().size() > 1) {
                                            encryptAndSend("You are the last administrator," +
                                                    " please set the new administrator before you leave");
                                        }
                                        else if(group.getAdmins().size() == 1 && group.getList().size() == 1) {
                                            encryptAndSend("You are the last member of this group, and" +
                                                    " you left, so this group will be disbanded");
                                            Server.grList.remove(group);
                                            inGroups.remove(message_Obj.getContent());
                                        }
                                        else {
                                            for (ClientHandler thread : group.getList()){
                                                if(thread.name == this.name){
                                                    group.kickAdmins(thread);
                                                    group.kickMember(thread);
                                                    inGroups.remove(message_Obj.getContent());
                                                    encryptAndSend("You have leave group " + group.getGrName());
                                                    group.updateStates(this.getName() + " has leave group " + group.getGrName());
                                                    break;
                                                }
                                            }
                                        }
                                    }
                                    else {
                                        for (ClientHandler thread : group.getList()){
                                            if(thread.name == this.name){
                                                group.kickMember(thread);
                                                inGroups.remove(message_Obj.getContent());
                                                encryptAndSend("You have leave group " + group.getGrName());
                                                group.updateStates(this.getName() + " has leave group " + group.getGrName());
                                                break;
                                            }
                                        }
                                    }
                                    break;
                                }
                            }
                        }
                        else {
                            if(!checkInGroups(message_Obj.getContent())) {
                                encryptAndSend("Group " + message_Obj.getContent() + " does not exist");
                            }
                            else{
                                encryptAndSend("You not a member of group " + message_Obj.getContent());
                            }
                        }
                        break;
                    //Kick that member
                    case "kick":
                        for(Group group : Server.grList){
                            if(group.getGrName().equals((message_Obj.getReceiver()))) {
                                checkGroup = true;
                                if(group.getAdmins().contains(this)){
                                    for (ClientHandler thread : group.getList()){
                                        if(thread.name.equals(message_Obj.getContent())){
                                            checkClient = true;
                                            if(group.getAdmins().contains(thread)) {
                                                encryptAndSend(message_Obj.getContent()
                                                        + " is one of the group administrators");
                                            }
                                            else {
                                                group.kickMember(thread);
                                                thread.inGroups.remove(message_Obj.getReceiver());
                                                encryptAndSend("Kick " + message_Obj.getContent() + " successfully!");
                                                thread.encryptAndSend("The administrator has kicked" +
                                                        " you out from group " + group.getGrName());
                                                group.updateStates(thread.getName() + " has been kicked." +
                                                        " By " + this.getName());
                                            }
                                            break;
                                        }
                                    }
                                    if(checkClient == false) {
                                        encryptAndSend("That member doesn't exist!");
                                    }
                                }
                                else {
                                        encryptAndSend("You don't have enough permission on group " + message_Obj.getReceiver());
                                }
                                break;
                            }
                        }
                        if(!checkGroup){
                            encryptAndSend("Group " + message_Obj.getReceiver() + " does not exist");
                        }
                        break;
                    //Ban that member
                    case "ban":
                        for(Group group : Server.grList){
                            if(group.getGrName().equals((message_Obj.getReceiver()))) {
                                checkGroup = true;
                                if(group.getAdmins().contains(this)){
                                    for (ClientHandler thread : group.getList()){
                                        if(thread.name.equals(message_Obj.getContent())) {
                                            checkClient = true;
                                            if(group.getAdmins().contains(thread)) {
                                                encryptAndSend(message_Obj.getContent()
                                                        + " is one of the group administrators");
                                            }
                                            else{
                                                group.getList().remove(thread);
                                                thread.inGroups.remove(group.getGrName());
                                                group.ban(thread);
                                                encryptAndSend(thread.getName() + " has been banned!");
                                                thread.encryptAndSend("The administrator of group " + group.getGrName()
                                                        + " has banned you");
                                                group.updateStates(thread.getName() + " has been banned!" +
                                                        " By " + this.getName());
                                            }
                                            break;
                                        }
                                    }
                                    if(checkClient == false) {
                                        encryptAndSend("That member doesn't exist!");
                                    }
                                }
                                else {
                                    encryptAndSend("You don't have enough permission on group " + group.getGrName());
                                }
                                break;
                            }
                        }
                        if(!checkGroup){
                            encryptAndSend("Group " + message_Obj.getReceiver() + " does not exist");
                        }
                        break;
                    //Unban that member
                    case "unban":
                        for(Group group : Server.grList){
                            if(group.getGrName().equals((message_Obj.getReceiver()))) {
                                checkGroup = true;
                                if(group.getAdmins().contains(this)){
                                    for (ClientHandler thread : group.getBanList()){
                                        if(thread.name.equals(message_Obj.getContent())){
                                            checkClient = true;
                                            group.unban(thread);
                                            encryptAndSend(thread.getName() + " has been unbanned!");
                                            thread.encryptAndSend("The administrator of group " + group.getGrName()
                                                    + " has unbanned you");
                                            group.updateStates(thread.getName() + " has been unbanned!" +
                                                    " By " + this.getName());
                                            break;
                                        }
                                    }
                                    if(checkClient == false) {
                                        encryptAndSend("That client doesn't exist!");
                                    }
                                }
                                else {
                                    encryptAndSend("You don't have enough permission on group " + group.getGrName());
                                }
                                break;
                            }
                        }
                        if(!checkGroup){
                            encryptAndSend("Group " + message_Obj.getReceiver() + " does not exist");
                        }
                        break;
                    //List all group states
                    case "states":
                        if(inGroups.contains(message_Obj.getContent())) {
                            for(Group group : Server.grList){
                                if(group.getGrName().equals((message_Obj.getContent()))) {
                                    if(group.getAdmins().contains(this)){
                                        for (int i = 0; i < group.getStates().size(); i++){
                                            encryptAndSend(group.getStates().get(i));
                                        }
                                    }
                                    else {
                                        encryptAndSend("You don't have enough permission on group "
                                                + group.getGrName());
                                    }
                                    break;
                                }
                            }
                        }
                        else {
                            if(!checkInGroups(message_Obj.getContent())) {
                                encryptAndSend("Group " + message_Obj.getContent()
                                        + " does not exist");
                            }
                            else{
                                encryptAndSend("You don't have enough permission on group "
                                        + message_Obj.getContent());
                            }
                        }
                        break;
                    //list all admins
                    case "admins":
                        if(inGroups.contains(message_Obj.getContent())) {
                            for(Group group : Server.grList){
                                if(group.getGrName().equals((message_Obj.getContent()))) {
                                    if(group.getAdmins().contains(this)){
                                        encryptAndSend("admins of group " + group.getGrName() + " is:");
                                        for (int i = 0; i < group.getAdmins().size(); i++){
                                            encryptAndSend(group.getAdmins().get(i).getName());
                                        }
                                    }
                                    else {
                                        encryptAndSend("You don't have enough permission on group " + group.getGrName());
                                    }
                                    break;
                                }
                            }
                        }
                        else {
                            if(!checkInGroups(message_Obj.getContent())) {
                                encryptAndSend("Group " + message_Obj.getContent() + " does not exist");
                            }
                            else{
                                encryptAndSend("You don't have enough permission on group " + message_Obj.getContent());
                            }
                        }
                        break;
                    //add new Admin
                    case"setAd":
                        for(Group group : Server.grList){
                            if(group.getGrName().equals((message_Obj.getReceiver()))) {
                                checkGroup = true;
                                if(group.getAdmins().contains(this)){
                                    for (ClientHandler thread : group.getList()){
                                        if(thread.name.equals(message_Obj.getContent())){
                                            checkClient = true;
                                            if(group.getAdmins().contains(thread)) {
                                                encryptAndSend(message_Obj.getContent()
                                                        + " is the administrators of this group already!");
                                            }
                                            else {
                                                group.addAdmin(thread);
                                                encryptAndSend("New administrator have been added!");
                                                thread.encryptAndSend("The administrator has set" +
                                                        " you to be the administrator of group " + group.getGrName());
                                                group.updateStates(thread.getName() + " have been set to be"
                                                        + " the group administrator. By " + this.getName());
                                            }
                                            break;
                                        }
                                    }
                                    if(checkClient == false) {
                                        encryptAndSend("That member doesn't exist!");
                                    }
                                }
                                else {
                                    encryptAndSend("You don't have enough permission on group " + message_Obj.getReceiver());
                                }
                                break;
                            }
                        }
                        if(!checkGroup){
                            encryptAndSend("Group " + message_Obj.getReceiver() + " does not exist");
                        }
                        break;
                    //List all banned client in that group.
                    case "banlist":
                        if(inGroups.contains(message_Obj.getContent())) {
                            for(Group group : Server.grList){
                                if(group.getGrName().equals((message_Obj.getContent()))) {
                                    if(group.getAdmins().contains(this)){
                                        encryptAndSend("Banned clients in group " + group.getGrName() + " is:");
                                        for (int i = 0; i < group.getBanList().size(); i++){
                                            encryptAndSend(group.getBanList().get(i).getName());
                                        }
                                    }
                                    else {
                                        encryptAndSend("You don't have enough permission on group " + group.getGrName());
                                    }
                                    break;
                                }
                            }
                        }
                        else {
                            if(!checkInGroups(message_Obj.getContent())) {
                                encryptAndSend("Group " + message_Obj.getContent() + " does not exist");
                            }
                            else{
                                encryptAndSend("You don't have enough permission on group " + message_Obj.getContent());
                            }
                        }
                        break;
                    default:
                        encryptAndSend("Wrong command!");
                }
            } catch (IOException e) {

                e.printStackTrace();
            }

        }
        try
        {
            // closing resources
            this.dis.close();
            this.dos.close();

        }catch(IOException e){
            e.printStackTrace();
        }
    }
}