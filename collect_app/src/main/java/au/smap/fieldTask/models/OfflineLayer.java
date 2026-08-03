package au.smap.fieldTask.models;

/**
 * An offline map layer (mbtiles) that the server has assigned to this user.
 * The file is downloaded by OfflineLayerDownloader.
 */
public class OfflineLayer {
    public int id;
    public String name;
    public String fileName;
    public long size;
    public String md5;
    public int version;
    public String url;
}
