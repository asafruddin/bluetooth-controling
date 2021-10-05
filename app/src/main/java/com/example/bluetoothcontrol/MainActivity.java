package com.example.bluetoothcontrol;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity {
    private TextView mainText;
    private Button buttonPress;
    private ListView listDevices;

    private BluetoothAdapter bluetoothAdapter;

    private ArrayAdapter<String> arrayAdapter;

    public List<String> bluetoothDevices = new ArrayList<String>();
    public List<String> devicesAddresses = new ArrayList<String>();

    private static final int REQUEST_ENABLE_BLUETOOTH = 100;
    private static final int PICK_FILE = 101;

    ActivityResultLauncher<Intent> activityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {

                }
            });

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction().toString();
            Log.d("BLUETOOTH CONTROL", action);

            if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                buttonPress.setClickable(true);
                buttonPress.setEnabled(true);
            } else if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice bluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String deviceName = bluetoothDevice.getName();
                String address = bluetoothDevice.getAddress();
                String rssi = String.valueOf(intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE));

                if (!devicesAddresses.contains(address)) {
                    devicesAddresses.add(address);

                    String deviceString = "";
                    if (deviceName == null || deviceName.isEmpty()) {
                        deviceString = address + " - RSSI " + rssi + " dB";
                    } else {
                        deviceString = deviceName + " - RSSI " + rssi + " dB";
                    }

                    bluetoothDevices.add(deviceString);
                    arrayAdapter.notifyDataSetChanged();
                }
            } else if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);

                if (state == BluetoothAdapter.STATE_ON) {
                    buttonPress.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            searchBluetoothDevice();
                        }
                    });
                } else if (state == BluetoothAdapter.STATE_OFF) {
                    Log.d("BLUETOOTH CONTROL", "State off");
                }
            } else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                switch (intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothAdapter.ERROR)) {
                    case BluetoothDevice.BOND_BONDING:
                        Toast.makeText(MainActivity.this, "Connecting", Toast.LENGTH_SHORT).show();
                    case BluetoothDevice.BOND_BONDED:
                        Toast.makeText(MainActivity.this, "Connected Successfully", Toast.LENGTH_LONG).show();
                    case BluetoothDevice.BOND_NONE:
                        Toast.makeText(MainActivity.this, "Connection failed", Toast.LENGTH_LONG).show();
                    case BluetoothDevice.ERROR:
                        Toast.makeText(MainActivity.this, "Connection failed", Toast.LENGTH_LONG).show();
                }
            }
        }
    };

    public void searchBluetoothDevice() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                startSearchingDevices();
            } else if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED) {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);

                builder.setMessage("Location permission required to share files via bluetooth");
                builder.setCancelable(false);
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
                    }
                });

                // show alert dialog
                AlertDialog alertDialog = builder.create();
                alertDialog.setTitle("Location Permission");
                alertDialog.show();
            }
        } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            startSearchingDevices();
        }
    }

    private void startSearchingDevices() {
        buttonPress.setEnabled(false);
        buttonPress.setClickable(false);

        bluetoothDevices.clear();
        devicesAddresses.clear();

        if (bluetoothAdapter.isEnabled()) {
            bluetoothAdapter.startDiscovery();
        } else {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            activityResultLauncher.launch(enableBtIntent);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        buttonPress = findViewById(R.id.buttonPress);
        mainText = findViewById(R.id.mainText);
        listDevices = findViewById(R.id.listDevices);

        arrayAdapter = new ArrayAdapter<>(this, R.layout.support_simple_spinner_dropdown_item, bluetoothDevices);

        listDevices.setAdapter(arrayAdapter);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        IntentFilter intentFilter = new IntentFilter();

        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);

        registerReceiver(broadcastReceiver, intentFilter);

        buttonPress.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                searchBluetoothDevice();
            }
        });

        listDevices.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                onListItemClick(i);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(broadcastReceiver);
    }

    protected void onListItemClick(int i) {
        if (bluetoothAdapter.isEnabled()) {
            if (BluetoothAdapter.checkBluetoothAddress(devicesAddresses.get(i))) {
                BluetoothDevice bluetoothDevice = bluetoothAdapter.getRemoteDevice(devicesAddresses.get(i));

                String deviceName = bluetoothDevices.get(i);
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("Bluetooth Device");
                builder.setMessage("Connect to Bluetooth device " + deviceName);

                builder.setPositiveButton("Connect", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if (bluetoothDevice.createBond()) {
                            Log.d("BLUETOOTH CONTROL", "Connection started");
                        } else {
                            switch (bluetoothDevice.getBondState()) {
                                case BluetoothDevice.BOND_NONE:
                                    Toast.makeText(MainActivity.this, "Connection Failed", Toast.LENGTH_LONG).show();
                                case BluetoothDevice.BOND_BONDING:
                                    Toast.makeText(MainActivity.this, "Connecting...", Toast.LENGTH_SHORT).show();
                                case BluetoothDevice.BOND_BONDED:
                                    Toast.makeText(MainActivity.this, "Connected", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                });

                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                });

                AlertDialog alertDialog = builder.create();
                alertDialog.setCancelable(false);

                alertDialog.show();
            } else {
                Toast.makeText(MainActivity.this, "Invalid MAC Address", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(MainActivity.this, "Enable Bluetooth", Toast.LENGTH_SHORT).show();
        }
    }
}