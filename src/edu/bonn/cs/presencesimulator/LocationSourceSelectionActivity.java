/*Copyright (C) 2012 Krischan Udelhoven
Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package edu.bonn.cs.presencesimulator;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * @author Krischan Udelhoven <krischan.udelhoven@gmail.com>
 * @version 1.0
 * @since 2012-08-01
 */

public class LocationSourceSelectionActivity extends Activity {
	private List<LocationSource> locationSources = new ArrayList<LocationSource>();
	private ArrayAdapter<LocationSource> listViewAdapter;
	private ReceiveAvailableLocationSourcesFromBroadcast broadcastReceiver;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.location_source_selection);
		
		ListView listView = (ListView) findViewById(R.id.userList);
		this.listViewAdapter = new ArrayAdapter<LocationSource>(this,
				android.R.layout.simple_list_item_1, android.R.id.text1, new ArrayList<LocationSource>());
		listView.setAdapter(this.listViewAdapter);
		
		listView.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view, int position,
					long id) {
				LocationSourceSelectionActivity.this.connectToLocationSource(listViewAdapter.getItem(position));
			}
		});
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		this.broadcastReceiver = new ReceiveAvailableLocationSourcesFromBroadcast(this);
		this.broadcastReceiver.execute();
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (this.broadcastReceiver.getSock() != null) {
			this.broadcastReceiver.getSock().close();
		}
		this.broadcastReceiver.cancel(true);
	}
	
	public void connectToLocationSource(LocationSource locationSource) {
		if (locationSource != null) {
			Intent connectIntent = new Intent(this, LocationReceiverActivity.class);
			connectIntent.putExtra("locationSource", locationSource);
			startActivity(connectIntent);
		} else {
			Toast.makeText(getApplicationContext(), "No location source to connect to!", Toast.LENGTH_SHORT).show();
		}
	}
	
	public void fetchLocationSourcesManually(View view) {
		String serverName = null;
		int serverPort = -1;
		
		EditText addressEditText = (EditText) findViewById(R.id.serverAddress);
		String serverAddress = addressEditText.getText().toString();
		serverName = serverAddress.split(":")[0];
		serverPort = Integer.parseInt(serverAddress.split(":")[1]);
		
		if ((serverName != null) && (serverPort != -1)) {
			ReceiveAvailableLocationSourcesFromAddress manualFetchStatus= new ReceiveAvailableLocationSourcesFromAddress(this, serverName, serverPort);
			manualFetchStatus.execute();
		}
	}

	public void statusReceived(String status) {
		if (status == null) {
			Toast.makeText(getApplicationContext(), "Couldn't fetch server status!", Toast.LENGTH_SHORT).show();
			return;
		}
		updateUsersFromStatus(status);
	}
	
	public void updateUsersFromStatus(String status) {
	
		Document doc = this.getDomElement(status);
		
		if (doc == null)
			return;
		
		Node updateServiceNode = doc.getElementsByTagName("updateService").item(0);
		String serverName = ((Attr)(updateServiceNode.getAttributes().item(0))).getValue();
		int serverPort = Integer.parseInt(((Attr)(updateServiceNode.getAttributes().item(1))).getValue());
		
		List<LocationSource> newUsers = new ArrayList<LocationSource>();
		
		NodeList userNodes = doc.getElementsByTagName("user");
		for(int i=0; i < userNodes.getLength() ; i++) {
			Node userNode = userNodes.item(i);
	
			String id = ((Attr)(userNode.getAttributes().item(0))).getValue();
			
			String name = ((Attr)(userNode.getAttributes().item(1))).getValue();
			
			LocationSource user = new LocationSource(id, name);
			user.setServerName(serverName);
			user.setServerPort(serverPort);
			
			newUsers.add(user);
	    }
		int newUserCount = 0;
		int removedUserCount = 0;
		for (LocationSource user : this.getUsersFromGivenServer(serverName, serverPort)) {
			if (!newUsers.contains(user)) {
				this.locationSources.remove(user);
				this.listViewAdapter.remove(user);
				removedUserCount++;
			}
		}
		
		for (LocationSource newUser : newUsers) {
			if (!this.locationSources.contains(newUser)) {
				this.locationSources.add(newUser);
				this.listViewAdapter.add(newUser);
				newUserCount++;
			}
		}
		
		if ((newUserCount == 0)&&(removedUserCount == 0)){
			Toast.makeText(getApplicationContext(), "No new user found!", Toast.LENGTH_SHORT).show();
		} else if (newUserCount == 0) {
			Toast.makeText(getApplicationContext(), removedUserCount + " user removed", Toast.LENGTH_SHORT).show();
		} else if (removedUserCount == 0) {
			Toast.makeText(getApplicationContext(), newUserCount+ " new user found", Toast.LENGTH_SHORT).show();
		} else {
			Toast.makeText(getApplicationContext(), removedUserCount + " user removed and " + newUserCount + " new user found", Toast.LENGTH_SHORT).show();
		}
		
		TextView header = (TextView)findViewById(R.id.header);
		if (this.locationSources.size() > 0) {
			header.setText(R.string.AvailableUsers);
		}
		else {
			header.setText(R.string.NoUsers);
		}
	}
	
	private List<LocationSource> getUsersFromGivenServer(String serverName, int serverPort) {
		List<LocationSource> usersFromServer = new ArrayList<LocationSource>();
		for (LocationSource user : this.locationSources) {
			if (user.getServerName().equals(serverName) && (user.getServerPort() == serverPort)) {
				usersFromServer.add(user);
			}
		}
		return usersFromServer;
	}
	
	public Document getDomElement(String xml){
	    Document doc = null;
	    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
	    try { 
	        DocumentBuilder db = dbf.newDocumentBuilder();
	
	        InputSource is = new InputSource();
	            is.setCharacterStream(new StringReader(xml));
	            doc = db.parse(is); 
	
	        } catch (ParserConfigurationException e) {
	            Log.e("Error: ", e.getMessage());
	            return null;
	        } catch (SAXException e) {
	            Log.e("Error: ", e.getMessage());
	            return null;
	        } catch (IOException e) {
	            Log.e("Error: ", e.getMessage());
	            return null;
	        }
	        return doc;
	}
}
