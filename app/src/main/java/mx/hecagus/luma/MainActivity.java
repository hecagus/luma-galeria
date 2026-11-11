package mx.hecagus.luma;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.ClipData;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Insets;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.inputmethod.InputMethodManager;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public final class MainActivity extends Activity {
    private static final int BG=0xFF0E141C, SURFACE=0xFF19222D, ACCENT=0xFFC0F4D0,
        TEXT=0xFFF3F6F7, MUTED=0xFF93A4B7;
    private static final int PHOTO_PERMISSION=10, TRASH_REQUEST=20;
    private static final String SELECTED="android.permission.READ_MEDIA_VISUAL_USER_SELECTED";
    private final ExecutorService scan=Executors.newSingleThreadExecutor(), fullImage=Executors.newSingleThreadExecutor();
    private final List<MediaRepository.Photo> photos=new ArrayList<>(), visible=new ArrayList<>(), viewerPhotos=new ArrayList<>();
    private final List<Album> albums=new ArrayList<>();
    private SharedPreferences preferences;
    private Set<String> favorites;
    private ThumbnailLoader thumbnails;
    private Future<?> scanTask, imageTask;
    private int scanGeneration, imageGeneration, tab=0, viewerIndex, order=0, columns=3;
    private String albumKey, albumName;
    private FrameLayout root, content, viewer;
    private LinearLayout base, bottom;
    private TextView heading, subtitle, sectionTitle, sectionCount, accessBanner, viewerName, viewerCount, viewerFavorite;
    private View backButton;
    private EditText search;
    private GridView grid;
    private PhotoAdapter adapter;
    private ZoomImageView zoom;
    private ProgressBar photoProgress;
    private boolean destroyed=false, loading=false, viewerOpen=false;
    private Uri pendingTrash;

    private static final class Album {
        final String key, name; final MediaRepository.Photo cover; int count;
        Album(MediaRepository.Photo photo) { key=photo.albumKey;name=photo.album;cover=photo;count=1; }
    }
    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        preferences=getSharedPreferences("luma",MODE_PRIVATE);
        favorites=new HashSet<>(preferences.getStringSet("favorites",Collections.<String>emptySet()));
        order=preferences.getInt("order",0);columns=preferences.getInt("columns",3);
        thumbnails=new ThumbnailLoader(getContentResolver());
        getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE|View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        createUi();
        if (Build.VERSION.SDK_INT>=33) getOnBackInvokedDispatcher().registerOnBackInvokedCallback(0,this::navigateBack);
    }
    private void createUi() {
        root=new FrameLayout(this);root.setBackgroundColor(BG);setContentView(root);
        root.setOnApplyWindowInsetsListener((view,insets)->{
            if (Build.VERSION.SDK_INT>=30) {
                Insets bars=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());
                Insets ime=insets.getInsets(WindowInsets.Type.ime());root.setPadding(bars.left,bars.top,bars.right,Math.max(bars.bottom,ime.bottom));
            } else root.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());
            return insets;
        });
        base=new LinearLayout(this);base.setOrientation(LinearLayout.VERTICAL);base.setFocusableInTouchMode(true);root.addView(base,match());base.requestFocus();
        LinearLayout header=row();header.setGravity(Gravity.CENTER_VERTICAL);header.setPadding(dp(20),dp(15),dp(14),dp(12));
        backButton=iconButton("back","Volver a los álbumes",this::navigateBack);backButton.setVisibility(View.GONE);header.addView(backButton,lp(44,48));
        LinearLayout titles=vertical();heading=text("Galería Luma",29,TEXT,true);heading.setLetterSpacing(-.04f);heading.setSingleLine(true);heading.setEllipsize(TextUtils.TruncateAt.END);
        subtitle=text("Tu galería, sin ruido.",12,MUTED,false);titles.addView(heading);titles.addView(subtitle);header.addView(titles,new LinearLayout.LayoutParams(0,-2,1));
        View options=iconButton("more","Opciones de la galería",null);options.setOnClickListener(v->showOptions(v));header.addView(options,lp(48,48));base.addView(header);
        LinearLayout searchBar=row();searchBar.setGravity(Gravity.CENTER_VERTICAL);searchBar.setPadding(dp(14),0,dp(8),0);searchBar.setBackground(round(SURFACE,16));
        searchBar.addView(new IconView(this,"search",MUTED),lp(21,21));
        search=new EditText(this);search.setTextColor(TEXT);search.setHintTextColor(MUTED);search.setTextSize(14);search.setSingleLine(true);search.setHint("Buscar por nombre, álbum o fecha");search.setBackgroundColor(Color.TRANSPARENT);search.setPadding(dp(10),0,0,0);search.setContentDescription("Buscar fotos por nombre, álbum o fecha");
        searchBar.addView(search,new LinearLayout.LayoutParams(0,dp(52),1));searchBar.addView(iconButton("close","Borrar búsqueda",()->search.setText("")),lp(36,44));
        LinearLayout.LayoutParams searchLp=lp(-1,52);searchLp.setMargins(dp(20),0,dp(20),dp(12));base.addView(searchBar,searchLp);
        search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int c,int f){}public void onTextChanged(CharSequence s,int a,int b,int c){refresh();}public void afterTextChanged(Editable e){}});
        accessBanner=text("",12,ACCENT,false);accessBanner.setPadding(dp(20),dp(8),dp(20),dp(8));accessBanner.setBackgroundColor(0xFF183026);accessBanner.setOnClickListener(v->requestPhotos());base.addView(accessBanner,lp(-1,-2));
        LinearLayout section=row();section.setGravity(Gravity.CENTER_VERTICAL);section.setPadding(dp(22),dp(10),dp(22),dp(12));sectionTitle=text("Tus fotos",17,TEXT,true);sectionCount=text("",12,MUTED,false);
        section.addView(sectionTitle,new LinearLayout.LayoutParams(0,-2,1));section.addView(sectionCount);base.addView(section);
        content=new FrameLayout(this);base.addView(content,new LinearLayout.LayoutParams(-1,0,1));
        grid=new GridView(this);grid.setPadding(dp(16),0,dp(16),dp(12));grid.setClipToPadding(false);grid.setHorizontalSpacing(dp(5));grid.setVerticalSpacing(dp(5));grid.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);grid.setSelector(android.R.color.transparent);grid.setVerticalScrollBarEnabled(false);
        adapter=new PhotoAdapter();grid.setAdapter(adapter);grid.setOnItemClickListener((parent,view,position,id)->{
            hideKeyboard();
            if (isAlbumGrid()) { Album album=albums.get(position);albumKey=album.key;albumName=album.name;search.setText("");refresh();grid.setSelection(0); }
            else if (position<visible.size()) openViewer(position);
        });
        content.addView(grid,match());bottom=new LinearLayout(this);bottom.setOrientation(LinearLayout.HORIZONTAL);bottom.setPadding(dp(12),dp(8),dp(12),dp(7));bottom.setBackgroundColor(BG);base.addView(bottom);buildBottom();
        root.requestApplyInsets();refresh();
    }
    @Override public void onResume() { super.onResume();reload(); }
    private boolean hasFullAccess() {
        return checkSelfPermission(Build.VERSION.SDK_INT>=33?"android.permission.READ_MEDIA_IMAGES":Manifest.permission.READ_EXTERNAL_STORAGE)==PackageManager.PERMISSION_GRANTED;
    }
    private boolean hasAccess() { return hasFullAccess() || (Build.VERSION.SDK_INT>=34 && checkSelfPermission(SELECTED)==PackageManager.PERMISSION_GRANTED); }
    private void requestPhotos() {
        if (Build.VERSION.SDK_INT>=34) requestPermissions(new String[]{"android.permission.READ_MEDIA_IMAGES",SELECTED},PHOTO_PERMISSION);
        else if (Build.VERSION.SDK_INT>=33) requestPermissions(new String[]{"android.permission.READ_MEDIA_IMAGES"},PHOTO_PERMISSION);
        else requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},PHOTO_PERMISSION);
    }
    @Override public void onRequestPermissionsResult(int request,String[] permissions,int[] results) {
        super.onRequestPermissionsResult(request,permissions,results);if(request==PHOTO_PERMISSION)reload();
    }
    private void reload() {
        final int generation=++scanGeneration;if(scanTask!=null)scanTask.cancel(true);
        if(!hasAccess()) { loading=false;photos.clear();visible.clear();thumbnails.clear();if(viewerOpen)closeViewer();refresh();return; }
        loading=true;sectionCount.setText("Actualizando…");
        scanTask=scan.submit(()->{
            List<MediaRepository.Photo> found;
            try { found=MediaRepository.load(getContentResolver()); }
            catch (Exception e) { runOnUiThread(()->{if(!destroyed&&generation==scanGeneration){loading=false;photos.clear();thumbnails.clear();if(viewerOpen)closeViewer();refresh();toast("No se pudieron leer las fotos. Revisa el acceso en Opciones.");}});return; }
            runOnUiThread(()->{
                if(destroyed||generation!=scanGeneration)return;
                loading=false;photos.clear();photos.addAll(found);sortPhotos();refresh();
                if(viewerOpen){String current=viewerPhotos.get(viewerIndex).uri.toString();boolean allowed=false;for(MediaRepository.Photo photo:photos)if(photo.uri.toString().equals(current)){allowed=true;break;}if(!allowed)closeViewer();}
            });
        });
    }
    private void sortPhotos() {
        Collections.sort(photos,(a,b)->{
            int result=order==2?a.sortName.compareTo(b.sortName):order==1?Long.compare(a.date,b.date):Long.compare(b.date,a.date);
            return result!=0?result:a.uri.toString().compareTo(b.uri.toString());
        });
    }
    private boolean isAlbumGrid() { return tab==1&&albumKey==null; }
    private void refresh() {
        if(grid==null)return;
        visible.clear();albums.clear();String query=search.getText().toString();String[] words=GalleryLogic.normalize(query).split("\\s+");
        LinkedHashMap<String,Album> groups=new LinkedHashMap<>();
        for(MediaRepository.Photo photo:photos){
            if(tab==2&&!favorites.contains(photo.uri.toString()))continue;
            if(albumKey!=null&&!albumKey.equals(photo.albumKey))continue;
            boolean matches=true;for(String word:words)if(!photo.searchText.contains(word)){matches=false;break;}if(!matches)continue;
            visible.add(photo);
            Album album=groups.get(photo.albumKey);if(album==null)groups.put(photo.albumKey,new Album(photo));else album.count++;
        }
        albums.addAll(groups.values());
        heading.setText(albumKey==null?"Galería Luma":albumName);backButton.setVisibility(albumKey==null?View.GONE:View.VISIBLE);
        subtitle.setText(albumKey==null?"Tu galería, sin ruido.":"Álbum · En tu dispositivo");
        sectionTitle.setText(isAlbumGrid()?"Tus álbumes":tab==2?"Tus favoritos":albumKey!=null?"Fotos del álbum":"Tus fotos");
        sectionCount.setText(loading?"Actualizando…":isAlbumGrid()?albums.size()+" álbumes":visible.size()+" fotos");
        boolean partial=hasAccess()&&!hasFullAccess();accessBanner.setVisibility(partial?View.VISIBLE:View.GONE);
        accessBanner.setText("Acceso a fotos seleccionadas · Toca para elegir más");
        applyColumns();adapter.notifyDataSetChanged();
        for(int i=content.getChildCount()-1;i>0;i--)content.removeViewAt(i);
        int count=isAlbumGrid()?albums.size():visible.size();
        if(!hasAccess())showEmpty("photos","Tus fotos, sin ruido.","Luma necesita acceso para mostrar las fotos de tu teléfono. Puedes permitir todas o elegir solo algunas. Sin anuncios, cuentas ni conexión a Internet.","Elegir acceso a fotos",this::requestPhotos,true);
        else if(count==0&&!loading)showEmpty(tab==2?"heart":"photos",query.isEmpty()?(tab==2?"Aquí van tus favoritas.":"Todavía no hay fotos."):"No encontramos coincidencias.",query.isEmpty()?(tab==2?"Abre una foto y toca el corazón para guardarla aquí.":partial?"No hay fotos disponibles con el acceso actual. Puedes seleccionar más desde la franja superior.":"Las fotos guardadas por la cámara y otras apps aparecerán aquí."):"Prueba con parte del nombre, un álbum o un mes.",query.isEmpty()?"Actualizar":"Limpiar búsqueda",query.isEmpty()?this::reload:()->search.setText(""),false);
        else if(loading&&count==0){ProgressBar progress=new ProgressBar(this);content.addView(progress,new FrameLayout.LayoutParams(dp(40),dp(40),Gravity.CENTER));}
    }
    private void applyColumns() {
        int width=getResources().getConfiguration().screenWidthDp;
        grid.setNumColumns(isAlbumGrid()?(width>=700?3:2):(width>=700?columns+2:columns));
    }
    private void showEmpty(String icon,String title,String description,String action,Runnable callback,boolean settings) {
        LinearLayout box=vertical();box.setGravity(Gravity.CENTER);box.setPadding(dp(30),dp(20),dp(30),dp(24));
        IconView image=new IconView(this,icon,ACCENT);box.addView(image,lp(64,64));
        TextView name=text(title,25,TEXT,true);name.setGravity(Gravity.CENTER);LinearLayout.LayoutParams nameLp=lp(-1,-2);nameLp.topMargin=dp(22);box.addView(name,nameLp);
        TextView body=text(description,14,MUTED,false);body.setGravity(Gravity.CENTER);body.setLineSpacing(dp(4),1);LinearLayout.LayoutParams bodyLp=lp(-1,-2);bodyLp.setMargins(0,dp(12),0,dp(24));box.addView(body,bodyLp);
        box.addView(actionButton(action,callback,true),lp(-1,50));
        if(settings){TextView manage=actionButton("Abrir permisos de Android",this::openSettings,false);LinearLayout.LayoutParams p=lp(-1,48);p.topMargin=dp(9);box.addView(manage,p);}
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.addView(box,new ScrollView.LayoutParams(-1,-1));content.addView(scroll,match());
    }
    private void buildBottom() {
        bottom.removeAllViews();String[] labels={"Fotos","Álbumes","Favoritos"},icons={"photos","albums","heart"};
        for(int i=0;i<3;i++){
            final int chosen=i;LinearLayout item=vertical();item.setGravity(Gravity.CENTER);item.setPadding(0,dp(8),0,dp(7));
            item.setBackground(round(i==tab?0xFF233A30:BG,18));item.setContentDescription(labels[i]);item.setClickable(true);item.setFocusable(true);
            item.addView(new IconView(this,icons[i],i==tab?ACCENT:MUTED),lp(23,23));TextView label=text(labels[i],11,i==tab?ACCENT:MUTED,i==tab);label.setGravity(Gravity.CENTER);label.setPadding(0,dp(4),0,0);item.addView(label);
            LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(0,dp(66),1);params.setMargins(dp(4),0,dp(4),0);bottom.addView(item,params);
            item.setOnClickListener(v->{tab=chosen;albumKey=null;albumName=null;search.setText("");hideKeyboard();buildBottom();refresh();grid.setSelection(0);});
        }
    }
    private void showOptions(View anchor) {
        PopupMenu menu=new PopupMenu(this,anchor);
        menu.getMenu().add(0,1,0,"Actualizar fotos");
        android.view.SubMenu sort=menu.getMenu().addSubMenu("Ordenar fotos");
        sort.add(1,10,0,"Más recientes").setCheckable(true).setChecked(order==0);
        sort.add(1,11,1,"Más antiguas").setCheckable(true).setChecked(order==1);
        sort.add(1,12,2,"Nombre de archivo").setCheckable(true).setChecked(order==2);
        menu.getMenu().add(0,2,1,columns==3?"Cuadrícula de 4 columnas":"Cuadrícula de 3 columnas");
        menu.getMenu().add(0,3,2,"Cambiar acceso a fotos");menu.getMenu().add(0,4,3,"Permisos de Android");menu.getMenu().add(0,5,4,"Acerca de Galería Luma");
        menu.setOnMenuItemClickListener(item->{
            int id=item.getItemId();if(id==1)reload();else if(id==2){columns=columns==3?4:3;preferences.edit().putInt("columns",columns).apply();refresh();}
            else if(id==3)requestPhotos();else if(id==4)openSettings();else if(id==5)about();
            else if(id>=10&&id<=12){order=id-10;preferences.edit().putInt("order",order).apply();sortPhotos();refresh();grid.setSelection(0);}return true;
        });menu.show();
    }
    private void openSettings() { startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+getPackageName()))); }
    private void about() {
        new AlertDialog.Builder(this).setTitle("Galería Luma · 1.0.1").setMessage("Tu galería, sin ruido.\n\nCreada por HecAgus. Sin anuncios, suscripciones, cuentas ni rastreadores.\n\nNo tiene permiso de Internet. Las fotos se leen desde Android; Luma no las copia a una nube. Los favoritos y las preferencias se guardan solo en el dispositivo.\n\nCompartir abre el selector de Android para que tú elijas el destino. La papelera solicita confirmación al sistema.\n\nBúsqueda por nombre de archivo, álbum y fecha; no reconoce personas ni contenido de las imágenes.").setPositiveButton("Entendido",null).show();
    }
    private void openViewer(int index) {
        viewerPhotos.clear();viewerPhotos.addAll(visible);viewerIndex=index;viewerOpen=true;
        viewer=new FrameLayout(this);viewer.setBackgroundColor(Color.BLACK);root.addView(viewer,match());
        LinearLayout layout=vertical();viewer.addView(layout,match());
        LinearLayout top=row();top.setGravity(Gravity.CENTER_VERTICAL);top.setPadding(dp(6),dp(6),dp(12),dp(4));
        top.addView(iconButton("back","Cerrar foto",this::closeViewer),lp(48,48));
        viewerName=text("",13,TEXT,true);viewerName.setSingleLine(true);viewerName.setEllipsize(TextUtils.TruncateAt.MIDDLE);top.addView(viewerName,new LinearLayout.LayoutParams(0,-2,1));
        viewerCount=text("",11,MUTED,false);viewerCount.setPadding(dp(12),0,0,0);top.addView(viewerCount);layout.addView(top,lp(-1,58));
        FrameLayout imageFrame=new FrameLayout(this);layout.addView(imageFrame,new LinearLayout.LayoutParams(-1,0,1));
        zoom=new ZoomImageView(this,this::moveViewer);imageFrame.addView(zoom,match());
        photoProgress=new ProgressBar(this);imageFrame.addView(photoProgress,new FrameLayout.LayoutParams(dp(36),dp(36),Gravity.CENTER));
        LinearLayout navigation=row();navigation.setGravity(Gravity.CENTER);navigation.addView(actionButton("‹  Anterior",()->moveViewer(-1),false),new LinearLayout.LayoutParams(0,dp(44),1));
        navigation.addView(actionButton("Siguiente  ›",()->moveViewer(1),false),new LinearLayout.LayoutParams(0,dp(44),1));layout.addView(navigation);
        LinearLayout tools=row();tools.setPadding(dp(8),dp(5),dp(8),dp(8));
        tools.addView(viewerTool("share","Compartir",this::share),new LinearLayout.LayoutParams(0,dp(66),1));
        LinearLayout fav=viewerTool("heart","Favorito",this::toggleFavorite);viewerFavorite=(TextView)fav.getChildAt(1);tools.addView(fav,new LinearLayout.LayoutParams(0,dp(66),1));
        tools.addView(viewerTool("info","Detalles",this::details),new LinearLayout.LayoutParams(0,dp(66),1));
        if(Build.VERSION.SDK_INT>=30)tools.addView(viewerTool("trash","Papelera",this::trash),new LinearLayout.LayoutParams(0,dp(66),1));layout.addView(tools);
        showPhoto();
    }
    private LinearLayout viewerTool(String icon,String name,Runnable callback) {
        LinearLayout tool=vertical();tool.setGravity(Gravity.CENTER);tool.setContentDescription(name);tool.setFocusable(true);tool.setClickable(true);tool.setBackground(ripple(Color.BLACK,12));
        tool.addView(new IconView(this,icon,TEXT),lp(22,22));TextView label=text(name,10,MUTED,false);label.setGravity(Gravity.CENTER);label.setPadding(0,dp(6),0,0);tool.addView(label);tool.setOnClickListener(v->callback.run());return tool;
    }
    private MediaRepository.Photo currentPhoto() { return viewerPhotos.get(viewerIndex); }
    private void showPhoto() {
        if(!viewerOpen||viewerPhotos.isEmpty())return;
        final MediaRepository.Photo photo=currentPhoto();final int generation=++imageGeneration;
        viewerName.setText(photo.name);viewerCount.setText((viewerIndex+1)+" / "+viewerPhotos.size());updateFavoriteLabel();zoom.setImageDrawable(null);photoProgress.setVisibility(View.VISIBLE);
        if(imageTask!=null)imageTask.cancel(true);
        imageTask=fullImage.submit(()->{
            Bitmap bitmap;
            try {
                int target=Math.min(3200,Math.max(getResources().getDisplayMetrics().widthPixels,getResources().getDisplayMetrics().heightPixels)*2);
                bitmap=PhotoDecoder.decode(getContentResolver(),photo.uri,target);
            }catch(Exception|OutOfMemoryError e){runOnUiThread(()->{if(!destroyed&&viewerOpen&&generation==imageGeneration){photoProgress.setVisibility(View.GONE);toast("No se pudo abrir esta imagen. Puede que el archivo ya no esté disponible.");}});return;}
            runOnUiThread(()->{if(!destroyed&&viewerOpen&&generation==imageGeneration){photoProgress.setVisibility(View.GONE);zoom.setImageBitmap(bitmap);}else bitmap.recycle();});
        });
    }
    private void moveViewer(int delta) {
        if(!viewerOpen)return;int next=viewerIndex+delta;if(next<0||next>=viewerPhotos.size()){toast(delta<0?"Esta es la primera foto.":"Esta es la última foto.");return;}viewerIndex=next;showPhoto();
    }
    private void closeViewer() {
        if(!viewerOpen)return;viewerOpen=false;imageGeneration++;if(imageTask!=null)imageTask.cancel(true);zoom.setImageDrawable(null);root.removeView(viewer);viewer=null;viewerPhotos.clear();refresh();
    }
    private void updateFavoriteLabel() { if(viewerFavorite!=null&&viewerOpen)viewerFavorite.setText(favorites.contains(currentPhoto().uri.toString())?"Guardada":"Favorito"); }
    private void toggleFavorite() {
        if(!viewerOpen)return;String key=currentPhoto().uri.toString();boolean was=favorites.remove(key);if(!was)favorites.add(key);
        preferences.edit().putStringSet("favorites",new HashSet<>(favorites)).apply();updateFavoriteLabel();toast(was?"Se quitó de favoritos.":"Guardada en favoritos.");
    }
    private void share() {
        if(!viewerOpen)return;MediaRepository.Photo photo=currentPhoto();Intent send=new Intent(Intent.ACTION_SEND);send.setType(photo.mime);send.putExtra(Intent.EXTRA_STREAM,photo.uri);
        send.setClipData(ClipData.newRawUri(photo.name,photo.uri));send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try{startActivity(Intent.createChooser(send,"Compartir foto"));}catch(Exception e){toast("No hay una app disponible para compartir esta imagen.");}
    }
    private void details() {
        MediaRepository.Photo p=currentPhoto();new AlertDialog.Builder(this).setTitle("Detalles de la foto").setMessage(p.name+"\n\n"+p.dateLabel+"\nÁlbum: "+p.album+"\nCarpeta: "+(p.path.isEmpty()?"No disponible":p.path)+"\nDimensiones: "+p.width+" × "+p.height+" px\nTamaño: "+GalleryLogic.fileSize(p.size)+"\nFormato: "+p.mime).setPositiveButton("Cerrar",null).show();
    }
    private void trash() {
        if(!viewerOpen||Build.VERSION.SDK_INT<30)return;pendingTrash=currentPhoto().uri;
        try{PendingIntent request=MediaStore.createTrashRequest(getContentResolver(),Collections.singletonList(pendingTrash),true);startIntentSenderForResult(request.getIntentSender(),TRASH_REQUEST,null,0,0,0);}
        catch(Exception e){pendingTrash=null;toast("Android no permite enviar este archivo a la papelera.");}
    }
    @Override protected void onActivityResult(int request,int result,Intent data) {
        super.onActivityResult(request,result,data);
        if(request==TRASH_REQUEST){if(result==RESULT_OK){if(viewerOpen)closeViewer();thumbnails.clear();toast("Foto enviada a la papelera de Android.");reload();}pendingTrash=null;}
    }
    private void navigateBack() {
        if(viewerOpen){closeViewer();return;}if(albumKey!=null){albumKey=null;albumName=null;search.setText("");refresh();return;}if(search.length()>0){search.setText("");hideKeyboard();return;}finish();
    }
    @Override public void onBackPressed() { navigateBack(); }
    @Override public void onConfigurationChanged(Configuration config) { super.onConfigurationChanged(config);applyColumns();adapter.notifyDataSetChanged(); }
    @Override protected void onDestroy() { destroyed=true;scanGeneration++;imageGeneration++;scan.shutdownNow();fullImage.shutdownNow();thumbnails.close();super.onDestroy(); }
    private final class PhotoAdapter extends BaseAdapter {
        public int getCount(){return isAlbumGrid()?albums.size():visible.size();}
        public Object getItem(int position){return isAlbumGrid()?albums.get(position):visible.get(position);}
        public long getItemId(int position){return position;}
        @Override public View getView(int position,View recycled,ViewGroup parent){
            boolean albumMode=isAlbumGrid();MediaRepository.Photo photo=albumMode?albums.get(position).cover:visible.get(position);
            int available=grid.getWidth()>0?grid.getWidth():getResources().getDisplayMetrics().widthPixels;
            int side=Math.max(dp(48),(available-dp(32)-(grid.getNumColumns()-1)*dp(5))/grid.getNumColumns());
            Cell cell=recycled instanceof Cell?(Cell)recycled:new Cell();cell.setLayoutParams(new android.widget.AbsListView.LayoutParams(-1,side+(albumMode?dp(60):0)));
            cell.setBackground(round(SURFACE,albumMode?14:7));cell.setClipToOutline(true);cell.image.setLayoutParams(new FrameLayout.LayoutParams(-1,side,Gravity.TOP));thumbnails.load(cell.image,photo);
            cell.footer.setVisibility(albumMode?View.VISIBLE:View.GONE);cell.heart.setVisibility(!albumMode&&favorites.contains(photo.uri.toString())?View.VISIBLE:View.GONE);
            if(albumMode){Album album=albums.get(position);cell.title.setText(album.name);cell.count.setText(album.count+" fotos");cell.setContentDescription("Álbum "+album.name+", "+album.count+" fotos");}
            else cell.setContentDescription(photo.name+", "+photo.dateLabel+(favorites.contains(photo.uri.toString())?", favorita":""));
            return cell;
        }
    }
    private final class Cell extends FrameLayout {
        final ImageView image;final LinearLayout footer;final TextView title,count;final FrameLayout heart;
        Cell(){super(MainActivity.this);image=new ImageView(MainActivity.this);image.setScaleType(ImageView.ScaleType.CENTER_CROP);image.setBackgroundColor(SURFACE);addView(image);
            footer=vertical();footer.setPadding(dp(10),dp(8),dp(10),dp(8));title=text("",13,TEXT,true);title.setSingleLine(true);title.setEllipsize(TextUtils.TruncateAt.END);count=text("",11,MUTED,false);count.setPadding(0,dp(3),0,0);footer.addView(title);footer.addView(count);addView(footer,new FrameLayout.LayoutParams(-1,dp(60),Gravity.BOTTOM));
            heart=new FrameLayout(MainActivity.this);heart.setPadding(dp(5),dp(5),dp(5),dp(5));heart.setBackground(round(0xCC101923,20));heart.addView(new IconView(MainActivity.this,"heart",ACCENT),match());FrameLayout.LayoutParams h=new FrameLayout.LayoutParams(dp(27),dp(27),Gravity.TOP|Gravity.RIGHT);h.setMargins(0,dp(6),dp(6),0);addView(heart,h);
        }
    }
    private TextView text(String value,float size,int color,boolean bold){TextView v=new TextView(this);v.setText(value);v.setTextSize(size);v.setTextColor(color);v.setFontFeatureSettings("kern");v.setTypeface(Typeface.create(bold?"sans-serif-medium":"sans-serif",Typeface.NORMAL));return v;}
    private TextView actionButton(String value,Runnable callback,boolean primary){TextView v=text(value,14,primary?BG:ACCENT,true);v.setGravity(Gravity.CENTER);v.setMinHeight(dp(48));v.setPadding(dp(12),dp(10),dp(12),dp(10));v.setBackground(ripple(primary?ACCENT:Color.TRANSPARENT,14));v.setFocusable(true);v.setOnClickListener(w->callback.run());return v;}
    private View iconButton(String icon,String description,Runnable callback){FrameLayout frame=new FrameLayout(this);frame.setBackground(ripple(Color.TRANSPARENT,24));frame.setContentDescription(description);frame.setFocusable(true);frame.setClickable(true);frame.addView(new IconView(this,icon,TEXT),new FrameLayout.LayoutParams(dp(23),dp(23),Gravity.CENTER));if(callback!=null)frame.setOnClickListener(v->callback.run());return frame;}
    private GradientDrawable round(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
    private RippleDrawable ripple(int color,int radius){return new RippleDrawable(ColorStateList.valueOf(0x33FFFFFF),round(color,radius),round(Color.WHITE,radius));}
    private LinearLayout row(){LinearLayout view=new LinearLayout(this);view.setOrientation(LinearLayout.HORIZONTAL);return view;}
    private LinearLayout vertical(){LinearLayout view=new LinearLayout(this);view.setOrientation(LinearLayout.VERTICAL);return view;}
    private LinearLayout.LayoutParams lp(int w,int h){return new LinearLayout.LayoutParams(w<0?w:dp(w),h<0?h:dp(h));}
    private FrameLayout.LayoutParams match(){return new FrameLayout.LayoutParams(-1,-1);}
    private int dp(float value){return Math.round(value*getResources().getDisplayMetrics().density);}
    private void toast(String value){Toast.makeText(this,value,Toast.LENGTH_SHORT).show();}
    private void hideKeyboard(){search.clearFocus();((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(search.getWindowToken(),0);}
}
