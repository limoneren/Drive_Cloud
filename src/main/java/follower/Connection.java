package follower;

import Utils.Command;
import main.FollowerMode;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;


public class Connection
{
    public static final String DEFAULT_SERVER_ADDRESS = "localhost";
    public static final int DEFAULT_SERVER_COMMAND_PORT = 4444;
    public static final int DEFAULT_SERVER_DATA_PORT = 4445;

    private Socket commandSocket;
    private Socket dataSocket;
    protected ObjectInputStream commandIs;
    protected ObjectOutputStream commandOs;

    protected String serverAddress;
    protected int serverCommandPort;
    protected int serverDataPort;

    protected BufferedInputStream dataIs;
    protected BufferedOutputStream dataOs;
    protected BufferedInputStream bisForFile = null;
    FileOutputStream fos = null;
    FileInputStream fis = null;

    /**
     * Constructor of Connection object
     * @param address The address of the master
     * @param serverCommandPort The chosen port for command flow between master and follower
     * @param serverDataPort The chosen port for data flow between master and follower
     */

    public Connection(String address, int serverCommandPort, int serverDataPort)
    {
        serverAddress = address;
        this.serverCommandPort = serverCommandPort;
        this.serverDataPort = serverDataPort;
    }

    /**
     * Establishes a socket connection to the server that is identified by the serverAddress and the serverPort
     */
    public void Connect()
    {
        try
        {
            commandSocket=new Socket(serverAddress, serverCommandPort);
            System.out.println("command socket kurdu " + serverCommandPort);


            commandOs = new ObjectOutputStream(commandSocket.getOutputStream());
            commandIs = null;
            try {
                commandIs = new ObjectInputStream(commandSocket.getInputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }


            System.out.println("Successfully connected to " + serverAddress + " on command and data port " + serverCommandPort);
        }
        catch (IOException e)
        {
            System.err.println("Error: no server has been found on " + serverAddress + "/" + serverCommandPort);
        }
    }

    /**
     * Finds the full path of a file from its hash
     * @param fileHash The hash of a file
     * @return full path of a file
     */

    public String askForFileFullPath(String fileHash) {
        String fullPath = "";

        try {
            commandOs.writeObject(new Command("ASK_FILE_PATH",fileHash));
            commandOs.flush();

            Object object = null;
            try {
                object = commandIs.readObject();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
            Command incomingCommand = ((Command) object);
            fullPath =  incomingCommand.getContent();

        } catch (IOException e) {
            e.printStackTrace();
        }

        return fullPath;
    }

    /**
     * Creates an delete command and sends it
     * @param fileHash The hash of a file
     * @param filePath The path of a file
     * @return a dummy response
     */
    public String sendDeleteCommand(String fileHash, String filePath) {
        String response = "";
        try {
            commandOs.writeObject(new Command("DELETE",filePath));
            commandOs.flush();


            Object object = null;
            try {
                object = commandIs.readObject();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
            //Command incomingCommand = ((Command) object);
            //String success =  incomingCommand.getType();

        } catch (IOException e) {
            e.printStackTrace();
        }
        return response;
    }
    /**
     * Uploads the given file from follower to the master
     * @param fileHash The hash of a file
     * @param filePath The path of a file
     * @return a dummy response
     */
    public String uploadFile(String fileHash, String filePath) {
        String response = "";


        try {
            commandOs.writeObject(new Command("UPLOAD",filePath));
            commandOs.flush();

            Object object = null;
            try {
                object = commandIs.readObject();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
            Command incomingCommand = ((Command) object);
            String success =  incomingCommand.getType();


            dataSocket=new Socket(serverAddress, serverDataPort);

            dataOs = new BufferedOutputStream(dataSocket.getOutputStream());

            dataIs = new BufferedInputStream(dataSocket.getInputStream());


            File myFile = new File(filePath);
            int j = -1;
            byte[] bufferWrite = new byte[8192];
            fis = new FileInputStream(myFile);
            bisForFile = new BufferedInputStream(fis);
            j = bisForFile.read(bufferWrite);

            while(j > -1) {
                dataOs.write(bufferWrite,0,j);
                System.out.println("j once: "+j);
                j = bisForFile.read(bufferWrite);
                System.out.println("j sonra: "+j);
                dataOs.flush();

            }
            dataOs.close();
            dataIs.close();


        } catch (IOException e) {
            e.printStackTrace();
        }


        return response;
    }
    /**
     * Creates a command to ask master for the file with the given parameters
     * @param fileHash The hash of a file
     * @param filePath The path of a file
     * @return a dummy response
     */
    public String askForFile(String fileHash, String filePath){
        String response = "";
        try {
            commandOs.writeObject(new Command("GET",fileHash));
            commandOs.flush();


            Object object = null;
            try {
                object = commandIs.readObject();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
            Command incomingCommand = ((Command) object);
            String receivedHash =  incomingCommand.getContent();
            response = receivedHash;


            dataSocket=new Socket(serverAddress, serverDataPort);

            dataOs = new BufferedOutputStream(dataSocket.getOutputStream());

            dataIs = new BufferedInputStream(dataSocket.getInputStream());

            int n = -1;
            byte[] bufferToReceive = new byte[8192];
            String newFilePath = "";
            for(int i = 0; i < 6; i++) {
                if(i == 4) {
                    newFilePath += filePath.split("/")[i] +"Follower";
                } else {
                    newFilePath += filePath.split("/")[i];
                }
                if(i != 5)
                    newFilePath += "/";
            }

            System.out.println(newFilePath);


            fos = new FileOutputStream(newFilePath);
            while ((n = dataIs.read(bufferToReceive)) > 0){
                fos.write(bufferToReceive, 0, n);
            }


        } catch (IOException e) {
            e.printStackTrace();
        }
        return response;
    }
    /**
     * Retransmits the files with the given hashes in case of an inconsistency
     * @param fileHash The hash of a file
     */

    private void sendRetransmit(String fileHash) {
        ArrayList<String> response = new ArrayList<>();
        try {
            commandOs.writeObject(new Command("RETRANSMIT",fileHash));
            commandOs.flush();

            Object object = null;
            try {
                object = commandIs.readObject();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
            Command incomingCommand = ((Command) object);



        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    /**
     * It sends a CONSISTENCY_CHECK_PASSED command after a successful transmission
     */
    public void sendCheckPassed() {
        ArrayList<String> response = new ArrayList<>();
        try {
            commandOs.writeObject(new Command("CONSISTENCY_CHECK_PASSED"));
            commandOs.flush();

            Object object = null;
            try {
                object = commandIs.readObject();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
            Command incomingCommand = ((Command) object);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * It sends a GET_ADDED_HASHES command to get the hashes of added files.
     * @return An arraylist of hashes of added files
     */

    public ArrayList<String> askForAddedFiles() {
        ArrayList<String> response = new ArrayList<>();
        try {
            commandOs.writeObject(new Command("GET_ADDED_HASHES"));
            commandOs.flush();

            Object object = null;
            try {
                object = commandIs.readObject();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
            Command incomingCommand = ((Command) object);
            response =  incomingCommand.getHashList();
            //System.out.println("Added Hashes are:");

        } catch (IOException e) {
            e.printStackTrace();
        }
        return response;
    }

    /**
     * It sends a GET_DELETED_HASHES command to get the hashes of deleted files.
     * @return An arraylist of hashes of deleted files
     */
    public ArrayList<String> askForDeletedFiles() {
        ArrayList<String> response = new ArrayList<>();
        try {
            commandOs.writeObject(new Command("GET_DELETED_HASHES"));
            commandOs.flush();

            Object object = null;
            try {
                object = commandIs.readObject();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
            Command incomingCommand = ((Command) object);
            response =  incomingCommand.getHashList();
            //System.out.println("Deleted Hashes are:");

        } catch (IOException e) {
            e.printStackTrace();
        }
        return response;
    }

    /**
     * It sends a GET_ALL_FILES_HASHES command to get the hashes of all files.
     * @return An arraylist of hashes of all files
     */

    public ArrayList<String> askForAllFiles() {
        ArrayList<String> response = new ArrayList<>();
        try {
            commandOs.writeObject(new Command("GET_ALL_FILES_HASHES"));
            commandOs.flush();

            Object object = null;
            try {
                object = commandIs.readObject();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
            Command incomingCommand = ((Command) object);
            response =  incomingCommand.getHashList();
            //System.out.println("All File Hashes are:");

        } catch (IOException e) {
            e.printStackTrace();
        }
        return response;
    }


    /**
     * Disconnects the socket and closes the buffers
     */
    public void Disconnect()
    {
        try
        {
            commandIs.close();
            commandOs.close();
            commandSocket.close();

            dataIs.close();
            dataOs.close();
            dataSocket.close();

            System.out.println("follower.ConnectionToMaster. SendForAnswer. Connection Closed");
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }
}

