package main;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.api.client.http.FileContent;
import com.google.api.services.drive.model.Change;
import com.google.api.services.drive.model.ChangeList;
import com.google.api.services.drive.model.StartPageToken;
import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.annotations.Beta;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;


import java.io.*;
import java.lang.reflect.Array;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.*;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.security.Key;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.zip.Adler32;
import java.util.zip.CRC32;
import java.util.zip.Checksum;
import javax.crypto.spec.SecretKeySpec;

public class DriveConnection {

    private static final String APPLICATION_NAME = "Google Drive API Java Quickstart";
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY_PATH = "tokens";

    /**
     * Global instance of all the scopes required by this connection. All scopes are given initially.
     * If modifying these scopes, delete your previously saved tokens/folder.
     */
    //private static final List<String> SCOPES = Collections.singletonList(DriveScopes.DRIVE_FILE);

    private static final List<String> SCOPES = new ArrayList(DriveScopes.all());

    public String DriveCloudId = "";

    public Drive service;
    private static final String CREDENTIALS_FILE_PATH = "/credentials.json";

    /**
     * Class constructor.
     */
    public DriveConnection() throws IOException, GeneralSecurityException {
        // Build a new authorized API client service.
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        service = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
                .setApplicationName(APPLICATION_NAME)
                .build();

        int DriveCloudChecker = 0;
        // Print the names and IDs for up to 10 files.
        FileList result = service.files().list()
                .setPageSize(10)
                .setFields("nextPageToken, files(id, name)")
                .execute();
        List<File> files = result.getFiles();
        if (files == null || files.isEmpty()) {
            System.out.println("No files found.");
        } else {
            //System.out.println("Files:");
            for (File file : files) {
                //System.out.printf("%s (%s)\n", file.getName(), file.getId());
                if (file.getName().equals("DriveCloud")) {
                    DriveCloudChecker = 1;
                    //System.out.println("DriveCloud already exists!");
                }
            }
        }


        for (File file : files) {
            if (file.getName().equals("DriveCloud")) {
                DriveCloudId = file.getId();
            }
        }

        if (DriveCloudChecker == 0) {
            DriveCloudId = createFolder(service);
            System.out.println("DriveCloud folder created with ID: " + DriveCloudId);
        }
        //uploadFile(service, DriveCloudId, "hop.txt");
        //fileList(service, files);
        //downloadFile(service, "hop.txt", files);
        //uploadFile(service, DriveCloudId, "asdasd.txt");
    }


    /**
     * Creates an authorized Credential object.
     *
     * @param HTTP_TRANSPORT The network HTTP Transport.
     * @return An authorized Credential object.
     * @throws IOException If the credentials.json file cannot be found.
     */
    private static Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) throws IOException {
        // Load client secrets.
        InputStream in = DriveConnection.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .build();
        return new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize("user");
    }


    /**
     * Returns the list of the files that are in the DriveCloud folder in Google Drive
     *
     * @return An arraylist of files
     */
    public ArrayList<String> getFileList() {
        ArrayList<String> fileL = new ArrayList<>();

        FileList result = null;
        try {
            result = service.files().list()
                    .setPageSize(10)
                    .setFields("nextPageToken, files(id, name)")
                    .execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
        List<File> files = result.getFiles();

        for (File file : files) {
            if (file.getName().equals("DriveCloud")) {
                return fileL;
            }
            //System.out.printf("- %s (%s)\n", file.getName(), file.getId());
            fileL.add(file.getName());

        }


        return fileL;
    }


    /**
     * Gets the list of all files on DriveCloud folder on Google Drive.
     *
     * @param service A new authorized API client service.
     * @param files   List of files in DriveCloud folder.
     * @return An arraylist of files
     * @throws IOException If the files cannot be found.
     */
    public static ArrayList<File> fileList(Drive service, List<File> files) throws java.io.IOException {
        ArrayList<File> CurrentFiles = new ArrayList<File>();
        System.out.printf("Files in the DriveCloud folder: \n");
        for (File file : files) {
            if (file.getName().equals("DriveCloud")) {
                return CurrentFiles;
            }
            System.out.printf("- %s (%s)\n", file.getName(), file.getId());
            CurrentFiles.add(file);

        }
        return CurrentFiles;
    }

    /**
     * Gets the ID of a file given a name.
     *
     * @param service  A new authorized API client service.
     * @param files    List of files in DriveCloud folder.
     * @param fileName A file name
     * @return A string of the given file's ID
     * @throws IOException If the files cannot be found.
     */
    public static String getIDFromFileName(Drive service, List<File> files, String fileName) throws java.io.IOException {
        String id = "";
        //System.out.printf("IDs in the DriveCloud folder: \n");
        for (File file : files) {
//            if(file.getName().equals("DriveCloud")){
//                return currentIDs;
//            }
            //System.out.printf("- %s (%s)\n", file.getName(), file.getId());
            if (file.getName().equals(fileName)) {
                id = file.getId();
            }

        }
        return id;
    }


    /**
     * Creates a file named DriveCloud on Google Drive
     *
     * @param service A new authorized API client service.
     * @throws IOException If the files cannot be found.
     */
    public static void createFile(Drive service) throws java.io.IOException {

        File fileMetadata = new File();
        fileMetadata.setName("DriveCloud");
        fileMetadata.setMimeType("application/vnd.google-apps.folder");

        File file = service.files().create(fileMetadata)
                .setFields("id")
                .execute();
        System.out.println("Folder ID: " + file.getId());
    }


    /**
     * Deletes the file from DriveCloud folder on Google Drive given a name.
     *
     * @param service  A new authorized API client service.
     * @param fileName A file name
     * @throws IOException If the files cannot be found.
     */
    public static void deleteFile(Drive service, String fileName) {
        try {
            FileList result = null;
            try {
                result = service.files().list()
                        .setPageSize(10)
                        .setFields("nextPageToken, files(id, name)")
                        .execute();
            } catch (IOException e) {
                e.printStackTrace();
            }
            List<File> files = result.getFiles();
            fileName = getIDFromFileName(service, files, fileName);
            service.files().delete(fileName).execute();
            service.files().emptyTrash();
        } catch (IOException e) {
            System.out.println("An error occurred: " + e);
        }
    }

    /**
     * Creates a folder named "DriveCloud" on Google Drive and returns its ID as a string.
     *
     * @param service A new authorized API client service.
     * @return A string of the given created folder's ID (DriveCloud folder)
     * @throws IOException If the files cannot be found.
     */
    public static String createFolder(Drive service) throws java.io.IOException {

        File fileMetadata = new File();
        fileMetadata.setName("DriveCloud");
        fileMetadata.setMimeType("application/vnd.google-apps.folder");

        File file = service.files().create(fileMetadata)
                .setFields("id")
                .execute();
        System.out.println("DriveCloud Folder ID: " + file.getId());
        return file.getId();
    }

    /**
     * Uploads a file to DriveCloud folder on Google Drive
     *
     * @param service        A new authorized API client service.
     * @param uploadFolderId List of files in DriveCloud folder.
     * @param name           A file name
     * @param fullPath       A file's full path
     * @throws IOException If the files cannot be found.
     */
    public static void uploadFile(Drive service, String uploadFolderId, String name, String fullPath) throws java.io.IOException {
        String folderId = uploadFolderId;
        File fileMetadata = new File();
        fileMetadata.setName(name);
        fileMetadata.setParents(Collections.singletonList(folderId));
        java.io.File filePath = new java.io.File(fullPath);
        FileContent mediaContent = new FileContent(getMimeTypeFromFileName(name), filePath);
        File file = service.files().create(fileMetadata, mediaContent)
                .setFields("id, parents")
                .execute();
        //System.out.println("File with ID uploaded to DriveCloud: " + file.getId());
    }

    /**
     * Sets a file's MimeType given its  name.
     * @param fileName A file's name
     * @return A string that includes MimeType.
     * @throws IOException If the files cannot be found.
     */
    public static String getMimeTypeFromFileName(String fileName) {
        String type = "";

        type = fileName.substring(fileName.lastIndexOf('.') + 1);
        //System.out.println("type: " + type);
        if (type.equals("txt")) {
            type = "text/plain";
        } else if (type.equals("jpg")) {
            type = "image/jpeg";
        } else if (type.equals("png")) {
            type = "image/png";
        } else if (type.equals("mp3")) {
            type = "audio/mpeg";
        } else if (type.equals("pdf")) {
            type = "application/pdf";
        } else if (type.equals("csv")) {
            type = "text/plain";
        } else if (type.equals("mkv")) {
            type = "video/mkv";
        } else {
            type = "image/jpeg";
        }

        return type;
    }


    /**
     * Downloads a file from DriveCloud folder on Google Drive to DriveCloud folder on Desktop.
     * @param service  A new authorized API client service.
     * @param fileName A file name
     * @param fullPath A file's full path
     * @throws IOException If the files cannot be found.
     */
    public static void downloadFile(Drive service, String fullPath, String fileName) throws IOException, GeneralSecurityException {
        FileList result = null;
        try {
            result = service.files().list()
                    .setPageSize(10)
                    .setFields("nextPageToken, files(id, name)")
                    .execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
        List<File> files = result.getFiles();
        String fileId = "";
        for (File file : files) {
            if (file.getName().equals(fileName)) {
                fileId = file.getId();
            }
        }

        if (fileId.equals("")) {
            System.out.println("File not found!");
        }
        OutputStream outputStream = new FileOutputStream(fullPath);
        try {
            Drive.Files.Get request = service.files().get(fileId);
            request.getMediaHttpDownloader().setProgressListener(new CustomProgressListener());
            request.executeMediaAndDownloadTo(outputStream);
        } catch (IOException e) {
            System.out.println("Error on Download!");
            e.printStackTrace();
        }
    }

}