package com.ray.mulitfunctionaldryer.view;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.ray.mulitfunctionaldryer.R;
import com.ray.mulitfunctionaldryer.component.BottomNavigation;

import java.util.HashSet;
import java.util.Set;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class SelectBTActivity extends AppCompatActivity {
    private BluetoothAdapter bluetoothAdapter;
    private final Set<BluetoothDevice> discoveredDevices = new HashSet<>();
    public String Address,Name;
    public BluetoothDevice BTDevice;
    private RecyclerViewAdapter recyclerViewAdapter;
    public boolean isConnected = false;
    private Toolbar toolbar;
    private Button buttonDiscovery;

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                final BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device == null) return;

                Log.d("onReceive", device.getName() + ":" + device.getAddress());

                discoveredDevices.add(device);
                updateList();
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                stopDiscovery();
            } else if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
                isConnected = true;
                System.out.println("conn");
                //Device is now connected
            } else if (BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED.equals(action)) {
                //Device is about to disconnect
            } else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                isConnected = false;
                System.out.println("disconn");
                //Device has disconnected
            }
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_selectbt);
        BottomNavInit();
        setToolbar();
        initView();
        initDiscoverBtn();
        initFilter();
        updateList();
    }

    private void initView(){
        final LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(RecyclerView.VERTICAL);

        final DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(this, layoutManager.getOrientation());

        recyclerViewAdapter = new RecyclerViewAdapter();
        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(recyclerViewAdapter);
        recyclerView.addItemDecoration(dividerItemDecoration);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            new AlertDialog.Builder(this)
                    .setTitle("本裝置不支援藍芽功能")
                    .setCancelable(false)
                    .setMessage("本裝置不支援藍芽功能，程式即將結束。")
                    .setNeutralButton("結束", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            finish();
                        }
                    })
                    .show();
        }
    }

    private void initDiscoverBtn(){
        buttonDiscovery = findViewById(R.id.btBTDiscover);
        buttonDiscovery.setOnClickListener(view -> {
            if (PackageManager.PERMISSION_GRANTED != ContextCompat.checkSelfPermission(SelectBTActivity.this, "android.permission.ACCESS_COARSE_LOCATION")) {
                ActivityCompat.requestPermissions(SelectBTActivity.this, new String[]{"android.permission.ACCESS_COARSE_LOCATION"}, 0);
                return;
            }

            if (!bluetoothAdapter.isEnabled()) {
                Intent intentBluetoothEnable = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivity(intentBluetoothEnable);
                return;
            }
            discoverDevices();
        });
    }

    private void initFilter(){
        final IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        registerReceiver(broadcastReceiver, filter);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(broadcastReceiver, filter);
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        registerReceiver(broadcastReceiver, filter);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        registerReceiver(broadcastReceiver, filter);
    }

    private void setToolbar(){
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(getString(R.string.select_bluetooth_toolbar_title));
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        toolbar.setNavigationOnClickListener(v ->{
            Intent intent = new Intent(this,MainActivity.class);
            startActivity(intent);
            finish();
        } );
    }

    private void BottomNavInit() {
        BottomNavigationView MyBtmNav = findViewById(R.id.Bottom_Main);
        BottomNavigation BtmNav = new BottomNavigation(this, MyBtmNav,2);
        BtmNav.init();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        unregisterReceiver(broadcastReceiver);
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopDiscovery();
    }

    private void startDiscovery() {
        bluetoothAdapter.startDiscovery();
        buttonDiscovery.setText("正在搜尋藍芽裝置…");
    }

    private void stopDiscovery() {
        if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }
        buttonDiscovery.setText("搜尋附近的藍芽裝置");
    }

    private void discoverDevices() {
        stopDiscovery();

        discoveredDevices.clear();
        updateList();

        startDiscovery();
    }

    private void updateList() {
        recyclerViewAdapter.notifyDataSetChanged();
    }

    public class RecyclerViewHolder extends RecyclerView.ViewHolder {
        private ImageView icon;
        private TextView textName, textAddress;
        public BluetoothDevice device;
        private boolean isPaired;

        RecyclerViewHolder(@NonNull View itemView) {
            super(itemView);

            icon = itemView.findViewById(R.id.icon);
            textName = itemView.findViewById(R.id.textName);
            textAddress = itemView.findViewById(R.id.textAddress);

            itemView.setOnClickListener(view -> {
                stopDiscovery();
                Name = device.getName();
                Address = device.getAddress();
                BTDevice = device;
                Intent intent = new Intent(SelectBTActivity.this, MainActivity.class);
                intent.putExtra("DeviceName", device.getName());
                intent.putExtra("DeviceAddress", device.getAddress());
                SharedPreferences BT = getSharedPreferences("BTDetail" , MODE_PRIVATE);
                BT.edit()
                        .putString("Name" , Name)
                        .putString("Address" , Address)
                        .apply();
                startActivity(intent);
                finish();
            });
        }

        void loadDevice(@NonNull BluetoothDevice device, boolean isPaired) {
            this.device = device;
            this.isPaired = isPaired;

            String name = this.device.getName();
            if (name == null) name = "尚未選擇裝置";

            icon.setImageResource(this.isPaired ? R.drawable.ic_bluetooth_black_24dp : R.drawable.ic_bluetooth_searching_black_24dp);
            textName.setText(name);
            textAddress.setText(this.device.getAddress());
        }
    }

    private class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewHolder> {
        @NonNull
        @Override
        public RecyclerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.bluetooth_device_item, parent, false);
            return new RecyclerViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerViewHolder holder, int position) {
            Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
            if (position < pairedDevices.size()) {
                holder.loadDevice(pairedDevices.toArray(new BluetoothDevice[0])[position], true);
            } else {
                holder.loadDevice(discoveredDevices.toArray(new BluetoothDevice[0])[position - pairedDevices.size()], false);
            }
        }

        @Override
        public int getItemCount() {
            return bluetoothAdapter.getBondedDevices().size() + discoveredDevices.size();
        }
    }
}

