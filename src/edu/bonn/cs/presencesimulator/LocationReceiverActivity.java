/*Copyright (C) 2012 Krischan Udelhoven
Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package edu.bonn.cs.presencesimulator;

import java.util.Date;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

/**
 * @author Krischan Udelhoven <krischan.udelhoven@gmail.com>
 * @version 1.0
 * @since 2012-08-01
 */

public class LocationReceiverActivity extends Activity {

	private Messenger messenger = null;
	private LocationSource locationSource;

	private ServiceConnection conn = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder binder) {
			messenger = new Messenger(binder);
		}

		public void onServiceDisconnected(ComponentName className) {
			messenger = null;
		}
	};

	private Handler handler = new Handler() {
		public void handleMessage(Message message) {
			if (message.arg1 == LocationReceiverService.CONNECTIONLOST) {
				Toast.makeText(getApplicationContext(),
						"Disconnected from user", Toast.LENGTH_SHORT).show();
				LocationReceiverActivity.this.finish();
			} else {
				Bundle data = message.getData();
				if (data != null) {
					long time = data.getLong("LASTUPDATE");
					Date date = new Date(time);
					TextView lastUpdateTime = (TextView) findViewById(R.id.lastUpdateTime);
					lastUpdateTime.setText(" " + date.getHours() + ":"
							+ date.getMinutes() + ":" + date.getSeconds());
					
					Double lat = data.getDouble("LAT");
					TextView lastUpdateLat = (TextView) findViewById(R.id.lat);
					lastUpdateLat.setText(lat.toString());
					
					Double lng = data.getDouble("LNG");
					TextView lastUpdateLng = (TextView) findViewById(R.id.lng);
					lastUpdateLng.setText(lng.toString());
				}
			}
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.location_source_receiver);
		
		this.locationSource = (LocationSource)this.getIntent().getExtras().get("locationSource");

		TextView lastUpdateText = (TextView) findViewById(R.id.selectedUserName);
		lastUpdateText.setText(" " + this.locationSource.getName());

		Intent fetchUpdatesIntent = new Intent(this,
				LocationReceiverService.class);
		fetchUpdatesIntent.putExtra("locationSource", this.locationSource);

		this.startService(fetchUpdatesIntent);

		Toast.makeText(getApplicationContext(),
				"Background update service started", Toast.LENGTH_SHORT).show();
	}

	@Override
	protected void onResume() {
		super.onResume();
		Intent intent = null;
		intent = new Intent(this, LocationReceiverService.class);
		Messenger messenger = new Messenger(handler);
		intent.putExtra("MESSENGER", messenger);
		bindService(intent, conn, Context.BIND_AUTO_CREATE);
	}

	@Override
	protected void onPause() {
		super.onPause();
		unbindService(conn);
	}

	public void disconnect(View view) {
		Message msg = Message.obtain();
		try {
			messenger.send(msg);
		} catch (RemoteException e1) {
			e1.printStackTrace();
		}

		this.finish();
	}
}
