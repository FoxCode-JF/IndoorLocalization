package com.example.indoorlocalization;

import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.util.HashMap;

public class ScanActivity extends AppCompatActivity {

    final static String TAG = "INDOOR_LOCALIZATION: ";
    private int REQUEST_ENABLE_BT = 1;
    private BluetoothAdapter bluetoothAdapter;
    private int txPower = -60;

    private boolean mScanning;
    private boolean mCalibrated;

    private ScanResult mScanResult;
    private Handler handler = new Handler();
    private static final long SCAN_PERIOD = 10000; // Stops scanning after 10 seconds.

    static HashMap<String, Integer> MACandRSSIMap = new HashMap<String, Integer>() {
        {
            //put("E4:34:93:81:24:D9", -60);
            put("CC:98:8B:CF:BC:82", -60);
        }
    };

    private Button calibrateBtn;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.scan_activity);

//        calibrateBtn = (Button) findViewById(R.id.buttonCalibrate);
      /*  calibrateBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Log.e(TAG, "KALIBRACJA!!!");
            }
        });*/

 /*       calibrateBtn = (Button) findViewById(R.id.buttonCalibrate);

        calibrateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e(TAG, "KALIBRACJA!!!");
            }
        });*/
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
   /* private BluetoothAdapter.LeScanCallback leScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
            if(MACandRSSIMap.containsKey(device.getAddress()))
            {
                Log.e(TAG, "onLeScan: Address: " + device.getAddress() + " Name: " + device.getName() + " RSSI: " + rssi);
            }

        }
    };*/

    private ScanCallback leScanCallback =
            new ScanCallback() {
                @Override
                public void onScanResult(int callbackType, final ScanResult result) {
                    super.onScanResult(callbackType, result);
                    determineTxPower(result);

                    mScanResult = result;

                    if(!mCalibrated){
                        if(MACandRSSIMap.containsKey(result.getDevice().getAddress())) {
                            Log.e(TAG, "onLeScan: Address: " + result.getDevice().getAddress() +
                                    " Name: " + result.getDevice().getName() +
                                    " RSSI: " + result.getRssi() +
                                    " txPower: " + txPower +
                                    " Distance: " + calculateDistance(result.getRssi(), txPower)
                            );
                        }
                    }
                }
            };

/*    private void scanLeDevice() {
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
    }*/

    private void scanLeDevice() {
        if (!mScanning) {
            new Runnable() {
                @Override
                public void run() {
                    stopLeScan(bluetoothAdapter.getBluetoothLeScanner());
                }
            };

            startLeScan(bluetoothAdapter.getBluetoothLeScanner());
        } else {
            stopLeScan(bluetoothAdapter.getBluetoothLeScanner());
        }
    }

    private void startLeScan(BluetoothLeScanner leScanner) {
        mScanning = true;
        leScanner.startScan(leScanCallback);
    }

    private void stopLeScan(BluetoothLeScanner leScanner) {
        mScanning = false;
        leScanner.stopScan(leScanCallback);
    }

    private double calculateDistance(int RSSI, int txPower) {

        double ratio = RSSI*1.0/txPower;
        if (ratio < 1.0) {
            return Math.pow(ratio,10);
        }
        else {
            double accuracy =  (0.89976)*Math.pow(ratio,7.7095) + 0.111;
            return accuracy;
        }
    }
    private void determineTxPower(ScanResult result)
    {
        if(result.getTxPower() == ScanResult.TX_POWER_NOT_PRESENT)
        {
            txPower = -69;
        }
        else {txPower = result.getTxPower();}
    }

    private void calibrateAt1m(ScanResult result) {
        determineTxPower(result);
        double distance = calculateDistance(result.getRssi(), txPower);
        int RSSI = result.getRssi();
        String address = result.getDevice().getAddress();
        int i = 0;

        if(distance > 0.8 && distance < 1.2) {
            txPower = (txPower + RSSI)/2;
        }
        MACandRSSIMap.replace(address, txPower);
    }
}


