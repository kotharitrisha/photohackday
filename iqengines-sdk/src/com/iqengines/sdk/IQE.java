
package com.iqengines.sdk;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.iqengines.sdk.IQRemote.IQEQuery;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.YuvImage;
import android.os.SystemClock;
import android.util.Log;

/**
 * Facade for providing unified access to local and remote search functionality
 * 
 * @author smineyev
 *
 */
public class IQE {

    /*
     * Callback interface that IQE uses to notify client about each search step 
     */

    public interface OnResultCallback {
        /**
         * This method gets called by IQE when unique query id is assigned to search query
         * 
         * @param queryId 
         * 		  A {@link String} representing the ID assigned to this search query.
         * @param imgFile
         *        A {@link File} containing the image originally submitted by user.
         */
        public void onQueryIdAssigned(String queryId, File imgFile);
        
        /**
         * This method gets called by IQE whenever either search result is available or exception occurs
         * 
         *  @param queryId
         *  	   A {@link String} giving the query ID of the search query.
         *  @param objId
         *  	   A {@link String} giving the object ID of match found by IQE. null if no match found.
         *  @param objName
         *  	   A {@link String} which is the object name (label) of the match found by IQE.  null if no match found.
         *  @param objMeta
         *  	   A {@link String}which are object meta information of the match found by IQE.  null if no match found.
         *  @param remoteMatch
         *  	   A {@link Boolean}, equals to true if match was found remotely and false if locally. 
         *  @param e 
         *         An {@link Exception} that occurred during the search. null is search finished without exceptions.
         */
        public void onResult(String queryId, String objId, String objName, String objMeta,
                boolean remoteMatch, Exception e);
    }
      
    
    /**
     * Max time acceptable for local local search if used in continues mode (milliseconds).
     */
    public static final long MAX_TEST_LOCAL_SEARCH_TIME = 1500;
    /**
     * Tells if remote search is enable.
     */
    private boolean remoteSearch;
    /**
     * Tells if local search is enable.
     */
    private boolean localSearch;

    private static boolean DEBUG = true;

    private static String TAG = IQE.class.getName();

    private static final String NO_MATCH_FOUND_STR = "no match found";
    
    private IQLocalApi iqLocal;

    private IQRemote iqRemote;

    private Activity activity;

    private String deviceId;

    private AtomicBoolean iqeRunning = new AtomicBoolean(false);

    private AtomicBoolean indexInitialized = new AtomicBoolean(false);
    /**
     * Lock used during the remote search.
     */
    private Object newIncomingRemoteMatchSemaphore = new Object();

    private Map<String, OnResultCallback> remoteQueryMap = Collections
            .synchronizedMap(new HashMap<String, IQE.OnResultCallback>());
    
    
    /**
     * Constructor 
     * 
     * @param activity
     * 		  The {@link Activity} within the query is made.
     * @param remoteSearch
     * 		  A {@link Boolean} whether remote search is enabled.
     * @param localSearch
     * 		  A {@link Boolean} whether local search is enabled.
     * @param remoteKey
     * 		  A {@link String} : Unique developer's key for accessing IQ Engines web search. (IQ Engines.com->developer center->settings)
     * @param remoteSecret
     * 		  A {@link String} : Developer's secret key for accessing IQ Engines web search. (IQ Engines.com->developer center->settings)
     */
    
    
    public IQE(Activity activity, boolean remoteSearch, boolean localSearch, 
            String remoteKey, String remoteSecret) {
    	
        if (!remoteSearch && !localSearch) { 
        	throw new IllegalArgumentException("At least one type of search must be enabled");
        }
        
        this.activity = activity;
        this.remoteSearch = remoteSearch;
        this.localSearch = localSearch;
        deviceId = Utils.getDeviceId(activity);
        initIqSdk(remoteKey, remoteSecret);
    }
  
    
    private void initIqSdk(String remoteKey, String remoteSecret) {
    	
        if (localSearch) {
        	iqLocal = new IQLocal();
            File appDataDir = new File(activity.getApplicationInfo().dataDir);
                iqLocal.init(activity.getResources(), appDataDir);
                indexInitialized.set(true);
                
                synchronized (indexInitialized) {
                    indexInitialized.notifyAll();
                }
        }

        if (remoteSearch) {
        	iqRemote = new IQRemote(remoteKey, remoteSecret);
        }
        
    }
    
    
    /**
     * Searches in local index. Method blocks caller until search result is ready.
     * 
     * @param bmp
     * 		  A {@link Bitmap} image to find match for.
     * @param onResultCallback
     * 		  An {@link OnResultCallback} object to be called when query id is assigned and when result is found. 
     */
    
    
    public synchronized void searchWithImageLocal(Bitmap bmp, OnResultCallback onResultCallback) {
        File imgFile = Utils.saveBmpToFile(activity, bmp);
        searchWithImageLocal(imgFile, onResultCallback);    
    }
    
    
    /**
     * Searches in local index. Method blocks caller until search result is ready.
     * 
     * @param yuv
     *        An {@link YuvImage} image to find match for.
     * @param onResultCallback
     * 		  An {@link OnResultCallback} object to be called when query id is assigned and when result is found.
     */
    
    
    public synchronized void searchWithImageLocal(YuvImage yuv, OnResultCallback onResultCallback) {
        File imgFile = Utils.saveYuvToFile(activity, yuv);
        searchWithImageLocal(imgFile, onResultCallback);    
    }
    

    
    /**
     * Searches in local index. Method blocks caller until search result is ready.
     * 
     * @param imgFile
     *        A {@link File} object containing an image to find match for.
     * @param onResultCallback
     * 		  An {@link OnResultCallback} object to be called when query id is assigned and when result is found. 
     * 
     * @throws IllegalStateException
     */
    
    
    public synchronized void searchWithImageLocal(File imgFile, OnResultCallback onResultCallback) {
    	
        if (!localSearch) {
        	throw new IllegalStateException("localSearch is disabled");
        }

        String queryId = Long.toString(SystemClock.elapsedRealtime());
        onResultCallback.onQueryIdAssigned(queryId, imgFile);
        
        final Mat img = new Mat(imgFile.getPath());
        int objIdx = iqLocal.match(img);
        
        if (objIdx >= 0) {
            List<String> ids = iqLocal.getObjIds();
            final String objId = ids.get(objIdx);
            String objName = iqLocal.getObjName(objId);
            String objMeta = iqLocal.getObjMeta(objId);;
            onResultCallback.onResult(queryId, objId, objName, objMeta, false, null);
            
            Log.d(TAG,"------------------------- LOCAL MATCH FOUND -------------------------");
            Log.d(TAG, "Object id: " + objId);
            Log.d(TAG, "Object name: " + objName);
            Log.d(TAG, "Object meta: " + objMeta);
        } else {    	
            onResultCallback.onResult(queryId, null, null, null, false, null);
            Log.d(TAG,"------------------------- NO LOCAL MATCH FOUND -------------------------");
        }
        
    }
  

    /**
     * Searches on IQ engines' server. 
     * Method blocks caller while it's submitting query to server but actual result is delivered later using update-API. 
     * onResultCallback is called when result is ready.
     * 
     * @param bmp
     * 		  A {@link Bitmap} image to find match for.
     * @param onResultCallback
     *        An {@link OnResultCallback} object to be called when query id is assigned and when result is found.
     */
    
    
    public synchronized void searchWithImageRemote(Bitmap bmp, OnResultCallback onResultCallback) {
        bmp = Utils.cropBitmap(bmp,
                Math.min(IQRemote.MAX_IMAGE_SIZE, Math.min(bmp.getWidth(), bmp.getHeight())));

        File imgFile = Utils.saveBmpToFile(activity, bmp);

        searchWithImageRemote(imgFile, onResultCallback);
    }
   
    /**
     * Searches on IQ engines' server. 
     * Method blocks caller while it's submitting query to server but actual result is delivered later using update-API. 
     * onResultCallback is called when result is ready.
     * 
     * @param yuv
     * 		  A {@link Yuv} image to find match for.
     * @param onResultCallback
     *        An {@link OnResultCallback} object to be called when query id is assigned and when result is found.
     */  
    
    
    public synchronized void searchWithImageRemote(YuvImage yuv, OnResultCallback onResultCallback) {      
        File imgFile = Utils.cropYuv(yuv,IQRemote.MAX_IMAGE_SIZE,activity);
        searchWithImageRemote(imgFile, onResultCallback);
    }   
    
    

    /**
     * Searches on IQ Engines server. 
     * Method blocks caller while it's submitting query to server but actual result is delivered later using update-API. 
     * onResultCallback is called when result is ready. 
     * 
     * @param imgFile
     * 		  A {@link File} containing an image to find match for.
     * @param onResultCallback
     *        An {@link OnResultCallback} object to be called when query id is assigned and when result is found.
     */
    
    
    public synchronized void searchWithImageRemote(File imgFile, OnResultCallback onResultCallback) {
        if (!remoteSearch) {
            throw new IllegalStateException("remoteSearch is disabled");
        }

        IQEQuery query = null;
        try {
            query = iqRemote.query(imgFile, deviceId);
        } 
        catch (IOException e) {
            onResultCallback.onResult(null, null, null, null, true, e);
            Log.d(TAG,"------------------------- CAN'T ACCESS TO THE SERVER -------------------------");
            return;
        }
        String qid = query.getQID();
        onResultCallback.onQueryIdAssigned(qid, imgFile);
        remoteQueryMap.put(qid, onResultCallback);
        Log.d(TAG,"------------------------- REMOTE MATCH FOUND -------------------------");
        synchronized (newIncomingRemoteMatchSemaphore) {
            newIncomingRemoteMatchSemaphore.notifyAll();
        }

        if (DEBUG) {	
            Log.d(TAG, "remote query qid: " + qid);
        }
        
    }
  
    
    /**
     * Searches in local index and on IQ Engines server. 
     * First it search in local index.
     * If result not found it submits query to IQ Engines server.  
     * Method blocks caller while it's searching in local index and, if needed, submitting query to server. 
     * onResultCallback is called when result is ready. 
     * 
     * @param bmp
     * 		  A {@link Bitmap} image to find match for.
     * @param onResultCallback
     * 		  An {@link OnResultCallback} object to be called when query id is assigned and when result is found.
     */
    
    
    public synchronized void searchWithImage(Bitmap bmp, OnResultCallback onResultCallback) {
        bmp = Utils.cropBitmap(bmp,
                Math.min(IQRemote.MAX_IMAGE_SIZE,
                        Math.min(bmp.getWidth(), bmp.getHeight())));
        File imgFile = Utils.saveBmpToFile(activity, bmp);
        searchWithImage(imgFile, onResultCallback);
    }
    
    /**
     * Searches in local index and on IQ Engines server. 
     * First it search in local index.
     * If result not found it submits query to IQ Engines server.  
     * Method blocks caller while it's searching in local index and, if needed, submitting query to server. 
     * onResultCallback is called when result is ready. 
     * 
     * @param yuv
     * 		  A {@link YuvImage} image to find match for.
     * @param onResultCallback
     * 		  An {@link OnResultCallback} object to be called when query id is assigned and when result is found.
     */
    
    public synchronized void searchWithImage(YuvImage yuv, OnResultCallback onResultCallback) {      
        File imgFile = Utils.cropYuv(yuv,IQRemote.MAX_IMAGE_SIZE,activity);
        searchWithImage(imgFile, onResultCallback);
    }   
    
    
    /**
     * Searches in local index and on IQ Engines server.
     * First it search in local index.
     * If result not found it submits query to IQ Engines server.
     * Method blocks caller while it's searching in local index and, if needed, submitting query to server.
     * onResultCallback is called when result is ready.
     * 
     * @param imgFile
     * 		  A {@link File} object containing an image to find match for.
     * 
     * @param onResultCallback
     * 		  An {@link OnResultCallback} object to be called when query id is assigned and when result is found.
     * 
     * @throws IllegalStateException
     */
    
    
    public synchronized void searchWithImage(File imgFile, OnResultCallback onResultCallback) {
        
    	
    	if (localSearch) {	
            if (!remoteSearch){    	
            	searchWithImageLocal(imgFile, onResultCallback);            	
            }else{

            	final Mat img = new Mat(imgFile.getPath());
            	int objIdx = iqLocal.match(img);
            	Log.d(TAG,"resultat de la local search "+(objIdx >= 0));
            	if (objIdx >= 0) {
            		Log.d(TAG, "We have a local match!");
            		List<String> ids = iqLocal.getObjIds();
            		final String objId = ids.get(objIdx);
            		Log.d(TAG, "Object id: " + objId);
            		String objName = iqLocal.getObjName(objId);
            		Log.d(TAG, "Object name: " + objName);
            		String objMeta = iqLocal.getObjMeta(objId);
            		Log.d(TAG, "Object meta: " + objMeta);
            		String queryId = Long.toString(SystemClock.elapsedRealtime());
            		onResultCallback.onQueryIdAssigned(queryId, imgFile);
            		onResultCallback.onResult(queryId, objId, objName, objMeta, false, null);
            		return;
            	}else if (DEBUG){
                }
           }
        
    	}
    	
        if (remoteSearch) {	
       
            IQEQuery query = null;
            try {
                query = iqRemote.query(imgFile, deviceId);
            } catch (IOException e) {
                onResultCallback.onResult(null, null, null, null, true, e);
                return;
            }
            String qid = query.getQID();
            onResultCallback.onQueryIdAssigned(qid, imgFile);
            remoteQueryMap.put(qid, onResultCallback);

            synchronized (newIncomingRemoteMatchSemaphore) {
                newIncomingRemoteMatchSemaphore.notifyAll();
            }

            if (DEBUG) {
                Log.d(TAG, "remote query qid: " + qid);
            }

            return;	
        }
        
        throw new IllegalStateException("both remote and local searches are disabled");
    }

        
    /**
     * Method is to be called when user wants to resume update-thread. 
     * Usually called from Activity.onResume.
     */
    
    
    public void resume() {
        iqeRunning.set(true);
        if (remoteSearch) {
        
        	new RemoteResultUpdateThread().start();
        }
    }

    
    /**
     * Method is to be called when user wants to stop update-thread. 
     * Usually called from Activity.onPause.
     */
    
    
    public void pause() {
    	
        iqeRunning.set(false);
        
        synchronized (newIncomingRemoteMatchSemaphore) {
            newIncomingRemoteMatchSemaphore.notifyAll();
            
        }
    }
    
    
    /**
     * Method is to be called when user wants to destroy IQE (to free-up memory).
     * Usually called from Activity.onDestroy.
     */
    
    
    public synchronized void destroy() {
    	
        if (iqLocal != null) { 
        	iqLocal.destroy();
        }
        
        iqLocal = null;
        iqRemote = null;
    }
    
    
    /**
     * Checks whether local search index is initialized and ready for use.
     * 
     * @return true if local search index is initialized.
     */
    
    
    public boolean isIndexInitialized() {
        return indexInitialized.get();
    }

    
    /**
     * Method tests capability of device to run continuous local search. 
     * Performs a test run of 'match' method.
     *
     * @return a positive {@link Long}. It's the time for image conversion and local research.
     *         -1 otherwise
     */
    
    
    public long testLocalSearchCapability() {

                    long st = SystemClock.elapsedRealtime();
                    try {
                    InputStream is = activity.getAssets().open("iqedata/obj0/img0.jpg");
                    try {
                        Bitmap bm = BitmapFactory.decodeStream(is);
                        File bmpFile = Utils.saveBmpToFile(activity, bm);

                        long t = SystemClock.elapsedRealtime();
                        // wait until index is initialized
                        
                        while (!indexInitialized.get()) {
                            synchronized (indexInitialized) {
                                indexInitialized.wait(10);
                            }
                        }
                        
                        t = SystemClock.elapsedRealtime() - t;
                        Log.i(TAG,String.valueOf(t));
                        searchWithImageLocal(bmpFile, new OnResultCallback() {
                            @Override
                            public void onResult(String queryId, String objId, String objName, String objMeta,
                                    boolean remoteMatch, Exception e) {
                            	
                                if (objId == null) {
                                    Log.e(TAG, "Known image is not recornized by test match !!!");
                                }
                            }

                            @Override
                            public void onQueryIdAssigned(String queryId, File imgFile) {
                            }
                        });

                        return SystemClock.elapsedRealtime() - st - t;
                           
                    } finally {
                            is.close();
                    }
                    } catch (Exception e) {
                        e.printStackTrace();
                        return -1;
                    }
    }
    
    
/**
 * Thread looking for results from IQ Engines server for the remote research.
 * It also processes the data given by the server.
 * onResultCallback is called when result is ready.
 * 
 * @throws RunTimeException 
 */
    
    
    private class RemoteResultUpdateThread extends Thread {
        @Override
        public void run() {
            JSONObject results = null;

            while (iqeRunning.get()) {
                synchronized (newIncomingRemoteMatchSemaphore) {
                    try {
                        newIncomingRemoteMatchSemaphore.wait(1000);
                    } catch (InterruptedException e) {
                        new RuntimeException(e);
                    }
                }
                
                if (!iqeRunning.get()) {
                    break;
                }

                try {
                    String resultStr = null;
                    resultStr = iqRemote.update(deviceId, true);
                    if (DEBUG)
                        Log.d(TAG, "update: " + resultStr);

                    if (resultStr == null)
                        continue;

                    try {
                        JSONObject result = new JSONObject(resultStr);

                        if (!result.has("data"))
                            continue;

                        results = result.getJSONObject("data");
                    } catch (JSONException e) {
                        Log.w(TAG, "Can't parse result", e);
                        continue;
                    }
                } catch (IOException e) {
                    Log.w(TAG, "Server call failed", e);
                    continue;
                }

                try {
                    if (results.has("error")) {
                        final int error = results.getInt("error");
                        if (error != 0) {
                            Log.e(TAG, "Server return error: " + error);
                            continue;
                        }
                    }

                    final JSONArray resultsList = results.getJSONArray("results");
                    for (int i = 0, lim = resultsList.length(); i < lim; ++i) {
                    	
                        final JSONObject resultObj = resultsList.getJSONObject(i);
                        final String qid = resultObj.optString("qid");
                        
                        if (qid == null) {
                            Log.e(TAG, "update result qid is null");
                            continue;
                        }

                        final JSONObject qidData = resultObj.getJSONObject("qid_data");

                        final String labels;
                        final String meta;
                        
                        if (qidData.length() > 0) {
                            labels = qidData.optString("labels", null);
                            meta = qidData.optString("meta", null);
                        } else {
                            labels = NO_MATCH_FOUND_STR;
                            meta = null;
                        }

                        OnResultCallback onResultCallback = remoteQueryMap.get(qid);
                        
                        if (onResultCallback != null) {
                            onResultCallback.onResult(qid, null, labels, meta, true, null);
                        } else {	
                            // most likely remote result arrived before UI thread registered callback
                            // instead of implementing synchronization we just sleep for a sec and try again
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                              throw new RuntimeException(e);
                            }
                            
                            onResultCallback = remoteQueryMap.get(qid);
                            
                            if (onResultCallback != null) {
                                onResultCallback.onResult(qid, null, labels, meta, true, null);
                            } else {
                                Log.w(TAG, "OnResultCallback is null for qid: " + qid);
                            }
                        }
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "JSON error", e);
                }
            }
        }
    }
}