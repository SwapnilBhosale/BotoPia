package gslab.com.myapplication;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

/**
 * Created by GS-0913 on 24-03-2017.
 */

public class Hackathon extends Application {

    private static Hackathon mInstance;
    private static Context context;
    private static Context baseContext;
    private static String TAG = Hackathon.class.getSimpleName();

    @Override
    public void onCreate() {
        super.onCreate();
        mInstance = this;
        context = this;
        baseContext = getBaseContext();
    }

    public static Context getContext() {
        return context;
    }

    public static synchronized Hackathon getInstance() {
        return mInstance;
    }

    public static Context getAppBaseContext(){
        return baseContext;
    }


    public static void logoutUser(){
        try{
            JSONObject obj = new JSONObject();
            final PrefManager prefManager = new PrefManager(getAppBaseContext());
            obj.put("session_id",prefManager.getSessionKey());
            JsonObjectRequest req = new JsonObjectRequest(Request.Method.POST, Config.URL_LOGOUT, obj, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    prefManager.setLoggedIn(false);
                    prefManager.setName("");
                    prefManager.setEmail("");
                    prefManager.setSessionKey("");
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.e(TAG, "onErrorResponse: ",error );
                }
            });
            Volley.newRequestQueue(context).add(req);
        }catch(Exception e){
            e.printStackTrace();
        }

    }



}
