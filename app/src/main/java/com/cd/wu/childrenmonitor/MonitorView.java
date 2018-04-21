package com.cd.wu.childrenmonitor;

import android.animation.Animator;
import android.animation.LayoutTransition;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ApplicationInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.PaintDrawable;
import android.support.v7.app.AlertDialog;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Choreographer;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by wu on 18-4-21.
 */

public class MonitorView extends LinearLayout implements
         View.OnTouchListener {
    static final String TAG = "MonitorView";
    static final boolean DEBUG = false;
    private Context mContext;
    private TextView tv_left,tv_right;
    private Button bt_result1,bt_result2,bt_result3,bt_result4,bt_result5,bt_result6,bt_result7,bt_result8,bt_result9;
    Button[] buttons;

    private boolean mShowing;


    private Runnable mPreloadTasksRunnable;
    private boolean mApplicationsDirty = true;

    private MonitorService mMonitorService = null;

    boolean mClicked = false;
    private int val1=(int)(Math.random()*20%20);
    private int val2=(int)(Math.random()*20%20);

    private int faultCnt=0;

    public  int getFaultCnt(){
        return faultCnt;
    }

    public void setLauncherService(MonitorService service) {
        mMonitorService = service;
    }

    public boolean isShowing() {
        return mShowing;
    }
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && !event.isCanceled()) {
            show(false, true);
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }


    public void show(boolean show, boolean animate) {
        if (show) {
            mClicked = false;
        } else {
            mClicked = true;
        }
        show(show, animate, null);
    }

    public void show(boolean show, boolean animate,
                     ArrayList<ApplicationInfo> recentTaskDescriptions) {
        if (show) {
            // Need to update list of recent apps before we set visibility so this view's
            // content description is updated before it gets focus for TalkBack mode

        } else {
            // Need to set recent tasks to dirty so that next time we load, we
            // refresh the list of tasks
            mApplicationsDirty = true;
        }
        if (animate) {
            if (mShowing != show) {
                mShowing = show;
                if (show) {
                    setVisibility(View.VISIBLE);
                }

            }
        } else {
            mShowing = show;
            setVisibility(show ? View.VISIBLE : View.GONE);

        }
        if (show) {
            setFocusable(true);
            setFocusableInTouchMode(true);
            requestFocus();
        }
    }

    public void hide(boolean animate) {

        mClicked = false;

        if (!animate) {
            setVisibility(View.GONE);
        }

        if (mMonitorService != null) {
            mMonitorService.animateCollapse();
        }
    }


    /**
     * Whether the panel is showing, or, if it's animating, whether it will be
     * when the animation is done.
     */
    public MonitorView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MonitorView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
        updateValuesFromResources();
    }

    public void updateValuesFromResources() {
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        tv_left = (TextView) findViewById(R.id.tv_left);
        tv_right= (TextView) findViewById(R.id.tv_right);

        bt_result1= (Button) findViewById(R.id.bt_result1);
        bt_result2= (Button) findViewById(R.id.bt_result2);
        bt_result3= (Button) findViewById(R.id.bt_result3);
        bt_result4= (Button) findViewById(R.id.bt_result4);
        bt_result5= (Button) findViewById(R.id.bt_result5);
        bt_result6= (Button) findViewById(R.id.bt_result6);
        bt_result7= (Button) findViewById(R.id.bt_result7);
        bt_result8= (Button) findViewById(R.id.bt_result8);
        bt_result9= (Button) findViewById(R.id.bt_result9);

        buttons=new Button[]{bt_result1,bt_result2, bt_result3,
                bt_result4, bt_result5,bt_result6,
                bt_result7, bt_result8,bt_result9};

        InitVal();
    }

    private  void InitVal(){
        val1=(int)(Math.random()*20%20);
        val2=(int)(Math.random()*20%20);

        tv_left.setText(""+val1+"+"+val2+"=");

        final int valResult=val1+val2;
        int num=(int)((Math.random()*9)%9);
        for(int i=0;i<9;i++){
            if(i==num){
                buttons[i].setText(""+valResult);
            }else{
                int tmp=(int)(Math.random()*20%20);
                if(tmp==valResult){
                    tmp+=1;
                }
                buttons[i].setText(""+tmp);
            }

            buttons[i].setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    Button bt=(Button)view;
                    String valResult2=bt.getText().toString();

                    tv_right.setText(bt.getText());
                    int valInt2=0;
                    try {
                        valInt2 = Integer.parseInt(valResult2);
                    }catch (Exception e){

                    }
                    if(valResult==valInt2){
                        tv_right.setTextColor(Color.BLUE);
                        hide(false);
                    }else{
                        tv_right.setTextColor(Color.RED);
                        //错误次数加1
                        faultCnt++;
                    }
                }
            });
        }
    }

    private void createCustomAnimations(LayoutTransition transitioner) {
        transitioner.setDuration(200);
        transitioner.setStartDelay(LayoutTransition.CHANGE_DISAPPEARING, 0);
        transitioner.setAnimator(LayoutTransition.DISAPPEARING, null);
    }

    @Override
    protected void onVisibilityChanged(View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        if (DEBUG)
            Log.v(TAG, "onVisibilityChanged(" + changedView + ", " + visibility + ")");

        if(visibility==VISIBLE) {
            faultCnt = 0;
            tv_right.setText("?");
        }
        InitVal();
    }

    // additional optimization when we have sofware system buttons - start loading the recent
    // tasks on touch down
    @Override
    public boolean onTouch(View v, MotionEvent ev) {
        if (!mShowing) {
            int action = ev.getAction() & MotionEvent.ACTION_MASK;
            if (action == MotionEvent.ACTION_DOWN) {
                // If we set our visibility to INVISIBLE here, we avoid an extra call to
                // onLayout later when we become visible (because onLayout is always called
                // when going from GONE)
                post(mPreloadTasksRunnable);
            } else if (action == MotionEvent.ACTION_CANCEL) {
                setVisibility(GONE);

                // Remove the preloader if we haven't called it yet
                removeCallbacks(mPreloadTasksRunnable);
            } else if (action == MotionEvent.ACTION_UP) {
                // Remove the preloader if we haven't called it yet
                removeCallbacks(mPreloadTasksRunnable);
                if (!v.isPressed()) {
                    setVisibility(GONE);
                }
            }
        }
        return false;
    }



}