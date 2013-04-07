/*Copyright (C) 2012 Krischan Udelhoven
Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package edu.bonn.cs.presencesimulator;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

import android.os.AsyncTask;
import android.util.Log;

/**
 * @author Krischan Udelhoven <krischan.udelhoven@gmail.com>
 * @version 1.0
 * @since 2012-08-01
 */

public class ReceiveAvailableLocationSourcesFromBroadcast extends AsyncTask<Void, String, Void> {

	private static int BROADCASTPORT = 9123;

	private LocationSourceSelectionActivity context;
	private DatagramSocket sock;

	public ReceiveAvailableLocationSourcesFromBroadcast(LocationSourceSelectionActivity context) {
		this.context = context;
	}

	@Override
	protected Void doInBackground(Void... voids) {

		this.sock = null;
		try {
			this.sock = new DatagramSocket(
					ReceiveAvailableLocationSourcesFromBroadcast.BROADCASTPORT);
		} catch (SocketException e) {
			Log.e(ReceiveAvailableLocationSourcesFromBroadcast.class.getName(),
					"Coundn't connect to broadcast socket!");
			e.printStackTrace();
			return null;
		}

		if (this.sock == null) {
			Log.e(ReceiveAvailableLocationSourcesFromBroadcast.class.getName(),
					"Coundn't connect to broadcast socket!");
			return null;
		}

		byte[] buf = new byte[512];
		while (true) {
			DatagramPacket packet = new DatagramPacket(buf, buf.length);

			try {
				this.sock.receive(packet);
				byte[] statusData = packet.getData();
				String statusString = new String(statusData);
				statusString = statusString.split("\n")[0];
				this.publishProgress(statusString);
			} catch (SocketException e1) {
				e1.printStackTrace();
				return null;
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			}
		}
	}

	public DatagramSocket getSock() {
		return this.sock;
	}

	@Override
	protected void onProgressUpdate(String... status) {
		super.onProgressUpdate(status);
		this.context.updateUsersFromStatus(status[0]);
	}

}
