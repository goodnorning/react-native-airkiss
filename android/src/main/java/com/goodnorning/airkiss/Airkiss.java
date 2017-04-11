package com.goodnorning.airkiss;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.SparseArray;
import android.os.AsyncTask;
import android.util.Base64;

import com.facebook.common.logging.FLog;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Callback;
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
    private MyTask task2 = null;

    public Airkiss(ReactApplicationContext reactContext) {
        super(reactContext);
    }

   	@Override
    public String getName() {
        return TAG;
    }

    @ReactMethod
    public void start(final String ssid, final String password, final Callback callback) {
    	task1 = new AirKissTask(callback,new AirKissEncoder(ssid, password));
        task1.execute();
    }

    @ReactMethod
    public void startGetDeviceInfo(final Callback callback){
        task2 = new MyTask(callback);
        task2.execute();
    }

	@ReactMethod
    public void stop() {
    	task1.stop();
        if (task2 != null)
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
        private volatile boolean mGotNotify = false;
        private String mDevice;

        public MyTask(final Callback callback) {
        	mCallback = callback;
        }

        @Override
        protected void onPreExecute() {
            new Thread(new Runnable() {
              public void run() {
                    byte[] buffer = new byte[15000];
                    try {
                        DatagramSocket udpServerSocket = new DatagramSocket(12476);
                        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                        udpServerSocket.setSoTimeout(1000);
                        while (true) {
                            if (getStatus() == Status.FINISHED) {
                                System.out.println("@@@@@@@@@@@@@@@@@@@@@@");
                                break;
                            }
                        	if (mStop)
                        		break;
                            try {
                                udpServerSocket.receive(packet);
                                byte receivedData[] = packet.getData();
                                System.out.println("==========================::"+packet.getLength());
                                if (parseNotifyData(receivedData,packet.getLength())) {
                                    mGotNotify = true;
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

    	private boolean parseNotifyData(byte[] data, int length) {
            Log.d("parseNotifyData","----------");
            byte[] newData = new byte[200];
            int count = 0;
            boolean flag = false;
            Log.d("parseNotifyData","length:"+length);
            while(count<length-4 && length<200){
                byte num0 = data[count++];
                byte num1 = data[count++];
                byte num2 = data[count++];
                byte num3 = data[count++];
                System.out.println(num0+""+num1+""+num2+""+num3);
                if (num0 == -3 && num1 == 1 && num2 == -2 && num3 == -4) {
                    Log.d("parseNotifyData","find magic num");
                    System.arraycopy(data,count,newData,0,length-count);
                    flag = true;
                    break;
                }
            }

            if (flag) {
                count = 0;
                length = length-count;
                while(count<length-2&&length>2) {
                    int cmd = (newData[count++]<<8|newData[count++])&0xFFFF;
                    if(cmd == AIRKISS_LAN_SSDP_NOTIFY_CMD) {
                        Log.d("parseNotifyData","got device data");
                        byte[] buffer = new byte[200];
                        for (int i=0;i<200;i++) {
                            buffer[i]=0;
                        }
                        int len = length-count;
                        System.out.println(len);
                        System.arraycopy(newData,count,buffer,0,len);
                        // for (int i=0;i<len;i++) {
                        //     // System.out.println(buffer[i]);
                        //     if (buffer[i]>0x7F)
                        //         buffer[i]='\0';
                        // }
                    
                        mDevice = new String(buffer,0,len);
                        Log.d("parseNotifyData------",mDevice);
                        return true;
                    }           
                }
            }
            return false;
        }

        @Override
        protected Void doInBackground(Void... params) {
            Log.d("parseNotifyData------","doInBackground");
            int count = 0;
            while(!mGotNotify && count < 60) {
                count++;
                try {  
                    Thread.sleep(500);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

        @Override
        protected void onCancelled(Void params) {
        }

        @Override
        protected void onPostExecute(Void params) {
            Log.d("parseNotifyData------","onPostExecute :"+mGotNotify);
            WritableMap result = Arguments.createMap();
            if (mGotNotify) {
                result.putInt("code",1);
                result.putString("device",mDevice);
            } else {
                result.putInt("code",-1);
            }
            mCallback.invoke(result);
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

        public AirKissTask(final Callback callback,AirKissEncoder encoder) {
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
            WritableMap result = Arguments.createMap();
            if (mDone) {
                result.putInt("code",1);
            } else {
                result.putInt("code",-1);
            }
            mCallback.invoke(result);
        }

        public void stop() {
        	mStop = true;
        }
    }

}
