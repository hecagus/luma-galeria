package mx.hecagus.luma;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class MediaRepository {
    public static final class Photo {
        public final Uri uri;
        public final String name, album, albumKey, path, mime, dateLabel, searchText, sortName;
        public final long date, size;
        public final int width, height;
        Photo(Uri uri, String name, String album, String albumKey, String path, String mime,
              long date, long size, int width, int height, String dateLabel) {
            this.uri=uri; this.name=name; this.album=album; this.albumKey=albumKey;
            this.path=path; this.mime=mime; this.date=date; this.size=size;
            this.width=width; this.height=height; this.dateLabel=dateLabel;
            this.searchText=GalleryLogic.normalize(name+" "+album+" "+dateLabel);this.sortName=GalleryLogic.normalize(name);
        }
    }
    public static List<Photo> load(ContentResolver resolver) {
        ArrayList<Photo> photos = new ArrayList<>();
        String[] columns = {"_id","_display_name","bucket_id","bucket_display_name","relative_path",
            "mime_type","datetaken","date_added","_size","width","height","volume_name"};
        String selection="is_pending = 0" + (Build.VERSION.SDK_INT>=30 ? " AND is_trashed = 0" : "");
        SimpleDateFormat dates = new SimpleDateFormat("d 'de' MMMM 'de' yyyy", new Locale("es","MX"));
        try (Cursor cursor=resolver.query(MediaStore.Images.Media.getContentUri("external"),columns,
                selection,null,"date_added DESC, _id DESC")) {
            if (cursor == null) return photos;
            while (cursor.moveToNext()) {
                if (Thread.currentThread().isInterrupted()) break;
                long id=cursor.getLong(0), date=GalleryLogic.timestamp(cursor.getLong(6),cursor.getLong(7));
                String volume=string(cursor,11,"external_primary"), album=string(cursor,3,"Sin álbum");
                Uri uri=ContentUris.withAppendedId(MediaStore.Images.Media.getContentUri(volume),id);
                photos.add(new Photo(uri,string(cursor,1,"Foto"),album,volume+":"+string(cursor,2,album),
                    string(cursor,4,""),string(cursor,5,"image/*"),date,cursor.getLong(8),
                    cursor.getInt(9),cursor.getInt(10),date>0?dates.format(new Date(date)):"Fecha desconocida"));
            }
        }
        return photos;
    }
    private static String string(Cursor cursor,int column,String fallback) {
        String value=cursor.isNull(column)?null:cursor.getString(column);
        return value==null||value.isEmpty()?fallback:value;
    }
}
