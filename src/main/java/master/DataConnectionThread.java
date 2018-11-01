package master;

import Utils.Command;
import main.MasterMode;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;

class DataConnectionThread extends Thread
{
    //protected BufferedReader is;
    //protected ObjectOutputStream os;

    protected Socket s;
    //private String line = new String();
    FileInputStream fis;
    FileOutputStream fos;

    BufferedInputStream bisForFile;
    BufferedInputStream bisForSocket;
    BufferedOutputStream bos;
    //Command currentCommand;
    String currentHash;

    /**
     * Creates a thread for the data connection line
     *
     * @param s socket to create a thread on
     * @param fileHash hash of the file that is going to be transmitted
     *                 or the filePath for an empty file that is going to be created for transmission
     */
    public DataConnectionThread(Socket s, String fileHash)
    {
        this.s = s;
        //currentCommand = cmd;
        this.currentHash = fileHash;
    }

    /**
     * The data thread, runs whenever it's called.
     */
    public void run()
    {


        //String hashOfOutgoingFile = currentCommand.getContent();

        //File fileToSend = MasterMode.getFileFromHashAtMasterDriveCloud(hashOfOutgoingFile);

        try
        {
            bos = new BufferedOutputStream(s.getOutputStream());
            bisForSocket = new BufferedInputStream(s.getInputStream());

        }
        catch (IOException e)
        {
            System.err.println("bos and bisForSocket initialization error");
        }



        try {

            if(currentHash.indexOf("DriveCloud") != -1) {


                int j = -1;
                byte[] bufferToReceive = new byte[8192];

                bisForSocket = new BufferedInputStream(s.getInputStream());

                //current = 0;

                //System.out.println("/n/nBefore update");
                //MasterMode.printArrayList(MasterMode.hashOfAllFilesAtMasterDriveCloud);

                String newFilePath = "";
                for(int i = 0; i < 6; i++) {
                    if(i == 4) {
                        newFilePath += "DriveCloud";
                    } else {
                        newFilePath += currentHash.split("/")[i];
                    }
                    if(i != 5)
                        newFilePath += "/";
                }

                fos = new FileOutputStream(newFilePath);
                //System.out.println("fos created");
                j = bisForSocket.read(bufferToReceive);
                //System.out.println("j ilk: "+j);
                while (j > -1){
                    fos.write(bufferToReceive, 0, j);
                    //System.out.println("Deadlock 6");
                    j = bisForSocket.read(bufferToReceive);
                    //System.out.println("j: "+j);
                    //current += n;
                }

                //System.out.println("New Path: "+newFilePath);
                MasterMode.hashOfAllFilesAtMasterDriveCloud.add(MasterMode.getHashOfAFile(new File(newFilePath)));
                MasterMode.extraFileList.add(MasterMode.getHashOfAFile(new File(newFilePath))); // for follower -> master -> drive
                //System.out.println("Istedigim yer calisti");
                //MasterMode.loadProperties();
                //System.out.println("/n/nAfter update");
                //MasterMode.printArrayList(MasterMode.hashOfAllFilesAtMasterDriveCloud);


            } else {
                // send file
                //File fileToSend = new File(FILE_TO_SEND);
                int n = -1;
                byte[] bufferToSend = new byte[8192];

                fis = new FileInputStream(MasterMode.getFileFromHashAtMasterDriveCloud(currentHash));

                bisForFile = new BufferedInputStream(fis);

                while((n = bisForFile.read(bufferToSend))>-1) {
                    bos.write(bufferToSend,0,n);
                    bos.flush();
                }

                //System.out.println("Yazim islemi bitti");
                MasterMode.extraFileList2.clear();
            }


        } catch (IOException e) {
            e.printStackTrace();
        }
        finally
        {
            try
            {
                System.out.println("Closing the connection");
                if (bisForFile != null)
                {
                    bisForFile.close();
                    System.err.println(" Socket Input Stream Closed");
                }

                if (bos != null)
                {
                    bos.close();
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
