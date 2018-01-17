package com.flicktekltd.clip.upload;

//new UploadFileAsync().execute("");

import android.os.AsyncTask;
import android.util.Log;

import com.flicktekltd.clip.FlicktekCommands;
import com.flicktekltd.clip.FlicktekManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class UploadGestureAsync extends AsyncTask<String, Void, String> {
    private static final String TAG = "UploadGesture";
    private String mSourceFileUri;
    private byte[] mGestureRaw;
    private String mJsonConfig;
    private JSONObject mJsonObj;

    public static UploadGestureAsync getInstance(FlicktekCommands.onGestureRawData gestureData) {
        long unixTime = System.currentTimeMillis() / 1000L;
        String filename = FlicktekManager.getInstance().getMacAddress().replaceAll(":", "");

        if (FlicktekManager.getInstance().isCalibrating()) {
            filename += "_c" + gestureData.gesture;
        } else {
            filename += "_g" + gestureData.gesture;
        }

        filename += "_" + unixTime;
        UploadGestureAsync upload = new UploadGestureAsync(gestureData.byte_array, filename);
        upload.putString("gesture", "" + gestureData.gesture);
        upload.putString("gesture_string", "" + FlicktekManager.getGestureString(gestureData.gesture));
        upload.putString("mac", "" + FlicktekManager.getInstance().getMacAddress());

        if (FlicktekManager.getInstance().isCalibrating())
            upload.putString("type", "calibration");
        else
            upload.putString("type", "gesture");

        upload.execute("");
        return upload;
    }

    public void putString(String key, String data) {
        try {
            mJsonObj.put(key, data);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public UploadGestureAsync(byte[] gesture_raw, String sourceFileUri) {
        mSourceFileUri = sourceFileUri;
        mGestureRaw = gesture_raw;
        try {
            mJsonObj = new JSONObject();
            mJsonObj.put("name", sourceFileUri);
            mJsonObj.put("gesture_size", gesture_raw.length);
            mJsonObj.put("sampling_ticks", "" + FlicktekManager.getInstance().mSamplingRateTicks);
            mJsonObj.put("sampling_rate", "" + FlicktekCommands.getInstance().mSamplingRate);
        } catch (JSONException e) {
            e.printStackTrace();
            Log.e(TAG, "Failed parsing JSON");
        }
    }

    @Override
    protected String doInBackground(String... params) {
        try {
            Log.v(TAG, mSourceFileUri + " Starting upload");

            HttpURLConnection conn = null;
            DataOutputStream dos = null;
            String lineEnd = "\r\n";
            String twoHyphens = "--";
            String boundary = "*****";
            int serverResponseCode;

            try {
                String upLoadServerUri = "http://flicktek.engineer.blue/index.php?gestures=" +
                        FlicktekManager.getInstance().getMacAddress();

                URL url = new URL(upLoadServerUri);

                // Open a HTTP connection to the URL
                conn = (HttpURLConnection) url.openConnection();
                conn.setDoInput(true); // Allow Inputs
                conn.setDoOutput(true); // Allow Outputs
                conn.setUseCaches(false); // Don't use a Cached Copy
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Connection", "Keep-Alive");
                conn.setRequestProperty("ENCTYPE",
                        "multipart/form-data");
                conn.setRequestProperty("Content-Type",
                        "multipart/form-data;boundary=" + boundary);

                //conn.setRequestProperty("json", mSourceFileUri);
                //conn.setRequestProperty("raw", mSourceFileUri);

                dos = new DataOutputStream(conn.getOutputStream());

                dos.writeBytes(twoHyphens + boundary + lineEnd);
                dos.writeBytes("Content-Disposition: form-data; name=\"raw\";filename=\""
                        + mSourceFileUri + "\"" + lineEnd + lineEnd);

                dos.write(mGestureRaw, 0, mGestureRaw.length);
                dos.writeBytes(lineEnd);
                dos.writeBytes(twoHyphens + boundary + lineEnd);
                dos.writeBytes("Content-Disposition: form-data; name=\"json\";filename=\""
                        + mSourceFileUri + "\"" + lineEnd + lineEnd);

                String json = mJsonObj.toString();
                dos.writeBytes(json);
                dos.writeBytes(lineEnd);

                dos.writeBytes(lineEnd);
                dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

                // Responses from the server (code and message)
                serverResponseCode = conn.getResponseCode();
                String serverResponseMessage = conn
                        .getResponseMessage();

                if (serverResponseCode == 200) {
                    Log.v(TAG, mSourceFileUri + " Server OK " + serverResponseMessage);
                } else {
                    Log.v(TAG, mSourceFileUri + " Server return " + serverResponseMessage);
                }

                dos.flush();
                dos.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return "Executed";
    }

    @Override
    protected void onPostExecute(String result) {

    }

    @Override
    protected void onPreExecute() {
    }

    @Override
    protected void onProgressUpdate(Void... values) {
    }
}