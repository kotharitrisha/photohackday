package com.iqengines.sdk;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.net.wifi.WifiManager;
import android.util.Log;



public class Utils {
    private static String TAG = Utils.class.getName();
    private static boolean DEBUG = true;

    
    /**
     * Method used to crop a Bitmap picture.
     * 
     * @param origBmp 
     * 		  The {@link Bitmap} image Image to be cropped.
     * 
     * @param targetSize 
     * 		  An {@link Integer} The size required. 
     * 
     * @return A {@link Bitmap} cropped image.
     */
    
    
    public static Bitmap cropBitmap(Bitmap origBmp, int targetSize) {
        final int w = origBmp.getWidth();
        final int h = origBmp.getHeight();

        float scale = ((float) targetSize) / (w < h ? w : h);

        Matrix matrix = new Matrix();
        if (w > h) {
            matrix.postRotate(90);
        }
        matrix.postScale(scale, scale);

        if (DEBUG)
            Log.d(TAG, "origBmp: width=" + w + ", height=" + h);

        int pad = (int) ((float) (w > h ? w : h) - ((float) targetSize) / scale) / 2;
        if (DEBUG)
            Log.d(TAG, "pad=" + pad);

        int new_w = w - (w > h ? 2 * pad : 0);
        int new_h = h - (w > h ? 0 : 2 * pad);
        if (DEBUG)
            Log.d(TAG, "new_w=" + new_w + ", new_h=" + new_h);

        Bitmap thumb = Bitmap.createBitmap(origBmp, w > h ? pad : 0, w > h ? 0 : pad, new_w, new_h,
                matrix, true);

        if (DEBUG) {
            Log.d(TAG, "tumb dim:" + thumb.getWidth() + "x" + thumb.getHeight());
        }

        return thumb;
    }
    
    /**
     * Method used to crop a YUV picture.
     * 
     * @param origBmp 
     * 		  The {@link YuvImage} to be cropped.
     * 
     * @param targetSize 
     * 		  An {@link Integer} The size required. 
     * 
     * @return A {@link File} representing the cropped YUV compressed to JPEG.
     */
   
    public static File cropYuv(YuvImage origYuv, int targetSize, Context ctx) {
    	
    	int w = origYuv.getWidth();
    	int h = origYuv.getHeight();
    	
    	int left = (int)(targetSize >= w ? 0 : (float)( (w-targetSize)/2 ) );
    	int right = (int)(targetSize >= w ? w : targetSize + (float)( (w-targetSize)/2 ) );
    	int top = (int)(targetSize >= h ? 0 : (float)( (h-targetSize)/2 ) );
    	int bottom = (int)(targetSize >= h ? h : targetSize + (float)( (w-targetSize)/2 ) );
    	
    	File dir = ctx.getDir("snapshots", Context.MODE_PRIVATE);
        File of = new File(dir, "snapshot.jpg");
        try {
            FileOutputStream fo = new FileOutputStream(of);
            
            try {
            	Log.d(TAG, "START COMPRESSION PICTURE YUVFILE");
                origYuv.compressToJpeg(new Rect(left, top, right, bottom), 100, fo);
                Log.d(TAG,"END COMPRESSION PICTURE YUVFILE");
            }finally {
                fo.close();
            }
            
        }catch (IOException e) {	
        	Log.e(TAG, "Can't store picture", e);
            throw new RuntimeException(e);
        }
        
        return of;

    }    
  
   
    
    /**
    * Transform a {@link Bitmap} picture into a {@link File} to be analyzed.
    * Pictures are first compressed to a JPEG format.
    * 
    * @param ctx 
    *        The {@link context}.
    * @param bmp
    * 		 The {@link Bitmap} to be converted.
    * 
    * @return The {@link File} object.
    * 
    * @throws RuntimeException
    **/

    
    public static File saveBmpToFile(Context ctx, Bitmap bmp) {
    	File dir = ctx.getDir("snapshots", Context.MODE_PRIVATE);
        File of = new File(dir, "snapshot.jpg");
        try {
            FileOutputStream fo = new FileOutputStream(of);
            
            try {
            	Log.d(TAG, "START COMPRESSION PICTURE BMPFILE");
                bmp.compress(CompressFormat.JPEG, 100, fo);
                Log.d(TAG,"END COMPRESSION PICTURE BMPFILE");   
            }finally {
            	fo.close();
            }
            
        } 
        catch (IOException e) {
        	Log.e(TAG, "Can't store picture", e);
            throw new RuntimeException(e);
        }
        
        return of;
    }
    
    
    /**
    * Transform a YUV picture into a File to be analyzed.
    * Pictures are first compressed to a JPEG format.
    * 
    * @param ctx
    * 		 The {@link context}.
    * @param yuv
    * 		 The {@link YuvImage} to be converted.
    * 
    * @return The {@link File} object.
    * 
    * @throws RuntimeException
    **/
    
    
    public static File saveYuvToFile(Context ctx, YuvImage yuv) {
        File dir = ctx.getDir("snapshots", Context.MODE_PRIVATE);
        File of = new File(dir, "snapshot.jpg");
        try {
            FileOutputStream fo = new FileOutputStream(of);
            
            try {
            	Log.d(TAG, "START COMPRESSION PICTURE YUVFILE");
                yuv.compressToJpeg(new Rect(0,0,yuv.getWidth(),yuv.getHeight()), 100, fo);
                Log.d(TAG,"END COMPRESSION PICTURE YUVFILE");
            }finally {
                fo.close();
            }
            
        }catch (IOException e) {	
        	Log.e(TAG, "Can't store picture", e);
            throw new RuntimeException(e);
        }
        
        return of;
    }
    
    
    /**
     * @param ctx
     * 		  The {@link context}.
     * 
     * @return A {@link String} representing the device's MAC address
     */
    
    
    public static String getDeviceId(Context ctx) {
        WifiManager wm = (WifiManager) ctx.getSystemService(Context.WIFI_SERVICE);
        String wmac = wm.getConnectionInfo().getMacAddress();
        return wmac;
    }


}
