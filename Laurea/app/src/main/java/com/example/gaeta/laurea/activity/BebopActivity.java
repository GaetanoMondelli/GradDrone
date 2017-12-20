package com.example.gaeta.laurea.activity;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.example.gaeta.laurea.R;
import com.example.gaeta.laurea.drone.BebopDrone;
import com.example.gaeta.laurea.view.H264VideoView;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;
import com.parrot.arsdk.arcommands.ARCOMMANDS_ARDRONE3_MEDIARECORDEVENT_PICTUREEVENTCHANGED_ERROR_ENUM;
import com.parrot.arsdk.arcommands.ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM;
import com.parrot.arsdk.arcontroller.ARCONTROLLER_DEVICE_STATE_ENUM;
import com.parrot.arsdk.arcontroller.ARControllerCodec;
import com.parrot.arsdk.arcontroller.ARFrame;
import com.parrot.arsdk.ardiscovery.ARDiscoveryDeviceService;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.locks.ReentrantLock;



public class BebopActivity extends AppCompatActivity {
    private static final String TAG = "BebopActivity";
    private BebopDrone mBebopDrone;

    private ProgressDialog mConnectionProgressDialog;
    private ProgressDialog mDownloadProgressDialog;

    private H264VideoView mVideoView;

    private TextView mBatteryLabel;
    private Button mTakeOffLandBt;
    private Button mDownloadBt;

    private int mNbMaxDownload;
    private int mCurrentDownloadIndex;

    private ReentrantLock BarLock;
    private BarcodeDetector barcodeDetector;
    Frame.Builder frameb = new Frame.Builder();
    SparseArray<Barcode> barcodes = new SparseArray<Barcode>(); +

    private double CENTRE_X = 1280/2;
    private double CENTRE_Y = 576/2;
    private double EPSILON = 30.0;

    private double barcode_x = -1.0;
    private double barcode_y = -1.0;
    private double barcode_area = -1.0;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.BarLock = new ReentrantLock();
        setContentView(R.layout.activity_bebop);
        barcodeDetector = new BarcodeDetector.Builder(getApplicationContext()).build();
        initIHM();

        Intent intent = getIntent();
        ARDiscoveryDeviceService service = intent.getParcelableExtra(DeviceListActivity.EXTRA_DEVICE_SERVICE);
        mBebopDrone = new BebopDrone(this, service);
        mBebopDrone.addListener(mBebopListener);




    }

    @Override
    protected void onStart() {
        super.onStart();

        // show a loading view while the bebop drone is connecting
        if ((mBebopDrone != null) && !(ARCONTROLLER_DEVICE_STATE_ENUM.ARCONTROLLER_DEVICE_STATE_RUNNING.equals(mBebopDrone.getConnectionState())))
        {
            mConnectionProgressDialog = new ProgressDialog(this, R.style.AppCompatAlertDialogStyle);
            mConnectionProgressDialog.setIndeterminate(true);
            mConnectionProgressDialog.setMessage("Connecting ...");
            mConnectionProgressDialog.setCancelable(false);
            mConnectionProgressDialog.show();

            // if the connection to the Bebop fails, finish the activity
            if (!mBebopDrone.connect()) {
                finish();
            }
        }
        //mBebopDrone.setMaxAltitude((float)1.0);

        new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Bitmap b = mVideoView.getBitmap();
                if(b==null) {
                    //Log.e(TAG, "Immagine nulla");
                    return;
                }
               // Log.e(TAG, "Processata una immagine"+ android.os.Process.myTid());
                Frame framegcm = frameb.setBitmap(b).build();
                barcodes = barcodeDetector.detect(framegcm);


                String text= "";
                //Log.e(TAG, "Barcode" + barcodes.size()+ b.getWidth());




                if (barcodes.size() > 0) {
                    Point[] points = barcodes.valueAt(0).cornerPoints;
                    int width = points[1].x-points[0].x;
                    int height = points[3].y-points[0].y;
                    barcode_area = width*height/2;
                    barcode_x = (points[1].x+points[0].x)/2;
                    barcode_y = (points[3].y+points[0].y)/2;
                    text =  barcodes.valueAt(0).displayValue;
                    Log.e(TAG, "Trovato un barcode = " + barcodes.valueAt(0).displayValue+"cx,cy: "+barcode_x+","+barcode_y+" "+barcode_area);
                    Log.e(TAG, "Bitmap = " + b.getWidth()+b.getHeight());
                }
                else{
                    barcode_area = -1.0;
                    barcode_x = -1.0;
                    barcode_y = -1.0;
                }
                autoPilot(barcode_x,barcode_y,barcode_area,text);
            }
        }, 0, 500);
    }



    public void driftLeft()
    {

        mBebopDrone.setRoll((byte) -50);
        mBebopDrone.setFlag((byte) 1);
        try {
            //Log.i(TAG, "Moving Left");
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        mBebopDrone.setRoll((byte) 0);
        mBebopDrone.setFlag((byte) 0);
    }

    public void driftForward()
    {

        mBebopDrone.setPitch((byte) 50);
        mBebopDrone.setFlag((byte) 1);
        try {
            //Log.i(TAG, "Moving Forward");
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        mBebopDrone.setPitch((byte) 0);
        mBebopDrone.setFlag((byte) 0);
    }

    public void driftBackward()
    {

        mBebopDrone.setPitch((byte) -50);
        mBebopDrone.setFlag((byte) 1);
        try {
            //Log.i(TAG, "Moving Back");
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        mBebopDrone.setPitch((byte) 0);
        mBebopDrone.setFlag((byte) 0);
    }

    public void driftRight()
    {
        mBebopDrone.setRoll((byte) 50);
        mBebopDrone.setFlag((byte) 1);
        try {
            Log.i(TAG, "Moving Right");
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        mBebopDrone.setRoll((byte) 0);
        mBebopDrone.setFlag((byte) 0);
    }



    public void autoPilot(double cx, double cy, double area, String barcode){
        Log.e(TAG, "AUTOPILOT"+cx+" "+cy+" "+area);
        if(cx < 0 || cy < 0 || area <0 ){
            return;}
        if(cx < CENTRE_X - EPSILON){
            //move_left
            Log.e(TAG, "MOVE: LEFT");
            driftLeft();
        }
        if(cx > CENTRE_X + EPSILON) {
            //move_right
            Log.e(TAG, "MOVE: RIGHT");
            driftRight();
        }
        if(cy < CENTRE_Y - (EPSILON-10)){
            //move_forward
            Log.e(TAG, "MOVE: FORWARD "+cy+">"+CENTRE_Y);
            driftForward();
        }
        if(cy > CENTRE_Y + (EPSILON-10) ) {
            //move_backward
            Log.e(TAG, "MOVE: BACKWARD "+cy+"<"+CENTRE_Y);
            driftBackward();
        }

        return;


    }

    @Override
    public void onBackPressed() {
        if (mBebopDrone != null)
        {
            mConnectionProgressDialog = new ProgressDialog(this, R.style.AppCompatAlertDialogStyle);
            mConnectionProgressDialog.setIndeterminate(true);
            mConnectionProgressDialog.setMessage("Disconnecting ...");
            mConnectionProgressDialog.setCancelable(false);
            mConnectionProgressDialog.show();

            if (!mBebopDrone.disconnect()) {
                finish();
            }
        }
    }

    @Override
    public void onDestroy()
    {
        mBebopDrone.dispose();
        super.onDestroy();
    }

    private void initIHM() {
        mVideoView = (H264VideoView) findViewById(R.id.videoView);

        findViewById(R.id.emergencyBt).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mBebopDrone.emergency();
            }
        });

        mTakeOffLandBt = (Button) findViewById(R.id.takeOffOrLandBt);
        mTakeOffLandBt.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                switch (mBebopDrone.getFlyingState()) {
                    case ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_LANDED:
                        mBebopDrone.takeOff();
                        break;
                    case ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_FLYING:
                    case ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_HOVERING:
                        mBebopDrone.land();
                        break;
                    default:
                }
            }
        });

        findViewById(R.id.takePictureBt).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mBebopDrone.takePicture();
            }
        });

        findViewById(R.id.test).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                driftRight();
            }
        });

        mDownloadBt = (Button)findViewById(R.id.downloadBt);
        mDownloadBt.setEnabled(false);
        mDownloadBt.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mBebopDrone.getLastFlightMedias();

                mDownloadProgressDialog = new ProgressDialog(BebopActivity.this, R.style.AppCompatAlertDialogStyle);
                mDownloadProgressDialog.setIndeterminate(true);
                mDownloadProgressDialog.setMessage("Fetching medias");
                mDownloadProgressDialog.setCancelable(false);
                mDownloadProgressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mBebopDrone.cancelGetLastFlightMedias();
                    }
                });
                mDownloadProgressDialog.show();
            }
        });

        findViewById(R.id.gazUpBt).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        v.setPressed(true);
                        mBebopDrone.setGaz((byte) 50);
                        break;

                    case MotionEvent.ACTION_UP:
                        v.setPressed(false);
                        mBebopDrone.setGaz((byte) 0);
                        break;

                    default:

                        break;
                }

                return true;
            }
        });

        findViewById(R.id.gazDownBt).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        v.setPressed(true);
                        mBebopDrone.setGaz((byte) -50);
                        break;

                    case MotionEvent.ACTION_UP:
                        v.setPressed(false);
                        mBebopDrone.setGaz((byte) 0);
                        break;

                    default:

                        break;
                }

                return true;
            }
        });

        findViewById(R.id.yawLeftBt).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        v.setPressed(true);
                        mBebopDrone.setYaw((byte) -50);
                        break;

                    case MotionEvent.ACTION_UP:
                        v.setPressed(false);
                        mBebopDrone.setYaw((byte) 0);
                        break;

                    default:

                        break;
                }

                return true;
            }
        });

        findViewById(R.id.yawRightBt).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        v.setPressed(true);
                        mBebopDrone.setYaw((byte) 50);
                        break;

                    case MotionEvent.ACTION_UP:
                        v.setPressed(false);
                        mBebopDrone.setYaw((byte) 0);
                        break;

                    default:

                        break;
                }

                return true;
            }
        });

        findViewById(R.id.forwardBt).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        v.setPressed(true);
                        mBebopDrone.setPitch((byte) 50);
                        mBebopDrone.setFlag((byte) 1);
                        break;

                    case MotionEvent.ACTION_UP:
                        v.setPressed(false);
                        mBebopDrone.setPitch((byte) 0);
                        mBebopDrone.setFlag((byte) 0);
                        break;

                    default:

                        break;
                }

                return true;
            }
        });

        findViewById(R.id.backBt).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        v.setPressed(true);
                        mBebopDrone.setPitch((byte) -50);
                        mBebopDrone.setFlag((byte) 1);
                        break;

                    case MotionEvent.ACTION_UP:
                        v.setPressed(false);
                        mBebopDrone.setPitch((byte) 0);
                        mBebopDrone.setFlag((byte) 0);
                        break;

                    default:

                        break;
                }

                return true;
            }
        });

        findViewById(R.id.tilt_forward).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                mBebopDrone.changeOrientation((byte)-180);
                return true;
            }
        });

        findViewById(R.id.tilt_backard).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                mBebopDrone.changeOrientation((byte)-100);
                /*mBebopDrone.setRoll((byte) -30); //cambia inclinazione per rallentarlo
                mBebopDrone.setFlag((byte) 1);
                try {
                    Log.i(TAG, "Moving Left");
                    Thread.sleep(50); // cambia hold!
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                mBebopDrone.setRoll((byte) 0);
                mBebopDrone.setFlag((byte) 0);
                //metti altro hold?*/
                return true;
            }
        });

        findViewById(R.id.rollLeftBt).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        v.setPressed(true);
                        mBebopDrone.setRoll((byte) -50);
                        mBebopDrone.setFlag((byte) 1);
                        break;

                    case MotionEvent.ACTION_UP:
                        v.setPressed(false);
                        mBebopDrone.setRoll((byte) 0);
                        mBebopDrone.setFlag((byte) 0);
                        break;

                    default:

                        break;
                }

                return true;
            }
        });

        findViewById(R.id.rollRightBt).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        v.setPressed(true);
                        mBebopDrone.setRoll((byte) 50);
                        mBebopDrone.setFlag((byte) 1);
                        break;

                    case MotionEvent.ACTION_UP:
                        v.setPressed(false);
                        mBebopDrone.setRoll((byte) 0);
                        mBebopDrone.setFlag((byte) 0);
                        break;

                    default:

                        break;
                }

                return true;
            }
        });

        mBatteryLabel = (TextView) findViewById(R.id.batteryLabel);
    }

    private final BebopDrone.Listener mBebopListener = new BebopDrone.Listener() {
        @Override
        public void onDroneConnectionChanged(ARCONTROLLER_DEVICE_STATE_ENUM state) {
            switch (state)
            {
                case ARCONTROLLER_DEVICE_STATE_RUNNING:
                    mConnectionProgressDialog.dismiss();
                    break;

                case ARCONTROLLER_DEVICE_STATE_STOPPED:
                    // if the deviceController is stopped, go back to the previous activity
                    mConnectionProgressDialog.dismiss();
                    finish();
                    break;

                default:
                    break;
            }
        }

        @Override
        public void onBatteryChargeChanged(int batteryPercentage) {
            mBatteryLabel.setText(String.format("%d%%", batteryPercentage));
        }

        @Override
        public void onPilotingStateChanged(ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM state) {
            switch (state) {
                case ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_LANDED:
                    mTakeOffLandBt.setText("Take off");
                    mTakeOffLandBt.setEnabled(true);
                    mDownloadBt.setEnabled(true);
                    break;
                case ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_FLYING:
                case ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_HOVERING:
                    mTakeOffLandBt.setText("Land");
                    mTakeOffLandBt.setEnabled(true);
                    mDownloadBt.setEnabled(false);
                    break;
                default:
                    mTakeOffLandBt.setEnabled(false);
                    mDownloadBt.setEnabled(false);
            }
        }

        @Override
        public void onPictureTaken(ARCOMMANDS_ARDRONE3_MEDIARECORDEVENT_PICTUREEVENTCHANGED_ERROR_ENUM error) {
            Log.i(TAG, "Picture has been taken");
        }

        @Override
        public void configureDecoder(ARControllerCodec codec) {
            mVideoView.configureDecoder(codec);
        }

        @Override
        public void onFrameReceived(ARFrame frame) {
            mVideoView.displayFrame(frame);
        }

        @Override
        public void onMatchingMediasFound(int nbMedias) {
            mDownloadProgressDialog.dismiss();

            mNbMaxDownload = nbMedias;
            mCurrentDownloadIndex = 1;

            if (nbMedias > 0) {
                mDownloadProgressDialog = new ProgressDialog(BebopActivity.this, R.style.AppCompatAlertDialogStyle);
                mDownloadProgressDialog.setIndeterminate(false);
                mDownloadProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                mDownloadProgressDialog.setMessage("Downloading medias");
                mDownloadProgressDialog.setMax(mNbMaxDownload * 100);
                mDownloadProgressDialog.setSecondaryProgress(mCurrentDownloadIndex * 100);
                mDownloadProgressDialog.setProgress(0);
                mDownloadProgressDialog.setCancelable(false);
                mDownloadProgressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mBebopDrone.cancelGetLastFlightMedias();
                    }
                });
                mDownloadProgressDialog.show();
            }
        }

        @Override
        public void onDownloadProgressed(String mediaName, int progress) {
            mDownloadProgressDialog.setProgress(((mCurrentDownloadIndex - 1) * 100) + progress);
        }

        @Override
        public void onDownloadComplete(String mediaName) {
            mCurrentDownloadIndex++;
            mDownloadProgressDialog.setSecondaryProgress(mCurrentDownloadIndex * 100);

            if (mCurrentDownloadIndex > mNbMaxDownload) {
                mDownloadProgressDialog.dismiss();
                mDownloadProgressDialog = null;
            }
        }
    };
}
