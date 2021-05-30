import java.util.StringTokenizer;

//For easier to manage and for translation to JSON format
public class Message {
    private String sender, receiver, content, action, input;
    public Message (String input, String sender) {
        this.input = input;
        this.sender = sender;
        receiver = "";
        content = "";
        action = "";
    }
    public String getContent() {
        return content;
    }
    public String getAction() {
        return action;
    }
    public String getReceiver() {
        return receiver;
    }

    public void splitStringIntoValues() {
        int count = 0;
        //Use StringTokenizer to split input into content,
        // receiver and action, sender will take later from
        // the variable 'name' in the class Client.
        StringTokenizer st = new StringTokenizer(input, ";");
        int countToken = st.countTokens();

        if(countToken == 0) {
            //for not error when the user just enters without input anything.
        }
        else if(countToken == 1) {
            action = st.nextToken();
        }
        else if(st.countTokens() == 2) {
            action = st.nextToken();
            content = st.nextToken();
        }
        else if(st.countTokens() == 3) {
            action = st.nextToken();
            content = st.nextToken();
            receiver = st.nextToken();
        }
    }
}
