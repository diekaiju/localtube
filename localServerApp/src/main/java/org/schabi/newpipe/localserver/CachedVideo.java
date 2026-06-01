package org.schabi.newpipe.localserver;

public class CachedVideo {
    private int id;
    private String title;
    private String url;
    private String uploader;
    private String thumbnailLocalPath;
    private String videoLocalPath;
    private String description;
    private String status;
    private int progress;
    private long timestamp;

    public CachedVideo(int id, String title, String url, String uploader, String thumbnailLocalPath, String videoLocalPath, String description, String status, int progress, long timestamp) {
        this.id = id;
        this.title = title;
        this.url = url;
        this.uploader = uploader;
        this.thumbnailLocalPath = thumbnailLocalPath;
        this.videoLocalPath = videoLocalPath;
        this.description = description;
        this.status = status;
        this.progress = progress;
        this.timestamp = timestamp;
    }

    public int getId() { return id; }
    public String getTitle() { return title; }
    public String getUrl() { return url; }
    public String getUploader() { return uploader; }
    public String getThumbnailLocalPath() { return thumbnailLocalPath; }
    public String getVideoLocalPath() { return videoLocalPath; }
    public String getDescription() { return description; }
    public String getStatus() { return status; }
    public int getProgress() { return progress; }
    public long getTimestamp() { return timestamp; }
}
