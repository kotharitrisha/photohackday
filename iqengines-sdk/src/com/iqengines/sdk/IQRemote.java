package com.iqengines.sdk;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Serializable;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.TimeZone;
import java.util.TreeMap;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Hex;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

public class IQRemote implements Serializable {
    
    public final static int MAX_IMAGE_SIZE = 480;
    
    /*
     * CONSTRUCTORS
     */
    
    /**
     * Constructor
     *
     * @param key
     *            A non-<code>null</code> {@link String} : Your API key.
     * @param secret
     *            A non-<code>null</code> {@link String} : Your API secret.
     */
    public IQRemote(String key, String secret) {
        this.key = key;
        this.secret = secret;
    }

    /*
     * PUBLIC METHODS
     */
    
    /**
     * Upload an image to the IQ Engines' server.
     *
     * @param image
     *            A non-<code>null</code> {@link File} : The image file you
     *            would like to get labels for. The image's height and width
     *            should be less than 640 pixels.
     * @param deviceId
     * 			  Device unique identifier
     * @return A non-<code>null</code> {@link IQEQuery}
     * @throws IOException 
     */
    public IQEQuery query(File image, String deviceId) throws IOException {
        return query(image, null, null, deviceId, true, null, null, null);
    }

    /**
     * Upload an image to the IQ Engines' server.
     *
     * @param image
     *            A non-<code>null</code> {@link File} : The image file you
     *            would like to get labels for. The image's height and width
     *            should be less than 640 pixels.
     * @param webhook
     *            A possibly-<code>null</code> {@link String} : The URL where
     *            the results are sent via HTTP POST once the labels have been
     *            computed.
     * @param extra
     *            A possibly-<code>null</code> {@link String} : A string that
     *            is posted back when the webhook is called. It is useful for
     *            passing JSON-encoded extra parameters about the query that
     *            might be needed in your application to process the labels.
     * @param device_id
     *            A possibly-<code>null</code> {@link String} : The unique
     *            identification of the device that is querying the API.
     * @param json
     *            If this parameter is true, the results are in the JSON format,
     *            otherwise the results are in the XML format.
     * @param gps_altitude
     *            A possibly-<code>null</code> {@link String} : The altitude of
     *            your GPS coordinates.
     * @param gps_longitude
     *            A possibly-<code>null</code> {@link String} : The longitude of
     *            your GPS coordinates.
     * @param gps_latitude
     *            A possibly-<code>null</code> {@link String} : The latitude of
     *            your GPS coordinates.
     * @return A non-<code>null</code> {@link IQEQuery}
     * @throws IOException 
     */
    public IQEQuery query(File image, String webhook, String extra,
            String device_id, boolean json,
            String gps_altitude, String gps_longitude,
            String gps_latitude) throws IOException {
        TreeMap<String, String> fields = new TreeMap<String, String>();

        // Optional parameters
        if (webhook != null) {
            fields.put("webhook", webhook);
        }
        if (extra != null) {
            fields.put("extra", extra);
        }
        if (device_id != null) {
            fields.put("device_id", device_id);
        }
        if (json) {
            fields.put("json", "1");
        }
        if (gps_altitude != null) {
            fields.put("gps_altitude", gps_altitude);
        }
        if (gps_longitude != null) {
            fields.put("gps_longitude", gps_longitude);
        }
        if (gps_latitude != null) {
            fields.put("gps_latitude", gps_latitude);
        }

        // Required parameters
        fields.put("img", image.getPath());
        fields.put("time_stamp", now());
        fields.put("api_key", key);
        fields.put("api_sig", buildSignature(fields));

        if (image.exists()) {
            return new IQEQuery(post(IQESelector.query, fields), fields.get("api_sig"));
        } else {
            return new IQEQuery("Error : File '" + image.getPath() + "' doesn't exists", fields.get("api_sig"));
        }
    }

    
    /**
     * The Update API is a long polling request to our server that returns a
     * list of qids along with the labels that have been successfully processed
     * by our image labeling engine. The Update API times out after 90 seconds.
     *
     * @return A non-<code>null</code> {@link String}
     * @throws IOException 
     */
    
    
    public String update() throws IOException {
        return update(null, false);
    }

    
    /**
     * The Update API is a long polling request to our server that returns a
     * list of qids along with the labels that have been successfully processed
     * by our image labeling engine. The Update API times out after 90 seconds.
     *
     * @param device_id
     *            A possibly-<code>null</code> {@link String} : The unique
     *            identification of the device that is querying the API. If you
     *            are using the API on multiple mobile devices, you should pass
     *            the device_id as a parameter to the Query API and Update API.
     *            This ensures that the Update API returns only results
     *            corresponding to image queries sent by the device.
     * @param json
     *            If this parameter is true, the results are in the JSON format,
     *            otherwise the results are in the XML format.
     *
     * @return A non-<code>null</code> {@link String}
     * @throws IOException 
     */
    
    
    public String update(String device_id, boolean json) throws IOException {
        TreeMap<String, String> fields = new TreeMap<String, String>();

        // Optional parameters
        if (device_id != null) {
            fields.put("device_id", device_id);
        }
        if (json) {
            fields.put("json", "1");
        }

        // Required parameters
        fields.put("time_stamp", now());
        fields.put("api_key", key);
        fields.put("api_sig", buildSignature(fields));

        return post(IQESelector.update, fields);
    }

    
    /**
     * The Upload API allows you to upload images to the IQ Engines object 
     * search database through a RESTful interface. Once the computer vision
     * system has successfully indexed your image, you will be able to search
     * for it using the query api.
     * 
     * @param images
     *          A non-<code>null</code> {@link ArrayList} of {@link File} : A 
     *          set of image files you would like to associate to this object.
     * @param name
     *          A non-<code>null</code> {@link String} : The human-readable name 
     *          of the object. This is what we return when you use the query
     *          api.
     *
     * @return A non-<code>null</code> {@link String}
     * @throws IOException 
     */
    
    
    public String upload(ArrayList<File> images, String name) throws IOException {
        return upload(images, name, null, null, false, null);
    }

    
    /**
     * The Upload API allows you to upload images to the IQ Engines object
     * search database through a RESTful interface. Once the computer vision
     * system has successfully indexed your image, you will be able to search
     * for it using the query api.
     *
     * @param images
     *          A non-<code>null</code> {@link ArrayList} of {@link File} : A 
     *          set of image files you would like to associate to this object.
     * @param name
     *          A non-<code>null</code> {@link String} : The human-readable name 
     *          of the object. This is what we return when you use the query
     *          api.
     * @param custom_id
     *          A possibly-<code>null</code> {@link String} : A unique id within
     *          the collection you are uploading. This custom_id can be used to
     *          reference this object’s meta data at a later point in time.
     * @param meta
     *          A possibly-<code>null</code> {@link String} : This is a
     *          json-dictionary encoded as a string of additional meta-data to
     *          be linked to the uploaded image.
     *          (e.g. {‘isbn’: ‘9780393326291’})
     * @param json
     *          If this parameter is true, the results are in the JSON format,
     *          otherwise the results are in the XML format.
     * @param collection
     *          A possibly-<code>null</code> {@link String} : This field is a
     *          string that ties multiple objects together into a ‘collection’.
     *          You can use this field to more easily look up objects belonging
     *          to certain groups later.
     *
     * @return A non-<code>null</code> {@link String}
     * @throws IOException 
     */
    
    
    public String upload(ArrayList<File> images, String name, String custom_id, String meta, boolean json, String collection) throws IOException {
        TreeMap<String, String> fields = new TreeMap<String, String>();

        // Optional parameters
        if (custom_id != null) {
            fields.put("custom_id", custom_id);
        }
        if (meta != null) {
            fields.put("meta", meta);
        }
        if (collection != null) {
            fields.put("collection", collection);
        }
        if (json) {
            fields.put("json", "1");
        }

        // Required parameters
        int index = 0;
        for (File image : images) {
            if (!image.exists()) {
                return "Error : File '" + image.getPath() + "' doesn't exists";
            }
            fields.put("images" + ++index, image.getPath());
        }
        fields.put("name", name);
        fields.put("time_stamp", now());
        fields.put("api_key", key);
        fields.put("api_sig", buildSignature(fields));


        return post(IQESelector.object, fields);
    }

    
    /**
     * The Result API is used to retrieve the labels
     *
     * @param qid
     *            A non-<code>null</code> {@link String} : The unique identifier
     *            of the image for which you want to retrieve the results.
     * @param json
     *            If this parameter is true, the results are in the JSON format,
     *            otherwise the results are in the XML format.
     *
     * @return A non-<code>null</code> {@link String}
     * @throws IOException 
     */
    
    
    public String result(String qid, boolean json) throws IOException {
        TreeMap<String, String> fields = new TreeMap<String, String>();

        // Optional parameter
        if (json) {
            fields.put("json", "1");
        }

        // Required parameters
        fields.put("time_stamp", now());
        fields.put("api_key", key);
        fields.put("qid", qid);
        fields.put("api_sig", buildSignature(fields));

        return post(IQESelector.result, fields);
    }

    /*
     * PRIVATE METHODS
     */
    
    
    /**
     * Returns the current time stamp using the following formatting :
     * "YYYYmmDDHHMMSS"
     *
     * @return a non null {@link String}
     */
    
    
    private String now() {
    	
        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        String date_format = "yyyyMMddkkmmss";
        SimpleDateFormat sdf = new SimpleDateFormat(date_format);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
 
        return sdf.format(c.getTime());
    }

    
    /**
     * Computes the signature of the API call
     *
     * @param fields
     *            A non-<code>null</code> {@link TreeMap} that contains the
     *            arguments of the request.
     * @return A non-<code>null</code> {@link String} : The message
     *         authentication code
     */
    
    
    private String buildSignature(TreeMap<String, String> fields) {
        // join key value pairs together
        String result = null;
        String raw_string = "";
        Iterator<String> i = fields.keySet().iterator();
        while (i.hasNext()) {
            String tmpKey = i.next();
            String value = fields.get(tmpKey);
            if (tmpKey.equals("img")) {
                // if the argument is an image, only keep the name of the file
                File image = new File(value);
                raw_string += "img" + image.getName();
            } else if (tmpKey.startsWith("images")) {
                // if the argument is an image, only keep the name of the file
                File image = new File(value);
                raw_string += "images" + image.getName();
            } else {
                raw_string += tmpKey + value;
            }
        }
        try {
            String HMAC_SHA1_ALGORITHM = "HmacSHA1";
            byte[] secret_bytes = secret.getBytes();
            SecretKeySpec secret_key = new SecretKeySpec(secret_bytes, HMAC_SHA1_ALGORITHM);

            Mac m = Mac.getInstance("HmacSHA1");
            m.init(secret_key);
            byte[] signature_raw = m.doFinal(raw_string.getBytes());

            // Convert raw bytes to Hex
            byte[] signature_hex = new Hex().encode(signature_raw);

            //  Covert array of Hex bytes to a String
            result = new String(signature_hex, "ISO-8859-1");
        } catch (NoSuchAlgorithmException e) {
            return "Exception : NoSuchAlgorithmException" + e.getMessage();
        } catch (InvalidKeyException e) {
            return "Exception : InvalidKeyException" + e.getMessage();
        } catch (UnsupportedEncodingException e) {
            return "Exception : UnsupportedEncodingException" + e.getMessage();
        }
        return result;
    }

    
    /**
     * Post fields and files to an http host as multipart/form-data.
     *
     * @param selector
     *            A non-<code>null</code> {@link IQESelector} : The type of post
     *            message.
     * @param fields
     *            A non-<code>null</code> {@link TreeMap} : The fields
     * @return A non-<code>null</code> {@link String} : The server's response.
     * @throws IOException 
     */
    
    
    private String post(IQESelector selector, TreeMap<String, String> fields) throws IOException {
        String result = "error";
        String url = "http://api.iqengines.com/v1.2/" + selector + "/";

        HttpClient client = new DefaultHttpClient();
        HttpPost httppost = new HttpPost(url);
        MultipartEntity entity = new MultipartEntity();

        Iterator<String> i = fields.keySet().iterator();
        while (i.hasNext()) {
            String tmpKey = i.next();
            if (tmpKey.equals("img")) {
                File img = new File(fields.get(tmpKey));
                entity.addPart(tmpKey, new FileBody(img));
            } else if (tmpKey.startsWith("images")) {
                File img = new File(fields.get(tmpKey));
                entity.addPart("images", new FileBody(img));
            } else {
                entity.addPart(tmpKey, new StringBody(fields.get(tmpKey)));
            }
        }
        httppost.setEntity(entity);

        HttpResponse response = client.execute(httppost);
        HttpEntity resEntity = response.getEntity();

        if (resEntity != null) {
            long length = resEntity.getContentLength();
            // Check if we have to stream the result of the query
            if (length != -1 && length < 2048) {
                result = EntityUtils.toString(resEntity);
            } else {
                InputStream instream = resEntity.getContent();

                Writer writer = new StringWriter();
                char[] buffer = new char[1024];

                try {
                    Reader reader = new BufferedReader(new InputStreamReader(instream));

                    int n;
                    while ((n = reader.read(buffer)) != -1) {
                        writer.write(buffer, 0, n);
                    }
                } finally {
                    instream.close();
                }
                
                result = writer.toString();
            }
            
            resEntity.consumeContent();
        }
        
        return result;
    }

    
    /**
     * This class is used to store the results of the Query API : The unique
     * identifier of the image and the server's response.
     *
     * @author Vincent Garrigues
     */
    
    
    public class IQEQuery implements Serializable {

    	
        /**
         * Creates a new {@link IQEQuery}.
         *
         * @param result A non-<code>null</code> {@link String}.
         * @param qid A non-<code>null</code> {@link String}.
         */
    	
    	
        public IQEQuery(String result, String qid) {
            this.result = result;
            this.qid = qid;
        }

        
        /**
         * The server's response.
         *
         * @return A non-<code>null</code> {@link String}.
         */
        
        
        public String getResult() {
            return result;
        }

        
        /**
         * The unique identifier of the image for which you want to retrieve the
         * results.
         *
         * @return A non-<code>null</code> {@link String}.
         */
        
        
        public String getQID() {
            return qid;
        }
        /** The result */
        private String result;
        /** The query id */
        private String qid;
        /** Generated serial id */
        private static final long serialVersionUID = 1930709349669617215L;
    }

    
    /**
     * An enumeration for the different urls.
     *
     * @author Vincent Garrigues
     */
    
    
    public enum IQESelector {

        query,
        update,
        result,
        object
    }
    
    /** Your API key */
    
    private final String key;

    /** Your API secret */
    
    private final String secret;
    
    /** Generated serial id */
    
    private static final long serialVersionUID = -8870882783562183990L;
}
