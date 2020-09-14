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
import android.util.ArrayMap;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;

public class ScanActivity extends AppCompatActivity  {

    final static String TAG = "INDOOR_LOCALIZATION: ";
    private static final int  EARTH_RADIUS = 6371000;
    private int REQUEST_ENABLE_BT = 1;
    private BluetoothAdapter bluetoothAdapter;
    private int txPower = -60;
    private double myLong = 0;
    private double myLat = 0;

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
            put("88:40:3B:EE:97:6B", -60);  // watch
            put("CC:98:8B:CF:BC:82", -60);  // headphones
//            Thingy's:
            put("dc:ee:f9:e0:3d:4e", -60);
            put("f9:fd:a4:88:ce:9e", -60);
            put("c2:ad:fe:9c:c0:83", -60);
            put("da:84:e1:48:94:e9", -60);
            put("db:27:6f:79:e3:a5", -60);
            put("d8:08:fc:e6:9a:67", -60);
            put("cc:cf:ae:c4:c7:cb", -60);
            put("f4:63:6b:d0:77:b9", -60);
            put("e1:80:f0:25:83:1c", -60);
            put("c0:88:dd:ec:c1:3d", -60);
        }
    };

    static HashMap<String, Integer> MACandRSSIMap = new HashMap<String, Integer>();

//    Map devices to coordinates
    static final HashMap<String, Pair<Double, Double> > deviceCoordinates = new HashMap<String, Pair<Double, Double> >() {
        {
            put("dc:ee:f9:e0:3d:4e", new Pair<Double, Double>(52.238976, 6.856558));
            put("f9:fd:a4:88:ce:9e", new Pair<Double, Double>(52.238755, 6.856647));
            put("c2:ad:fe:9c:c0:83", new Pair<Double, Double>(52.238804, 6.856384));
            put("da:84:e1:48:94:e9", new Pair<Double, Double>(52.238865, 6.856193));
            put("db:27:6f:79:e3:a5", new Pair<Double, Double>(52.238875, 6.856024));
            put("d8:08:fc:e6:9a:67", new Pair<Double, Double>(52.239053, 6.856413));
            put("cc:cf:ae:c4:c7:cb", new Pair<Double, Double>(52.238907, 6.856808));
            put("f4:63:6b:d0:77:b9", new Pair<Double, Double>(52.238714, 6.856247));
            put("e1:80:f0:25:83:1c", new Pair<Double, Double>(52.238813, 6.855889));
            put("c0:88:dd:ec:c1:3d", new Pair<Double, Double>(52.238943, 6.856113));
        }
    };

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
                            Log.i(TAG, "onLeScan: Address: " + mScanResult.getDevice().getAddress() +
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
        List<Map.Entry<String, Integer> > temp;
        ArrayMap<String, Integer> closestBeacons;

        int listSize = sortedBeacons.size();
//        get closest beacons
        if(listSize > 3) {
            temp = sortedBeacons.subList(listSize - 3, listSize);
            closestBeacons = new ArrayMap<>();

            for (Map.Entry<String, Integer> beacon : temp) {
                closestBeacons.put(beacon.getKey(), beacon.getValue());
            }
            AtomicInteger i = new AtomicInteger();
            Vector<Pair<Double, Double> > pointVec = new Vector<>();

//            translate longitude and latitude to x, y coordinates
            for (Map.Entry<String, Integer> entry: closestBeacons.entrySet()) {
                System.out.println("\n\n" + entry.getKey() + " " + entry.getValue() + "\n\n");
                String deviceName = entry.getKey();
                if(deviceCoordinates.containsKey(deviceName)) {
                    double x = EARTH_RADIUS * Math.cos(deviceCoordinates.get(entry.getKey()).first) * Math.cos(deviceCoordinates.get(entry.getKey()).second);
                    double y = EARTH_RADIUS * Math.sin(deviceCoordinates.get(entry.getKey()).first) * Math.cos(deviceCoordinates.get(entry.getKey()).second);

                    if (pointVec.size() < 3)
                        pointVec.addElement(Pair.create(x, y));
                    else {
                        pointVec.set(i.get(), Pair.create(x, y));
                        i.getAndIncrement();
                    }
                }
            }


            if(!pointVec.isEmpty()){
                for (Pair<Double, Double> iter: pointVec) {
                    System.out.println("first: " + iter.first + "second: " + iter.second );
                }
//                translate points so pointVec[0] is at the origin
                double px0 = pointVec.get(0).first;
                double py0 = pointVec.get(0).second;
                pointVec.set(0, Pair.create(0.0, 0.0));
                pointVec.set(0, Pair.create(pointVec.get(1).first - px0, pointVec.get(1).second - py0));
                pointVec.set(0, Pair.create(pointVec.get(2).first - px0, pointVec.get(2).second - py0));
            }
//                rotate coordinate system so that pointVec[1] is on the X axis
//                pointVec[1] = (d,0) where d = distance(pointVec[1], (0,0))
//                i_hat = unit vector in direction of pointVec[1]
//                pointVec[2].first = dotProduct(i_hat, pointVec[2])
//                pointVec[2].second = sqrt( (length( pointVec[2] ) )^2 -  (pointVec[2].second)^2 )
                double d1 = Math.sqrt(Math.pow(pointVec.get(1).first, 2) + Math.pow(pointVec.get(1).second, 2));
                double i_hatX = pointVec.get(1).first / d1;
                double i_hatY = pointVec.get(1).second / d1;
                pointVec.set(1, Pair.create(d1, 0.0));

                double d2 = Math.sqrt(Math.pow(pointVec.get(2).first, 2) + Math.pow(pointVec.get(2).second, 2));
                double dotProduct = i_hatX * pointVec.get(2).first + i_hatY * pointVec.get(2).second;
                double pointVecYCoord = Math.sqrt(Math.pow(d2, 2) - Math.pow(dotProduct, 2));

                pointVec.set(2, Pair.create(dotProduct, pointVecYCoord));
                /*
                *   ACTUAL Detection algorithm
                *   Bounding Box
                *
                */

//                1. Determine vertices of bounding rectangle
            double r0 = calculateDistance(closestBeacons.valueAt(0), MACandTxPowerMap.get(closestBeacons.keyAt(0)));
            double r1 = calculateDistance(closestBeacons.valueAt(1), MACandTxPowerMap.get(closestBeacons.keyAt(1)));
            double r2 = calculateDistance(closestBeacons.valueAt(2), MACandTxPowerMap.get(closestBeacons.keyAt(2)));
            double aX = pointVec.get(2).first - r2;

        }
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


