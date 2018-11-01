package main;

import com.google.api.services.drive.Drive;
import master.MasterServer;

import javax.swing.filechooser.FileSystemView;
import java.io.*;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

import static java.util.concurrent.TimeUnit.SECONDS;

public class MasterMode {

    public static ArrayList<String> hashOfAllFilesAtMasterDriveCloud;
    public static ArrayList<String> extraFileList = new ArrayList<>(); // follower -> master -> drive
    public static ArrayList<String> extraFileList2 = new ArrayList<>(); // drive -> master -> follower
    public static HashMap<String,String> hashToPathMap = new HashMap<>();
    static HashMap<String,String> hashToPathMapExtra = new HashMap<>();

    private ArrayList<String> addedToMaster = new ArrayList<>();
    private ArrayList<String> deletedFromMaster = new ArrayList<>();
    private ArrayList<String> driveFileList = new ArrayList<>();
    private ArrayList<String> deletedFromDrive = new ArrayList<>();
    DriveConnection driveConnection;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    /**
     * Class constructor.
     */
    public MasterMode(){

        createDriveCloudDirectory();
        loadProperties();


        //Get file input stream for reading the file content
        //File file = new File(Paths.get(System.getProperty("user.home") + "/Desktop/DriveCloud/test.txt").toString());
        //System.out.println(getHashOfAFile(file));

        //getDeletedHashes();
        //getAddedHashes();
        //printArrayList(getHashOfAllFilesAtMasterDriveCloud());


        try {
            driveConnection = new DriveConnection();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        }


        Executors.newSingleThreadExecutor().execute(new Runnable() {
            @Override
            public void run() {
                initializeServer();
            }
        });

        checkMasterAndDrivePeriodically();


    }

    /**
     * Checks whether Master and Drive is synchronized periodically.
     */
    private void checkMasterAndDrivePeriodically() {

        final Runnable checkPeriodically = new Runnable() {
            public void run() {
                System.out.println("---------[DriveCycle] check starts");


                // master a addded: case 1
                addedToMaster = getAddedHashes();
                System.out.println("ADDED TO THE MASTER BEFORE MODIFICAITON");
                printArrayList(addedToMaster);
                addedToMaster.addAll(extraFileList);

                System.out.println("ADDED TO THE MASTER");
                printArrayList(addedToMaster);

                deletedFromMaster = getDeletedHashes();

                System.out.println("DELETED FROM THE MASTER");
                printArrayList(deletedFromMaster);

                driveFileList = driveConnection.getFileList();
                System.out.println("********** Files in Drive:");
                printArrayList(driveFileList);
                System.out.println();
                System.out.println("********** Files in Locale:");
                printArrayList(hashOfAllFilesAtMasterDriveCloud);
                System.out.println();

                synchronize();
                System.out.println("---------[DriveCycle] ended\n");
            }
        };

        final ScheduledFuture<?> checkHandle = scheduler.scheduleAtFixedRate(checkPeriodically, 1, 10, SECONDS);

    }

    /**
     * Synchronize if Master & Drive is not synchronized already.
     */
    private void synchronize() {

        if(addedToMaster.size() > 0) {
            System.out.println("---------------case 1: Addition to Master");
            addToDrive();
            extraFileList.clear();
        } else if(deletedFromMaster.size() > 0){
            System.out.println("---------------case 2: Deletion from Master");
            deleteFromDrive();
        } else if(driveFileList.size() < (hashOfAllFilesAtMasterDriveCloud.size()))  {
            System.out.println("---------------case 3: Deletion from Drive");
            deleteFromMaster();
        } else if(driveFileList.size() > hashOfAllFilesAtMasterDriveCloud.size())  {
            System.out.println("---------------case 4: Addition to Drive");
            addToMaster();
        }

//            addToFollowerFromMaster();
//        }else if(addedToFollower.size() > 0) {
//            System.out.println("---------------case 3");
//            sendToMasterFromFollower();
//        }else if(deletedFromFollower.size() > 0) {
//            System.out.println("---------------case 4");
//            deleteFromMasterToSync();
//        }

    }

    /**
     * Add the missing file in master to the master from Drive.
     */
    private void addToMaster() {

        ArrayList<String> fileNameListAtMaster = new ArrayList<>();

        for(String fileHash : hashOfAllFilesAtMasterDriveCloud){

            String fileName = getFileFromHashAtMasterDriveCloud(fileHash).getName();
            fileNameListAtMaster.add(fileName);

        }

        for(String fileName : driveFileList){

            if(!fileNameListAtMaster.contains(fileName)){

                File toBeAdded = new File(getDriveCloudPath()+"/"+fileName);

                try {
                    driveConnection.downloadFile(driveConnection.service,getDriveCloudPath()+"/"+fileName,fileName);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (GeneralSecurityException e) {
                    e.printStackTrace();
                }


                System.out.println("TO BE ADDED "+toBeAdded.getAbsolutePath());
                MasterMode.hashOfAllFilesAtMasterDriveCloud.add(MasterMode.getHashOfAFile(new File(toBeAdded.getAbsolutePath())));
                MasterMode.extraFileList2.add(MasterMode.getHashOfAFile(new File(toBeAdded.getAbsolutePath())));

                //System.out.println("");



                hashToPathMap.put(getHashOfAFile(toBeAdded), toBeAdded.getAbsolutePath().toString());

                System.out.println("---------extrafilelist 2");
                printArrayList(extraFileList2);
            }

        }



    }

    /**
     * Deletes the extra file in Master, comparing it with Drive folder.
     */
    private void deleteFromMaster() {

        ArrayList<String> fileNameListAtMaster = new ArrayList<>();

        for(String fileHash : hashOfAllFilesAtMasterDriveCloud){

            String fileName = getFileFromHashAtMasterDriveCloud(fileHash).getName();
            fileNameListAtMaster.add(fileName);

        }

        for(String fileName : fileNameListAtMaster){

            if(!driveFileList.contains(fileName)){

                File toBeDeleted = new File(getDriveCloudPath()+"/"+fileName);
                System.out.println("TO BE DELETED "+toBeDeleted.getAbsolutePath());
                MasterMode.hashOfAllFilesAtMasterDriveCloud.remove(MasterMode.getHashOfAFile(new File(toBeDeleted.getAbsolutePath())));
                toBeDeleted.delete();
            }

        }










    }

    /**
     * Deletes the extra file on Drive, comparing it with Master's folder.
     */
    private void deleteFromDrive() {

        for(String s : deletedFromMaster){

            String fullPath = hashToPathMap.get(s);
            String fileName = fullPath.substring(fullPath.lastIndexOf('/') + 1);

            //System.out.println("tekli path: "+fileName);


            //System.out.println("[DRIVE] full path i aldik 3 "+fullPath);
            //File justAdded = new File(filePath);

            driveConnection.deleteFile(driveConnection.service, fileName);

        }

    }


    /**
     * Adds the missing file on drive from Master, comparing it with Master's folder.
     */
    private void addToDrive() {


        for(String s : addedToMaster){
            String fileName = getFileFromHashAtMasterDriveCloud(s).getName();
            //System.out.println("tekli path: "+fileName);

            String fullPath = getFileFromHashAtMasterDriveCloud(s).getAbsolutePath().toString();

            //System.out.println("[DRIVE] full path i aldik 3 "+fullPath);
            //File justAdded = new File(filePath);

            MasterMode.extraFileList2.add(s);
            MasterMode.hashToPathMap.put(s,fullPath);


            try {
                driveConnection.uploadFile(driveConnection.service, driveConnection.DriveCloudId, fileName, fullPath );
            } catch (IOException e) {
                e.printStackTrace();
            }


        }


    }

    /**
     * Casts to threads for multiple followers.
     */
    private void initializeServer() {

        MasterServer masterServer = new MasterServer(MasterServer.DEFAULT_SERVER_COMMAND_PORT, MasterServer.DEFAULT_SERVER_DATA_PORT);

    }

    /**
     * Gets newly added hashes' list on Master's folder.
     * @return An array list of newly added files.
     */
    public static ArrayList<String> getAddedHashes() {
        ArrayList<String> currentHashOfAllFilesAtMasterDriveCloud = getHashOfAllFilesAtMasterDriveCloud();

        ArrayList<String> addedHashes = new ArrayList<>();
        for(String h : currentHashOfAllFilesAtMasterDriveCloud){
            if(!hashOfAllFilesAtMasterDriveCloud.contains(h)){
                addedHashes.add(h);
            }
        }
        for(String s : addedHashes){
            hashOfAllFilesAtMasterDriveCloud.add(s);
            hashToPathMap.put(s, getFileFromHashAtMasterDriveCloud(s).getAbsolutePath().toString());

        }

        return addedHashes;
    }

    /**
     * Gets newly deleted hashes' list on Master's folder..
     * @return An array list of newly deleted files.
     */
    public static ArrayList<String> getDeletedHashes() {
        ArrayList<String> currentHashOfAllFilesAtMasterDriveCloud = getHashOfAllFilesAtMasterDriveCloud();

        ArrayList<String> deletedHashes = new ArrayList<>();
        for(String h : hashOfAllFilesAtMasterDriveCloud){
            if(!currentHashOfAllFilesAtMasterDriveCloud.contains(h)){
                deletedHashes.add(h);
            }
        }
        for(String s : deletedHashes){
            hashOfAllFilesAtMasterDriveCloud.remove(s);
        }

        return deletedHashes;
    }

    /**
     * Updates the propoerties' according to their last instance.
     */
    public static void loadProperties() {
        hashOfAllFilesAtMasterDriveCloud = getHashOfAllFilesAtMasterDriveCloud();
    }

    /**
     * Creates a DriveCloud directory for Desktop.
     */
    private void createDriveCloudDirectory() {

        String desktopPath = getDesktopPath();
        createDirectoryaAtPath(desktopPath);

    }

    /**
     * Checks the created directory on the given path.
     *
     * @param desktopPath The path of DriveCloud folder on the Desktop.
     */
    private void createDirectoryaAtPath(String desktopPath) {
        boolean isSuccess = Paths.get(desktopPath, "DriveCloud").toFile().mkdir();
        if(isSuccess){
            System.out.println("DriveCloud folder has successfully created on desktop");
        } else {
            System.out.println("Error: Cannot create DriveCloud folder on desktop");
        }
    }

    /**
     * Gets the desktop's path.
     * @return A string of desktop path.
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
     * Gets the DriveCloud's path on Desktop.
     * @return A string of DriveCloud path.
     */
    public static String getDriveCloudPath() {
        String osName = System.getProperty("os.name").toLowerCase();
        String pathToDriveCloud = "";
        if(osName.contains("mac")){
            pathToDriveCloud = getDesktopPath() + "/DriveCloud";
        } else {
            pathToDriveCloud = getDesktopPath() + "\\DriveCloud";
        }
        return pathToDriveCloud;
    }

    /**
     * Returns a file from Master's DriveCloud.
     *
     * @param hash Hash of the intended file.
     * @return The intended file.
     */
    public static File getFileFromHashAtMasterDriveCloud(String hash){

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
        if(file == null)
            System.out.println("File null geldi");
        return file;
    }

    /**
     * Returns the hash of all files at Master's DriveCloud.
     * @return An array list of hash of all files at Master's DriveCloud
     */
    public static ArrayList<String> getHashOfAllFilesAtMasterDriveCloud(){

        ArrayList<String> hashList = new ArrayList<>();

        File[] allFiles = new File(getDriveCloudPath()).listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return !file.isDirectory();
            }
        });
        //System.out.println("getHashOfAllFilesAtMasterDriveCloud calisti "+Arrays.toString(allFiles));
        for(File f : allFiles){
            hashList.add(getHashOfAFile(f));
            if(f != null && f != null) {
                hashToPathMap.put(getHashOfAFile(f), f.getAbsolutePath().toString());
            }
        }



        return hashList;
    }

    /**
     * Prints all the list's items line by line.
     * @param arrayList Gets an array list as input.
     */
    public static void printArrayList(ArrayList<String> arrayList){
        for(String s: arrayList){
            System.out.println(s);
        }
    }

    /**
     * Returns the hash of a single file from Master's DriveCloud.
     * @param file Takes a file as an input to take its hash.
     * @return Hash of the given file as a string.
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




}
