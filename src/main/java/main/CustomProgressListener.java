package main;

import java.io.IOException;
import com.google.api.client.googleapis.media.MediaHttpDownloader;
import com.google.api.client.googleapis.media.MediaHttpDownloaderProgressListener;



public class CustomProgressListener implements MediaHttpDownloaderProgressListener {
    /**
     * Listens the progress while downloading from Google Drive.
     * @param downloader A new authorized API client downloader.
     */
    public void progressChanged(MediaHttpDownloader downloader) {

        switch (downloader.getDownloadState()) {
            case MEDIA_IN_PROGRESS:
                System.out.println(downloader.getProgress());
                break;
            case MEDIA_COMPLETE:
                System.out.println("Download is complete!");
        }
    }
}