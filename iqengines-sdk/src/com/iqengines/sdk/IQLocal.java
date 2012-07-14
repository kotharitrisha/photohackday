package com.iqengines.sdk;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

public class IQLocal implements IQLocalApi {
    
	public static final int IMAGE_SIZE = 150;

	public static final String TAG = IQLocal.class.getSimpleName();
	
    private static final int CMD_EXIT = 1;
    private static final int CMD_LOAD = 2;
    private static final int CMD_TRAIN = 3;
    private static final int CMD_MATCH = 4;
    private static final int CMD_COMPUTE = 5;
    private static final int CMD_INIT = 6;
    
    static {
        System.loadLibrary("iqindex");
        System.loadLibrary("iqengines-sdk");
    }
    
    private long nativeObj;
    
    private WorkerThread workerThread;
    private Handler workerHandler;
	private File mDataPath;
	private Object signal = new Object();
    
    private native long nativeCreate();
    private native void nativeDestroy(long nativeObj);
    private native int load(long nativeObj, String indexPath, String imagesPath);
    private native int match(long nativeObj, long addr);
    private native int train(long nativeObj);
    private native int compute(long nativeObj, long addr, String arg1, String arg2);
    
    private native int getObjCount(long nativeObj);
    private native String getObjId(long nativeObj, int idx);
    private native String getObjName(long nativeObj, String objId);
    private native String getObjMeta(long nativeObj, String objId);
    
    private static class ArgInit {
    	
    	public ArgInit(Resources res, File appDataDir, OnReady callback) {
    		this.appDataDir = appDataDir;
    		this.res = res;
    		this.callback = callback;
    	}
    	
		File appDataDir;
    	Resources res;
		OnReady callback;
    };
    
    
    
    private static class ArgLoad {
        String indexPath;
        String imagesPath;
        OnReady callback;
        
        public ArgLoad(String indexPath, String imagesPath, OnReady callback) {
            this.indexPath = indexPath;
            this.imagesPath = imagesPath;
            this.callback = callback;
        }
    };
    
    
    
    private static class ArgMatch {
        Mat img;
        OnReady callback;
        
        public ArgMatch(Mat img, OnReady callback) {
            this.img = img;
            this.callback = callback;
        }
    };
    
    
    
    private static class ArgCompute {
        Mat img;
        String arg1;
        String arg2;
        OnReady callback;
        
        public ArgCompute(Mat img, String arg1, String arg2, OnReady callback) {
            super();
            this.img = img;
            this.arg1 = arg1;
            this.arg2 = arg2;
            this.callback = callback;
        }
    };
    
    
    
    private class WorkerThread extends Thread {
        @Override
        public void run() {
            Looper.prepare();
            workerHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    switch (msg.what) {
                    case CMD_EXIT:
                        Looper.myLooper().quit();
                        break;
                    case CMD_LOAD:
                        ArgLoad argLoad = (ArgLoad)msg.obj;
                        handleLoad(argLoad.indexPath, argLoad.imagesPath, argLoad.callback);
                        break;
                    case CMD_TRAIN:
                        OnReady callback = (OnReady)msg.obj;
                        handleTrain(callback);
                        break;
                    case CMD_MATCH:
                        ArgMatch argMatch = (ArgMatch)msg.obj;
                        handleMatch(argMatch.img, argMatch.callback);
                        break;
                    case CMD_COMPUTE:
                        ArgCompute argCompute = (ArgCompute)msg.obj;
                        handleCompute(argCompute.img, argCompute.arg1, argCompute.arg2, argCompute.callback);
                        break;
                    case CMD_INIT:
                    	ArgInit argInit = (ArgInit)msg.obj;
                    	handleInit(argInit.appDataDir, argInit.res, argInit.callback);
                    	break;
                    }
                }

				
            };
            synchronized (signal) {
            	signal.notifyAll();	
			}
            
            Looper.loop();
        }
        
        public void finish() throws InterruptedException {
            workerHandler.sendMessage(Message.obtain(workerHandler, CMD_EXIT));
        }
    };
    
    
    
    public interface OnReady {
        public void onReady(int resultCode);
    }
    
    
    
	public IQLocal() {
	    nativeObj = nativeCreate();
	}
	
	/* (non-Javadoc)
	 * @see com.iqengines.sdk.IQLocalApi#destroy()
	 */
	@Override
	public void destroy() {
	    if (workerThread != null) {
	        try {
                workerThread.finish();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
	        workerThread = null;
	    }
	    if (nativeObj != 0) {
    	    nativeDestroy(nativeObj);
    	    nativeObj = 0;
	    }
	}
	
	
	@Override
	protected void finalize() throws Throwable {
	    destroy();
	    super.finalize();
	}
	
	
	
	private void prepareAsync() {
	    if (workerThread != null)
	        return;
	    workerThread = new WorkerThread();
	    workerThread.start();
	    
	    while (workerHandler == null) {
	    	synchronized (signal) {
				try {
					signal.wait(10);
				} catch (InterruptedException e) {
				}
			}
	    }
	}
	
	
	/* (non-Javadoc)
	 * @see com.iqengines.sdk.IQLocalApi#match(com.iqengines.sdk.Mat)
	 */
	@Override
	public int match(Mat img) {
		
	    final int width = img.cols();
	    final int height = img.rows();
	    final int min = width < height ? width: height;
	    final Mat cropped = img.submat((width - min/2)/2, (height - min/2)/2, min/2, min/2);
	    Mat scaled = cropped.resize(IMAGE_SIZE, IMAGE_SIZE);
		return match(nativeObj, scaled.nativeObj);
	}
	
	
	/* (non-Javadoc)
	 * @see com.iqengines.sdk.IQLocalApi#match(com.iqengines.sdk.Mat, com.iqengines.sdk.IQIndex.OnReady)
	 */
	@Override
	public void match(Mat img, OnReady callback) {
	    if (callback == null)
	        throw new IllegalArgumentException();

	    workerHandler.sendMessage(Message.obtain(workerHandler, CMD_MATCH,
	            new ArgMatch(img, callback)));
	}
	
	private void handleMatch(Mat img, OnReady callback) {
	    int resultCode = match(img);
	    callback.onReady(resultCode);
	}
	
	
	/* (non-Javadoc)
	 * @see com.iqengines.sdk.IQLocalApi#compute(com.iqengines.sdk.Mat, java.lang.String, java.lang.String)
	 */
	@Override
	public int compute(Mat img, String arg1, String arg2) {
	    return compute(nativeObj, img.nativeObj, arg1, arg2);
	}
	
	
	/* (non-Javadoc)
	 * @see com.iqengines.sdk.IQLocalApi#compute(com.iqengines.sdk.Mat, java.lang.String, java.lang.String, com.iqengines.sdk.IQIndex.OnReady)
	 */
	@Override
	public void compute(Mat img, String arg1, String arg2, OnReady callback) {
	    if (callback == null)
	        throw new IllegalArgumentException();
	    
	    workerHandler.sendMessage(Message.obtain(workerHandler, CMD_COMPUTE,
	            new ArgCompute(img, arg1, arg2, callback)));
	}
	
	private void handleInit(File appDataDir, Resources res, OnReady callback) {
	    try {
	    	init(res, appDataDir);
	    	callback.onReady(0);
	    } catch (Exception e) { 
	    	callback.onReady(-1);
	    }
	}
	
	private void handleCompute(Mat img, String arg1, String arg2, OnReady callback) {
	    int resultCode = compute(img, arg1, arg2);
	    callback.onReady(resultCode);
	}
	
	
	/* (non-Javadoc)
	 * @see com.iqengines.sdk.IQLocalApi#load(java.io.File, java.io.File)
	 */
	@Override
	public int load(File index, File images) {
	    return load(index.getPath(), images.getPath());
	}
	
	
	/* (non-Javadoc)
	 * @see com.iqengines.sdk.IQLocalApi#load(java.lang.String, java.lang.String)
	 */
	@Override
	public int load(String indexPath, String imagesPath) {
	    return load(nativeObj, indexPath, imagesPath);
	}
	
	
	/* (non-Javadoc)
	 * @see com.iqengines.sdk.IQLocalApi#load(java.io.File, java.io.File, com.iqengines.sdk.IQIndex.OnReady)
	 */
	@Override
	public void load(File index, File images, OnReady callback) {
	    load(index.getPath(), images.getPath(), callback);
	}
	
	
	/* (non-Javadoc)
	 * @see com.iqengines.sdk.IQLocalApi#load(java.lang.String, java.lang.String, com.iqengines.sdk.IQIndex.OnReady)
	 */
	@Override
	public void load(String indexPath, String imagesPath, OnReady callback) {
	    if (callback == null)
	        throw new IllegalArgumentException();
	    
	    workerHandler.sendMessage(Message.obtain(workerHandler, CMD_LOAD,
	            new ArgLoad(indexPath, imagesPath, callback)));
	}
	
	
	private void handleLoad(String indexPath, String imagesPath, OnReady callback) {
	    int resultCode = load(indexPath, imagesPath);
	    callback.onReady(resultCode);
	}
	
	
	/* (non-Javadoc)
	 * @see com.iqengines.sdk.IQLocalApi#train()
	 */
	@Override
	public int train() {
	    return train(nativeObj);
	}
	
	
	/* (non-Javadoc)
	 * @see com.iqengines.sdk.IQLocalApi#train(com.iqengines.sdk.IQIndex.OnReady)
	 */
	@Override
	public void train(OnReady callback) {
	    if (callback == null)
	        throw new IllegalArgumentException();
	    
	    workerHandler.sendMessage(Message.obtain(workerHandler, CMD_TRAIN, callback));
	}
	
	
	/* (non-Javadoc)
	 * @see com.iqengines.sdk.IQLocalApi#init(android.content.res.Resources, java.io.File, com.iqengines.sdk.IQIndex.OnReady)
	 */
	@Override
	public void init(Resources res, File appDataDir, OnReady callback) {
	    if (callback == null)
	        throw new IllegalArgumentException();
	    
	    prepareAsync();
	    
	    workerHandler.sendMessage(Message.obtain(workerHandler, CMD_INIT, 
	    		new ArgInit(res, appDataDir, callback)));		
	}
	
	
	/* (non-Javadoc)
	 * @see com.iqengines.sdk.IQLocalApi#init(android.content.res.Resources, java.io.File)
	 */
	@Override
	public void init(Resources res, File appDataDir) {
        mDataPath = new File(appDataDir, "iqedata");

		if (unpackInitialAssets(res, appDataDir)) {
			File index = new File(mDataPath, "objects.json");
			load(index, mDataPath);
	
			train();
		}
	}
	
	
	private void handleTrain(OnReady callback) {
	    int resultCode = train();
	    callback.onReady(resultCode);
	}
	
	
	public List<String> getObjIds() {
	    int count = getObjCount(nativeObj);
	    List<String> list = new ArrayList<String>(count);
	    for (int i = 0; i < count; ++i)
	        list.add(getObjId(nativeObj, i));
	    return list;
	}
	
	
	public String getObjName(String objId) {
	    return getObjName(nativeObj, objId);
	}
	
	
	public String getObjMeta(String objId) {
	    return getObjMeta(nativeObj, objId);
	}
	
	
    private boolean unpackInitialAssets(Resources res, File appDataDir) {
        AssetManager am = res.getAssets();
        File inRoot = new File("iqedata");
        
        try {
            unpackAssets(am, inRoot, mDataPath);
            return true;
        } catch (IOException e) {
            Log.e(TAG, "Can't unpack initial iq-index", e);
            return false;
        }
    }
    
    
    private static void unpackAssets(AssetManager am, File in, File out) throws IOException {
        String[] children = am.list(in.getPath());
        if (children != null && children.length > 0) {
            if (!out.exists())
                out.mkdirs();
            
            for (String child : children) {
                unpackAssets(am, new File(in, child), new File(out, child));
            }
        }
        else {
            InputStream is = null;
            OutputStream os = null;
            try {
                is = am.open(in.getPath());
                os = new FileOutputStream(out);
                
                byte[] buf = new byte[4096];
                int len;
                while ((len = is.read(buf)) > 0)
                    os.write(buf, 0, len);
            }
            finally {
                if (is != null)
                    is.close();
                if (os != null)
                    os.close();
            }
        }
    }

    
	public File getDataPath() {
		return mDataPath;
	}
}
