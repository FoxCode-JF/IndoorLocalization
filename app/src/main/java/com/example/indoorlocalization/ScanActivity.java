package com.example.indoorlocalization;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.math.MathUtils;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.ArrayMap;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;

public class ScanActivity extends AppCompatActivity  implements LocationListener, OnMapReadyCallback {

    final static String TAG = "INDOOR_LOCALIZATION: ";
    private static final int  EARTH_RADIUS = 6371000;
    private int REQUEST_ENABLE_BT = 1;
    private BluetoothAdapter bluetoothAdapter;
    private int txPower = -60;
    private double myLong = 0;
    private double myLat = 0;
    private double px0;
    private double py0;

    private boolean mScanning;
    private boolean mCalibrated;
    private LocationManager locationManager;
    private Marker mMarker;
    private GoogleMap mMap;
    private double latitude;
    private double longitude;
    private boolean first = true;
    private Marker iMarker;
    private ScanResult mScanResult;
    private Handler handler = new Handler();
    private static final long SCAN_PERIOD = 10000; // Stops scanning after 10 seconds.

    Button calibrateBtn;
    Button startBtn;
    Button stopBtn;
    static HashMap<String, Integer> MACandTxPowerMap = new HashMap<String, Integer>() {
        {
//            put("F8:DF:15:C1:B5:7E", -60);  // watch
//            put("CC:98:8B:CF:BC:82", -60);  // Jerek headphones
//            put("38:18:4C:17:54:80", -60);  // Andreas Headphones
//            put("1B:FC:EE:F5:93:3D", -60); // Jerek Phone
//            put("64:A2:F9:B5:28:69", -60); // Andreas Phone
//
//
//            put("3B:C8:4F:17:35:30", -60); // Andreas Phone
//            put("12:1E:3C:32:41:4C", -60); // Jerek Phone
//
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


//            TEST COORDINATES

//            put("F8:DF:15:C1:B5:7E", new Pair<Double, Double>(52.238804, 6.856384));  // watch
//            put("CC:98:8B:CF:BC:82", new Pair<Double, Double>(52.238813, 6.855889));  // Jerek headphones
//            put("38:18:4C:17:54:80", new Pair<Double, Double>(52.238943, 6.856113));  // Andreas Headphones
//            put("1B:FC:EE:F5:93:3D", new Pair<Double, Double>(52.238755, 6.856647)); // Jerek Phone
//            put("38:18:4C:17:54:80", new Pair<Double, Double>(52.238943, 6.856113));  // Andreas Headphones
//
//            put("3B:C8:4F:17:35:30", new Pair<Double, Double>(52.238714, 6.856247)); // Andreas Phone
//            put("12:1E:3C:32:41:4C", new Pair<Double, Double>(52.238755, 6.856647)); // Jerek Phone

           /*
            13:7D:D4:1B:D2:C3 -78
            3C:7A:91:AA:98:DC -85
            0D:A5:43:D7:67:88 -88
            4A:0C:24:B0:4D:AE -82
            2A:39:77:B3:66:AA -91
            07:C9:59:B6:98:3D -96
            46:93:ED:5E:6F:99 -82
            1E:D4:58:7D:55:4B -95
            09:B4:44:AF:3B:0C -99
            53:F4:86:49:29:1D -98
            07:4A:27:F7:19:7A -73
            CC:98:8B:CF:BC:82 -49
            54:DD:86:AB:1A:CC -93
            50:F1:4F:EE:59:28 -91
            25:7E:F5:38:98:62 -101
            71:E2:34:17:B6:62 -77
            18:18:00:3D:61:39 -94
            66:93:EF:6E:93:17 -84
            25:BB:D0:64:49:5F -73
            2D:A7:82:23:7F:6A -65
            2A:6A:0E:9E:E5:25 -87
            5A:97:E7:70:97:23 -97
            0D:B6:78:A7:20:6A -95
            3A:3F:33:A5:61:42 -95
            08:96:10:E2:14:5F -90
            03:4A:D7:47:C5:2B -94*/

        }
    };

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.scan_activity);
        MapFragment mapFragment = (MapFragment) getFragmentManager()
                .findFragmentById(R.id.map);

        mapFragment.getMapAsync(this);
        checkPermission();
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10, 1 , this);

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

                if(mScanning)  stopLeScan(bluetoothAdapter.getBluetoothLeScanner());
                startLeScan(bluetoothAdapter.getBluetoothLeScanner());
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Log.e(TAG, "Calibrating!!!");
//                      TODO: find a way to discard scan result == null
                        if(mScanResult != null && mScanResult.getDataStatus() == ScanResult.DATA_COMPLETE) {
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

                    if(MACandTxPowerMap.containsKey(result.getDevice().getAddress()))
                    {
                        mScanResult = result;
                        getDeviceTxPower(result);
                        Log.i(TAG, "onLeScan: Address: " + result.getDevice().getAddress() +
                                " Name: " + result.getDevice().getName() +
                                " RSSI: " + result.getRssi() +
                                " txPower: " + txPower +
                                " Distance: " + calculateDistance(result.getRssi(), txPower)
                        );
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
        if(listSize >= 3) {
            temp = sortedBeacons.subList(listSize - 3, listSize);
            closestBeacons = new ArrayMap<>();

            for (Map.Entry<String, Integer> beacon : temp) {
                closestBeacons.put(beacon.getKey(), beacon.getValue());
            }
            AtomicInteger i = new AtomicInteger();
            Vector<Pair<Double, Double> > pointVec = new Vector<>();

//            translate longitude and latitude to x, y coordinates
            for (Map.Entry<String, Integer> entry: closestBeacons.entrySet()) {
//                System.out.println("\n\n" + entry.getKey() + " " + entry.getValue() + "\n\n");
                String deviceName = entry.getKey();
                if(deviceCoordinates.containsKey(deviceName)) {
                    mMap.addMarker(new MarkerOptions()
                            .position(new LatLng(deviceCoordinates.get(entry.getKey()).first, deviceCoordinates.get(entry.getKey()).second))
                            .icon(BitmapDescriptorFactory.defaultMarker(180))
                            .title(deviceName));


                    double x = EARTH_RADIUS * Math.cos(Math.toRadians(deviceCoordinates.get(entry.getKey()).first)) * Math.cos(Math.toRadians(deviceCoordinates.get(entry.getKey()).second));
                    double y = EARTH_RADIUS * Math.sin(Math.toRadians(deviceCoordinates.get(entry.getKey()).first)) * Math.cos(Math.toRadians(deviceCoordinates.get(entry.getKey()).second));
                    if (pointVec.size() < 3)
                        pointVec.addElement(Pair.create(x, y));
                    else {
                        pointVec.set(i.get(), Pair.create(x, y));
                        i.getAndIncrement();
                    }
                }
            }


            if(!pointVec.isEmpty()){
               /* for (Pair<Double, Double> iter: pointVec) {
                    System.out.println("first: " + iter.first + "second: " + iter.second );
                }*/
//                translate points so pointVec[0] is at the origin
                px0 = pointVec.get(0).first;
                py0 = pointVec.get(0).second;
                pointVec.set(0, Pair.create(0.0, 0.0));
                pointVec.set(1, Pair.create(pointVec.get(1).first - px0, pointVec.get(1).second - py0));
                pointVec.set(2, Pair.create(pointVec.get(2).first - px0, pointVec.get(2).second - py0));

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
            double aX, cX;
            aX = cX = pointVec.get(2).first - r2;
            double aY, bY;
            aY = bY = pointVec.get(2).second + r2;
            double bX, dX;
            bX = dX = pointVec.get(0).first + r0;
            double cY, dY;
            cY = dY = pointVec.get(1).second + r1;

            double middleX = (bX - aX) / 2;
            double middleY = (cY - aY) / 2;
            double middleLen = Math.sqrt(Math.pow(middleX, 2) - Math.pow(middleY, 2));

            double distanceBack = Math.sqrt(Math.pow(px0, 2) + Math.pow(py0, 2));
            double iX = px0 / distanceBack;
            double iY = py0 / distanceBack;
            double dotProduct_middle = iX * middleX + iY * middleY;

            double middleYCoord = Math.sqrt(Math.pow(middleLen, 2) - Math.pow(dotProduct_middle, 2)) +py0;
            dotProduct_middle += px0;
            double longti = Math.atan(middleYCoord/dotProduct_middle);
            double lati = Math.acos(middleYCoord/(EARTH_RADIUS * Math.sin(longti)));

            myLong = Math.toDegrees(longti);
            myLat = Math.toDegrees(lati);

            System.out.println(longti + "\n" + lati + "\n\n\n");
            iMarker.setPosition(new LatLng(longti, lati));



            }

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

    @Override
    public void onLocationChanged(@NonNull Location location) {
        latitude = location.getLatitude();
        longitude = location.getLongitude();
        mMarker.setPosition(new LatLng(latitude, longitude));
        if(first){
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(latitude, longitude), 16));
            first = false;
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMarker = mMap.addMarker(new MarkerOptions()
                .position(new LatLng(latitude, longitude))
                .title("GPS position"));
        mMarker.setIcon((BitmapDescriptorFactory.defaultMarker(270)));
        iMarker = mMap.addMarker(new MarkerOptions()
                .position(new LatLng(0, 0))
                .title("Bounding box position"));
        iMarker.setIcon((BitmapDescriptorFactory.defaultMarker(90)));
    }
    public void checkPermission() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {


            } else {
                ActivityCompat.requestPermissions(this, new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,}, 1);
            }
        }
    }
}


