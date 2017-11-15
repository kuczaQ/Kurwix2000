package com.adam.kurwix2000;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.format.Formatter;
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
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Enumeration;

public class ServerActivity extends AppCompatActivity {
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
                        runOnUiThread(new PrintServerMsg("Client says: " + buff));
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
        ServerSocket ss;
        Thread receiveWorker;

        ArrayDeque<String> toSendBuffer = new ArrayDeque<String>();


        @Override
        public void run() {
            do {
                restart = false;
                run = true;
                runOnUiThread(new PrintServerMsg("Server is starting..."));
                try {
                    final InetAddress ip = InetAddress.getLocalHost();

                    try { //create server
                        ss = new ServerSocket(PORT);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                printToConsole("Server running @ " +
                                        getLocalIpAddress());
                                setTitle("Server running");
                                enableControls();
                            }
                        });
                    } catch (Exception e) {
                        runOnUiThread(new PrintStackTrace(e));
                        runOnUiThread(new PrintServerMsg("\nCouldn't start the server! Please try again..."));
                    }
                } catch (UnknownHostException e) {
                    printStackTrace(e);
                }

                runOnUiThread(new Runnable() {
                      @Override
                      public void run() {
                          printToConsole("Waiting for connection...");
                          setTitle("Waiting for connection...");
                      }
                  }
                );

                try (Socket s = ss.accept()) {
                    DataOutputStream stream = new DataOutputStream(s.getOutputStream());

                    receiveWorker = new ReceiveWorker(s);
                    receiveWorker.start();

                    runOnUiThread(new Runnable() {
                          @Override
                          public void run() {
                              printToConsole("Connected with " + s.getInetAddress().getHostAddress());
                              setTitle("Chatting with: " + s.getInetAddress().getHostAddress());
                          }
                      }
                    );

                    String buff;
                    restarted = true;

                    while (run) {
                        buff = getNextBufferedMessage();
                        if (buff != null) {
                            stream.writeUTF(buff);
                            stream.flush();
                        }

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
                ss.close();
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

        public String getLocalIpAddress() {
            try {
                for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                    NetworkInterface intf = en.nextElement();
                    for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                        InetAddress inetAddress = enumIpAddr.nextElement();
                        if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
                            return inetAddress.getHostAddress();
                        }
                    }
                }
            } catch (SocketException ex) {
                ex.printStackTrace();
            }
            return null;
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
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        //Intent intent = getIntent();
        console = findViewById(R.id.consoleServer);
        scrollView = findViewById(R.id.scrollViewServer);
//        try {
//            ip = InetAddress.getLocalHost();
//        } catch(Exception e) {
//            printStackTrace(e);
//        }

        setTitle("Server starting...");
        //this.setTitle("Chatting with: " + ip.toString().replaceFirst("/", ""));

        start();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        stop();
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

    public void stop() {
        worker.setToStop();
    }

//    protected boolean connected() {
//        return socket != null;
//    }

    protected void enableControls() {
        findViewById(R.id.msgFieldServer).setEnabled(true);
        findViewById(R.id.sendButtonServer).setEnabled(true);
    }

    protected void disableControls() {
        findViewById(R.id.msgFieldServer).setEnabled(false);
        findViewById(R.id.sendButtonServer).setEnabled(false);
    }

//    protected void disconnect() {
//        if (connected()) {
//            try {
//                worker.setToStop();
//                worker.join();
//                socket.close();
//                findViewById(R.id.msgFieldServer).setEnabled(false);
//                findViewById(R.id.sendButtonServer).setEnabled(false);
//            } catch (Exception e) {
//                printStackTrace(e);
//            }
//        }
//    }

    public void sendMessage(View view) {
        EditText et = findViewById(R.id.msgFieldServer);
        final String msg = et.getText().toString();
        et.setText("");
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
        //
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
    }

    private void printStackTrace(Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        printToConsole(sw.toString());
    }

}
