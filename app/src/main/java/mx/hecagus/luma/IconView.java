package mx.hecagus.luma;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.View;

/** Small original line icons; no icon fonts or external asset service. */
public final class IconView extends View {
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final String kind;
    private int color;
    public IconView(Context context,String kind,int color) { super(context); this.kind=kind; this.color=color; setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO); }
    public void setColor(int value) { color=value; invalidate(); }
    @Override protected void onDraw(Canvas c) {
        super.onDraw(c); c.save(); float unit=Math.min(getWidth(),getHeight())/24f;
        c.translate((getWidth()-24*unit)/2,(getHeight()-24*unit)/2); c.scale(unit,unit);
        p.setColor(color);p.setStrokeWidth(1.7f);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);p.setStyle(Paint.Style.STROKE);
        Path path=new Path();
        switch(kind) {
            case "photos":
                c.drawRoundRect(new RectF(3,4,21,20),3,3,p);c.drawCircle(16.4f,8.6f,1.7f,p);
                path.moveTo(4,17);path.lineTo(9,11);path.lineTo(14,17);path.lineTo(17,14);path.lineTo(20,17);c.drawPath(path,p);break;
            case "albums":
                c.drawRoundRect(new RectF(6,7,21,21),3,3,p);path.moveTo(3,16);path.lineTo(3,5);path.quadTo(3,3,5,3);path.lineTo(16,3);c.drawPath(path,p);break;
            case "heart":
                path.moveTo(12,20);path.cubicTo(8,17,3,13,3,8);path.cubicTo(3,2,10,2,12,7);path.cubicTo(14,2,21,2,21,8);path.cubicTo(21,13,16,17,12,20);c.drawPath(path,p);break;
            case "search":c.drawCircle(10.5f,10.5f,6.5f,p);c.drawLine(16,16,21,21,p);break;
            case "more":p.setStyle(Paint.Style.FILL);c.drawCircle(12,5,1.4f,p);c.drawCircle(12,12,1.4f,p);c.drawCircle(12,19,1.4f,p);break;
            case "back":c.drawLine(15,5,8,12,p);c.drawLine(8,12,15,19,p);break;
            case "close":c.drawLine(6,6,18,18,p);c.drawLine(6,18,18,6,p);break;
            case "share":c.drawCircle(18,5,2.5f,p);c.drawCircle(5,12,2.5f,p);c.drawCircle(18,19,2.5f,p);c.drawLine(7.4f,10.6f,15.6f,6.4f,p);c.drawLine(7.4f,13.4f,15.6f,17.6f,p);break;
            case "info":c.drawCircle(12,12,9,p);c.drawLine(12,11,12,17,p);p.setStyle(Paint.Style.FILL);c.drawCircle(12,7,1,p);break;
            case "trash":c.drawLine(4,6,20,6,p);c.drawLine(9,3,15,3,p);path.moveTo(6,6);path.lineTo(7,21);path.lineTo(17,21);path.lineTo(18,6);c.drawPath(path,p);c.drawLine(10,10,10,17,p);c.drawLine(14,10,14,17,p);break;
            case "shield":path.moveTo(12,2);path.lineTo(21,6);path.lineTo(20,14);path.quadTo(18,20,12,22);path.quadTo(6,20,4,14);path.lineTo(3,6);path.close();c.drawPath(path,p);path.reset();path.moveTo(8,12);path.lineTo(11,15);path.lineTo(17,9);c.drawPath(path,p);break;
            default:c.drawCircle(12,12,8,p);break;
        }
        c.restore();
    }
}
