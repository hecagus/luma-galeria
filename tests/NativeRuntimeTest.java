package mx.hecagus.luma;

import static org.junit.Assert.*;
import android.Manifest;
import android.app.Activity;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.ProviderInfo;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import org.robolectric.shadows.ShadowContentResolver;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=35,manifest=Config.NONE,qualifiers="w360dp-h740dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class NativeRuntimeTest {
    private ActivityController<MainActivity> controller;
    private MainActivity activity;
    @After public void tearDown(){if(controller!=null)controller.pause().stop().destroy();}
    private void start(boolean allow)throws Exception{
        RuntimeEnvironment.getApplication().getSharedPreferences("luma",0).edit().clear().commit();
        Photos provider=new Photos();ProviderInfo info=new ProviderInfo();info.authority="media";
        provider.attachInfo(RuntimeEnvironment.getApplication(),info);ShadowContentResolver.registerProviderInternal("media",provider);
        if(allow)Shadows.shadowOf(RuntimeEnvironment.getApplication()).grantPermissions("android.permission.READ_MEDIA_IMAGES");
        controller=Robolectric.buildActivity(MainActivity.class);activity=controller.create().start().resume().visible().get();
        for(int i=0;i<100;i++){Shadows.shadowOf(Looper.getMainLooper()).idle();if(!(Boolean)get("loading"))break;Thread.sleep(20);}
        Shadows.shadowOf(Looper.getMainLooper()).idle();
    }
    private Object get(String name)throws Exception{Field f=MainActivity.class.getDeclaredField(name);f.setAccessible(true);return f.get(activity);}
    private Object call(String name,Class<?>[] types,Object... args)throws Exception{Method m=MainActivity.class.getDeclaredMethod(name,types);m.setAccessible(true);return m.invoke(activity,args);}
    private Object call(String name)throws Exception{return call(name,new Class<?>[]{});}
    private boolean hasText(View v,String value){if(v instanceof TextView&&((TextView)v).getText().toString().contains(value))return true;if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++)if(hasText(((ViewGroup)v).getChildAt(i),value))return true;return false;}
    @Test public void startsWithoutPermissionsAndExplainsAccess()throws Exception{
        start(false);assertTrue(hasText((View)get("root"),"Elegir acceso a fotos"));assertFalse((Boolean)call("hasAccess"));assertEquals(0,((List<?>)get("photos")).size());
        capture("permission");
    }
    @Test public void readsThreePhotosAndFiltersByAccentInsensitiveAlbum()throws Exception{
        start(true);assertEquals(3,((List<?>)get("photos")).size());
        ((EditText)get("search")).setText("camara");assertEquals(2,((GridView)get("grid")).getCount());
        ((EditText)get("search")).setText("captura");assertEquals(1,((GridView)get("grid")).getCount());
        ((EditText)get("search")).setText("no-existe");assertEquals(0,((GridView)get("grid")).getCount());assertTrue(hasText((View)get("root"),"No encontramos coincidencias"));
        ((EditText)get("search")).setText("");capture("photos");
    }
    @Test public void opensAlbumAndReturnsToAlbumList()throws Exception{
        start(true);((LinearLayout)get("bottom")).getChildAt(1).performClick();GridView grid=(GridView)get("grid");assertEquals(2,grid.getCount());
        grid.performItemClick(null,0,0);assertEquals(2,grid.getCount());assertEquals("Cámara",((TextView)get("heading")).getText().toString());
        call("navigateBack");assertEquals(2,grid.getCount());assertEquals("Luma",((TextView)get("heading")).getText().toString());capture("albums");
    }
    @Test public void favoritesPersistAndShareGrantsReadAccessToCurrentPhoto()throws Exception{
        start(true);call("openViewer",new Class<?>[]{int.class},0);call("toggleFavorite");
        assertEquals(1,activity.getSharedPreferences("luma",0).getStringSet("favorites",java.util.Collections.<String>emptySet()).size());
        call("share");Intent chooser=Shadows.shadowOf(activity).getNextStartedActivity();assertEquals(Intent.ACTION_CHOOSER,chooser.getAction());
        Intent send=chooser.getParcelableExtra(Intent.EXTRA_INTENT);assertEquals(Intent.ACTION_SEND,send.getAction());assertNotNull(send.getParcelableExtra(Intent.EXTRA_STREAM));assertTrue((send.getFlags()&Intent.FLAG_GRANT_READ_URI_PERMISSION)!=0);
        call("closeViewer");((LinearLayout)get("bottom")).getChildAt(2).performClick();assertEquals(1,((GridView)get("grid")).getCount());
    }
    @Test public void recognizesSelectedPhotosPermissionWithoutFullPermission()throws Exception{
        start(false);Shadows.shadowOf(RuntimeEnvironment.getApplication()).grantPermissions("android.permission.READ_MEDIA_VISUAL_USER_SELECTED");
        assertTrue((Boolean)call("hasAccess"));assertFalse((Boolean)call("hasFullAccess"));call("reload");
        for(int i=0;i<100;i++){Shadows.shadowOf(Looper.getMainLooper()).idle();if(!(Boolean)get("loading"))break;Thread.sleep(20);}
        assertEquals(View.VISIBLE,((TextView)get("accessBanner")).getVisibility());
    }
    @Test public void revokingAccessClearsPhotosAndClosesViewer()throws Exception{
        start(true);call("openViewer",new Class<?>[]{int.class},0);Shadows.shadowOf(RuntimeEnvironment.getApplication()).denyPermissions("android.permission.READ_MEDIA_IMAGES");call("reload");
        assertFalse((Boolean)get("viewerOpen"));assertEquals(0,((List<?>)get("photos")).size());assertTrue(hasText((View)get("root"),"Elegir acceso a fotos"));
    }
    @Test @Config(sdk={29,36}) public void launchesAtMinimumAndTargetSdk()throws Exception{
        start(false);assertTrue(hasText((View)get("root"),"Elegir acceso a fotos"));assertFalse((Boolean)get("viewerOpen"));
    }
    @Test public void viewerDecodesPhotoAndClampsNavigationAtEnds()throws Exception{
        start(true);
        call("openViewer",new Class<?>[]{int.class},0);
        for(int i=0;i<100;i++){Shadows.shadowOf(Looper.getMainLooper()).idle();if(((ZoomImageView)get("zoom")).getDrawable()!=null)break;Thread.sleep(20);}
        assertNotNull(((ZoomImageView)get("zoom")).getDrawable());call("moveViewer",new Class<?>[]{int.class},-1);assertEquals(0,(int)get("viewerIndex"));
        call("moveViewer",new Class<?>[]{int.class},1);assertEquals(1,(int)get("viewerIndex"));call("closeViewer");assertFalse((Boolean)get("viewerOpen"));
    }
    private void capture(String name)throws Exception{
        View view=(View)get("root");view.measure(View.MeasureSpec.makeMeasureSpec(1080,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(2220,View.MeasureSpec.EXACTLY));view.layout(0,0,1080,2220);
        Thread.sleep(250);Shadows.shadowOf(Looper.getMainLooper()).idle();
        Bitmap bitmap=Bitmap.createBitmap(1080,2220,Bitmap.Config.ARGB_8888);view.draw(new Canvas(bitmap));File folder=new File("build/previews");folder.mkdirs();try(FileOutputStream out=new FileOutputStream(new File(folder,name+".png"))){bitmap.compress(Bitmap.CompressFormat.PNG,100,out);}bitmap.recycle();
    }
    public static final class Photos extends ContentProvider{
        public boolean onCreate(){return true;}
        public Cursor query(Uri uri,String[] projection,String selection,String[] args,String sort){
            MatrixCursor c=new MatrixCursor(projection);c.addRow(new Object[]{1L,"Bosque.jpg","1","Cámara","DCIM/Camera/","image/jpeg",1750000000000L,1750000000L,2000000L,1200,800,"external_primary"});
            c.addRow(new Object[]{2L,"Mar.jpg","1","Cámara","DCIM/Camera/","image/jpeg",1740000000000L,1740000000L,1500000L,1200,800,"external_primary"});
            c.addRow(new Object[]{3L,"Captura.png","2","Capturas","Pictures/Screenshots/","image/png",1730000000000L,1730000000L,500000L,1080,1920,"external_primary"});return c;
        }
        public String getType(Uri uri){return "image/jpeg";}
        @Override public ParcelFileDescriptor openFile(Uri uri,String mode)throws FileNotFoundException{
            String id=uri.getLastPathSegment();File file=new File("tests/fixtures/"+id+".png");return ParcelFileDescriptor.open(file,ParcelFileDescriptor.MODE_READ_ONLY);
        }
        public Uri insert(Uri uri,ContentValues values){return null;}
        public int delete(Uri uri,String selection,String[] args){return 0;}
        public int update(Uri uri,ContentValues values,String selection,String[] args){return 0;}
    }
}
