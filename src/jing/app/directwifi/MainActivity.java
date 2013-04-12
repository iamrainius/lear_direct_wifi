package jing.app.directwifi;

import java.util.ArrayList;
import java.util.Collection;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.PeerListListener;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity implements OnClickListener, android.content.DialogInterface.OnClickListener {
    
    private WifiP2pManager mManager;
    private Channel mChannel;
    private BroadcastReceiver mReceiver;
    private IntentFilter mIntentFilter;
    
    private Button mDiscover;
    private TextView mDevices;
	public ArrayAdapter mAdapter;
	private ArrayList<WifiP2pDevice> mDeviceList = new ArrayList<WifiP2pDevice>();
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
    	
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        mDiscover = (Button) findViewById(R.id.discover);
        mDiscover.setOnClickListener(this);
        
        mDevices = (TextView) findViewById(R.id.peers);
        
        mManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mManager.initialize(this, getMainLooper(), null);
        mReceiver = new WiFiDirectReceiver(mManager, mChannel, this);
        
        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mReceiver, mIntentFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mReceiver);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }

    private class WiFiDirectReceiver extends BroadcastReceiver {
        
        private WifiP2pManager mManager;
        private Channel mChannel;
        private MainActivity mActivity;
        
        public WiFiDirectReceiver(WifiP2pManager manager, Channel channel, MainActivity activity) {
            super();
            mManager = manager;
            mChannel = channel;
            mActivity = activity;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            
            if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
            	
                int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
                if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                    Toast.makeText(mActivity, "Wi-Fi Direct is enabled.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(mActivity, "Wi-Fi Direct is disabled.", Toast.LENGTH_SHORT).show();
                }
            
            } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
            
            	if (mManager != null) {
                    mManager.requestPeers(mChannel, new PeerListListener() {
                        
						@Override
                        public void onPeersAvailable(WifiP2pDeviceList peers) {
                        	if (peers != null) {
                                mDeviceList.addAll(peers.getDeviceList());
                                ArrayList<String> deviceNames = new ArrayList<String>();
                                for (WifiP2pDevice device : mDeviceList) {
                                	deviceNames.add(device.deviceName);
                                }
                                if (deviceNames.size() > 0) {
                                    mAdapter = new ArrayAdapter<String>(mActivity, android.R.layout.simple_list_item_1, deviceNames);
                                    showDeviceListDialog();
                                } else {
                                	Toast.makeText(mActivity, "Device list is empty.", Toast.LENGTH_SHORT).show();
                                }
                        	}
                        }
                    });
                }
            
            } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
                
            } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
                
            }
        }
        
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
        case R.id.discover:
            onDiscover();
            break;
        }
    }

    private void onDiscover() {
        mManager.discoverPeers(mChannel, new ActionListener() {
            
            @Override
            public void onSuccess() {
                Toast.makeText(MainActivity.this, "Discover peers successfully.", Toast.LENGTH_SHORT).show();
            }
            
            @Override
            public void onFailure(int reason) {
                Toast.makeText(MainActivity.this, "Discover peers failed.", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void showDeviceListDialog() {
    	DeviceListDialog deviceListDialog = new DeviceListDialog();
    	deviceListDialog.show(getFragmentManager(), "devices");
	}
    
    private class DeviceListDialog extends DialogFragment {

		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			builder.setTitle("Select a device")
				   .setSingleChoiceItems(mAdapter, 0, MainActivity.this)
				   .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
						}
					   
				   });
				
			return builder.create();
		}
    	
    }

	@Override
	public void onClick(DialogInterface dialog, int which) {
		onDeviceSelected(which);
		dialog.dismiss();
	}

	private void onDeviceSelected(int which) {
		WifiP2pDevice device = mDeviceList.get(which);
		if (device == null) {
			return;
		}
		
		WifiP2pConfig config = new WifiP2pConfig();
		config.deviceAddress = device.deviceAddress;
		mManager.connect(mChannel, config, new ActionListener() {
			
			@Override
			public void onSuccess() {
				Toast.makeText(MainActivity.this, "Connected", Toast.LENGTH_SHORT).show();
			}
			
			@Override
			public void onFailure(int reason) {
				Toast.makeText(MainActivity.this, "Failed to connect", Toast.LENGTH_SHORT).show();
			}
		});
	}
}
