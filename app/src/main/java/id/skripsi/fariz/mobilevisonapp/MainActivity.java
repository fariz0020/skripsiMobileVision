package id.skripsi.fariz.mobilevisonapp;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Handler;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.JsonArrayRequest;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    private SurfaceView cameraView;
    private TextView textBlockContent;
    private CameraSource cameraSource;
    private RelativeLayout relativeLayout;

    private static String TAG = MainActivity.class.getSimpleName();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        cameraView = (SurfaceView) findViewById(R.id.surface_view);
        textBlockContent = (TextView) findViewById(R.id.text_value);
        relativeLayout = (RelativeLayout) findViewById(R.id.relativeLayout);

        final TinyDB tinydb = new TinyDB(getApplicationContext());

        TextRecognizer textRecognizer = new TextRecognizer.Builder(getApplicationContext()).build();
        if (!textRecognizer.isOperational()) {
            Log.w("MainActivity", "Detector dependencies are not yet available.");
        }

        cameraSource = new CameraSource.Builder(getApplicationContext(), textRecognizer)
                .setFacing(CameraSource.CAMERA_FACING_BACK)
                .setRequestedPreviewSize(1280, 1024)
                .setRequestedFps(2.0f)
                .setAutoFocusEnabled(true)
                .build();

        cameraView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                try {
                    //noinspection MissingPermission
                    cameraSource.start(cameraView.getHolder());
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                cameraSource.stop();
            }
        });

        if(!isNetworkConnected()){
            Snackbar snackbar = Snackbar
                    .make(relativeLayout, "Koneksi Internet Terputus", Snackbar.LENGTH_INDEFINITE)
                    .setAction("KELUAR", new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN){
                                finishAffinity();
                            } else{
                                finish();
                            }
                        }
                    });

            snackbar.show();
        }

        textRecognizer.setProcessor(new Detector.Processor<TextBlock>() {
            @Override
            public void release() {

            }

            @Override
            public void receiveDetections(Detector.Detections<TextBlock> detections) {
                Log.d("Main", "receiveDetections");
                final SparseArray<TextBlock> items = detections.getDetectedItems();
                if (items.size() != 0) {
                    textBlockContent.post(new Runnable() {
                        @Override
                        public void run() {
                            final StringBuilder value = new StringBuilder();

                                    for (int i = 0; i < items.size(); ++i) {
                                        final TextBlock item = items.valueAt(i);
                                        Log.d("TAG textblock", item.getValue());
                                        final int j = i;

                                                getTranslate(item.getValue(), new VolleyCallback(){
                                                    @Override
                                                    public void onSuccess(String result){
                                                        tinydb.putString("teks"+j, result);

                                                        Log.d("TAG-onSuccess-"+j, result);
                                                    }

                                                    @Override
                                                    public void onFailure(String resultFail){
                                                        tinydb.putString("teks"+j, resultFail);

                                                        Log.d("TAG-onFailure-"+j, resultFail);
                                                    }
                                                });

                                        value.append(tinydb.getString("teks"+j));
                                        value.append("\n");
                                        Log.d("TAG textblock-append", value.toString());


                                    }

                                    //update text block content to TextView
                                    Log.d("TAG textblock-after ", value.toString());
                                    textBlockContent.setText(value.toString());


                        }
                    });
                }

            }

            //end receive detector
        });


    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraSource.release();
    }

    private void getTranslate(final String string, final VolleyCallback callback) {
        final String url = String.format(Config.URL_API+Config.API_TRANSLATE+"?text=%1$s", string);
        JsonArrayRequest req = new JsonArrayRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        Log.d(TAG, response.toString());
                        Log.d(TAG + "url", url);
                        try {
                            // Parsing json array response
                            // loop through each json object
                            for (int i = 0; i < response.length(); i++) {

                                JSONObject api = (JSONObject) response
                                        .get(i);

                                String terjemah = api.getString("terjemah");

                                callback.onSuccess(terjemah);

                            }


                        } catch (JSONException e) {
                            /*callback.onSuccess(string);*/
                            callback.onFailure(string);
                            e.printStackTrace();
                            /*Toast.makeText(AppController.getInstance().getApplicationContext(),
                                    "Error: " + e.getMessage(),
                                    Toast.LENGTH_LONG).show();*/
                        }

                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

                /*callback.onSuccess(string);*/
                callback.onFailure(string);
                VolleyLog.d(TAG, "Error: " + error.getMessage());
                /*Toast.makeText(AppController.getInstance().getApplicationContext(),
                        error.getMessage(), Toast.LENGTH_SHORT).show();*/
            }
        });

        // Adding request to request queue
        AppController.getInstance().addToRequestQueue(req);
    }

    public interface VolleyCallback{
        void onSuccess(String result);
        void onFailure(String string);
    }

    private boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        return cm.getActiveNetworkInfo() != null;
    }

}
