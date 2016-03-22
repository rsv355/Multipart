package com.example.krishnakumar.multipart;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.http.Headers;
import android.os.AsyncTask;
import android.preference.PreferenceActivity;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.util.EntityUtils;

public class MainActivity extends AppCompatActivity {
    private static final int SELECT_FILE1 = 1;
    private static final int SELECT_FILE2 = 2;
    String selectedPath1 = "NONE";
    String selectedPath2 = "NONE";
    TextView tv, res;
    ProgressDialog progressDialog;
    Button b1, b2, b3;
    HttpEntity resEntity;
    final Charset ENCODING_TYPE = Charset.forName("UTF-8");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tv = (TextView) findViewById(R.id.tv);
        res = (TextView) findViewById(R.id.res);
        tv.setText(tv.getText() + selectedPath1 + "," + selectedPath2);
        b1 = (Button) findViewById(R.id.Button01);
        b2 = (Button) findViewById(R.id.Button02);
        b3 = (Button) findViewById(R.id.upload);
        b1.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                openGallery(SELECT_FILE1);
            }
        });
        b2.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                openGallery(SELECT_FILE2);
            }
        });
        b3.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                showFileChooser();

               /* if ((selectedPath1.trim().equalsIgnoreCase("NONE"))) {
                    progressDialog = ProgressDialog.show(MainActivity.this, "", "Uploading files to server.....", false);
                    Thread thread = new Thread(new Runnable() {
                        public void run() {
                            doFileUpload();
                            runOnUiThread(new Runnable() {
                                public void run() {
                                    if (progressDialog.isShowing())
                                        progressDialog.dismiss();
                                }
                            });
                        }
                    });
                    thread.start();
                } else {
                    Toast.makeText(getApplicationContext(), "Please select two files to upload.", Toast.LENGTH_SHORT).show();
                }*/
            }
        });


        //TODO just direct click on upolad, file url data is pending
    }

    public void openGallery(int req_code) {

        Intent intent = new Intent();
        intent.setType("text/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select file to upload "), req_code);
    }

   /* public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (resultCode == RESULT_OK) {
            Uri selectedImageUri = data.getData();
            if (requestCode == SELECT_FILE1)
            {
                selectedPath1 = getPath(selectedImageUri);
               Log.e("selectedPath1 : ","" + selectedPath1);
            }
            if (requestCode == SELECT_FILE2)
            {
                selectedPath2 = getPath(selectedImageUri);
                System.out.println("selectedPath2 : " + selectedPath2);
            }
            tv.setText("Selected File paths : " + selectedPath1 + "," + selectedPath2);
        }
    }*/


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case 101:
                if (resultCode == RESULT_OK) {
                    Uri uri = data.getData();
                    Log.e("FILE URI", "File Uri: " + uri.toString());
                    // String path = uri.getPath();
                    String path = null;
                    if (uri != null && "content".equals(uri.getScheme())) {
                        Cursor cursor = getContentResolver().query(uri, new String[]{android.provider.MediaStore.Images.ImageColumns.DATA}, null, null, null);
                        cursor.moveToFirst();
                        path = cursor.getString(0);
                        cursor.close();
                    } else {
                        path = uri.getPath();
                    }
                    Log.e("FILE URI", "File Path: " + path);
                    final File sourceFile = new File(path);
                    String filename = path.substring(path.lastIndexOf("/") + 1);
                    Log.e("FILE URI", "File fileName: " + filename);


                    String type = null;
                    String extension = MimeTypeMap.getFileExtensionFromUrl(uri.toString());
                    if (extension != null) {
                        type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
                    }

                    new AsyncTask<Void, Void, Void>() {

                        @Override
                        protected Void doInBackground(Void... params) {

                            try {
                                doFileUploadAnother(sourceFile);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }

                            //test3(sourceFile);
                            return null;
                        }
                    }.execute();

                    //uploadRetrofit(sourceFile);
                    Log.e("type", type);
                    Log.e("size in MB", (double) sourceFile.length() / (1024 * 1024) + "");

                } else {
                    Log.e("FILE URI", "File Path: " + "else");
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    public String getPath(Uri uri) {
        String[] projection = {MediaStore.Images.Media.DATA};
        Cursor cursor = managedQuery(uri, projection, null, null, null);
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        return cursor.getString(column_index);
    }

    private void showFileChooser() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.setType("file/*");
        try {
            startActivityForResult(Intent.createChooser(intent, "Select a File to Upload"), 101);
        } catch (android.content.ActivityNotFoundException ex) {
            // Potentially direct the user to the Market with a Dialog
            Toast.makeText(MainActivity.this, "Please install a File Manager.", Toast.LENGTH_SHORT).show();
        }
    }



    private void test3(File sourceFile) {
        String url = "http://ws-srv-net/Applications/Androids/MapleAppServices/Configuration.svc/json/UploadProfileImage/1";
        //String url = "http://ws-srv-net/Applications/Androids/MapleAppServices/Configuration.svc/json/MultipartUserLogin";
        try {
            HttpClient httpclient = new DefaultHttpClient();

            HttpPost httppost = new HttpPost(url);
           // httppost.addHeader("Content-Type", "multipart/form-data; boundary=--");
            httppost.addHeader("Content-Type", "application/octet-stream");
            InputStreamEntity reqEntity = new InputStreamEntity(
                    new FileInputStream(sourceFile), -1);
            // reqEntity.setContentType("application/octet-stream");


            //reqEntity.setContentType("multipart/form-data");
            reqEntity.setChunked(true); // Send in multiple parts if needed
            httppost.setEntity(reqEntity);
            HttpResponse response = httpclient.execute(httppost);
            //Do something with response...

        } catch (Exception e) {
            // show error
        }
    }


    private void doFileUploadAnother(File f) throws Exception {

        HttpClient httpclient = new DefaultHttpClient();
        httpclient.getParams().setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
        HttpPost httppost = new HttpPost("http://ws-srv-net/Applications/Androids/MapleAppServices/Configuration.svc/json/MultipartUserLogin");
        String boundary = "--";
        httppost.setHeader("Content-type", "multipart/form-data; boundary=" + boundary);

        Bitmap b = BitmapFactory.decodeFile(f.getAbsolutePath());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        b.compress(Bitmap.CompressFormat.JPEG, 85, baos);
        byte[] imageBytes = baos.toByteArray();
        ByteArrayBody bab = new ByteArrayBody(imageBytes, new File(f.getAbsolutePath()).getName() + ".jpg");

        HttpEntity entity = MultipartEntityBuilder.create()
                .setMode(HttpMultipartMode.BROWSER_COMPATIBLE)
                .setBoundary(boundary)
                .addPart("DOB", new StringBody("03/03/1992"))
                .addPart("ProfilePic", bab)
                .addPart("DeviceID", new StringBody("101"))
                .addPart("Email", new StringBody("abc@gmail.com"))
                .addPart("GCMID", new StringBody("GCM101"))
                .addPart("Gender", new StringBody("Male"))
                .addPart("Mobile", new StringBody("1234567890"))
                .addPart("Name", new StringBody("Android user1"))
                .addPart("SignupById", new StringBody("Facebook"))
                .addPart("SignupWith", new StringBody("FB101"))
                .build();

        httppost.setEntity(entity);
        try {
            HttpResponse response = httpclient.execute(httppost);

            entity = response.getEntity();
            final String response_str = EntityUtils.toString(entity);
            if (entity != null) {
                Log.e("RESPONSE", response_str);
                runOnUiThread(new Runnable() {
                    public void run() {
                        try {
                            res.setTextColor(Color.GREEN);
                            res.setText("n Response from server : n " + response_str);
                            Toast.makeText(getApplicationContext(), "Upload Complete. Check the server uploads directory.", Toast.LENGTH_LONG).show();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        } catch (Exception e) {

        }


    }



}