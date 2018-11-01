package main;

import follower.Connection;
//import follower.ConnectionToMaster;
//import follower.ConnectionToMasterForData;

import javax.swing.filechooser.FileSystemView;
import java.io.*;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

import static java.util.concurrent.TimeUnit.*;


public class FollowerMode {

    static ArrayList<String> hashOfAllFilesAtFollowerDriveCloud;
    static HashMap<String,String> hashToPathMap = new HashMap<>();
    //ConnectionToMaster connectionToMaster = null;
    Connection connection = null;
    //ConnectionToMasterForData connectionToMasterForData = null;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private ArrayList<String> addedToMaster = new ArrayList<>();
    private ArrayList<String> deletedFromMaster = new ArrayList<>();
    private ArrayList<String> addedToFollower = new ArrayList<>();
    private ArrayList<String> deletedFromFollower = new ArrayList<>();
    //private static int numberOfDriveClouds = 0;



    /**
     * Class constructor.
     */
    public FollowerMode() {

        createDriveCloudDirectory();
        loadProperties();

        connectToMaster();
        //connectToMasterForData();

        checkMasterPeriodically();
    }


    /**
     * checks master over the command socket periodically
     */
    public void checkMasterPeriodically() {

        final Runnable checkPeriodically = new Runnable() {
            public void run() {
                System.out.println("---------[SingleCycle] Requesting the file hashes from the master");
                addedToMaster = connection.askForAddedFiles();
                //printArrayList(addedToMaster);
                deletedFromMaster = connection.askForDeletedFiles();
                //printArrayList(deletedFromMaster);
                addedToFollower = FollowerMode.getAddedHashes();
                deletedFromFollower = FollowerMode.getDeletedHashes();
                //System.out.println("Silinecekler: ");
                //printArrayList(deletedFromFollower);
                synchronize();
                System.out.println("---------[SingleCycle] ended\n\n\n");
            }
        };

        final ScheduledFuture<?> checkHandle = scheduler.scheduleAtFixedRate(checkPeriodically, 1, 10, SECONDS);

    }

    /**
     * synchronize master and follower according to detected changes
     */
    private void synchronize() {

        if(deletedFromMaster.size() > 0){
            System.out.println("---------------case 1: Deletion from Master");
            deleteFromFollowerToSync();
        }else if(addedToMaster.size() > 0){
            System.out.println("---------------case 2: Addition to Master");
            addToFollowerFromMaster();
        }else if(addedToFollower.size() > 0) {
            System.out.println("---------------case 3: Addition to Follower");
            sendToMasterFromFollower();
        }else if(deletedFromFollower.size() > 0) {
            System.out.println("---------------case 4: Deletion from Follower");
            deleteFromMasterToSync();
        }

    }

    /**
     * Whenever a file is deleted from follower, this is called to delete the same file from the master.
     */
    private void deleteFromMasterToSync() {
        // TODO Auto-generated method stub
        String filePath;
        String checkParity;
        for(String s : deletedFromFollower){
            //filePath = FollowerMode.getFileFromHashAtFollowerDriveCloud(s).getAbsolutePath().toString();
            filePath = hashToPathMap.get(s);
            System.out.println("------- full path i aldik 3 "+filePath);
            //File justAdded = new File(filePath);
            checkParity = connection.sendDeleteCommand(s,filePath);
        }
    }


    /**
     * Whenever a file is added to follower, this is called to upload the same file to the master.
     */
    private void sendToMasterFromFollower() {
        //connectionToMasterForData = new ConnectionToMasterForData(ConnectionToMaster.DEFAULT_SERVER_ADDRESS, ConnectionToMaster.DEFAULT_SERVER_COMMAND_PORT);
        //connectionToMasterForData.Connect();
        //connectToMasterForData();
        String filePath;
        String checkParity;
        for(String s : addedToFollower){
            filePath = FollowerMode.getFileFromHashAtFollowerDriveCloud(s).getAbsolutePath().toString();
            System.out.println("full path i aldik 3 "+filePath);
            //File justAdded = new File(filePath);

            checkParity = connection.uploadFile(s,filePath);

        }

    }

    /**
     * Whenever a file is added to master, this is called to add the same file to the follower.
     */
    private void addToFollowerFromMaster() {

        //connectionToMasterForData = new ConnectionToMasterForData(ConnectionToMaster.DEFAULT_SERVER_ADDRESS, ConnectionToMaster.DEFAULT_SERVER_COMMAND_PORT);
        //connectionToMasterForData.Connect();
        //connectToMasterForData();
        String filePath;
        String receivedHash;
        for(String s : addedToMaster){
            filePath = connection.askForFileFullPath(s);
            //System.out.println("full path i aldik "+filePath);
            //File justAdded = new File(filePath);
            receivedHash = connection.askForFile(s,filePath);
            String calculatedHash = FollowerMode.getHashOfAFile(new File(filePath));

            if(calculatedHash.equals(receivedHash)){
                connection.sendCheckPassed();
                System.out.println("Consistency check for "+filePath.substring(filePath.lastIndexOf('/')+1)+ " passed!");
            } else {
                addToFollowerFromMaster();
                System.out.println("Retransmmit request for file "+filePath.substring(filePath.lastIndexOf('/')+1));
            }

            hashOfAllFilesAtFollowerDriveCloud.add(s);

        }

    }

    /**
     * Whenever a file is deleted from master, this is called to delete the same file from the follower.
     */
    private void deleteFromFollowerToSync() {

        for(String s : deletedFromMaster){
            File toBeDeleted = getFileFromHashAtFollowerDriveCloud(s);
            hashOfAllFilesAtFollowerDriveCloud.remove(s);
            toBeDeleted.delete();
        }

    }

    /**
     * Establish a connection to the master
     */
    private void connectToMaster() {


//        connectionToMaster = new ConnectionToMaster(ConnectionToMaster.DEFAULT_SERVER_ADDRESS, ConnectionToMaster.DEFAULT_SERVER_COMMAND_PORT, ConnectionToMaster.DEFAULT_SERVER_DATA_PORT);
//        connectionToMaster.Connect();
//        connectToMasterForData();
        connection = new Connection(Connection.DEFAULT_SERVER_ADDRESS, Connection.DEFAULT_SERVER_COMMAND_PORT, Connection.DEFAULT_SERVER_DATA_PORT);
        connection.Connect();
        //connectToMasterForData();

    }

    /**
     * Get the added hashes to the follower between two time frames. Update the array list that
     * keeps all the file hashes.
     *
     * @return array list of the added hashes
     */
    public static ArrayList<String> getAddedHashes() {
        ArrayList<String> currentHashOfAllFilesAtFollowerDriveCloud = getHashOfAllFilesAtFollowerDriveCloud();

        ArrayList<String> addedHashes = new ArrayList<>();
        for(String h : currentHashOfAllFilesAtFollowerDriveCloud){
            if(!hashOfAllFilesAtFollowerDriveCloud.contains(h)){
                addedHashes.add(h);
            }
        }
        for(String s : addedHashes){
            hashOfAllFilesAtFollowerDriveCloud.add(s);
            hashToPathMap.put(s, getFileFromHashAtFollowerDriveCloud(s).getAbsolutePath().toString());
        }
        return addedHashes;
    }

    /**
     * Get the deleted hashes from the follower between two time frames. Update the array list that
     * keeps all the file hashes.
     *
     * @return array list of the deleted hashes
     */
    public static ArrayList<String> getDeletedHashes() {
        ArrayList<String> currentHashOfAllFilesAtFollowerDriveCloud = getHashOfAllFilesAtFollowerDriveCloud();

        ArrayList<String> deletedHashes = new ArrayList<>();
        for(String h : hashOfAllFilesAtFollowerDriveCloud){
            if(!currentHashOfAllFilesAtFollowerDriveCloud.contains(h) && !deletedHashes.contains(h)){
                deletedHashes.add(h);
            }
        }
        for(String s : deletedHashes){
            hashOfAllFilesAtFollowerDriveCloud.remove(s);
        }
        return deletedHashes;
    }

    /**
     * load the array list that keeps all the file hashes at
     * first initializaiton
     */
    private void loadProperties() {
        hashOfAllFilesAtFollowerDriveCloud = getHashOfAllFilesAtFollowerDriveCloud();
    }

    /**
     * Create the drive cloud directory for the follower
     */
    private void createDriveCloudDirectory() {

        String desktopPath = getDesktopPath();
        createDirectoryaAtPath(desktopPath);
        //numberOfDriveClouds++;

    }

    /**
     * Get the drive cloud follower number to handle with the single computer case
     * @return integer of count of followers
     */
    private static int getDriveCloudFollowerFolderNumber() {
        int count = 0;

        File file = null;

        File[] allFiles = new File(getDesktopPath()).listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.isDirectory();
            }
        });

        for(File f : allFiles){
            if(f.getName().indexOf("DriveCloudFollower") != -1){
                count++;
            }
        }

        return count;
    }


    /**
     * Create a directory at a given path
     *
     * @param desktopPath a string of desktop path
     */
    private void createDirectoryaAtPath(String desktopPath) {
        boolean isSuccess;
        int count = getDriveCloudFollowerFolderNumber();
        if(count == 0){
            isSuccess = Paths.get(desktopPath, "DriveCloudFollower").toFile().mkdir();
        } else {
            isSuccess = Paths.get(desktopPath, "DriveCloudFollower"+count).toFile().mkdir();
        }
        if(isSuccess){
            System.out.println("DriveCloud folder has successfully created on desktop");
        } else {
            System.out.println("Error: Cannot create DriveCloud folder on desktop");
        }
    }

    /**
     * Get the desktop path for different OS.
     * @return a string of desktop path
     */
    private static String getDesktopPath() {
        String osName = System.getProperty("os.name").toLowerCase();
        String pathToDesktop = "";
        if(osName.contains("mac")){
            pathToDesktop = System.getProperty("user.home") + "/Desktop";
        } else {
            pathToDesktop = Paths.get(FileSystemView.getFileSystemView().getHomeDirectory().getAbsolutePath()).toString();
        }
        return pathToDesktop;
    }

    /**
     * Get the path of the drive cloud follower folder
     * @return  a string of drive cloud follower folder path
     */
    private static String getDriveCloudPath() {
        String osName = System.getProperty("os.name").toLowerCase();
        String pathToDriveCloud = "";
        if(osName.contains("mac")){
            if(getDriveCloudFollowerFolderNumber() == 1){
                pathToDriveCloud = getDesktopPath() + "/DriveCloudFollower";
            } else {
                pathToDriveCloud = getDesktopPath() + "/DriveCloudFollower" +getDriveCloudFollowerFolderNumber();
            }

        } else {
            if(getDriveCloudFollowerFolderNumber() == 1){
                pathToDriveCloud = getDesktopPath() + "\\DriveCloudFollower";
            } else {
                pathToDriveCloud = getDesktopPath() + "\\DriveCloudFollower" +getDriveCloudFollowerFolderNumber();
            }

        }
        return pathToDriveCloud;
    }

    /**
     * Get the file from the hash of a file at follower
     *
     * @param hash a string of file's hash.
     * @return file with given hash
     */
    private static File getFileFromHashAtFollowerDriveCloud(String hash){
        //System.out.println("hash: "+hash);
        File file = null;

        File[] allFiles = new File(getDriveCloudPath()).listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return !file.isDirectory();
            }
        });

        for(File f : allFiles){
            if(getHashOfAFile(f).equals(hash)){
                file = f;
                break;
            }
        }
        return file;
    }

    /**
     * Get hash of all files at the follower drive cloud
     * @return an array list of string of file hashes
     */
    public static ArrayList<String> getHashOfAllFilesAtFollowerDriveCloud(){

        ArrayList<String> hashList = new ArrayList<>();

        File[] allFiles = new File(getDriveCloudPath()).listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return !file.isDirectory();
            }
        });

        for(File f : allFiles){
            hashList.add(getHashOfAFile(f));
            if(f != null && f != null) {
                hashToPathMap.put(getHashOfAFile(f), f.getAbsolutePath().toString());
            }

        }

        return hashList;
    }

    /**
     * Prints an array list for debugging purposes
     */
    public static void printArrayList(ArrayList<String> arrayList){
        for(String s: arrayList){
            System.out.println(s);
        }
    }

    /**
     * Get the hash of a file using SHA-1
     * @param file the file that the hash is going to be taken
     * @return a string of SHA-1 hash of the file
     */
    public static String getHashOfAFile(File file){

        //Get file input stream for reading the file content
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        //Create byte array to read data in chunks
        byte[] byteArray = new byte[1024];
        int bytesCount = 0;

        //Read file data and update in message digest
        try {
            bytesCount = fis.read(byteArray);
        } catch (IOException e) {
            e.printStackTrace();
        }
        while (bytesCount != -1) {
            md.update(byteArray, 0, bytesCount);
            try {
                bytesCount = fis.read(byteArray);
            } catch (IOException e) {
                e.printStackTrace();
            }
        };

        //close the stream; We don't need it now.
        try {
            fis.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        //Get the hash's bytes
        byte[] bytes = md.digest();

        //convert the byte to hex format method
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            sb.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1));
        }

        return sb.toString();

    }

    public void disconnectFromMaster(){
        connection.Disconnect();
    }




}
