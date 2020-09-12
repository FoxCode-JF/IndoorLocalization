package com.example.indoorlocalization;

import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.MacAddress;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import java.util.HashMap;
import java.util.Vector;
import java.util.logging.LogRecord;

public class ScanActivity extends AppCompatActivity {



    final static String TAG = "INDOOR_LOCALIZATION: ";
    private int REQUEST_ENABLE_BT = 1;
    private BluetoothAdapter bluetoothAdapter;

    private boolean mScanning;
//    private Handler handler = new Handler();
//    private static final long SCAN_PERIOD = 10000; // Stops scanning after 10 seconds.

    HashMap<String, Integer> MACandRSSIMap = new HashMap<String, Integer>() {
        {
            put("E4:34:93:81:24:D9", 1);
            put("CC:98:8B:CF:BC:82", 1);

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);



        final BluetoothManager bluetoothManager =(BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

        // Ensures Bluetooth is available on the device and it is enabled. If not,
        // displays a dialog requesting user permission to enable Bluetooth.
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        scanLeDevice();
    }
    // Device scan callback.
    private BluetoothAdapter.LeScanCallback leScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
            if(MACandRSSIMap.containsKey(device.getAddress()))
            {
                Log.e(TAG, "onLeScan: Address: " + device.getAddress() + " Name: " + device.getName() + " RSSI: " + rssi);
            }

        }
    };

    private void scanLeDevice() {
        if (!mScanning) {
            // Stops scanning after a pre-defined scan period.
            new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    bluetoothAdapter.stopLeScan(leScanCallback);
                }
            };

            mScanning = true;
            bluetoothAdapter.startLeScan(leScanCallback);
        } else {
            mScanning = false;
            bluetoothAdapter.stopLeScan(leScanCallback);
        }
    }
}


