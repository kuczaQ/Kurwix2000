package com.adam.kurwix2000;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import java.io.DataInputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;

public class ChatActivity extends AppCompatActivity {
    private class ChatWorkerThread extends Thread {
        class PrintServerMsg implements Runnable {
            final String msg;
            PrintServerMsg(String msg) { this.msg = msg; }

            public void run() {
                printToConsole(msg);
            }
        }
        class PrintStackTrace implements Runnable {
            final Exception e;
            PrintStackTrace(Exception e) { this.e = e; }

            public void run() {
                printStackTrace(e);
            }
        }

        volatile boolean run, restart, restarted = false;
        Socket s;

        @Override
        public void run() {
            do {
                restart = false;
                run = true;
                runOnUiThread(new PrintServerMsg("Client is starting..."));

                try {
                    s = new Socket(ip, PORT);
                    DataInputStream stream = new DataInputStream(s.getInputStream());

                    runOnUiThread(new PrintServerMsg(null) {
                        @Override
                        public void run() {
                            printToConsole("Client running!");
                            enableControls();
                        }
                    });

                    String buff;
                    restarted = true;

                    while (run) {
                        buff = stream.readUTF();
                        runOnUiThread(new PrintServerMsg("Server says: " + buff));
                    }

                    //s.close();

                } catch (SocketException e) {

                } catch (Exception e) {
                    runOnUiThread(new PrintStackTrace(e));
                    runOnUiThread(new PrintServerMsg("Couldn't start the client, " +
                            "please check your IP address and try again..."));
                }
            } while(restart);
        }

        public synchronized void setToStop() {
            try {
                s.close();
            } catch (Exception e) {
            }
            run = false;
        }
        public void restart() {
            if (restarted) {
                restarted = false;
                restart = true;
                setToStop();
            }
        }
    }

    //########################################
    private static final int PORT = 8888;

    ArrayList<String> text = new ArrayList<String>();
    InetAddress ip;
    static TextView console;
    ChatWorkerThread worker;
    Socket socket;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    // handle button activities
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.refresh) {
            console.setText("");
            worker.setToStop();

            worker = new ChatWorkerThread();
            worker.start();
            //worker.restart();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        ip = (InetAddress) intent.getSerializableExtra(MainActivity.IP_ADDRESS);
        setContentView(R.layout.activity_chat);
        console = (TextView) findViewById(R.id.consoleClient);
        this.setTitle("Chatting with: " + ip.toString().replaceFirst("/", ""));

        //connect();
    }

    @Override
    protected void onResume() {
        super.onResume();
        worker = new ChatWorkerThread();
        worker.start();

    }

    @Override
    protected void onPause() {
        super.onPause();
        disconnect();
    }

    protected boolean connected() {
        return socket != null;
    }

    protected void enableControls() {
        findViewById(R.id.msgFieldClient).setEnabled(true);
        findViewById(R.id.sendButtonClient).setEnabled(true);
    }

    protected void disableControls() {
        findViewById(R.id.msgFieldClient).setEnabled(false);
        findViewById(R.id.sendButtonClient).setEnabled(false);
    }

    protected void disconnect() {
        if (connected()) {
            try {
                worker.setToStop();
                worker.join();
                socket.close();
                findViewById(R.id.msgFieldClient).setEnabled(false);
                findViewById(R.id.sendButtonClient).setEnabled(false);
            } catch (Exception e) {
                printStackTrace(e);
            }
        }
    }

    public void sendMessage(View view) {

    }

    public void printToConsole(Object o) {
        console.setText(console.getText() + o.toString() + "\n");
    }

    public void printToConsole(String s) {
        console.setText(console.getText() + s + "\n");
    }

    private void printStackTrace(Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        printToConsole(sw.toString());
    }
}
