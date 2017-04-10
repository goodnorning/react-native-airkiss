package com.goodnorning.airkiss;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.SparseArray;
import android.os.AsyncTask;

import com.facebook.common.logging.FLog;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.GuardedAsyncTask;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;

// import java.io.IOException;
// import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutionException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;

import android.util.Log;

public final class Airkiss extends ReactContextBaseJavaModule {
    private static final String TAG = "Airkiss";
    private AirKissTask task1;
    private MyTask task2;

    public Airkiss(ReactApplicationContext reactContext) {
        super(reactContext);
    }

   	@Override
    public String getName() {
        return TAG;
    }

    @ReactMethod
    public void start(final String ssid, final String password, final Callback callback1, final Callback callback2) {
    	task1 = new AirKissTask(callback1, new AirKissEncoder(ssid, password));
        task2 = new MyTask(callback2);
        task1.execute();
        task2.execute();
    }

	@ReactMethod
    public void stop() {
    	task1.stop();
    	task2.stop();
    }



	private class MyTask extends AsyncTask<Void,Void,Void> {
        private static final int PORT1 = 12476;
        // private static final byte  kMagic_Num_0             =0xFD;
        // private static final byte  kMagic_Num_1             =0x01;
        // private static final byte  kMagic_Num_2             =0xFE;
        // private static final byte  kMagic_Num_3             =0xFC;
        private static final int AIRKISS_LAN_SSDP_NOTIFY_CMD = 0x1002;  
        private Callback mCallback;
        private boolean mStop = false;

        public MyTask(final Callback callback) {
        	mCallback = callback;
        }

        @Override
        protected void onPreExecute() {
            new Thread(new Runnable() {
              public void run() {
                    byte[] buffer = new byte[200];
                    try {
                        DatagramSocket udpServerSocket = new DatagramSocket(12476);
                        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                        // int replyByteCounter = 0;
                        udpServerSocket.setSoTimeout(1000);
                        while (true) {
                            // if (getStatus() == Status.FINISHED) {
                            //     System.out.println("@@@@@@@@@@@@@@@@@@@@@@");
                            //     break;
                            // }
                        	if (mStop)
                        		break;
                            try {
                                udpServerSocket.receive(packet);
                                byte receivedData[] = packet.getData();
                                System.out.println("==========================::"+packet.getLength());
                                for (byte b : receivedData) {
                                    System.out.println(b);
                                }
                                // byte[] inBuff = new byte[200];
                                // System.out.println("==========================::"+ new String(inBuff, 0 , packet.getLength())); 
                                if (parseNotifyData(receivedData))
                                    break;
                            } catch (SocketTimeoutException e) {
                                e.printStackTrace();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }

                        udpServerSocket.close();
                    } catch (SocketException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }

    	private boolean parseNotifyData(byte[] data) {
            Log.d("parseNotifyData","----------");
            byte[] newData = new byte[200];
            int length = data.length;
            int count = 0;
            boolean flag = false;
            Log.d("parseNotifyData","length:"+length);
            while(count<length-4 && length<200){
                byte num0 = (byte)(data[count++]&0xFF);
                byte num1 = (byte)(data[count++]&0xFF);
                byte num2 = (byte)(data[count++]&0xFF);
                byte num3 = (byte)(data[count++]&0xFF);
                System.out.println(num0+""+num1+""+num2+""+num3);
                if (num0 == -3 && num1 == 1 && num2 == -2 && num3 == -4) {
                    Log.d("parseNotifyData","find magic num");
                    System.arraycopy(data,count,newData,0,length-count);
                    flag = true;
                    break;
                }
            }
            count = 0;
            length = length-count;
            if (flag) {
                while(count<length-2&&length>2) {
                    int cmd = (newData[count++]<<8|newData[count++])&0xFFFF;
                    if(cmd == AIRKISS_LAN_SSDP_NOTIFY_CMD) {
                        Log.d("parseNotifyData","got device data");
                        mCallback(true)
                        return true;
                    }           
                }
            }
            return false;
        }

        @Override
        protected Void doInBackground(Void... params) {
 

            return null;
        }

        @Override
        protected void onCancelled(Void params) {
        }

        @Override
        protected void onPostExecute(Void params) {
      
        }  
        public void stop() {
        	mStop = true;
        }
    }

	private class AirKissTask extends AsyncTask<Void, Void, Void> {
        private static final int PORT = 10000;
        private final byte DUMMY_DATA[] = new byte[1500];
        private static final int REPLY_BYTE_CONFIRM_TIMES = 5;
        private Context mContext;
        private DatagramSocket mSocket;

        private char mRandomChar;
        private AirKissEncoder mAirKissEncoder;
        private Callback mCallback;
        private volatile boolean mDone = false;
        private boolean mStop = false;

        public AirKissTask(final Callback callback, AirKissEncoder encoder) {
            mCallback = callback;
            mRandomChar = encoder.getRandomChar();
            mAirKissEncoder = encoder;
        }

        @Override
        protected void onPreExecute() {

            new Thread(new Runnable() {
                public void run() {
                    byte[] buffer = new byte[15000];
                    try {
                        DatagramSocket udpServerSocket = new DatagramSocket(PORT);
                        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                        int replyByteCounter = 0;
                        udpServerSocket.setSoTimeout(1000);
                        while (true) {
                            if (getStatus() == Status.FINISHED)
                                break;
                            if (mStop)
                            	break;

                            try {
                                udpServerSocket.receive(packet);
                                byte receivedData[] = packet.getData();
                                for (byte b : receivedData) {
                                    if (b == mRandomChar)
                                        replyByteCounter++;
                                }

                                if (replyByteCounter > REPLY_BYTE_CONFIRM_TIMES) {
                                    mDone = true;
                                    break;
                                }
                            } catch (SocketTimeoutException e) {
                                e.printStackTrace();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }

                        udpServerSocket.close();
                    } catch (SocketException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }

        private void sendPacketAndSleep(int length) {
            try {
                DatagramPacket pkg = new DatagramPacket(DUMMY_DATA,
                                                        length,
                                                        InetAddress.getByName("255.255.255.255"),
                                                        PORT);
                mSocket.send(pkg);
                Thread.sleep(4);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                mSocket = new DatagramSocket();
                
                mSocket.setBroadcast(true);
            } catch (Exception e) {
                e.printStackTrace();
            }

            int encoded_data[] = mAirKissEncoder.getEncodedData();
            for (int i = 0; i < encoded_data.length; ++i) {
                sendPacketAndSleep(encoded_data[i]);
                if (i % 200 == 0) {
                    if (isCancelled() || mDone)
                        return null;
                }
            }

            return null;
        }

        @Override
        protected void onCancelled(Void params) {
        }

        @Override
        protected void onPostExecute(Void params) {
 
        }

        public void stop() {
        	mStop = true;
        }
    }

}
