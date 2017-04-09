package gslab.com.myapplication;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;

import java.util.Locale;

public class SplashActivity extends AppCompatActivity {

    private final int SPLASH_DISPLAY_LENGTH = 3000;
    private String TAG = SplashActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        final Activity act = this;

        new Handler().postDelayed(new Runnable(){
            @Override
            public void run() {
                /* Create an Intent that will start the Menu-Activity. */
                Intent intent = null;
                Log.d(TAG, "run: dsad");
                PrefManager pref = new PrefManager(getApplicationContext());
                if(checkIfLoggedIn()){
                    intent = new Intent(SplashActivity.this,MainActivity.class);
                }else{
                    intent = new Intent(SplashActivity.this,LoginActivity.class);
                }
                startActivity(intent);
                finish();
            }
        }, SPLASH_DISPLAY_LENGTH);
    }
    private boolean checkIfLoggedIn(){

        PrefManager pref = new PrefManager(getApplicationContext());
        boolean isLogged = pref.isLoggedIn();
        Log.d(TAG, "checkIfLoggedIn: "+isLogged);
        if(isLogged)
            return true;
        return false;
    }


}
