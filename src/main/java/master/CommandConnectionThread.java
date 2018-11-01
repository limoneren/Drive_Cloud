package master;

import main.MasterMode;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

import Utils.Command;

class CommandConnectionThread extends Thread
{
    protected ObjectInputStream is;
    protected ObjectOutputStream os;
    protected Socket s;
    private ServerSocket serverDataSocket;

    //private ServerSocket serverDataSocket;
    private String line = new String();
    //Socket dataSocket;

    /**
     * Creates a thread for the command connection line
     *
     * @param s socket to create a thread on
     * @param dataPort the port that the socket is built on
     */
    public CommandConnectionThread(Socket s, int dataPort)
    {
        this.s = s;

        try {
            serverDataSocket = new ServerSocket(dataPort);
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("server data socket yaratildi");
//
//        try {
//            dataSocket = serverDataSocket.accept();
//            System.out.println("server data socket yaratildi");
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }

    /**
     * The command thread, runs always
     */
    public void run()
    {

        try
        {
            os = new ObjectOutputStream(s.getOutputStream());
            //is = new BufferedReader(new InputStreamReader(s.getInputStream()));

            //commandIs = null; //Error Line!
            try {
                is = new ObjectInputStream(s.getInputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }
            //os = new PrintWriter(s.getOutputStream());


        }
        catch (IOException e)
        {
            System.err.println("master.MasterServer Thread. Run. IO error in server thread");
        }

        try
        {

            Object object = null;
            try {
                object = is.readObject();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
            Command incomingCommand = ((Command)object);
            line = incomingCommand.getType();
            while (line.compareTo("QUIT") != 0)
            {

                System.out.println("[INFO]: Current Command is "+line);
                if(line.equals("GET_ALL_FILES_HASHES")){
                    ArrayList<String> hashOfAllFiles = MasterMode.getHashOfAllFilesAtMasterDriveCloud();

                    MasterMode.printArrayList(hashOfAllFiles);
                    //System.out.println("testt");

                    os.writeObject(new Command("RESPONSE",hashOfAllFiles));

                }else if(line.equals("GET_ADDED_HASHES")){

                    ArrayList<String> dummy = new ArrayList<>();
                    dummy.addAll(MasterMode.getAddedHashes());
                    dummy.addAll(MasterMode.extraFileList2);


                    //MasterMode.hashToPathMap.put(MasterMode.extraFileList2.get(0),MasterMode.getDriveCloudPath());

                    ArrayList<String> hashOfAddedFiles = dummy;
                    System.out.println("HASH_OF_ADDED_FILES:");
                    MasterMode.printArrayList(hashOfAddedFiles);
                    System.out.println("----\n");
                    os.writeObject(new Command("RESPONSE",hashOfAddedFiles));

                }else if(line.equals("GET_DELETED_HASHES")){

                    ArrayList<String> hashOfDeletedFiles = MasterMode.getDeletedHashes();
                    System.out.println("HASH_OF_DELETED_FILES:");
                    MasterMode.printArrayList(hashOfDeletedFiles);
                    System.out.println("----\n");
                    //System.out.println("testt4");

                    os.writeObject(new Command("RESPONSE",hashOfDeletedFiles));

                } else if(line.equals("ASK_FILE_PATH")){

                    File f = MasterMode.getFileFromHashAtMasterDriveCloud(incomingCommand.getContent());
                    String fullPath = f.getAbsolutePath();
                    //System.out.println("ASK_FILE_PATH CommandCon icindeyiz");
                    //System.out.println("ASK_FILE_PATH'deki Path: "+fullPath);
                    os.writeObject(new Command("RESPONSE_FILE_PATH",fullPath));

                }  else if(line.equals("CONSISTENCY_CHECK_PASSED")){


                    os.writeObject(new Command("HURREY"));

                    System.out.println("CONSISTENCY_CHECK_PASSED");

                }  else if(line.equals("RETRANSMIT")){

                    os.writeObject(new Command("FINE"));
                    System.out.println("RETRANSMIT TIME");

                }  else if(line.equals("UPLOAD")){

                    //File f = MasterMode.getFileFromHashAtMasterDriveCloud(incomingCommand.getContent());
                    //String fullPath = f.getAbsolutePath();
                    //System.out.println("UPLOAD icindeyiz");
                    os.writeObject(new Command("YOLLA"));
                    String filePath = incomingCommand.getContent();

                    Socket dataSocket;
                    //String fileHash = incomingCommand.getContent();
                    System.out.println("Now I will receive the file");

                    dataSocket = serverDataSocket.accept();
                    System.out.println("Data connection was established with a follower client on the address of " + dataSocket.getRemoteSocketAddress());
                    DataConnectionThread dataConnectionThread = new DataConnectionThread(dataSocket, filePath);
                    dataConnectionThread.start();
//                    try {
//                        dataConnectionThread.join();
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }

                } else if(line.equals("DELETE")){

                    //File f = MasterMode.getFileFromHashAtMasterDriveCloud(incomingCommand.getContent());
                    //String fullPath = f.getAbsolutePath();
                    //System.out.println("----------------DELETE icindeyiz");
                    os.writeObject(new Command("SILIYOM HACI"));
                    String filePath = incomingCommand.getContent();

                    String newFilePath = "";
                    for(int i = 0; i < 6; i++) {
                        if(i == 4) {
                            newFilePath += "DriveCloud";
                        } else {
                            newFilePath += filePath.split("/")[i];
                        }
                        if(i != 5)
                            newFilePath += "/";
                    }
                    //System.out.println(newFilePath);

                    File toBeDeleted = new File(newFilePath);
                    //System.out.println("TOBEDELETED"+toBeDeleted.getAbsolutePath());
                    MasterMode.hashOfAllFilesAtMasterDriveCloud.remove(MasterMode.getHashOfAFile(new File(newFilePath)));
                    toBeDeleted.delete();

                } else if(incomingCommand.getType().equals("GET")){

                    Socket dataSocket;
                    String fileHash = incomingCommand.getContent();

                    String localHash = MasterMode.getHashOfAFile(MasterMode.getFileFromHashAtMasterDriveCloud(fileHash));
                    os.writeObject(new Command("HASH_FOR_PARITY_CHECK",localHash));


                    //System.out.println("Now I will send the file hash with " + fileHash);

                    dataSocket = serverDataSocket.accept();
                    System.out.println("Data connection was established with a follower client on the address of " + dataSocket.getRemoteSocketAddress());
                    DataConnectionThread dataConnectionThread = new DataConnectionThread(dataSocket, fileHash);
                    dataConnectionThread.start();


                    //DataConnectionThread dataConnectionThread = new DataConnectionThread(dataSocket,new Command("GET", incomingCommand.getContent()));

//                    dataConnectionThread.start();
//                    try {
//                        dataConnectionThread.join();
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }

                    //System.out.println("cok iyi");




                }

                //System.out.println("testt2");
                //os.println(line);
                os.flush();
                //System.out.println("Client " + s.getRemoteSocketAddress() + " sent : " + line);
                //line = is.readLine();
                object = null;
                try {
                    object = is.readObject();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
                incomingCommand =  ((Command)object);
                line = incomingCommand.getType();
            }
        }
        catch (IOException e)
        {
            line = this.getName(); //reused String line for getting thread name
            System.err.println("master.MasterServer Thread. Run. IO Error/ Client " + line + " terminated abruptly");
        }
        catch (NullPointerException e)
        {
            line = this.getName(); //reused String line for getting thread name
            System.err.println("master.MasterServer Thread. Run.Client " + line + " Closed");
        } finally
        {
            try
            {
                System.out.println("Closing the connection");
                if (is != null)
                {
                    is.close();
                    System.err.println(" Socket Input Stream Closed");
                }

                if (os != null)
                {
                    os.close();
                    System.err.println("Socket Out Closed");
                }
                if (s != null)
                {
                    s.close();
                    System.err.println("Socket Closed");
                }

            }
            catch (IOException ie)
            {
                System.err.println("Socket Close Error");
            }
        }//end finally
    }




}