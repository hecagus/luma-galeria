package mx.hecagus.luma;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;
import android.util.Size;
import android.widget.ImageView;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public final class ThumbnailLoader {
    private final ContentResolver resolver;
    private final Handler main=new Handler(Looper.getMainLooper());
    private final ThreadPoolExecutor executor=new ThreadPoolExecutor(3,3,10,TimeUnit.SECONDS,
        new ArrayBlockingQueue<Runnable>(96),new ThreadPoolExecutor.DiscardOldestPolicy());
    private final LruCache<String,Bitmap> cache;
    private volatile boolean closed;
    public ThumbnailLoader(ContentResolver resolver) {
        this.resolver=resolver;
        int bytes=(int)Math.min(32L*1024*1024,Runtime.getRuntime().maxMemory()/10);
        cache=new LruCache<String,Bitmap>(bytes) {
            @Override protected int sizeOf(String key,Bitmap bitmap) { return bitmap.getAllocationByteCount(); }
        };
    }
    public void load(ImageView view,MediaRepository.Photo photo) {
        String key=photo.uri.toString(); view.setTag(key); view.setImageDrawable(null);
        Bitmap bitmap=cache.get(key);
        if (bitmap!=null) { view.setImageBitmap(bitmap); return; }
        if (closed) return;
        executor.execute(()->{
            Bitmap found;
            try { found=resolver.loadThumbnail(photo.uri,new Size(420,420),null); }
            catch (Exception e) { found=null; }
            catch (OutOfMemoryError e) { return; }
            if(found==null){try{found=PhotoDecoder.decode(resolver,photo.uri,420);}catch(Exception|OutOfMemoryError e){return;}}
            if (closed) return;
            cache.put(key,found);
            final Bitmap ready=found;
            main.post(()->{ if (!closed && key.equals(view.getTag())) view.setImageBitmap(ready); });
        });
    }
    public void clear() { cache.evictAll(); executor.getQueue().clear(); }
    public void close() { closed=true; executor.shutdownNow(); cache.evictAll(); main.removeCallbacksAndMessages(null); }
}
