package mx.hecagus.luma;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.widget.ImageView;

public final class ZoomImageView extends ImageView {
    public interface Navigation { void move(int delta); }
    private final Matrix transform=new Matrix();
    private final ScaleGestureDetector pinch;
    private final GestureDetector gestures;
    private float base=1,scale=1;
    public ZoomImageView(Context context,Navigation navigation) {
        super(context);setScaleType(ScaleType.MATRIX);
        setContentDescription("Foto. Pellizca para ampliar; doble toque para zoom; desliza para cambiar de foto.");
        pinch=new ScaleGestureDetector(context,new ScaleGestureDetector.SimpleOnScaleGestureListener(){
            @Override public boolean onScale(ScaleGestureDetector detector) {
                float next=Math.max(base,Math.min(base*6,scale*detector.getScaleFactor()));
                transform.postScale(next/scale,next/scale,detector.getFocusX(),detector.getFocusY());scale=next;bound();return true;
            }
        });
        gestures=new GestureDetector(context,new GestureDetector.SimpleOnGestureListener(){
            @Override public boolean onDown(MotionEvent e) { return true; }
            @Override public boolean onDoubleTap(MotionEvent e) {
                float next=scale>base*1.1f?base:base*2.5f;
                transform.postScale(next/scale,next/scale,e.getX(),e.getY());scale=next;bound();return true;
            }
            @Override public boolean onScroll(MotionEvent a,MotionEvent b,float dx,float dy) {
                if (!pinch.isInProgress() && scale>base*1.05f) { transform.postTranslate(-dx,-dy);bound(); }
                return true;
            }
            @Override public boolean onFling(MotionEvent a,MotionEvent b,float vx,float vy) {
                if (a!=null && b!=null && scale<=base*1.05f && Math.abs(b.getX()-a.getX())>70 && Math.abs(vx)>Math.abs(vy)) {
                    navigation.move(vx<0?1:-1);return true;
                }
                return false;
            }
        });
    }
    @Override public void setImageDrawable(Drawable drawable) { super.setImageDrawable(drawable);post(this::fit); }
    @Override protected void onSizeChanged(int w,int h,int oldW,int oldH) { super.onSizeChanged(w,h,oldW,oldH);fit(); }
    public void fit() {
        Drawable d=getDrawable();if(d==null||getWidth()==0||getHeight()==0||d.getIntrinsicWidth()<=0||d.getIntrinsicHeight()<=0)return;
        base=Math.min((float)getWidth()/d.getIntrinsicWidth(),(float)getHeight()/d.getIntrinsicHeight());scale=base;
        transform.reset();transform.postScale(base,base);
        transform.postTranslate((getWidth()-d.getIntrinsicWidth()*base)/2,(getHeight()-d.getIntrinsicHeight()*base)/2);setImageMatrix(transform);
    }
    private void bound() {
        Drawable d=getDrawable();if(d==null)return;
        RectF rect=new RectF(0,0,d.getIntrinsicWidth(),d.getIntrinsicHeight());transform.mapRect(rect);
        float dx=rect.width()<getWidth()?(getWidth()-rect.width())/2-rect.left:rect.left>0?-rect.left:rect.right<getWidth()?getWidth()-rect.right:0;
        float dy=rect.height()<getHeight()?(getHeight()-rect.height())/2-rect.top:rect.top>0?-rect.top:rect.bottom<getHeight()?getHeight()-rect.bottom:0;
        transform.postTranslate(dx,dy);setImageMatrix(transform);
    }
    @Override public boolean onTouchEvent(MotionEvent event) { pinch.onTouchEvent(event);gestures.onTouchEvent(event);return true; }
    @Override public boolean performClick() { super.performClick();return true; }
}
