package com.adam.kurwix2000;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.TextView;

import java.net.InetAddress;

public class MainActivity extends AppCompatActivity {
    public static final String IP_ADDRESS = "com.adam.IP_ADDRESS";

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        web = (WebView)findViewById(R.id.webView1);
        // requestWindowFeature(Window.FEATURE_SWIPE_TO_DISMISS);

//        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
//                WindowManager.LayoutParams.FLAG_FULLSCREEN);



    }

    public void startServer(View view) {
        Intent intent = new Intent(this, ServerActivity.class);
        startActivity(intent);
    }

    /** Called when the user taps the Send button */
    public void startClient(View view) {
        EditText IPAddressEditText = (EditText) findViewById(R.id.IPAddressEditText);
        Intent intent = new Intent(this, ChatActivity.class);
        TextView errMsg = (TextView) findViewById(R.id.ipErrorMsg);

        String ipString = IPAddressEditText.getText().toString();
        if (ipString.equals("")) {
            errMsg.setText(R.string.ipErrMsg);
            return;
        }

        try {
            InetAddress ip = InetAddress.getByName(ipString);
            intent.putExtra(IP_ADDRESS, ip);

        } catch (Exception e) {
            errMsg.setText(R.string.ipErrMsg);
            return;
        }

        startActivity(intent);
        errMsg.setText("");
    }
}
