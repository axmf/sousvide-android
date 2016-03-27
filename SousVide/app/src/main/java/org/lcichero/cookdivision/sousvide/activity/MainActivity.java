package org.lcichero.cookdivision.sousvide.activity;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.github.ivbaranov.rxbluetooth.Action;
import com.github.ivbaranov.rxbluetooth.RxBluetooth;

import org.lcichero.cookdivision.sousvide.R;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

public class MainActivity extends Activity {
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final String TAG = "SOUSVIDE_BT";
    // private static String btAddress = "00:12:08:20:08:04"; // Arduino
    private static String btAddress = "74:DE:2B:42:92:9D"; // Pc Server

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothSocket mmSocket;
    private BluetoothDevice mmDevice;
    private OutputStream mmOutputStream;
    private InputStream mmInputStream;
    private Thread workerThread;
    byte[] readBuffer;
    int readBufferPosition;
    int counter;
    volatile boolean stopWorker;

    private Button buttonSubtractDegree;
    private Button buttonAddDegree;
    private TextView textViewBluetoothStatusValue;
    private TextView textViewCurrentTempValue;
    private TextView textViewSetPointValue;
    private float currentTemp = 00F;
    private float setPoint = 60F;
    private final String CELSIUS_DEGREE_NOTATION = " " + (char) 0x00B0 + "C";

    //The BroadcastReceiver that listens for bluetooth broadcasts
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
                Log.e("", "device has been connected.");
                initializeBluetooth();
            } else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                Log.e("", "device has been disconnected.");
                textViewBluetoothStatusValue.setText("disconnected");
                textViewBluetoothStatusValue.setTextColor(Color.RED);
            }
        }
    };

    public void onCreate(Bundle paramBundle) {
        super.onCreate(paramBundle);
        setContentView(R.layout.activity_main);
        this.buttonSubtractDegree = ((Button) findViewById(R.id.buttonSubtractDegree));
        this.buttonAddDegree = ((Button) findViewById(R.id.buttonAddDegree));
        this.textViewBluetoothStatusValue = ((TextView) findViewById(R.id.textViewBluetoothStatusValue));
        this.textViewCurrentTempValue = ((TextView) findViewById(R.id.textViewCurrentTempValue));
        this.textViewSetPointValue = ((TextView) findViewById(R.id.textViewSetPointValue));

        this.textViewCurrentTempValue.setText(String.format("%.2f", currentTemp) + CELSIUS_DEGREE_NOTATION);
        this.textViewSetPointValue.setText(String.format("%.2f", setPoint) + CELSIUS_DEGREE_NOTATION);

        this.buttonSubtractDegree.setOnClickListener(new View.OnClickListener() {
            public void onClick(View paramAnonymousView) {
                try {
                    if(!(setPoint - 1 <= 0)) {
                        setPoint--;
                        textViewSetPointValue.setText(String.format("%.2f", setPoint) + CELSIUS_DEGREE_NOTATION);
                        sendData(encryptSetPoint(setPoint));
                    } else {
                        Toast.makeText(MainActivity.this.getBaseContext(), "Para papu, no te puedo congelar el agua viteh.", Toast.LENGTH_LONG).show();
                    }
                } catch (Exception ex) {
                    Log.e(TAG, ex.getMessage(), ex);
                    Toast.makeText(MainActivity.this.getBaseContext(), "ERROR! Couldn't send data to SousVide device.", Toast.LENGTH_LONG).show();
                }
            }
        });
        this.buttonAddDegree.setOnClickListener(new View.OnClickListener() {
            public void onClick(View paramAnonymousView) {
                try {
                    if(!(setPoint + 1 >= 100)) {
                        setPoint++;
                        textViewSetPointValue.setText(String.format("%.2f", setPoint) + CELSIUS_DEGREE_NOTATION);
                        sendData(encryptSetPoint(setPoint));
                    } else {
                        Toast.makeText(MainActivity.this.getBaseContext(), "No monsters, mas de 100 °C el agua hierve, no jodamo.", Toast.LENGTH_LONG).show();
                    }

                } catch (Exception ex) {
                    Log.e(TAG, ex.getMessage(), ex);
                    Toast.makeText(MainActivity.this.getBaseContext(), "ERROR! Couldn't send data to SousVide device.", Toast.LENGTH_LONG).show();
                }
            }
        });

        IntentFilter filter1 = new IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED);
        IntentFilter filter2 = new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED);
        IntentFilter filter3 = new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        this.registerReceiver(mReceiver, filter1);
        this.registerReceiver(mReceiver, filter2);
        this.registerReceiver(mReceiver, filter3);
    }

    public void onPause() {
        super.onPause();
        try {
            if (this.mmOutputStream != null) {
                this.mmOutputStream.flush();
            }
        } catch (IOException ex) {
            Log.e(TAG, ex.getMessage(), ex);
            Toast.makeText(MainActivity.this.getBaseContext(), "ERROR! 'onPause1'", Toast.LENGTH_LONG).show();
            try {
                this.mmSocket.close();
            } catch (IOException ex2) {
                Log.e(TAG, ex2.getMessage(), ex2);
                Toast.makeText(MainActivity.this.getBaseContext(), "ERROR! 'onPause2'", Toast.LENGTH_LONG).show();
            }
        }
    }

    public void onResume() {
        super.onResume();
        initializeBluetooth();
    }

    private void initializeBluetooth() {
        this.mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (this.mBluetoothAdapter != null) {
            if (this.mBluetoothAdapter.isEnabled()) {
                Log.i(TAG, "Bluetooth is enabled.");
                this.textViewBluetoothStatusValue.setText("disconnected");
                this.textViewBluetoothStatusValue.setTextColor(Color.RED);
                try {
                    mmDevice = this.mBluetoothAdapter.getRemoteDevice(btAddress);
                    mmSocket = mmDevice.createRfcommSocketToServiceRecord(MY_UUID);
                    this.mBluetoothAdapter.cancelDiscovery();
                    this.mmSocket.connect();
                    this.mmOutputStream = this.mmSocket.getOutputStream();
                    mmInputStream = mmSocket.getInputStream();
                    this.textViewBluetoothStatusValue.setText("connected");
                    this.textViewBluetoothStatusValue.setTextColor(Color.GREEN);
                    addBluetoothDataListener();
                } catch (IOException ex) {
                    Log.e(TAG, ex.getMessage(), ex);
                    Toast.makeText(MainActivity.this.getBaseContext(), "ERROR! 'initializeBluetooth'", Toast.LENGTH_LONG).show();
                }
            } else {
                Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBluetooth, 0);
            }

        } else {
            Log.e(TAG, "Bluetooth adapter is null.");
        }
    }

    void addBluetoothDataListener() {
        final Handler handler = new Handler();
        final byte delimiter = 10; //This is the ASCII code for a newline character
        stopWorker = false;
        readBufferPosition = 0;
        readBuffer = new byte[1024];
        workerThread = new Thread(new Runnable() {
            public void run() {
                while (!Thread.currentThread().isInterrupted() && !stopWorker) {
                    try {
                        int bytesAvailable = mmInputStream.available();
                        if (bytesAvailable > 0) {
                            byte[] packetBytes = new byte[bytesAvailable];
                            mmInputStream.read(packetBytes);
                            for (int i = 0; i < bytesAvailable; i++) {
                                byte b = packetBytes[i];
                                if (b == delimiter) {
                                    byte[] encodedBytes = new byte[readBufferPosition];
                                    System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                    final String data = new String(encodedBytes, "US-ASCII");
                                    readBufferPosition = 0;

                                    handler.post(new Runnable() {
                                        public void run() {
                                            try {
                                                Log.e("JEJE", data);
                                                String charOne = Character.toString(data.charAt(0));
                                                String charTwo = Character.toString(data.charAt(data.length() - 1));
                                                if ("#".equals(charOne) &&
                                                        "#".equals(charTwo) &&
                                                        data.length() == 25) {
                                                    // #78,00&39,00@78,80&39,40#
                                                    String newData = data.replace("#", "");
                                                    newData = newData.replace(",", ".");
                                                    String[] dataArray = newData.split("@");

                                                    String[] setPointArray = dataArray[0].split("&");
                                                    Float setPointValue = Float.valueOf(setPointArray[0]);
                                                    Float setPointCheck = Float.valueOf(setPointArray[1]);
                                                    if((setPointValue / 2) == setPointCheck) {
                                                        textViewSetPointValue.setText(String.format("%.2f", setPointValue) + CELSIUS_DEGREE_NOTATION);
                                                    }

                                                    String[] currentTempArray = dataArray[1].split("&");
                                                    Float currentTempValue = Float.valueOf(currentTempArray[0]);
                                                    Float currentTempCheck = Float.valueOf(currentTempArray[1]);
                                                    if((currentTempValue / 2) == currentTempCheck) {
                                                        textViewCurrentTempValue.setText(String.format("%.2f", currentTempValue) + CELSIUS_DEGREE_NOTATION);
                                                    }
                                                }
                                            } catch(Exception e) {
                                                Log.e("Error", "Algo salio mal! :S", e);
                                            }
                                        }
                                    });
                                } else {
                                    readBuffer[readBufferPosition++] = b;
                                }
                            }
                        }
                    } catch (IOException ex) {
                        stopWorker = true;
                    }
                }
            }
        });

        workerThread.start();
    }

    private String encryptSetPoint(Float setPoint) {
        return "#" + String.format("%.2f", setPoint) + "&" + String.format("%.2f", setPoint / 2) + "#";
    }

    void sendData(String data) throws IOException {
        mmOutputStream.write(data.getBytes());
    }
}