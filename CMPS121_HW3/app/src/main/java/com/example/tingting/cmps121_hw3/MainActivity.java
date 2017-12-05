package com.example.tingting.cmps121_hw3;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Rect;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import com.example.tingting.cmps121_hw3.MyService.MyBinder;


public class MainActivity extends AppCompatActivity implements View.OnClickListener,
        com.example.tingting.cmps121_hw3.MyServiceTask.ResultCallback{

    public Button exitButton;
    public Button clearButton;
    public TextView textView;

    public static final int DISPLAY_NUMBER = 10;
    private Handler mUiHandler;
    private SurfaceView mSurfaceView;
    private SurfaceHolder mSurfaceHolder;
    private Rect mSurfaceSize;

    private static final String LOG_TAG = "MainActivity";

    // Service connection variables.
    private boolean serviceBound;
    private MyService myService;


    public void initLayout()
    {
        exitButton = findViewById(R.id.exit);
        clearButton = findViewById(R.id.clear);
        textView = findViewById(R.id.text);

        exitButton.setOnClickListener(this);
        clearButton.setOnClickListener(this);

        mUiHandler = new Handler(getMainLooper(), new UiCallback());
        serviceBound = false;

        // Prevents the screen from dimming and going to sleep.
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initLayout();
        Intent intent = new Intent(this, MyService.class);
        startService(intent);
        bindMyService();
    }

    @Override
    public void onClick(View view)
    {
        switch (view.getId()){
            case R.id.clear:
                // clear page
                Log.i("MyService", "clear button pressed");
                textView.setText("");
                myService.resetData();
                break;

            case R.id.exit:
                // stop task, service then activity
                //Unbinds the service
                Log.i("MyService", "Unbinding");
                if (serviceBound == true){
                    unbindService(serviceConnection);
                    serviceBound = false;
                }
                // Stops the service.
                Log.i(LOG_TAG, "Stopping.");
                Intent intent = new Intent(this, MyService.class);
                stopService(intent);
                Log.i(LOG_TAG, "Stopped.");

                // stops activity
                finish();
                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Starts the service, so that the service will only stop when explicitly stopped.
//        Intent intent = new Intent(this, MyService.class);
//        startService(intent);
//        bindMyService();
//        Log.i(LOG_TAG, "onResume()");

        TextView tv = findViewById(R.id.text);
        if(myService != null){
            if (myService.didItMove() == true){
                Log.i(LOG_TAG, "MOVED");
                tv.setText("The phone moved!");
            }
            else{
                Log.i(LOG_TAG, "NOT MOVED YET");
                tv.setText("Everything was quiet");
            }
        }
    }

    private void bindMyService() {
        Log.i(LOG_TAG, "Starting the service");
        Intent intent = new Intent(this, MyService.class);
        Log.i("LOG_TAG", "Trying to bind");
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }


    // Service connection code.
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder serviceBinder) {
            // We have bound to the camera service.
            MyBinder binder = (MyBinder) serviceBinder;
            myService = binder.getService();
            serviceBound = true;
            // Let's connect the callbacks.
            Log.i("MyService", "Bound succeeded, adding the callback");
            myService.addResultCallback(MainActivity.this);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            serviceBound = false;
        }
    };

    @Override
    protected void onPause() {
        if (serviceBound) {
            if (myService != null) {
                myService.removeResultCallback(this);
            }
            Log.i("MyService", "Unbinding");
            if (serviceBound == true){
                unbindService(serviceConnection);
                serviceBound = false;
            }
        }
        super.onPause();
    }

    /**
     * This function is called from the service thread.  To process this, we need
     * to create a message for a handler in the UI thread.
     */
    @Override
    public void onResultReady(ServiceResult result) {
        if (result != null) {
            Log.i(LOG_TAG, "Preparing a message for " + result.booleanValue);
        } else {
            Log.e(LOG_TAG, "Received an empty result!");
        }
        mUiHandler.obtainMessage(DISPLAY_NUMBER, result).sendToTarget();
    }

    /**
     * This Handler callback gets the message generated above.
     * It is used to display the integer on the screen.
     */
    private class UiCallback implements Handler.Callback {
        @Override
        public boolean handleMessage(Message message) {
            if (message.what == DISPLAY_NUMBER) {
                // Gets the result.
                ServiceResult result = (ServiceResult) message.obj;
                // Displays it.
                if (result != null) {
                    if (result.booleanValue){
                        Log.i(LOG_TAG, "MOVED");
                        textView.setText("The phone moved!");
                    }
                    else{
                        Log.i(LOG_TAG, "NOT MOVED YET");
                        textView.setText("Everything was quiet");
                    }
                }
                Log.i(LOG_TAG, "Displaying: " + result.booleanValue);
                if (serviceBound && myService != null) {
                    Log.i(LOG_TAG, "Releasing result holder for " + result.booleanValue);
                    myService.releaseResult(result);
                }
            } else {
                Log.e(LOG_TAG, "Error: received empty message!");
            }
            return true;
        }
    }
}
