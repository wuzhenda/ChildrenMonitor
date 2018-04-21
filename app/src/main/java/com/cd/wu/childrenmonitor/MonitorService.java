package com.cd.wu.childrenmonitor;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import static android.view.ViewGroup.LayoutParams.FILL_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import static android.view.WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED;
import static android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
import static android.view.WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
import static android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
import static android.view.WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
import static android.view.WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
import static android.view.WindowManager.LayoutParams.TYPE_TOAST;

/**
 * Created by wu on 18-4-21.
 */

public class MonitorService extends Service {

    private static final String TAG = "MonitorService";

    static final boolean DEBUG = true;

    public static final int MSG_OPEN_APPS_PANEL = 1020;
    public static final int MSG_CLOSE_APPS_PANEL = 1021;

    private final BroadcastReceiver mApplicationsReceiver = new ApplicationsIntentReceiver();
    private final BroadcastReceiver mScreenOffReceiver = new ScreenOffReceiver();

    private WindowManager mWindowManager = null;

    MonitorView mMonitorView = null;

    LayoutInflater mInflater = null;

    boolean mIsViewAdded = false;

    boolean mIsMonitorRegistered = false;

    H mHandler = new H();

    public class TouchOutsideListener implements View.OnTouchListener {
        private int mMsg;
        private MonitorView mLauncherView;

        public TouchOutsideListener(int msg, MonitorView panel) {
            mMsg = msg;
            mLauncherView = panel;
        }

        public boolean onTouch(View v, MotionEvent ev) {
            final int action = ev.getAction();
            if (action == MotionEvent.ACTION_OUTSIDE
                    || (action == MotionEvent.ACTION_DOWN
                    )) {
                //if we get outside touch, we hide the applist view.
                mHandler.removeMessages(mMsg);
                mHandler.sendEmptyMessage(mMsg);
                return true;
            }
            return false;
        }
    }

    private class H extends Handler {
        public void handleMessage(Message m) {
            //switch view, show or hide.
            switch (m.what) {
                case MSG_OPEN_APPS_PANEL:
                    if (DEBUG) Log.d(TAG, "opening MonitorView");
                    if (mMonitorView != null) {
                        mMonitorView.show(true, true);
                    }
                    break;
                case MSG_CLOSE_APPS_PANEL:
                    if (DEBUG) Log.d(TAG, "closing MonitorView");
                    if (mMonitorView != null && mMonitorView.isShowing()) {
                        mMonitorView.show(false, true);
                    }
                    break;
            }
        }
    }

    public void animateCollapse() {

        mHandler.removeMessages(MSG_CLOSE_APPS_PANEL);
        mHandler.sendEmptyMessage(MSG_CLOSE_APPS_PANEL);

        int faultCnt=mMonitorView.getFaultCnt();
        if(faultCnt>2){
            mHandler.sendEmptyMessageDelayed(MSG_OPEN_APPS_PANEL,10*1000);
        }else{
            mHandler.sendEmptyMessageDelayed(MSG_OPEN_APPS_PANEL,5*60*1000);
        }
        if (DEBUG) Log.d(TAG, "animateCollapse "+faultCnt);
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        //TODO:
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        openMonitor();

        mWindowManager = (WindowManager)getApplicationContext().getSystemService( Context.WINDOW_SERVICE);

        mInflater = LayoutInflater.from(getApplicationContext());
        mMonitorView = (MonitorView)mInflater.inflate(R.layout.monitor_layout, null, false);
        //mMonitorView.setVisibility(View.GONE);
        mMonitorView.setOnTouchListener(new TouchOutsideListener(MSG_CLOSE_APPS_PANEL, mMonitorView));


        int width = 1000;//(int)getResources().getDimension(R.dimen.grid_width);
        int height =1000;// (int)getResources().getDimension(R.dimen.grid_height);

        if (DEBUG)
            Log.i(TAG,"onCreate(), width = " + width + ", height=" +height);

        WindowManager.LayoutParams wmParams;

        wmParams = new WindowManager.LayoutParams();
        wmParams.width = FILL_PARENT;//WRAP_CONTENT;
        wmParams.height = FILL_PARENT;
        wmParams.gravity = Gravity.TOP | Gravity.START;
        wmParams.x = 50;//Resources.getSystem().getDisplayMetrics().widthPixels - mVideoViewWidth - dp12;
        wmParams.y = 50;//Resources.getSystem().getDisplayMetrics().heightPixels - mVideoViewHeight - dp12 - mStatusBarHeight - mNavigationBarHeight;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            wmParams.type = TYPE_APPLICATION_OVERLAY;
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            wmParams.type = TYPE_TOAST;
        } else {
            wmParams.type = TYPE_SYSTEM_ALERT;
        }

        //wmParams.flags = FLAG_NOT_FOCUSABLE | FLAG_NOT_TOUCH_MODAL | FLAG_HARDWARE_ACCELERATED | FLAG_LAYOUT_NO_LIMITS;
        wmParams.flags = FLAG_NOT_TOUCH_MODAL;
        wmParams.format = PixelFormat.RGBA_8888;
        wmParams.windowAnimations = android.R.style.Animation_Translucent;


//        wmParams.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_STATE_UNCHANGED
//                | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING;

        wmParams.setTitle("输入结果");

        if (mMonitorView != null) {
            mWindowManager.addView(mMonitorView, wmParams);
            mMonitorView.setLauncherService(this);

            mIsViewAdded = true;
        }else{

        }

        mHandler.removeMessages(MSG_OPEN_APPS_PANEL);
        mHandler.sendEmptyMessageDelayed(MSG_OPEN_APPS_PANEL,1*1000);
        if (DEBUG) Log.d(TAG, "Runnable 1 seconds");
    }

    @Override
    public void onDestroy() {

        closeMonitor();

        removeView();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent == null) {
            if (DEBUG)
                Log.i(TAG, "onStartCommand(), intent == null!");
            return START_STICKY;
        }

        final String action = intent.getAction();

        if (DEBUG)
            Log.i(TAG, "onStartCommand(), action = " + action + ", startId = " + startId);

        if (action.equals("com.cd.wu.childrenmonitor.INIT_ACTION")) {

        } else if (action.equals("com.cd.wu.childrenmonitor.START_ACTION")) {
            int msg = (mMonitorView.getVisibility() == View.VISIBLE)
                    ? MSG_CLOSE_APPS_PANEL : MSG_OPEN_APPS_PANEL;
            mHandler.removeMessages(msg);
            mHandler.sendEmptyMessage(msg);
        } else if (action.equals("com.cd.wu.childrenmonitor.STOP_ACTION")) {
            //back or home key ?
            int msg = MSG_CLOSE_APPS_PANEL;
            mHandler.removeMessages(msg);
            mHandler.sendEmptyMessage(msg);
        }

        return START_STICKY;
    }

    private synchronized void removeView() {

        if (DEBUG)
            Log.i(TAG, "removeView(), mIsViewAdded = " + mIsViewAdded);

        if ((mMonitorView != null) && (mIsViewAdded != false)) {

            mWindowManager.removeView(mMonitorView);
        }

        mIsViewAdded = false;
    }

    public void openMonitor() {

        if (DEBUG)
            Log.i(TAG, "openMonitor(), mIsMonitorRegistered = " + mIsMonitorRegistered);

        if (mIsMonitorRegistered == false) {
            //package monitor
            IntentFilter filter = new IntentFilter(Intent.ACTION_PACKAGE_ADDED);
            filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
            filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
            filter.addDataScheme("package");
            registerReceiver(mApplicationsReceiver, filter);

            //screen off monitor
            filter = new IntentFilter();
            filter.addAction(Intent.ACTION_SCREEN_OFF);
            registerReceiver(mScreenOffReceiver, filter);

            mIsMonitorRegistered = true;
        }
    }

    public void closeMonitor() {

        if (DEBUG)
            Log.i(TAG, "closeMonitor(), mIsMonitorRegistered = " + mIsMonitorRegistered);

        if (mIsMonitorRegistered != false) {
            unregisterReceiver(mApplicationsReceiver);
            unregisterReceiver(mScreenOffReceiver);

            mIsMonitorRegistered = false;
        }
    }

    public class ScreenOffReceiver extends BroadcastReceiver {
        private static final String TAG = "ScreenOffReceiver";

        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action.equals(Intent.ACTION_SCREEN_OFF)) {
                if (DEBUG)
                    Log.i(TAG, "screen off, hide view!");

                animateCollapse();
            }
        }
    }

    /**
     * Receives notifications when applications are added/removed.
     */
    private class ApplicationsIntentReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
           // mMonitorView.refreshApplicationsList();
        }
    }


}
