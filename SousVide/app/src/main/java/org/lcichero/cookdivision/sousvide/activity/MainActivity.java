package org.lcichero.cookdivision.sousvide.activity;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.lcichero.cookdivision.sousvide.R;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class MainActivity extends Activity {
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final String TAG = "SOUSVIDE_BT";
    private static String btAddress = "00:12:08:20:08:04";

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
                    setPoint--;
                    textViewSetPointValue.setText(String.format("%.2f", setPoint) + CELSIUS_DEGREE_NOTATION);
                    sendData(String.valueOf(String.format("%.2f", setPoint)));
                } catch (IOException ex) {
                    Log.e(TAG, ex.getMessage(), ex);
                    Toast.makeText(MainActivity.this.getBaseContext(), "ERROR! Couldn't send data to SousVide device.", Toast.LENGTH_LONG).show();
                }
            }
        });
        this.buttonAddDegree.setOnClickListener(new View.OnClickListener() {
            public void onClick(View paramAnonymousView) {
                try {
                    setPoint++;
                    textViewSetPointValue.setText(String.format("%.2f", setPoint) + CELSIUS_DEGREE_NOTATION);
                    sendData(String.valueOf(String.format("%.2f", setPoint)));
                } catch (IOException ex) {
                    Log.e(TAG, ex.getMessage(), ex);
                    Toast.makeText(MainActivity.this.getBaseContext(), "ERROR! Couldn't send data to SousVide device.", Toast.LENGTH_LONG).show();
                }
            }
        });
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
        addBluetoothDataListener();
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
                                            textViewCurrentTempValue.setText(data + CELSIUS_DEGREE_NOTATION);
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

    void sendData(String data) throws IOException {
        mmOutputStream.write(data.getBytes());
    }
}