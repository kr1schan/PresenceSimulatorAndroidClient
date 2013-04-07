/*Copyright (C) 2012 Krischan Udelhoven
Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package edu.bonn.cs.presencesimulator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.UnknownHostException;

import android.app.ProgressDialog;
import android.os.AsyncTask;

/**
 * @author Krischan Udelhoven <krischan.udelhoven@gmail.com>
 * @version 1.0
 * @since 2012-08-01
 */

public class ReceiveAvailableLocationSourcesFromAddress extends AsyncTask<Void, Void, Void> {

	private LocationSourceSelectionActivity connect;
	private String serverName;
	private int serverPort;
	private ProgressDialog progressDialog;
	private String status;

	public ReceiveAvailableLocationSourcesFromAddress(LocationSourceSelectionActivity connect, String serverName, int serverPort) {
		this.connect = connect;
		this.serverName = serverName;
		this.serverPort = serverPort;
	}

	@Override
	protected void onPreExecute() {
		progressDialog = new ProgressDialog(this.connect);
		progressDialog.setMessage("Manual fetching...");
		progressDialog.setIndeterminate(true);
		progressDialog.setCancelable(false);
		progressDialog.show();
	}

	@Override
	protected Void doInBackground(Void... voids) {
		Socket sock = null;
		try {
			sock = new Socket(this.serverName, this.serverPort);
			sock.setKeepAlive(true);
			sock.setSoTimeout(3000);
		} catch (NumberFormatException e) {
			e.printStackTrace();
			return null;
		} catch (UnknownHostException e) {
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}

		if (sock != null) {
			try {
				BufferedReader br = new BufferedReader(new InputStreamReader(
						sock.getInputStream()));
				this.status = br.readLine();
				br.close();
				sock.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	@Override
	protected void onPostExecute(Void result) {
		if (this.progressDialog.isShowing()) {
			this.progressDialog.dismiss();
		}
		this.connect.statusReceived(this.status);
	}
}
