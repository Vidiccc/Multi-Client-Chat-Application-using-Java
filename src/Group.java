import java.util.ArrayList;
import java.util.Vector;

public class Group {
    private String groupName = "";
    public Vector<ClientHandler> groupMember = new Vector<>();
    private ArrayList<ClientHandler> administrators = new ArrayList<>();
    private ArrayList<ClientHandler> banList = new ArrayList<>();
    private ArrayList<String> messagesList = new ArrayList<>();
    private ArrayList<String> states = new ArrayList<>();


    public Group(String grName, ClientHandler newAdmin) {
        groupName = grName;
        administrators.add(newAdmin);
    }

    public String getGrName() {
        return groupName;
    }

    public ArrayList<ClientHandler> getAdmins() {
        return administrators;
    }

    public ArrayList<ClientHandler> getBanList() {
        return banList;
    }

    public Vector<ClientHandler> getList() {
        return groupMember;
    }

    public ArrayList<String> getMessagesList() {
        return messagesList;
    }

    public ArrayList<String> getStates() {
        return states;
    }

    public ArrayList<String> addToList() {
        return states;
    }

    public ArrayList<String> addToMessagesList() {
        return states;
    }

    public ArrayList<String> addToStates() {
        return states;
    }

    public void ban(ClientHandler banned) {
        banList.add(banned);
    }

    public void unban(ClientHandler banned) {
        banList.remove(banned);
    }

    public void addAdmin(ClientHandler newAdmin) {
        administrators.add(newAdmin);
    }

    public void addMember(ClientHandler member) {
        groupMember.add(member);
    }

    public void kickAdmins(ClientHandler newAdmin) {
        administrators.remove(newAdmin);
    }

    public void kickMember(ClientHandler member) {
        groupMember.remove(member);
    }

    public boolean checkBannedList(ClientHandler user) {
        for(ClientHandler client : banList) {
            System.out.println(user);
            System.out.println(client);
            if(client == user) {
                return true;
            }
        }
        return false;
    }

    public void updateMessagesList(String newMsg) {
        messagesList.add(newMsg);
        if(messagesList.size() > 30){
            messagesList.remove(0);
        }
    }

    public void updateStates(String newState) {
        states.add(newState);
        if(states.size() > 30){
            states.remove(0);
        }
    }
}