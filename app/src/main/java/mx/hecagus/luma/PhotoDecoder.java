package mx.hecagus.luma;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import java.io.IOException;
import java.io.InputStream;

/** Bounded decoding, with a stream fallback for providers without usable descriptors. */
public final class PhotoDecoder {
    private PhotoDecoder() {}
    public static Bitmap decode(ContentResolver resolver,Uri uri,int maximum)throws IOException {
        try {
            return ImageDecoder.decodeBitmap(ImageDecoder.createSource(resolver,uri),(decoder,info,source)->{
                decoder.setTargetSampleSize(GalleryLogic.sampleSize(info.getSize().getWidth(),info.getSize().getHeight(),maximum));
                decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);
            });
        } catch(IOException|RuntimeException descriptorFailure) {
            BitmapFactory.Options options=new BitmapFactory.Options();options.inJustDecodeBounds=true;
            try(InputStream input=resolver.openInputStream(uri)){BitmapFactory.decodeStream(input,null,options);}
            if(options.outWidth<=0||options.outHeight<=0)throw new IOException("Unsupported or unavailable image",descriptorFailure);
            options.inJustDecodeBounds=false;options.inSampleSize=GalleryLogic.sampleSize(options.outWidth,options.outHeight,maximum);
            Bitmap bitmap;
            try(InputStream input=resolver.openInputStream(uri)){bitmap=BitmapFactory.decodeStream(input,null,options);}
            if(bitmap==null)throw new IOException("Image could not be decoded",descriptorFailure);
            int orientation=ExifInterface.ORIENTATION_NORMAL;
            try(InputStream input=resolver.openInputStream(uri)){orientation=new ExifInterface(input).getAttributeInt(ExifInterface.TAG_ORIENTATION,ExifInterface.ORIENTATION_NORMAL);}catch(IOException|RuntimeException ignored){}
            Matrix matrix=new Matrix();
            switch(orientation){
                case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:matrix.setScale(-1,1);break;
                case ExifInterface.ORIENTATION_ROTATE_180:matrix.setRotate(180);break;
                case ExifInterface.ORIENTATION_FLIP_VERTICAL:matrix.setScale(1,-1);break;
                case ExifInterface.ORIENTATION_TRANSPOSE:matrix.setRotate(90);matrix.postScale(-1,1);break;
                case ExifInterface.ORIENTATION_ROTATE_90:matrix.setRotate(90);break;
                case ExifInterface.ORIENTATION_TRANSVERSE:matrix.setRotate(270);matrix.postScale(-1,1);break;
                case ExifInterface.ORIENTATION_ROTATE_270:matrix.setRotate(270);break;
                default:return bitmap;
            }
            Bitmap oriented=Bitmap.createBitmap(bitmap,0,0,bitmap.getWidth(),bitmap.getHeight(),matrix,true);
            if(oriented!=bitmap)bitmap.recycle();return oriented;
        }
    }
}
