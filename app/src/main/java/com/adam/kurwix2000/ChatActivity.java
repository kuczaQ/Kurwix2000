package com.adam.kurwix2000;

import android.content.Intent;
import android.os.AsyncTask;
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
import java.util.ArrayList;

public class ChatActivity extends AppCompatActivity {
    private class ConnectTask extends AsyncTask<InetAddress, Integer, Socket> {
        protected Socket doInBackground(InetAddress... inetAddresses) {
            return ChatActivity.connect(inetAddresses[0]);
        }

        protected void onPostExecute(Socket result) {
            //ChatActivity.
        }
    }

    class ChatWorkerThread extends Thread {
        volatile boolean run = true;
        ChatActivity parent;

        public ChatWorkerThread(ChatActivity parent) {
            this.parent = parent;
        }

        @Override
        public void run() {
            printToConsole("Client is starting...");

            try {
                DataInputStream stream = new DataInputStream(parent.socket.getInputStream());

                String buff = "";
                do {
                    //stream.writeUTF(buff);
                    //stream.flush();
                    System.out.println(buff);
                } while (!buff.equals("STOP"));

               // stream.writeUTF(buff);
                //stream.flush();
            } catch (Exception e) {
                printStackTrace(e);
            }
        }

        public void setToStop() {
            run = false;
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
            disconnect();
            //connect();
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
        console = (TextView) findViewById(R.id.console);
        this.setTitle("Chatting with: " + ip.toString().replaceFirst("/", ""));

        //connect();
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            new ConnectTask().execute(ip);
        } catch (Exception e) {
            printStackTrace(e);
        }
        //connect();
    }

    @Override
    protected void onPause() {
        super.onPause();
        disconnect();
    }

    protected boolean connected() {
        return socket != null;
    }

    protected static Socket connect(InetAddress ip) {
        Socket s;

            try {
                s = new Socket(ip, PORT);
//                findViewById(R.id.msgField).setEnabled(true);
//                findViewById(R.id.sendButton).setEnabled(true);

//                worker = new ChatWorkerThread(this);
//                worker.start();

                return s;
            } catch (Exception e) {
//                if (socket != null) {
//                    try {
//                        socket.close();
//                    } catch (Exception e1){}
//                    socket = null;
//                }
                printStackTrace(e);
                return null;
            }


    }

    protected void disconnect() {
        if (connected()) {
            try {
                worker.setToStop();
                worker.join();
                socket.close();
                findViewById(R.id.msgField).setEnabled(false);
                findViewById(R.id.sendButton).setEnabled(false);
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

    public static void printToConsole(String s) {
        console.setText(console.getText() + s + "\n");
    }

    private static void printStackTrace(Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        printToConsole(sw.toString());
    }
}
