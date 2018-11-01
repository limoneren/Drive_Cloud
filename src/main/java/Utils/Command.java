package Utils;


import java.io.Serializable;
import java.util.ArrayList;

public class Command implements Serializable {

    private String type;
    private String content;
    private ArrayList<String> hashList;

    /**
     * Class constructor: creates a new command given its type.
     *
     * @param type type of the command.
     */
    public Command(String type) {
        this.type = type;
    }

    /**
     * Class constructor: creates a new command given its type and hashList.
     *
     * @param type type of the command.
     * @param hashList list of added or deleted file hashes.
     */
    public Command(String type, ArrayList<String> hashList) {
        this.type = type;
        this.hashList = hashList;
    }

    /**
     * Default constructor.
     */
    public Command() {

    }

    /**
     * Class constructor: creates a new command given its type and content.
     *
     * @param type type of the command.
     * @param content a single content that a command can keep.
     */
    public Command(String type, String content){
        this.type = type;
        this.content = content;
    }

    /**
     * Gets command's type.
     * @return A string of type.
     */
    public String getType() {
        return type;
    }

    /**
     * Sets the type of a command.
     *
     * @param type A string of type.
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Gets command's content.
     * @return A string of content.
     */
    public String getContent() {
        return content;
    }

    /**
     * Sets the content of a command.
     *
     * @param content A string of content.
     */
    public void setContent(String content) {
        this.content = content;
    }

    /**
     * Gets command's hashList.
     * @return An ArrayList of string of hashList.
     */
    public ArrayList<String> getHashList() {
        return hashList;
    }

    /**
     * Sets the hashList of a command.
     *
     * @param hashList list of added or deleted file hashes.
     */
    public void setHashList(ArrayList<String> hashList) {
        this.hashList = hashList;
    }
}
