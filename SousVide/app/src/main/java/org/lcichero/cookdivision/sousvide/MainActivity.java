package org.lcichero.cookdivision.sousvide;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Build;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;


public class MainActivity extends ActionBarActivity {

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

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                        BluetoothAdapter.ERROR);
                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        //setButtonText("Bluetooth off");
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        //setButtonText("Turning Bluetooth off...");
                        break;
                    case BluetoothAdapter.STATE_ON:
                        //setButtonText("Bluetooth on");
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        //setButtonText("Turning Bluetooth on...");
                        //break;
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        instantiateFrontendElements();

        // Register for broadcasts on BluetoothAdapter state change
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(mReceiver, filter);

        //checkBTState();
        this.textViewCurrentTempValue.setText(String.format("%.2f", currentTemp) + CELSIUS_DEGREE_NOTATION);
        this.textViewSetPointValue.setText(String.format("%.2f", setPoint) + CELSIUS_DEGREE_NOTATION);

        this.buttonSubtractDegree.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View paramAnonymousView)
            {
                //MainActivity.this.sendData("s");
                Log.d(TAG, "Subtracting 1 degree..");
                setPoint--;
                textViewSetPointValue.setText(String.format("%.2f", setPoint) + CELSIUS_DEGREE_NOTATION);
            }
        });

        this.buttonAddDegree.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View paramAnonymousView)
            {
                //MainActivity.this.sendData("a");
                Log.d(TAG, "Adding 1 degree..");
                setPoint++;
                textViewSetPointValue.setText(String.format("%.2f", setPoint) + CELSIUS_DEGREE_NOTATION);
            }
        });
    }

    private void instantiateFrontendElements() {
        this.buttonSubtractDegree = ((Button) findViewById(R.id.buttonSubtractDegree));
        this.buttonAddDegree = ((Button) findViewById(R.id.buttonAddDegree));
        this.textViewBluetoothStatusValue = ((TextView) findViewById(R.id.textViewBluetoothStatusValue));
        this.textViewCurrentTempValue = ((TextView) findViewById(R.id.textViewCurrentTempValue));
        this.textViewSetPointValue = ((TextView) findViewById(R.id.textViewSetPointValue));
    }

    public void onResume()
    {
        super.onResume();
        this.textViewBluetoothStatusValue.setText("disconnected");
        this.textViewBluetoothStatusValue.setTextColor(Color.RED);
        //establishBluetoothConnection();
    }

    private void establishBluetoothConnection() {
        Log.d(TAG, "Trying to connect..");
        BluetoothDevice localBluetoothDevice = this.btAdapter.getRemoteDevice(address);
        try
        {
            this.btSocket = createBluetoothSocket(localBluetoothDevice);
            this.btAdapter.cancelDiscovery();
            Log.d(TAG, "Connecting..");
        }
        catch (IOException localIOException1)
        {
            try
            {
                this.btSocket.connect();
                Log.d(TAG, "Connection OK..");
                Log.d(TAG, "Create Socket..");
                try
                {
                    this.textViewBluetoothStatusValue.setText("connected");
                    this.textViewBluetoothStatusValue.setTextColor(Color.GREEN);
                    this.outStream = this.btSocket.getOutputStream();
                    return;
                }
                catch (IOException localIOException4)
                {
                    errorExit("Fatal Error", "In onResume() and output stream creation failed:" + localIOException4.getMessage() + ".");
                }
                localIOException1 = localIOException1;
                errorExit("Fatal Error", "In onResume() and socket create failed: " + localIOException1.getMessage() + ".");
            }
            catch (IOException localIOException2)
            {
                for (;;)
                {
                    try
                    {
                        this.btSocket.close();
                    }
                    catch (IOException localIOException3)
                    {
                        errorExit("Fatal Error", "In onResume() and unable to close socket during connection failure" + localIOException3.getMessage() + ".");
                    }
                }
            }
        }
    }

    private void checkBTState()
    {
        if (this.btAdapter == null)
        {
            errorExit("Fatal Error", "Bluetooth not support.");
            return;
        }
        if (this.btAdapter.isEnabled())
        {
            Log.d(TAG, "Bluetooth ON..");
            return;
        }
        startActivityForResult(new Intent("android.bluetooth.adapter.action.REQUEST_ENABLE"), 1);
    }

    private BluetoothSocket createBluetoothSocket(BluetoothDevice paramBluetoothDevice)
            throws IOException
    {
        if (Build.VERSION.SDK_INT >= 10) {
            try
            {
                BluetoothSocket localBluetoothSocket = (BluetoothSocket)paramBluetoothDevice.getClass().getMethod("createInsecureRfcommSocketToServiceRecord", new Class[] { UUID.class }).invoke(paramBluetoothDevice, new Object[] { MY_UUID });
                return localBluetoothSocket;
            }
            catch (Exception localException)
            {
                Log.e(TAG, "Could not create Insecure RFComm Connection.", localException);
            }
        }
        return paramBluetoothDevice.createRfcommSocketToServiceRecord(MY_UUID);
    }

    private void errorExit(String paramString1, String paramString2)
    {
        Toast.makeText(getBaseContext(), paramString1 + " - " + paramString2, 1).show();
        finish();
    }

    private void sendData(String paramString)
    {
        Object localObject = paramString.getBytes();
        Log.d(TAG, "Send data: " + paramString + "..");
        try
        {
            this.outStream.write((byte[])localObject);
            return;
        }
        catch (IOException ioex)
        {
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // Unregister broadcast listeners
        unregisterReceiver(mReceiver);
    }
}