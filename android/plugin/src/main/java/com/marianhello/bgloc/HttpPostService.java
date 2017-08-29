package com.marianhello.bgloc;

import android.os.Build;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URL;
import java.net.HttpURLConnection;
import java.io.OutputStreamWriter;
import java.util.Random;

public class HttpPostService {

    public static int postJSON(String url, JSONArray jsons, Map headers) throws IOException {

        JSONArray jsonLocations = new JSONArray();
        JSONObject location = null;
        JSONObject coords = null;
        JSONObject device = null;
        JSONObject loc = null;
        JSONObject locationElement = null;
        for (int i=0;i<jsons.length();i++){
           try {
               loc = (JSONObject) jsons.get(i);
                location = new JSONObject();
                locationElement = new JSONObject();
                coords = new JSONObject();
                device = new JSONObject();
                coords.put("latitude", loc.getDouble("latitude"));
                coords.put("longitude", loc.getDouble("longitude"));
                coords.put("accuracy", loc.getDouble("accuracy"));
                coords.put("speed", 0);
                coords.put("heading", 0);
                location.putOpt("coords", coords);
                location.put("odometer", 0);
                location.put("timestamp", loc.getLong("time"));
                location.put("provider", loc.get("provider"));
               location.put("geofence", null);
               location.put("activity", null);
               location.put("battery", null);
               location.put("extras", null);
               location.put("event", "");
               location.put("is_moving", true);
               location.put("uuid", Math.random());

               Iterator<Map.Entry<String, String>> it = headers.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry<String, String> pair = it.next();
                    if (pair.getKey().startsWith("X-DEVICE-")) {
                        device.put(pair.getKey().replaceAll("X-DEVICE-", ""), pair.getValue());
                    }
                }
                locationElement.put("location",location);
                locationElement.put("device",device);
                jsonLocations.put(locationElement);

            } catch (JSONException e) {

            }
        }


        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setDoOutput(true);
        conn.setFixedLengthStreamingMode(jsonLocations.length());
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        Iterator<Map.Entry<String, String>> it = headers.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, String> pair = it.next();
            if(!pair.getKey().startsWith("X-DEVICE-")){
                conn.addRequestProperty(pair.getKey(),pair.getValue());
            }
        }

        OutputStreamWriter os = null;
        try {
            os = new OutputStreamWriter(conn.getOutputStream());
            os.write(jsonLocations.toString());

        } finally {
            if (os != null) {
                os.flush();
                os.close();
            }
        }

        return conn.getResponseCode();
    }

    public static int postFile(String url, File file, Map headers, UploadingCallback callback) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();

        conn.setDoInput(false);
        conn.setDoOutput(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            conn.setFixedLengthStreamingMode(file.length());
        } else {
            conn.setChunkedStreamingMode(0);
        }
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        Iterator<Map.Entry<String, String>> it = headers.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, String> pair = it.next();
            conn.setRequestProperty(pair.getKey(), pair.getValue());
        }

        long progress = 0;
        int bytesRead = -1;
        byte[] buffer = new byte[1024];

        BufferedInputStream is = null;
        BufferedOutputStream os = null;
        try {
            is = new BufferedInputStream(new FileInputStream(file));
            os = new BufferedOutputStream(conn.getOutputStream());
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
                os.flush();
                progress += bytesRead;
                int percentage = (int) ((progress * 100L) / file.length());
                if (callback != null) {
                    callback.uploadListener(percentage);
                }
            }
        } finally {
            if (os != null) {
                os.flush();
                os.close();
            }
            if (is != null) {
                is.close();
            }
        }

        return conn.getResponseCode();
    }
}
