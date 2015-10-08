package com.nathan.android.bluetoothtest;

import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

	char[] letters;

	public static final int MESSAGE_STATE_CHANGE = 1;
	public static final int MESSAGE_READ = 2;
	public static final int MESSAGE_WRITE = 3;
	public static final int MESSAGE_DEVICE_NAME = 4;
	public static final int MESSAGE_TOAST = 5;

	public static final String PASS_ACCEPT = "1";
	public static final String PASS_DECLINE = "0";
	public static final String LOCKED = "2";
	public static final String UNLOCKED = "3";

   private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
   private static final String NAME = "BTCONNECT";
   private static final int REQUEST_ENABLE_BT = 1;
   private Button onBtn;
   private Button offBtn;
   private Button listBtn;
   private Button findBtn;
   private TextView text;
	private EditText password;
	private Button lock;
	private Button unlock;
   private BluetoothAdapter myBluetoothAdapter;
   private Set<BluetoothDevice> pairedDevices;
   private ArrayList<BluetoothDevice> devicesArray;
   private ListView myListView;
   private ArrayAdapter<String> BTArrayAdapter;
	private ConnectedThread mconnected;

	static Context context;


	//Managing messages sent from ConnectedThread (which manages data to and from the BT device)
	private static Handler mHandler = new Handler() {

		/*private final WeakReference<MainActivity> mTarget;
		mHandler(MainActivity target) {
			mTarget = new WeakReference<MainActivity>(target);
		}*/

		@Override
		public void handleMessage(Message msg)  {
			switch (msg.what) {

				//not currently used; may use later?
				case MESSAGE_WRITE:
					byte[] writeBuf = (byte[]) msg.obj;
					String writeMessage = new String(writeBuf);
					//other stuff?
					break;


				case MESSAGE_READ:
					byte[] readBuf = (byte[]) msg.obj;
					// construct a string from the valid bytes in the buff
					String readMessage = new String(readBuf, 0, msg.arg1);
					//convert to char for use in switch statement
					char readChar = readMessage.charAt(0);

					switch (readChar) {
						case '1':
							Toast.makeText(context,"Door Unlocked!" ,
									Toast.LENGTH_LONG).show();
							break;
						case '2':
							Toast.makeText(context,"Door Locked!" ,
									Toast.LENGTH_LONG).show();
							break;
						case '3':
							Toast.makeText(context,"Incorrect Password!" ,
									Toast.LENGTH_LONG).show();
							break;
					}

					break;

				//not currently used, may use later?
				case MESSAGE_TOAST:

						//Toast.makeText(getApplicationContext(), msg.getData().getString(Constants.TOAST),
						//		Toast.LENGTH_SHORT).show();

					break;
			}
		}
	};




   @Override
   protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.activity_main);

	   context = getApplicationContext(); //used for Handler later
	   letters = new char[26];
	   letters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();


      //
      myBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
      if(myBluetoothAdapter == null) { //if no Bluetooth capabilities
    	  //onBtn.setEnabled(false); -> rejected buttons, might bring back?
    	  //offBtn.setEnabled(false);
    	  listBtn.setEnabled(false);
    	  findBtn.setEnabled(false);
    	  text.setText("Status: not supported");
    	  
    	  Toast.makeText(getApplicationContext(),"Your device does not support Bluetooth",
         		 Toast.LENGTH_LONG).show();
      } else {
	      text = (TextView) findViewById(R.id.text);
	     /* onBtn = (Button)findViewById(R.id.turnOn);
	      onBtn.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				on(v);
			}
	      });
	      
	      offBtn = (Button)findViewById(R.id.turnOff);
	      offBtn.setOnClickListener(new OnClickListener() {
	  		
	  		@Override
	  		public void onClick(View v) {
	  			// TODO Auto-generated method stub
	  			off(v);
	  		}
	      });
	      */
	      listBtn = (Button)findViewById(R.id.paired); // for devices already paired
	      listBtn.setOnClickListener(new OnClickListener() {

	  		@Override
	  		public void onClick(View v) {
	  			// TODO Auto-generated method stub
	  			list(v);
	  		}
	      });
	      
	      findBtn = (Button)findViewById(R.id.search); //for new, available devices
	      findBtn.setOnClickListener(new OnClickListener() {
	  		
	  		@Override
	  		public void onClick(View v) {
				if(!myBluetoothAdapter.isEnabled()) {
					on(v);
				}

	  			find(v);
	  		}
	      });

		  password = (EditText)findViewById(R.id.editText); //user input

		  //action buttons
		  lock = (Button) findViewById(R.id.lock);
		  lock.setOnClickListener(new OnClickListener() {
			  @Override
			  public void onClick(View view) {
				  String tmpPass = password.getText().toString();
				  tmpPass = encrypt(tmpPass);
				  if(mconnected!=null){
					  tmpPass += "&L";
					mconnected.write(tmpPass.getBytes());
				  }
			  }
		  });

		  unlock = (Button) findViewById(R.id.unlock);
		  unlock.setOnClickListener(new OnClickListener() {
			  @Override
			  public void onClick(View view) {
				  String tmpPass = password.getText().toString();
				  tmpPass=encrypt(tmpPass);
				  if(mconnected!=null){
					  tmpPass += "&U";
					  mconnected.write(tmpPass.getBytes());
				  }
			  }
		  });


		  //available devices
	      myListView = (ListView)findViewById(R.id.listView1);
		  myListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			  @Override
			  public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
				  if(myBluetoothAdapter.isDiscovering()) {
					  myBluetoothAdapter.cancelDiscovery();
				  }
				  BluetoothDevice selectedDevice = devicesArray.get(i);
				  ConnectThread connect = new ConnectThread(selectedDevice);
				  BTArrayAdapter.clear();
				  connect.start();
			  }
		  });

	      // make ArrayAdapter that contains the bluetooth devices, and set it to the ListView
	      BTArrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
	      myListView.setAdapter(BTArrayAdapter);
		  devicesArray = new ArrayList<BluetoothDevice>();
      }
   }

	private String encrypt(String pass){ //caesar cipher
		char[] tmpCharArr = pass.toCharArray();
		int key = Character.getNumericValue(tmpCharArr[tmpCharArr.length-1]); //use the last element to determine number to shift
		for(int n=0; n<tmpCharArr.length-1;n++) {
			int val = Character.getNumericValue(tmpCharArr[n]);
			if(val != -1) {
				tmpCharArr[n] = letters[val + key];
			}
		}

		int keyVal = Character.getNumericValue(tmpCharArr[tmpCharArr.length-1]);
		if(keyVal != -1) {
			tmpCharArr[tmpCharArr.length-1] = letters[keyVal];
		}


		return new String(tmpCharArr);
	}

	//turn on bluetooth
   public void on(View view){
         Intent turnOnIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
         startActivityForResult(turnOnIntent, REQUEST_ENABLE_BT);

         Toast.makeText(getApplicationContext(),"Bluetooth turned on" ,
        		 Toast.LENGTH_LONG).show();
   }

	//when requesting to turn on bluetooth
   @Override
   protected void onActivityResult(int requestCode, int resultCode, Intent data) {
	   // TODO Auto-generated method stub
	   if(requestCode == REQUEST_ENABLE_BT){
		   if(myBluetoothAdapter.isEnabled()) {
			   text.setText("Status: Enabled");
		   } else {   
			   text.setText("Status: Disabled");
		   }
	   }
   }

	//for already paired devices
   public void list(View view){
	  // get paired devices
      pairedDevices = myBluetoothAdapter.getBondedDevices();
      
      // put it's one to the adapter
      for(BluetoothDevice device : pairedDevices)
    	  BTArrayAdapter.add(device.getName()+ "\n" + device.getAddress());

      Toast.makeText(getApplicationContext(),"Show Paired Devices",
    		  Toast.LENGTH_SHORT).show();
      
   }

	//broadcast reveiver for looking for bluetooth devices
   final BroadcastReceiver bReceiver = new BroadcastReceiver() {
	    public void onReceive(Context context, Intent intent) {
	        String action = intent.getAction();
	        // when device is found through discovery
	        if (BluetoothDevice.ACTION_FOUND.equals(action)) {
	             // get the BluetoothDevice object from Intent
	        	 BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
	        	 // add name and MAC address of the object to the arrayAdapter
				devicesArray.add(device);
	             BTArrayAdapter.add(device.getName() + "\n" + device.getAddress());
	             BTArrayAdapter.notifyDataSetChanged();
	        }
	    }
	};
	
   public void find(View view) {
	   if (myBluetoothAdapter.isDiscovering()) {
		   // the button is pressed when it discovers, so cancel the discovery
		   myBluetoothAdapter.cancelDiscovery();
	   }
	   else {
			BTArrayAdapter.clear();
			myBluetoothAdapter.startDiscovery();
			
			registerReceiver(bReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));	
		}    
   }

	//turn off bluetooth -> NOT USED, MIGHT BRING BACK?
   public void off(View view){
	  myBluetoothAdapter.disable();
	  text.setText("Status: Disconnected");
	  
      Toast.makeText(getApplicationContext(),"Bluetooth turned off",
    		  Toast.LENGTH_LONG).show();
   }


   @Override
   protected void onDestroy() {
	   // TODO Auto-generated method stub
	   super.onDestroy();
	   unregisterReceiver(bReceiver);
   }


	//connecting to the selected device
	private class ConnectThread extends Thread {
		private final BluetoothSocket mmSocket;
		private final BluetoothDevice mmDevice;

		public ConnectThread(BluetoothDevice device) {
			// Use a temporary object that is later assigned to mmSocket,
			// because mmSocket is final
			BluetoothSocket tmp = null;
			mmDevice = device;

			// Get a BluetoothSocket to connect with the given BluetoothDevice
			try {
				tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
			} catch (IOException e) { }
			mmSocket = tmp;
		}

		public void run() {
			// cancel discovery because device was chosen
			myBluetoothAdapter.cancelDiscovery();

			try {
				// Connect the device through the socket. This will block
				// until it succeeds or throws an exception
				mmSocket.connect();
			} catch (IOException connectException) {
				// Unable to connect; close the socket and get out
				try {
					mmSocket.close();
				} catch (IOException closeException) { }
				return;
			}

			// manage connected device in new thread
			mconnected = new ConnectedThread(mmSocket);
			mconnected.start();
		}

		//Will cancel an in-progress connection, and close the socket
		public void cancel() {
			try {
				mmSocket.close();
			} catch (IOException e) { }
		}
	}

	private class ConnectedThread extends Thread {
		private final BluetoothSocket mmSocket;
		private final InputStream mmInStream;
		private final OutputStream mmOutStream;

		public ConnectedThread(BluetoothSocket socket) {
			mmSocket = socket;
			InputStream tmpIn = null;
			OutputStream tmpOut = null;

			// Get the input and output streams, using temp objects because
			// member streams are final
			try {
				tmpIn = socket.getInputStream();
				tmpOut = socket.getOutputStream();
			} catch (IOException e) { }

			mmInStream = tmpIn;
			mmOutStream = tmpOut;
		}

		public void run() {
			byte[] buffer = new byte[1024];  // buffer store for the stream
			int bytes; // bytes returned from read()

			// Keep listening to the InputStream until an exception occurs
			while (true) {
				try {
					// Read from the InputStream
					bytes = mmInStream.read(buffer);
					// Send the obtained bytes to the UI activity
					mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer)
							.sendToTarget();
				} catch (IOException e) {
					break;
				}
			}
		}

		/* Call this from the main activity to send data to the remote device */
		public void write(byte[] bytes) {
			try {
				mmOutStream.write(bytes);
			} catch (IOException e) { }
		}

		/* Call this from the main activity to shutdown the connection */
		public void cancel() {
			try {
				mmSocket.close();
			} catch (IOException e) { }
		}
	}


}
