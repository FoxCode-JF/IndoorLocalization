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
import android.net.MacAddress;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class ScanActivity extends AppCompatActivity  {

    final static String TAG = "INDOOR_LOCALIZATION: ";
    private int REQUEST_ENABLE_BT = 1;
    private BluetoothAdapter bluetoothAdapter;
    private int txPower = -60;

    private boolean mScanning;
    private boolean mCalibrated;

    private ScanResult mScanResult;
    private Handler handler = new Handler();
    private static final long SCAN_PERIOD = 10000; // Stops scanning after 10 seconds.

    Button calibrateBtn;
    Button startBtn;
    Button stopBtn;
    static HashMap<String, Integer> MACandTxPowerMap = new HashMap<String, Integer>() {
        {
            put("88:40:3B:EE:97:6B", -60);
            put("CC:98:8B:CF:BC:82", -60);
        }
    };

    static HashMap<String, Integer> MACandRSSIMap = new HashMap<String, Integer>();



    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.scan_activity);


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
        calibrateBtn = findViewById(R.id.button);
        calibrateBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

//                if(mScanning)  stopLeScan(bluetoothAdapter.getBluetoothLeScanner());
                startLeScan(bluetoothAdapter.getBluetoothLeScanner());
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Log.e(TAG, "Calibrating!!!");
//                      TODO: find a way to discard scan result == null
                        if(mScanResult.getDataStatus() == ScanResult.DATA_COMPLETE) {
                            calibrateAt1m(mScanResult);
                            Log.e(TAG, "onLeScan: Address: " + mScanResult.getDevice().getAddress() +
                                    " Name: "+ mScanResult.getDevice().getName() +
                                    " RSSI: " + mScanResult.getRssi() +
                                    " txPower: " + txPower +
                                    " Distance: " + calculateDistance(mScanResult.getRssi(), txPower)
                            );
                        }
                    }
                });
            }
        });

        stopBtn = findViewById(R.id.stop_scan);
        stopBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        stopLeScan(bluetoothAdapter.getBluetoothLeScanner());
                    }
                });
            }
        });
        startBtn = findViewById(R.id.start_scan);
        startBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        startLeScan(bluetoothAdapter.getBluetoothLeScanner());
                    }
                });
            }
        });
    }

    private ScanCallback leScanCallback =
            new ScanCallback() {
                @Override
                public void onScanResult(int callbackType, final ScanResult result) {
                    super.onScanResult(callbackType, result);
//                    TODO: determine power correctry or delete function
//                    determineTxPower(result);

//                    if(MACandTxPowerMap.containsKey(result.getDevice().getAddress()))
                    {
                        mScanResult = result;
                        getDeviceTxPower(result);
                        /*Log.i(TAG, "onLeScan: Address: " + result.getDevice().getAddress() +
                                " Name: " + result.getDevice().getName() +
                                " RSSI: " + result.getRssi() +
                                " txPower: " + txPower +
                                " Distance: " + calculateDistance(result.getRssi(), txPower)
                        );*/
                        if(!MACandRSSIMap.containsKey(result.getDevice().getAddress())) {
                            MACandRSSIMap.put(result.getDevice().getAddress(), result.getRssi());
                        } else {
                            MACandRSSIMap.replace(result.getDevice().getAddress(), result.getRssi());
                        }
                        MACandRSSIMap.entrySet().forEach(entry->{
                            System.out.println(entry.getKey() + " " + entry.getValue());
                        });
                        boundingBox();
                    }
                }
            };

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
            double accuracy = (0.89976)*Math.pow(ratio,7.7095) + 0.111;
            return accuracy;
        }
    }
    private void getDeviceTxPower(ScanResult result)
    {
        int defaultTxPower = -52;
        String address = result.getDevice().getAddress();
        if(address != null && result.getTxPower() == ScanResult.TX_POWER_NOT_PRESENT)
        {
            if (MACandTxPowerMap.containsKey(address)) {
                txPower = MACandTxPowerMap.get(address);
            }
        }
        else if (result.getTxPower() != ScanResult.TX_POWER_NOT_PRESENT) {
            txPower = result.getTxPower();
        } else {
            txPower = defaultTxPower;
        }
    }

    private void calibrateAt1m(ScanResult result) {
//        determineTxPower(result);
        int RSSI = result.getRssi();
        String address = result.getDevice().getAddress();
        txPower = (txPower + RSSI)/2;
        MACandTxPowerMap.replace(address, txPower);
    }
    private void boundingBox() {
//        sort beacons by signal strength
        List<Map.Entry<String, Integer> > sortedBeacons = sortByValue(MACandRSSIMap);
        int listSize = sortedBeacons.size();
//        get closest beacons
        if(listSize > 3)
        {
            List<Map.Entry<String, Integer> > closestBeacons = sortedBeacons.subList(listSize - 3, listSize);
            HashMap<String, Integer> temp = new LinkedHashMap<String, Integer>();

            for (Map.Entry<String, Integer> aa : closestBeacons) {
                temp.put(aa.getKey(), aa.getValue());
            }

            temp.entrySet().forEach(entry->{
                System.out.println("\n\n" + entry.getKey() + " " + entry.getValue() + "\n\n");
            });
        }


        // put data from sorted list to hashmap


    }

    // function to sort hashmap by values
    public static List<Map.Entry<String, Integer> > sortByValue(HashMap<String, Integer> hm)
    {
        // Create a list from elements of HashMap
        List<Map.Entry<String, Integer> > list =
                new LinkedList<Map.Entry<String, Integer> >(hm.entrySet());

        // Sort the list
        Collections.sort(list, new Comparator<Map.Entry<String, Integer> >() {
            public int compare(Map.Entry<String, Integer> o1,
                               Map.Entry<String, Integer> o2)
            {
                return (o1.getValue()).compareTo(o2.getValue());
            }
        });

        /*// put data from sorted list to hashmap
        HashMap<String, Integer> temp = new LinkedHashMap<String, Integer>();
        for (Map.Entry<String, Integer> aa : list) {
            temp.put(aa.getKey(), aa.getValue());
        }*/
        return list;
    }
}


