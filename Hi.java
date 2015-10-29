package ru.dshgmbh.hi;

//import android.content.ActivityNotFoundException;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.RectF;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.Tag;
import android.nfc.tech.NfcF;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.app.Activity;
import java.io.IOException;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ExpandableListView;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;

import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.PreviewCallback;

import android.graphics.Matrix;
import android.graphics.Rect;

import org.json.JSONException;
import org.json.JSONObject;

import QR.IntentIntegrator;
import QR.IntentResult;

import android.nfc.NfcAdapter;

public class Hi extends Activity implements CompoundButton.OnCheckedChangeListener {
    // названия компаний (групп)
    //String[] groups = new String[] {"HTC", "Samsung", "LG"};

    // названия телефонов (элементов)
    String[][] elmOfGroups = new String[][]{ new String[] {"Sensation", "Desire", "Wildfire", "Hero"},
                             new String[] {"Galaxy S II", "Galaxy Nexus", "Wave"},
                             new String[] {"Optimus", "Optimus Link", "Optimus Black", "Optimus One"}};

    // коллекция для групп
    ArrayList<Map<String, String>> groupData;

    // коллекция для элементов одной группы
    ArrayList<Map<String, String>> childDataItem;

    // общая коллекция для коллекций элементов
    ArrayList<ArrayList<Map<String, String>>> childData;
    // в итоге получится childData = ArrayList<childDataItem>

    static final String ACTION_SCAN = "com.google.zxing.client.android.SCAN";

    // список аттрибутов группы или элемента
    Map<String, String> m;

    ExpandableListView elvMain;
    //SurfaceView cameraView;
    DB db;
    ToggleButton cameraBtn;

    //SurfaceHolder holder;
    //HolderCallback holderCallback;
    //Camera camera;

    final int CAMERA_ID = 0;
    final boolean FULL_SCREEN = false;

    private NfcAdapter mNfcAdapter;
    private PendingIntent mPendingIntent;
    private IntentFilter[] mIntentFilters;
    private String[][] mNFCTechLists;
    private TextView header;

    public void onCreate(Bundle savedInstance){
        super.onCreate(savedInstance);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.myscreen);

        //File file = new File("/data/data/ru.dshgmbh.hi/databases/BarMeter.sql");
        //file.delete();
/*
        cameraBtn = (ToggleButton) findViewById(R.id.openCamera);
        cameraBtn.setOnCheckedChangeListener(this);
        cameraView = (SurfaceView) findViewById(R.id.cameraView);
        cameraView.setVisibility(View.GONE);
        int cameraId = -1;
        int numOfCameras = Camera.getNumberOfCameras();
        for (int i = 0; i < numOfCameras; i++) {
            CameraInfo info = new CameraInfo();
            Camera.getCameraInfo(i, info);
            if (info.facing == CameraInfo.CAMERA_FACING_FRONT) cameraId = i;
            if (info.facing == CameraInfo.CAMERA_FACING_BACK) cameraId = i;
            }
        }
        camera = Camera.open(cameraId);
        holder = cameraView.getHolder();
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        holderCallback = new HolderCallback();
        holder.addCallback(holderCallback);
*/
        HashMap<String, String> hashMap = new HashMap<String, String>();
        String string = "{\"body\":\"\",\"issued\":\"DSH\",\"uniq_barcode\":\"0000-1001-0001\"}";
        try {
            JSONObject json = new JSONObject(string);

            hashMap.put("name", json.getString("issued"));
            hashMap.put("code", json.getString("uniq_barcode"));
        } catch (JSONException e) {
            // TODO Handle expection!
        }
        String name = hashMap.get("name");
        String code = hashMap.get("code");

        header = (TextView)findViewById(R.id.textView);

        header.setText(code);
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);

        if (mNfcAdapter == null) {
            // Stop here, we definitely need NFC
            Toast.makeText(this, "This device doesn't support NFC.", Toast.LENGTH_LONG).show();
            finish();
            return;

        }

        if (!mNfcAdapter.isEnabled()) {
            header.setText("NFC is disabled.");
        } else {
            header.setText("ready to read");
        }

        mPendingIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

        // set an intent filter for all MIME data
        IntentFilter ndefIntent = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);
        try {
            ndefIntent.addDataType("*/*");
            mIntentFilters = new IntentFilter[] { ndefIntent };
        } catch (Exception e) {
            Log.e("TagDispatch", e.toString());
        }

        mNFCTechLists = new String[][] { new String[] { NfcF.class.getName() } };


        db = new DB(this);

        //db.createTestDB(db.getWritableDatabase());

        ArrayList<String> groups = db.getAllGroups();

        groupData = new ArrayList<Map<String, String>>();

        // создаем коллекцию для коллекций элементов
        childData = new ArrayList<ArrayList<Map<String, String>>>();
        childData = db.getAllData();

        myExpandableAdapter adapter = new myExpandableAdapter(this, groups, childData);

        elvMain = (ExpandableListView) findViewById(R.id.elView);
        elvMain.setAdapter(adapter);

        elvMain.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
                Log.d("child","group #"+groupPosition+" child -"+ elmOfGroups[groupPosition][childPosition]);
                return false;
            }
        });
    }

    @Override
    public void onNewIntent(Intent intent) {
        String action = intent.getAction();
        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);

        String s ="";// = action + "\n\n" + tag.toString();

        // parse through all NDEF messages and their records and pick text type only
        Parcelable[] data = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
        if (data != null) {
            try {
                for (int i = 0; i < data.length; i++) {
                    NdefRecord[] recs = ((NdefMessage)data[i]).getRecords();
                    for (int j = 0; j < recs.length; j++) {
                        if (recs[j].getTnf() == NdefRecord.TNF_WELL_KNOWN &&
                                Arrays.equals(recs[j].getType(), NdefRecord.RTD_TEXT)) {
                            byte[] payload = recs[j].getPayload();
                            String textEncoding;
                            if ((payload[0] & 0200) == 0) textEncoding = "UTF-8";
                            else textEncoding = "UTF-16";
                            int langCodeLen = payload[0] & 0077;

                            s += ("\n\nNdefMessage[" + i + "], NdefRecord[" + j + "]:\n\"" +
                                    new String(payload, langCodeLen + 1, payload.length - langCodeLen - 1,
                                            textEncoding) + "\"");
                            s = new String(payload, langCodeLen + 1, payload.length - langCodeLen - 1,
                                    textEncoding);
                        }
                    }
                }
            } catch (Exception e) {
                Log.e("TagDispatch", e.toString());
            }
        }
        HashMap<String, String> hashMap = new HashMap<String, String>();
        try {
            JSONObject json = new JSONObject(s);

            hashMap.put("name", json.getString("issued"));
            hashMap.put("code", json.getString("uniq_barcode"));
        } catch (JSONException e) {
            // TODO Handle expection!
        }
        String name = hashMap.get("name");
        s = name+":"+hashMap.get("code");
        header.setText(s);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mNfcAdapter != null)
            mNfcAdapter.enableForegroundDispatch(this, mPendingIntent, mIntentFilters, mNFCTechLists);
    }

    @Override
    public void onPause() {
        super.onPause();

        if (mNfcAdapter != null)
            mNfcAdapter.disableForegroundDispatch(this);
    }
/*

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            // Все записи
            case R.id.camera:
                Log.d("button", "--- Все записи ---");

                break;
        }
    }
*/

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

        if (isChecked) {
            elvMain.setVisibility(View.GONE);
            //cameraView.setVisibility(View.VISIBLE);
            Log.d("toggle button", "close");
            onResume();
        }
        else {
            elvMain.setVisibility(View.VISIBLE);
           // cameraView.setVisibility(View.GONE);
            Log.d("toggle button", "open");
            onPause();
        }
    }
/*
    @Override
    protected void onResume() {
        super.onResume();
/*        camera.startPreview();
        try {
            camera.setPreviewDisplay(holder);
        } catch (IOException e) {
            e.printStackTrace();
        }
        //camera = Camera.open(CAMERA_ID);
        //setPreviewSize(FULL_SCREEN);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (camera != null)
            camera.release();
        camera = null;
    }

    class HolderCallback implements SurfaceHolder.Callback {

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            try {
                camera.setPreviewDisplay(holder);
                camera.startPreview();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width,
                                   int height) {
            camera.stopPreview();
            setCameraDisplayOrientation(CAMERA_ID);
            try {
                camera.setPreviewDisplay(holder);
                camera.startPreview();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {

        }
    }

    void setPreviewSize(boolean fullScreen) {

        // получаем размеры экрана
        View display = findViewById(R.id.cameraView);
        //Display display = getWindowManager().getDefaultDisplay();
        boolean widthIsMax = display.getWidth() > display.getHeight();

        // определяем размеры превью камеры
        float sizeW = camera.getParameters().getPreviewSize().width;
        float sizeH = camera.getParameters().getPreviewSize().height;

        RectF rectDisplay = new RectF();
        RectF rectPreview = new RectF();

        // RectF экрана, соотвествует размерам экрана
        rectDisplay.set(0, 0, display.getWidth(), display.getHeight());

        // RectF первью
        if (widthIsMax) {
            // превью в горизонтальной ориентации
            rectPreview.set(0, 0, sizeW, sizeH);
        } else {
            // превью в вертикальной ориентации
            rectPreview.set(0, 0, sizeH, sizeW);
        }

        Matrix matrix = new Matrix();
        // подготовка матрицы преобразования
        if (!fullScreen) {
            // если превью будет "втиснут" в экран (второй вариант из урока)
            matrix.setRectToRect(rectPreview, rectDisplay,
                    Matrix.ScaleToFit.START);
        } else {
            // если экран будет "втиснут" в превью (третий вариант из урока)
            matrix.setRectToRect(rectDisplay, rectPreview,
                    Matrix.ScaleToFit.START);
            matrix.invert(matrix);
        }
        // преобразование
        matrix.mapRect(rectPreview);

        // установка размеров surface из получившегося преобразования
        cameraView.getLayoutParams().height = (int) (rectPreview.bottom);
        cameraView.getLayoutParams().width = (int) (rectPreview.right);
    }

    void setCameraDisplayOrientation(int cameraId) {
        // определяем насколько повернут экран от нормального положения
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }

        int result = 0;

        // получаем инфо по камере cameraId
        CameraInfo info = new CameraInfo();
        Camera.getCameraInfo(cameraId, info);

        // задняя камера
        if (info.facing == CameraInfo.CAMERA_FACING_BACK) {
            result = ((360 - degrees) + info.orientation);
        } else
            // передняя камера
            if (info.facing == CameraInfo.CAMERA_FACING_FRONT) {
                result = ((360 - degrees) - info.orientation);
                result += 360;
            }
        result = result % 360;
        camera.setDisplayOrientation(result);

    }
    */
}

