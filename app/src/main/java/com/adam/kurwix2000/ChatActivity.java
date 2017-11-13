package com.adam.kurwix2000;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayDeque;
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

        class ReceiveWorker extends Thread {
            Socket s;

            public ReceiveWorker(Socket s) {
                this.s = s;
            }

            @Override
            public void run() {
                try {
                    DataInputStream stream = new DataInputStream(s.getInputStream());

                    String buff;
                    restarted = true;

                    while (run) {
                        buff = stream.readUTF();
                        runOnUiThread(new PrintServerMsg("Server says: " + buff));
                    }

                    //s.close();

                } catch (SocketException e) {
                    //exit?
                } catch (Exception e) {
                    runOnUiThread(new PrintStackTrace(e));

                }
            }
        }
        //######################
        volatile boolean run, restart, restarted = false;
        Socket s;
        Thread receiveWorker;

        ArrayDeque<String> toSendBuffer = new ArrayDeque<String>();

        @Override
        public void run() {
            do {
                restart = false;
                run = true;
                runOnUiThread(new PrintServerMsg("Client is starting..."));

                try { //connect to the server
                    s = new Socket(ip, PORT);
                    receiveWorker = new ReceiveWorker(s);
                    receiveWorker.start();

                    runOnUiThread(new PrintServerMsg(null) {
                        @Override
                        public void run() {
                            printToConsole("Client running!");
                            enableControls();
                        }
                    });
                } catch (Exception e) {
                    runOnUiThread(new PrintStackTrace(e));
                    runOnUiThread(new PrintServerMsg("Couldn't start the client, " +
                            "please check your IP address and try again..."));
                }

                try {
                    DataOutputStream stream = new DataOutputStream(s.getOutputStream());

                    String buff;
                    restarted = true;

                    while (run) {
                        buff = getNextBufferedMessage();
                        if (buff != null)
                            stream.writeUTF(buff);
                    }

                    //s.close();

                } catch (SocketException e) {
                    //exit?
                } catch (Exception e) {
                    runOnUiThread(new PrintStackTrace(e));

                }

            } while(restart);
        }

        private synchronized String getNextBufferedMessage() {
            return toSendBuffer.pollFirst();
        }

        public synchronized void addMessage(String msg) {
            toSendBuffer.addLast(msg);
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

    //########################################################################################################################
    private final int PORT = 8888;

    ArrayList<String> text = new ArrayList<String>();
    InetAddress ip;
    static TextView console;
    ChatWorkerThread worker;
    //Socket socket;
    ScrollView scrollView = null;

    //#####################
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        ip = (InetAddress) intent.getSerializableExtra(MainActivity.IP_ADDRESS);
        setContentView(R.layout.activity_chat);
        console = findViewById(R.id.consoleClient);
        scrollView = findViewById(R.id.scrollViewClient);
        this.setTitle("Chatting with: " + ip.toString().replaceFirst("/", ""));
        start();
        //connect();
    }

    @Override
    protected void onResume() {
        super.onResume();
        //start();

    }

    @Override
    protected void onPause() {
        super.onPause();
        //disconnect();
    }

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
            restart();
            //worker.restart();
        }
        return super.onOptionsItemSelected(item);
    }

    public void start() {
        if (worker == null) {
            worker = new ChatWorkerThread();
            worker.start();
        } else
            restart();
    }

    private void restart() {
        disableControls();
        console.setText("");
        worker.setToStop();

        worker = new ChatWorkerThread();
        worker.start();
    }

//    protected boolean connected() {
//        return socket != null;
//    }

    protected void enableControls() {
        findViewById(R.id.msgFieldClient).setEnabled(true);
        findViewById(R.id.sendButtonClient).setEnabled(true);
    }

    protected void disableControls() {
        findViewById(R.id.msgFieldClient).setEnabled(false);
        findViewById(R.id.sendButtonClient).setEnabled(false);
    }

//    protected void disconnect() {
//        if (connected()) {
//            try {
//                worker.setToStop();
//                worker.join();
//                socket.close();
//                findViewById(R.id.msgFieldClient).setEnabled(false);
//                findViewById(R.id.sendButtonClient).setEnabled(false);
//            } catch (Exception e) {
//                printStackTrace(e);
//            }
//        }
//    }

    public void sendMessage(View view) {
        final String msg = ((EditText)(findViewById(R.id.msgFieldClient))).getText().toString();
        Thread post = new Thread() {
            @Override
            public void run() {
                worker.addMessage(msg);
            }
        };
        post.start();

        printToConsole(msg);
        scrollDown();

    }

    public void scrollDown() {
        //Use instead of "scrollView.fullScroll(ScrollView.FOCUS_DOWN);"
        //if shit hits the fans...
//        View lastChild = scrollView.getChildAt(scrollView.getChildCount() - 1);
//        int bottom = lastChild.getBottom() + scrollView.getPaddingBottom();
//        int sy = scrollView.getScrollY();
//        int sh = scrollView.getHeight();
//        final int delta = bottom - (sy + sh);

        scrollView.postDelayed(new Runnable() {
            @Override
            public void run() {
                //scrollView.smoothScrollBy(0, delta);
                scrollView.fullScroll(ScrollView.FOCUS_DOWN);
            }
        }, 0);
    }

    public void printToConsole(Object o) {
        console.setText(console.getText() + o.toString() + "\n");
    }

    public void printToConsole(String s) {
        console.setText(console.getText() + s + "\n");
        scrollDown();
    }

    private void printStackTrace(Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        printToConsole(sw.toString());
    }
}
