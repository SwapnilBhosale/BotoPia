package gslab.com.myapplication;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Point;
import android.opengl.GLSurfaceView;
import android.os.AsyncTask;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeechService;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.MediaStream;
import org.webrtc.VideoRenderer;
import org.webrtc.VideoRendererGui;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import ai.api.AIDataService;
import ai.api.AIListener;
import ai.api.AIServiceException;
import ai.api.android.AIConfiguration;
import ai.api.android.AIService;
import ai.api.model.AIContext;
import ai.api.model.AIError;
import ai.api.model.AIRequest;
import ai.api.model.AIResponse;
import ai.api.model.Result;
import ai.api.model.Status;
import gslab.com.myapplication.adapter.ChatAdapter;
import gslab.com.myapplication.api_ai_listener.ApiAiListener;
import gslab.com.myapplication.bean.ChatMessage;
import io.socket.client.IO;
import io.socket.client.Socket;

import static android.view.MotionEvent.ACTION_BUTTON_PRESS;
import static android.view.MotionEvent.ACTION_BUTTON_RELEASE;
import static android.view.MotionEvent.ACTION_DOWN;
import static android.view.MotionEvent.ACTION_UP;

public class MainActivity extends AppCompatActivity implements AIListener, WebRtcClient.RtcListener {


    private EditText messageET;
    private ListView messagesContainer;
    private ImageView sendBtn, micButton;
    private ChatAdapter adapter;
    private ArrayList<ChatMessage> chatHistory;
    private static int counter = 1;
    public static boolean isWaitingCall = false;
    public static String callingName = "";
    public static String from = "";
    public static JSONObject payload = null;
    TextToSpeech t1;

    private AIConfiguration config = new AIConfiguration(Config.CLIENT_ACCESS_TOKEN,
            AIConfiguration.SupportedLanguages.English,
            AIConfiguration.RecognitionEngine.System);
    private AIService aiService;
    private AIDataService aiDataService;
    private boolean isListening = false;

    private static final String TAG = MainActivity.class.getSimpleName();
    private final static int VIDEO_CALL_SENT = 666;
    private static final String VIDEO_CODEC_VP9 = "VP9";
    private static final String AUDIO_CODEC_OPUS = "opus";
    // Local preview screen position before call is connected.
    private static final int LOCAL_X_CONNECTING = 0;
    private static final int LOCAL_Y_CONNECTING = 0;
    private static final int LOCAL_WIDTH_CONNECTING = 100;
    private static final int LOCAL_HEIGHT_CONNECTING = 100;
    // Local preview screen position after call is connected.
    private static final int LOCAL_X_CONNECTED = 72;
    private static final int LOCAL_Y_CONNECTED = 72;
    private static final int LOCAL_WIDTH_CONNECTED = 25;
    private static final int LOCAL_HEIGHT_CONNECTED = 25;
    // Remote video screen position
    private static final int REMOTE_X = 0;
    private static final int REMOTE_Y = 0;
    private static final int REMOTE_WIDTH = 100;
    private static final int REMOTE_HEIGHT = 100;
    private VideoRendererGui.ScalingType scalingType = VideoRendererGui.ScalingType.SCALE_ASPECT_FILL;
    private GLSurfaceView vsv;
    private VideoRenderer.Callbacks localRender;
    private VideoRenderer.Callbacks remoteRender;
    private WebRtcClient client;
    private String mSocketAddress;
    private String callerId;
    private RelativeLayout videoArea,container;
    private boolean isCall = false;
    private CallEventsReceiver callEventReceiver;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        setContentView(R.layout.activity_main);


        initApiAi();
        initControls();
        // setupSignalingServer();


        //mSocketAddress = "http://35.162.15.232:3000";
        //mSocketAddress = "http://192.168.1.105:3000";
        mSocketAddress = Config.SIGNALING_SERVER_HOST;

        vsv = (GLSurfaceView) videoArea.findViewById(R.id.glview_call_main);
        Log.d(TAG, "onCreate: VSV : "+vsv);
        init();

        callEventReceiver = new CallEventsReceiver();
        try {

            Log.d(TAG, "onCreate: inside try");
            VideoRendererGui.setView(vsv, new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "run: calling init");
                    //init();
                }
            });

            vsv.setPreserveEGLContextOnPause(true);
            vsv.setKeepScreenOn(true);
            // local and remote render
            remoteRender = VideoRendererGui.create(
                    REMOTE_X, REMOTE_Y,
                    REMOTE_WIDTH, REMOTE_HEIGHT, scalingType, false);
            localRender = VideoRendererGui.create(
                    LOCAL_X_CONNECTING, LOCAL_Y_CONNECTING,
                    LOCAL_WIDTH_CONNECTING, LOCAL_HEIGHT_CONNECTING, scalingType, true);
        }catch (Exception e){
            e.printStackTrace();
            Log.e(TAG, "onCreate: ",e );
        }

        //this.registerReceiver(callEventReceiver,new IntentFilter(Config.CALL_EVENT_RECEIVER));



        /*final Intent intent = getIntent();
        final String action = intent.getAction();

        if (Intent.ACTION_VIEW.equals(action)) {
            final List<String> segments = intent.getData().getPathSegments();
            callerId = segments.get(0);
        }*/


    }


    @Override
    public void onBackPressed() {
        Log.d(TAG, "onBackPressed: ");
        if(isCall) {
            isCall = false;
            container.setVisibility(View.VISIBLE);
            videoArea.setVisibility(View.GONE);
            isCall = false;
            client.onDestroy();
            init();
        }else{
            openAlertDialogue();
        }
        isWaitingCall = false;
        return;
    }

    public void openLoginActivity(){
        Intent intent = new Intent(this,LoginActivity.class);
        finish();
        startActivity(intent);
    }

    public void logoutUser(){
        Log.d(TAG, "logoutUser: ");
        try{
            JSONObject obj = new JSONObject();
            final PrefManager prefManager = new PrefManager(getApplicationContext());
            obj.put("sessionId",prefManager.getSessionKey());
            JsonObjectRequest req = new JsonObjectRequest(Request.Method.POST, Config.URL_LOGOUT, obj, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    Log.d(TAG, "onResponse: "+response);
                    prefManager.setLoggedIn(false);
                    prefManager.setName("");
                    prefManager.setEmail("");
                    prefManager.setSessionKey("");
                    openLoginActivity();
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.e(TAG, "onErrorResponse: ",error );
                }
            });
            Volley.newRequestQueue(getApplicationContext()).add(req);
        }catch(Exception e){
            e.printStackTrace();
        }

    }

    public void openAlertDialogue(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Logout");
        builder.setMessage("Do you really want to logout?");

        builder.setPositiveButton("YES", new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int which) {
                // Do nothing but close the dialog

                logoutUser();

            }
        });

        builder.setNegativeButton("NO", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {

                // Do nothing
                dialog.dismiss();
            }
        });

        AlertDialog alert = builder.create();
        alert.show();
    }


    public void setScreenAttribs(){
        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN
                        | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                        | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                        | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
    }

    private void init() {
        Log.d(TAG, "inside init: ");
        Point displaySize = new Point();
        getWindowManager().getDefaultDisplay().getSize(displaySize);
        PeerConnectionParameters params = new PeerConnectionParameters(
                true, false, displaySize.x, displaySize.y, 30, 1, VIDEO_CODEC_VP9, true, 1, AUDIO_CODEC_OPUS, true);

        client = new WebRtcClient(this, mSocketAddress, params, VideoRendererGui.getEGLContext());
        Log.d(TAG, "init: client obj created "+client);

    }


    public void initApiAi() {
        aiService = AIService.getService(this, config);

        aiService.setListener(this);
        aiDataService = new AIDataService(config);
        t1 = new TextToSpeech(MainActivity.this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int i) {
                if (i != TextToSpeech.ERROR)
                    t1.setLanguage(Locale.US);
            }
        });

        //aiService.startListening(); invoke this on microphone click event

    }

    private void initControls() {
        messagesContainer = (ListView) findViewById(R.id.messagesContainer);
        messageET = (EditText) findViewById(R.id.messageEdit);
        sendBtn = (ImageView) findViewById(R.id.chatSendButton);
        micButton = (ImageView) findViewById(R.id.micButton);

        TextView meLabel = (TextView) findViewById(R.id.meLbl);
        TextView companionLabel = (TextView) findViewById(R.id.friendLabel);
        container = (RelativeLayout) findViewById(R.id.container);
        companionLabel.setText("Botopia");// Hard Coded
        createChatHistoryList();

        videoArea = (RelativeLayout) findViewById(R.id.videoArea);

        sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String messageText = messageET.getText().toString();
                Log.d(TAG, "onClick: before check");
                if (TextUtils.isEmpty(messageText)) {
                    return;
                }

                Log.d(TAG, "onClick: inside button click");
                ChatMessage chatMessage = new ChatMessage();
                chatMessage.setId(counter++);//dummy
                chatMessage.setMessage(messageText);
                chatMessage.setDate(DateFormat.getDateTimeInstance().format(new Date()));
                chatMessage.setMe(true);
                chatHistory.add(chatMessage);
                messageET.setText("");
                displayMessage(chatMessage);
                sendMessageToBot(chatMessage);
            }
        });

        micButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });


        micButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {


                switch (motionEvent.getAction()) {
                    case ACTION_DOWN:
                        aiService.startListening();
                        Toast.makeText(MainActivity.this, "Button is pressed ", Toast.LENGTH_SHORT).show();
                        break;
                    case ACTION_UP:
                        Toast.makeText(MainActivity.this, "Button is release  ", Toast.LENGTH_SHORT).show();
                        aiService.stopListening();
                        break;
                }
                return true;
            }
        });
        /*micButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                //

                return false;
            }
        });
        micButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {

            }}
        });*/
    }

    public void onClick_sendButton(View view) {
        Toast.makeText(this, "Help", Toast.LENGTH_LONG).show();
        String messageText = messageET.getText().toString().trim();
        Log.d(TAG, "onClick: before check");
        if (TextUtils.isEmpty(messageText)) {
            return;
        }

        Log.d(TAG, "onClick: inside button click");
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setId(counter++);//dummy
        chatMessage.setMessage(messageText);
        chatMessage.setDate(DateFormat.getDateTimeInstance().format(new Date()));
        chatMessage.setMe(true);
        chatHistory.add(chatMessage);
        messageET.setText("");
        displayMessage(chatMessage);
        sendMessageToBot(chatMessage);

    }

    private void sendMessageToBot(ChatMessage chatMessage) {

        final AIRequest aiRequest = new AIRequest();
        Log.d(TAG, "sendMessageToBot: SessionID :  "+new PrefManager(getApplicationContext()).getSessionKey());
        AIContext cont = new AIContext();
        //cont.setName("sessionId");
        Map map = new HashMap<String, String>();
        map.put("sessionId",new PrefManager(getApplicationContext()).getSessionKey());
        cont.setName("mySessionId");
        cont.setParameters(map);
        aiRequest.addContext(cont);
        aiRequest.setSessionId(new PrefManager(getApplicationContext()).getSessionKey());
        aiRequest.setQuery(chatMessage.getMessage());
        Log.d(TAG, "sendMessageToBot: Request : "+aiRequest);
        new AsyncTask<AIRequest, Void, AIResponse>() {
            @Override
            protected AIResponse doInBackground(AIRequest... requests) {
                final AIRequest request = requests[0];
                try {
                    final AIResponse response = aiDataService.request(aiRequest);
                    return response;
                } catch (AIServiceException e) {
                }
                return null;
            }

            @Override
            protected void onPostExecute(AIResponse aiResponse) {
                if (aiResponse != null) {
                    // process aiResponse here
                    parseAndShowResponse(aiResponse);
                }
            }
        }.execute(aiRequest);
    }

    private void hideKeyboard() {
        InputMethodManager inputManager = (InputMethodManager) this.getSystemService(Context.INPUT_METHOD_SERVICE);
        inputManager.hideSoftInputFromWindow(this.getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
    }

    private void parseAndShowResponse(AIResponse result) {
        final Status status = result.getStatus();
        Log.d(TAG, "Status : " + status.getCode());
        if (status.getCode() == 200) {
            final Result res = result.getResult();
            final String speech = res.getFulfillment().getSpeech();

            String source = res.getFulfillment().getSource();
            if(source!=null)
                source = source.toLowerCase();
            ChatMessage msg = new ChatMessage();
            msg.setId(counter++);
            msg.setMe(false);
            msg.setMessage(speech);
            msg.setDate(DateFormat.getDateTimeInstance().format(new Date()));
            chatHistory.add(msg);
            displayMessage(msg);
            Log.d(TAG, "onResult: Message displayed successfully : " + speech);
            t1.speak(speech, TextToSpeech.QUEUE_FLUSH, null);

            if (source != null ) {
                switch (source) {
                    case "call":
                        setupUiAndCamera();
                        final String to = String.valueOf(res.getFulfillment().getData().get("to").toString().substring(1,res.getFulfillment().getData().get("to").toString().length()-1));

                        Log.d(TAG, "parseAndShowResponse TO : : "+res.getFulfillment().getData().get("to").toString());
                        Request request = new JsonArrayRequest(Request.Method.GET, Config.URL_GET_SESSIONS, null, new Response.Listener<JSONArray>() {

                            @Override
                            public void onResponse(JSONArray response) {
                                try {
                                    Log.d(TAG, "onResponse: " + response.toString());
                                    for (int i = 0; i < response.length(); i++) {
                                        Log.d(TAG, "onResponse: name : " + ((JSONObject) response.get(i)).getString("name")+" id : "+((JSONObject) response.get(i)).getString("id"));
                                        String id = ((JSONObject) response.get(i)).getString("id");
                                        String name = ((JSONObject) response.get(i)).getString("name");
                                        Log.d(TAG, "Comparison onResponse:  name : "+name+" comparison : "+name.compareToIgnoreCase(to));
                                        if(name.equalsIgnoreCase(to)){
                                            Log.e(TAG, "################### onResponse: Sending call : "+LoginActivity.name);
                                            final JSONObject obj = new JSONObject();
                                            obj.put("name",new PrefManager(getApplicationContext()).getName());
                                            Log.d(TAG, "onResponse: "+obj);
                                            client.sendMessage(id,"init",obj);
                                            //startCam();
                                            break;
                                        }

                                    }
                                }catch(Exception e){
                                    e.printStackTrace();
                                }
                            }
                        }, new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError error) {
                                Log.e(TAG, "onErrorResponse: Error in receiving the streams", error);
                            }
                        });
                        Volley.newRequestQueue(this).add(request);
                        break;
                    default:
                        Log.d(TAG, "unrecongnized case Switch: ");
                }
            }

        } else {
            Log.d(TAG, "REsponse is not 200 " + status.toString());
        }

    }


    public void openActiveCallScreen(String name,String from){
        Log.d(TAG, "openActiveCallScreen: ");
        Intent intent = new Intent(this,CallAnswerReject.class);
        intent.putExtra("callingParty",name);
        startActivity(intent);
    }

    public void setupUiAndCamera(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                //stuff that updates ui
                hideKeyboard();
                isCall = true;
                client.setCamera();
                setScreenAttribs();
                container.setVisibility(View.GONE);
                videoArea.setVisibility(View.VISIBLE);

                Log.d(TAG, "parseAndShowResponse : "+client);

                Log.d(TAG, "after run: ");
            }
        });


    }

    public void displayMessage(ChatMessage message) {
        adapter.add(message);
        adapter.notifyDataSetChanged();
        scroll();
    }

    private void scroll() {
        messagesContainer.setSelection(messagesContainer.getCount() - 1);
    }

    private void createChatHistoryList() {

        chatHistory = new ArrayList<ChatMessage>();
        adapter = new ChatAdapter(MainActivity.this, new ArrayList<ChatMessage>());
        messagesContainer.setAdapter(adapter);
    }

    @Override
    public void onResult(AIResponse result) {
        parseAndShowResponse(result);
    }

    @Override
    public void onError(AIError error) {
        Log.d(TAG, "onError: " + error.toString());
    }

    @Override
    public void onAudioLevel(float level) {

    }

    @Override
    public void onListeningStarted() {

    }

    @Override
    public void onListeningCanceled() {

    }

    @Override
    public void onListeningFinished() {

    }

    @Override
    public void onPause() {
        if (t1 != null) {
            t1.stop();
            t1.shutdown();
        }
        if (aiService != null) {
            aiService.pause();
        }
        vsv.onPause();
        if (client != null) {
            client.onPause();
        }
        //this.unregisterReceiver(callEventReceiver);
        super.onPause();
    }

    @Override
    protected void onResume() {


        Log.d(TAG, "onResume: isCall : "+isCall+" isWaitingCall : "+isWaitingCall);
        // use this method to reinit connection to recognition service
        if (aiService != null) {
            aiService.resume();
        }
        if(isCall)
            vsv.onResume();
        if (client != null) {
            client.onResume();
        }

        if(isWaitingCall){
            try {
                //client.setCamera();
                vsv.onResume();
                setupUiAndCamera();
                client.addPeerManually(from);
                client.createOfferManualy(from,payload);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        super.onResume();
    }

    @Override
    public void onCallReady(String callId) {
        startCam();
    }

    public void startCam() {
        // Camera settings
        Log.d(TAG, "startCam: " + new PrefManager(getApplicationContext()).getEmail());
        client.start(new PrefManager(getApplicationContext()).getEmail());
    }

    @Override
    protected void onDestroy() {
        if (client != null) {
            client.onDestroy();
        }
        super.onDestroy();
    }

    @Override
    public void onStatusChanged(final String newStatus) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), newStatus, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onLocalStream(MediaStream localStream) {
        Log.d(TAG, "onLocalStream: ");
        localStream.videoTracks.get(0).addRenderer(new VideoRenderer(localRender));
        VideoRendererGui.update(localRender,
                LOCAL_X_CONNECTING, LOCAL_Y_CONNECTING,
                LOCAL_WIDTH_CONNECTING, LOCAL_HEIGHT_CONNECTING,
                scalingType, false);
    }

    @Override
    public void onAddRemoteStream(MediaStream remoteStream, int endPoint) {
        Log.d(TAG, "onAddRemoteStream: ");
        remoteStream.videoTracks.get(0).addRenderer(new VideoRenderer(remoteRender));
        VideoRendererGui.update(remoteRender,
                REMOTE_X, REMOTE_Y,
                REMOTE_WIDTH, REMOTE_HEIGHT, scalingType, false);
        VideoRendererGui.update(localRender,
                LOCAL_X_CONNECTED, LOCAL_Y_CONNECTED,
                LOCAL_WIDTH_CONNECTED, LOCAL_HEIGHT_CONNECTED,
                scalingType, false);
    }

    @Override
    public void onRemoveRemoteStream(int endPoint) {
        VideoRendererGui.update(localRender,
                LOCAL_X_CONNECTING, LOCAL_Y_CONNECTING,
                LOCAL_WIDTH_CONNECTING, LOCAL_HEIGHT_CONNECTING,
                scalingType, false);
    }

    class CallEventsReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra(Config.CALL_EVENT);
            Log.d(TAG, "Received event for Call: " + message);
            setupUiAndCamera();
        }
    }
}
