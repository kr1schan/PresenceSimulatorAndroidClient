/*Copyright (C) 2012 Krischan Udelhoven
Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package edu.bonn.cs.presencesimulator;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.UnknownHostException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.util.Log;

/**
 * @author Krischan Udelhoven <krischan.udelhoven@gmail.com>
 * @version 1.0
 * @since 2012-08-01
 */

public class LocationReceiver extends AsyncTask<Void, Void, Void> {
	private String TAG = "PresenceSimulator";
	private Socket sock;
	private LocationSource locationSource;
	private LocationManager locationManager;
	private LocationReceiverService locationReceiverService;

	public LocationReceiver(LocationReceiverService locationReceiverService,
			LocationSource locationSource, LocationManager locationManager) {
		this.locationSource = locationSource;
		this.locationManager = locationManager;
		this.locationReceiverService = locationReceiverService;
	}

	@Override
	protected Void doInBackground(Void... voids) {
		if (!this.connect()) {
			locationReceiverService.cleanUp();
			return null;
		}

		try {
			this.sendLocationSourceId();
		} catch (IOException e) {
			Log.e(TAG, e.toString());
			locationReceiverService.cleanUp();
			return null;
		}

		while (true) {
			try {
				SAXParserFactory spf = SAXParserFactory.newInstance();
				SAXParser sp = spf.newSAXParser();

				XMLReader xr = sp.getXMLReader();

				DataHandler dataHandler = new DataHandler();
				xr.setContentHandler(dataHandler);

				xr.parse(new InputSource(this.sock.getInputStream()));
			} catch (ParserConfigurationException pce) {
				Log.e("SAX XML", "sax parse error", pce);
				break;
			} catch (SAXException se) {
				Log.e("SAX XML", "sax error", se);
				break;
			} catch (IOException ioe) {
				Log.e("SAX XML", "sax parse io error", ioe);
				break;
			}
		}
		locationReceiverService.cleanUp();
		return null;
	}

	private boolean connect() {
		this.sock = null;
		int i = 0;
		for (i = 0; i < 3; i++) {
			try {
				this.sock = new Socket(this.locationSource.getServerName(),
						this.locationSource.getServerPort());
				this.sock.setKeepAlive(true);
				break;
			} catch (NumberFormatException e) {
				e.printStackTrace();
			} catch (UnknownHostException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		if (i == 3) {
			Log.d("UpdateAsyncTask", "Could not connect Socket");
			return false;
		} else {
			Log.d("UpdateAsyncTask", "Socket Connected");
			return true;
		}
	}

	private void sendLocationSourceId() throws IOException {
		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(
				this.sock.getOutputStream()));
		String message = "<?xml version=\"1.0\"?><user id=\""
				+ this.locationSource.getId() + "\"/>";
		bw.write(message + "\n");
		bw.flush();
		Log.d("UpdateAsyncTask", "LocationSourceId sent.");
	}

	private void setLocation(Location location) {
		long currentTime = System.currentTimeMillis();
		location.setTime(currentTime);
		locationManager.setTestProviderLocation(LocationManager.GPS_PROVIDER,
				location);
	}

	public Socket getSock() {
		return this.sock;
	}

	private class DataHandler extends DefaultHandler {
		private Location location;

		@Override
		public void startElement(String namespaceURI, String localName,
				String qName, Attributes atts) throws SAXException {

			if (localName.equals("trkpt")) {
				this.location = new Location(LocationManager.GPS_PROVIDER);

				for (int i = 0; i < atts.getLength(); i++) {
					if (atts.getLocalName(i).equals("lat")) {
						this.location.setLatitude(Double.parseDouble(atts
								.getValue(i)));
					} else if (atts.getLocalName(i).equals("lon")) {
						this.location.setLongitude(Double.parseDouble(atts
								.getValue(i)));
					}
					location.setAccuracy(1);
					location.setAltitude(1);
					location.setBearing(0);
					location.setSpeed(0);
					location.setProvider(LocationManager.GPS_PROVIDER);
				}
			}
		}

		@Override
		public void endElement(String namespaceURI, String localName,
				String qName) throws SAXException {
			Log.v("endElement", localName);

			if (localName.equals("trkpt")) {
				LocationReceiver.this.setLocation(this.location);
				LocationReceiver.this.locationReceiverService
						.updateGUI(this.location);
			}
		}
	}

}
