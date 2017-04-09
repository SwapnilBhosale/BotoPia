package gslab.com.myapplication;

import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.DragEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.IOException;

public class CallAnswerReject extends AppCompatActivity implements View.OnTouchListener, View.OnDragListener{

    private static final String TAG = CallAnswerReject.class.getSimpleName();

    private MediaPlayer mediaPlayer;
    private FrameLayout startFrame, endFrame;
    private ImageView accept, reject, calling;
    private Vibrator vibrator;
    private int callId;
    private TextView callingPartName;
    String callingParty;
    private long[] VIBRATION_PATTERN = {0, 100, 300};


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        launchScreenEvenIfPhoneIsLocked();
        setContentView(R.layout.activity_call_answer_reject);
        //sdkManagerInstance = SDKManager.getInstance(this);

            callingParty = getIntent().getStringExtra("callingParty");
            Log.d(TAG, "onCreate: Calling Party : "+callingParty);

        initVars();

        initVibrator();
        startVibratingPhone();


    }

    private void startVibratingPhone() {
        vibrator.vibrate(VIBRATION_PATTERN, 0);
    }

    private void initVibrator() {
        vibrator = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);
    }

    private void initVars(){
        accept = (ImageView) findViewById(R.id.accept_incoming_call);
        reject = (ImageView) findViewById(R.id.reject_incoming_call);
        calling = (ImageView) findViewById(R.id.calling);

        startFrame = (FrameLayout) findViewById(R.id.start_call_frame);
        endFrame = (FrameLayout) findViewById(R.id.end_call_frame);

        callingPartName = (TextView) findViewById(R.id.calling_party_name);
        callingPartName.setText(callingParty+" Calling");
        //set ontouch listener for calling image
        calling.setOnTouchListener(this);

        //set ondrag listener for Accept and Reject Frame views
        startFrame.setOnDragListener(this);
        endFrame.setOnDragListener(this);

        // animate accept-reject
        shakeImage(accept);
        shakeImage(reject);
    }

    public void shakeImage(ImageView image) {
        Animation shake = AnimationUtils.loadAnimation(this, R.anim.shake);
        image.startAnimation(shake);
    }

    private void launchScreenEvenIfPhoneIsLocked() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                + WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                + WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                + WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
    }

    @Override
    public boolean onDrag(View v, DragEvent event) {
        //we want to make sure it is dropped only to left and right parent view
        Log.d(TAG, "onDrag: event.getAction()=" + event.getAction());
        switch (event.getAction()) {
            case DragEvent.ACTION_DROP:
                if (v.getId() == R.id.start_call_frame) {
                    startFrame.setVisibility(View.VISIBLE);
                    vibratePhone();
                    accept();
                } else if (v.getId() == R.id.end_call_frame) {
                    endFrame.setVisibility(View.VISIBLE);
                    vibratePhone();
                    reject();
                }
                View view = (View) event.getLocalState();
                view.setVisibility(View.VISIBLE);
                break;
            case DragEvent.ACTION_DRAG_ENDED:
                calling.post(new Runnable() {
                    @Override
                    public void run() {
                        calling.setVisibility(View.VISIBLE);
                    }
                });
                break;
        }
        return true;
    }

    private void reject() {
       // CallWrapper callWrapper = sdkManagerInstance.getCallWrapperByCallId(callId);
        //callWrapper.getCall().end();
        stopVibrating();
        stopIncomingCallRingtone();
        finish();
        MainActivity.isWaitingCall = false;
    }

    private void accept() {
        stopVibrating();
        stopIncomingCallRingtone();
        MainActivity.isWaitingCall = true;
        //startActiveCallScreen();
        finish();
    }

    private void stopVibrating() {
        if (vibrator != null) {
            vibrator.cancel();
        }
    }



    @Override
    public boolean onTouch(View view, MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            View.DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(view);
            view.startDrag(null, shadowBuilder, view, 0);
            view.setVisibility(View.INVISIBLE);

            // show accept-reject
            startFrame.setVisibility(View.VISIBLE);
            endFrame.setVisibility(View.VISIBLE);
            return true;
        }
        return false;
    }

    @Override
    protected void onStart() {
        super.onStart();
        startIncomingCallAudio();
    }

    private void vibratePhone() {
        vibrator.vibrate(100);
    }


    private void stopIncomingCallRingtone() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.reset();
            mediaPlayer = null;
        }
    }

    private void startIncomingCallAudio() {
        if (mediaPlayer != null && mediaPlayer.isPlaying())
            return;
        Uri alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
        mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(this, alert);
            mediaPlayer.prepare();
        } catch (IOException e) {
            Log.e(TAG, "startIncomingCallAudio: ", e);
            return;
        }
        mediaPlayer.setLooping(true);
        mediaPlayer.start();
    }

    private void startActiveCallScreen() {
        /*Intent intent = new Intent(this,ActiveCallActivity.class);
        intent.putExtra(SDKManager.CALL_ID, callId);
        // Don't add video frame. Let's add it once video channel update received.
        intent.putExtra(SDKManager.IS_VIDEO_CALL, false);
        startActivity(intent);
        sdkManagerInstance.startCall(sdkManagerInstance.getCallWrapperByCallId(callId));*/
    }

}
