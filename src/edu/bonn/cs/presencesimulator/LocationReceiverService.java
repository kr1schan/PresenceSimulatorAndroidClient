/*Copyright (C) 2012 Krischan Udelhoven
Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package edu.bonn.cs.presencesimulator;

import java.io.IOException;
import java.net.Socket;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;

/**
 * @author Krischan Udelhoven <krischan.udelhoven@gmail.com>
 * @version 1.0
 * @since 2012-08-01
 */

public class LocationReceiverService extends Service {
	public static final int CONNECTIONLOST = 6;

	private LocationSource locationSource;
	private LocationManager locationManager;
	private LocationReceiver fetchUpdates;
	private final Messenger inMessenger = new Messenger(new IncomingHandler());
	private Messenger outMessenger;

	class IncomingHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			Log.i(getClass().getName(), "Incoming message");
			LocationReceiverService.this.cleanUp();
		}
	}

	@Override
	public void onCreate() {
		super.onCreate();
		locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		locationManager.addTestProvider(LocationManager.GPS_PROVIDER, false,
				false, false, false, true, true, true, 0, 5);
		locationManager.setTestProviderEnabled(LocationManager.GPS_PROVIDER,
				true);
		Log.i(getClass().getName(), "Created");
	}

	@Override
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);
		this.locationSource = (LocationSource) intent.getExtras().get(
				"locationSource");
		this.fetchUpdates = new LocationReceiver(this, this.locationSource,
				locationManager);
		this.fetchUpdates.execute();
	}

	@Override
	public void onDestroy() {

		super.onDestroy();
	}

	@Override
	public IBinder onBind(Intent intent) {
		Bundle extras = intent.getExtras();
		if (extras != null) {
			outMessenger = (Messenger) extras.get("MESSENGER");
		}
		return inMessenger.getBinder();
	}

	public void updateGUI(Location location) {
		Message backMsg = Message.obtain();
		Bundle bundle = new Bundle();
		bundle.putLong("LASTUPDATE", location.getTime());
		bundle.putDouble("LAT", location.getLatitude());
		bundle.putDouble("LNG", location.getLongitude());
		backMsg.setData(bundle);
		try {
			outMessenger.send(backMsg);
		} catch (android.os.RemoteException e1) {
			Log.w(getClass().getName(), "Exception sending message", e1);
		}
	}

	public void cleanUp() {
		Socket sock = this.fetchUpdates.getSock();
		if (sock != null) {
			try {
				sock.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		this.fetchUpdates.cancel(true);
		Log.i(getClass().getName(), "Shutting down");
		Message backMsg = Message.obtain();
		backMsg.arg1 = LocationReceiverService.CONNECTIONLOST;

		try {
			outMessenger.send(backMsg);
		} catch (android.os.RemoteException e1) {
			Log.w(getClass().getName(), "Exception sending message", e1);
		}

		this.stopSelf();
	}
}
