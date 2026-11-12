package mx.hecagus.luma;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.UriPermission;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.provider.DocumentsContract.Document;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class MediaRepository {
    public static final class Photo {
        public final Uri uri;
        public final String name, album, albumKey, path, mime, dateLabel, searchText, sortName;
        public final long date, size, duration;
        public final int width, height;
        Photo(Uri uri, String name, String album, String albumKey, String path, String mime,
              long date, long size, int width, int height, String dateLabel) {
            this(uri, name, album, albumKey, path, mime, date, size, width, height, dateLabel, -1);
        }
        Photo(Uri uri, String name, String album, String albumKey, String path, String mime,
              long date, long size, int width, int height, String dateLabel, long duration) {
            this.uri=uri; this.name=name; this.album=album; this.albumKey=albumKey;
            this.duration=duration;
            this.path=path; this.mime=mime; this.date=date; this.size=size;
            this.width=width; this.height=height; this.dateLabel=dateLabel;
            this.searchText=GalleryLogic.normalize(name+" "+album+" "+dateLabel);this.sortName=GalleryLogic.normalize(name);
        }
        boolean isVideo() { return duration >= 0; }
    }
    public static List<Photo> load(ContentResolver resolver) {
        return load(resolver, false);
    }

    public static List<Photo> loadVideos(ContentResolver resolver) {
        return load(resolver, true);
    }

    static List<Photo> withSelectedVideos(Context context, List<Photo> indexedVideos) {
        ContentResolver resolver=context.getContentResolver();
        List<Photo> videos=new ArrayList<>(indexedVideos);
        Set<Uri> known=new HashSet<>();
        for(Photo video:indexedVideos){
            known.add(video.uri);
            known.add(ContentUris.withAppendedId(collection(MediaStore.VOLUME_EXTERNAL,true),ContentUris.parseId(video.uri)));
        }
        SimpleDateFormat dates=new SimpleDateFormat("d 'de' MMMM 'de' yyyy",new Locale("es","MX"));
        String[] columns={Document.COLUMN_DISPLAY_NAME,Document.COLUMN_MIME_TYPE,Document.COLUMN_SIZE,Document.COLUMN_LAST_MODIFIED};
        for(UriPermission grant:resolver.getPersistedUriPermissions()){
            if(Thread.currentThread().isInterrupted())break;
            if(!grant.isReadPermission())continue;
            Uri uri=grant.getUri();
            if(known.contains(uri))continue;
            Uri indexedUri=indexedVideoUri(context,uri);
            if(indexedUri!=null&&known.contains(indexedUri))continue;
            try(Cursor cursor=resolver.query(uri,columns,null,null,null)){
                if(cursor==null||!cursor.moveToFirst())continue;
                String mime=string(cursor,cursor.getColumnIndex(Document.COLUMN_MIME_TYPE),"video/*");
                if(!mime.startsWith("video/"))continue;
                long date=number(cursor,Document.COLUMN_LAST_MODIFIED);
                String name=string(cursor,cursor.getColumnIndex(Document.COLUMN_DISPLAY_NAME),"Video");
                videos.add(new Photo(uri,name,"Archivos","selected","",mime,date,number(cursor,Document.COLUMN_SIZE),
                    0,0,date>0?dates.format(new Date(date)):"Fecha desconocida",0));
                if(indexedUri!=null)known.add(indexedUri);
            } catch(RuntimeException unavailable) {
                // A selected file can be deleted or its provider can become unavailable.
            }
        }
        return videos;
    }

    private static Uri indexedVideoUri(Context context, Uri documentUri) {
        if(Build.VERSION.SDK_INT<31)return null;
        try {
            Uri mediaUri=MediaStore.getMediaUri(context,documentUri);
            if(mediaUri!=null&&mediaUri.getPathSegments().contains("file")){
                return ContentUris.withAppendedId(collection(mediaUri.getPathSegments().get(0),true),ContentUris.parseId(mediaUri));
            }
            return mediaUri;
        } catch(RuntimeException unavailable) {
            return null;
        }
    }

    private static List<Photo> load(ContentResolver resolver, boolean video) {
        ArrayList<Photo> photos = new ArrayList<>();
        String[] columns = {"_id","_display_name","bucket_id","bucket_display_name","relative_path",
            "mime_type","datetaken","date_added","_size","width","height","volume_name"};
        if (video) {
            columns=java.util.Arrays.copyOf(columns,columns.length+1);
            columns[columns.length-1]=MediaStore.Video.Media.DURATION;
        }
        String selection="is_pending = 0" + (Build.VERSION.SDK_INT>=30 ? " AND is_trashed = 0" : "");
        SimpleDateFormat dates = new SimpleDateFormat("d 'de' MMMM 'de' yyyy", new Locale("es","MX"));
        try (Cursor cursor=resolver.query(collection("external",video),columns,
                selection,null,"date_added DESC, _id DESC")) {
            if (cursor == null) return photos;
            while (cursor.moveToNext()) {
                if (Thread.currentThread().isInterrupted()) break;
                long id=cursor.getLong(0), date=GalleryLogic.timestamp(cursor.getLong(6),cursor.getLong(7));
                String volume=string(cursor,11,"external_primary"), album=string(cursor,3,"Sin álbum");
                Uri uri=ContentUris.withAppendedId(collection(volume,video),id);
                photos.add(new Photo(uri,string(cursor,1,video?"Video":"Foto"),album,volume+":"+string(cursor,2,album),
                    string(cursor,4,""),string(cursor,5,video?"video/*":"image/*"),date,cursor.getLong(8),
                    cursor.getInt(9),cursor.getInt(10),date>0?dates.format(new Date(date)):"Fecha desconocida",
                    video?Math.max(0,cursor.getLong(12)):-1));
            }
        }
        return photos;
    }
    private static Uri collection(String volume, boolean video) {
        return video ? MediaStore.Video.Media.getContentUri(volume) : MediaStore.Images.Media.getContentUri(volume);
    }

    private static String string(Cursor cursor,int column,String fallback) {
        String value=column<0||cursor.isNull(column)?null:cursor.getString(column);
        return value==null||value.isEmpty()?fallback:value;
    }

    private static long number(Cursor cursor,String name) {
        int column=cursor.getColumnIndex(name);
        return column<0||cursor.isNull(column)?0:cursor.getLong(column);
    }
}
