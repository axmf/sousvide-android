package org.lcichero.cookdivision.sousvide.activity;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.lcichero.cookdivision.sousvide.R;

import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;

public class MainActivity extends Activity {
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final String TAG = "sousvide_bt";
    private static String address = "00:12:08:20:08:04";

    private BluetoothAdapter btAdapter = null;
    private BluetoothSocket btSocket = null;
    private OutputStream outStream = null;

    private Button buttonSubtractDegree;
    private Button buttonAddDegree;
    private TextView textViewBluetoothStatusValue;
    private TextView textViewCurrentTempValue;
    private TextView textViewSetPointValue;
    private float currentTemp = 58F;
    private float setPoint = 60F;
    private final String CELSIUS_DEGREE_NOTATION =  " " + (char) 0x00B0 + "C";

    private void checkBTState() {
        if (this.btAdapter == null) {
            errorExit("Fatal Error", "Bluetooth not support");
            return;
        }
        if (this.btAdapter.isEnabled()) {
            Log.d(TAG, "...Bluetooth ON...");
            return;
        }
        startActivityForResult(new Intent("android.bluetooth.adapter.action.REQUEST_ENABLE"), 1);
    }

    private BluetoothSocket createBluetoothSocket(BluetoothDevice paramBluetoothDevice)
            throws IOException {
        if (Build.VERSION.SDK_INT >= 10) {
            try {
                BluetoothSocket localBluetoothSocket = (BluetoothSocket) paramBluetoothDevice.getClass().getMethod("createInsecureRfcommSocketToServiceRecord", new Class[]{UUID.class}).invoke(paramBluetoothDevice, new Object[]{MY_UUID});
                return localBluetoothSocket;
            } catch (Exception localException) {
                Log.e(TAG, "Could not create Insecure RFComm Connection", localException);
            }
        }
        return paramBluetoothDevice.createRfcommSocketToServiceRecord(MY_UUID);
    }

    private void errorExit(String paramString1, String paramString2) {
        Toast.makeText(getBaseContext(), paramString1 + " - " + paramString2, Toast.LENGTH_LONG).show();
        finish();
    }

    private void sendData(String paramString) {
        Object localObject = paramString.getBytes();
        Log.d(TAG, "...Send data: " + paramString + "...");
        try {
            this.outStream.write((byte[]) localObject);
            return;
        } catch (IOException ioex) {
            localObject = "In onResume() and an exception occurred during write: " + ioex.getMessage();
            paramString = (String) localObject;
            if (address.equals("00:00:00:00:00:00")) {
                paramString = localObject + ".\n\nUpdate your server address from 00:00:00:00:00:00 to the correct address on line 35 in the java code";
            }
            errorExit("Fatal Error", paramString + ".\n\nCheck that the SPP UUID: " + MY_UUID.toString() + " exists on server.\n\n");
        }
    }

    public void onCreate(Bundle paramBundle) {
        super.onCreate(paramBundle);
        setContentView(R.layout.activity_main);
        this.buttonSubtractDegree = ((Button) findViewById(R.id.buttonSubtractDegree));
        this.buttonAddDegree = ((Button) findViewById(R.id.buttonAddDegree));
        this.textViewBluetoothStatusValue = ((TextView) findViewById(R.id.textViewBluetoothStatusValue));
        this.textViewCurrentTempValue = ((TextView) findViewById(R.id.textViewCurrentTempValue));
        this.textViewSetPointValue = ((TextView) findViewById(R.id.textViewSetPointValue));
        this.btAdapter = BluetoothAdapter.getDefaultAdapter();
        checkBTState();
        this.buttonSubtractDegree.setOnClickListener(new View.OnClickListener() {
            public void onClick(View paramAnonymousView) {
                MainActivity.this.sendData("a");
                Toast.makeText(MainActivity.this.getBaseContext(), "Subtracting 1 degree.", Toast.LENGTH_LONG).show();
            }
        });
        this.buttonAddDegree.setOnClickListener(new View.OnClickListener() {
            public void onClick(View paramAnonymousView) {
                MainActivity.this.sendData("b");
                Toast.makeText(MainActivity.this.getBaseContext(), "Adding 1 degree.", Toast.LENGTH_LONG).show();
            }
        });
    }

    public void onPause() {
        super.onPause();
        Log.d(TAG, "...In onPause()...");
        if (this.outStream != null) {
        }
        try {
            this.outStream.flush();
        } catch (IOException localIOException1) {
            for (; ; ) {
                try {
                    this.btSocket.close();
                    return;
                } catch (IOException localIOException2) {
                    errorExit("Fatal Error", "In onPause() and failed to close socket." + localIOException2.getMessage() + ".");
                }
                localIOException1 = localIOException1;
                errorExit("Fatal Error", "In onPause() and failed to flush output stream: " + localIOException1.getMessage() + ".");
            }
        }
    }

    public void onResume() {
        super.onResume();
        this.textViewBluetoothStatusValue.setText("disconnected");
        this.textViewBluetoothStatusValue.setTextColor(Color.RED);
        Log.d(TAG, "...onResume - try connect...");
        BluetoothDevice localBluetoothDevice = this.btAdapter.getRemoteDevice(address);
        try {
            this.btSocket = createBluetoothSocket(localBluetoothDevice);
            this.btAdapter.cancelDiscovery();
            Log.d(TAG, "...Connecting...");
            try {
                this.btSocket.connect();
                Log.d(TAG, "...Connection ok...");
                Log.d(TAG, "...Create Socket...");
                try {
                    this.outStream = this.btSocket.getOutputStream();
                    this.textViewBluetoothStatusValue.setText("connected");
                    this.textViewBluetoothStatusValue.setTextColor(Color.GREEN);
                    return;
                } catch (IOException localIOException4) {
                    errorExit("Fatal Error", "In onResume() and output stream creation failed:" + localIOException4.getMessage() + ".");
                }
            } catch (IOException localIOException2) {
                for (; ; ) {
                    try {
                        this.btSocket.close();
                    } catch (IOException localIOException3) {
                        errorExit("Fatal Error", "In onResume() and unable to close socket during connection failure" + localIOException3.getMessage() + ".");
                    }
                }
            }
        } catch (IOException localIOException1) {
            localIOException1 = localIOException1;
            errorExit("Fatal Error", "In onResume() and socket create failed: " + localIOException1.getMessage() + ".");
        }
    }
}