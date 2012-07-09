package com.tzakch.polarmeter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.app.ActionBar.LayoutParams;
import android.app.Activity;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

public class MainActivity extends Activity implements View.OnClickListener {

	LinearLayout ll;
	LinearLayout rr;
	Handler m_handler;
	MainActivity m;
	Button refresh;
	HashMap<String, PolarMeterObject> map;

	public void refreshLayout(String response) {

		JSONObject jObject = null;
		try {
			jObject = new JSONObject(response);
			JSONArray sensorsArray = jObject.getJSONArray("sensors");
			for (int i = 0; i < sensorsArray.length(); i++) {

				String label = sensorsArray.getJSONObject(i).get("label")
						.toString();
				boolean enabled = sensorsArray.getJSONObject(i).getBoolean(
						"enabled");
				int id = sensorsArray.getJSONObject(i).getInt("id");
				String mac = sensorsArray.getJSONObject(i).get("mac_address")
						.toString();
				boolean plus = sensorsArray.getJSONObject(i).getBoolean("plus");
				Button btn;

				if (map.containsKey(mac)) {
					PolarMeterObject p = map.get(mac);
					p.enabled = enabled;
					p.label = label;
					p.id = id;
					p.plus = plus;
					btn = p.getButton(label, enabled);

					ArrayList<View> touchables = ll.getTouchables();

					for (View v : touchables) {
						if (v == btn) {
							btn.setText(label + ": " + (enabled ? "On" : "Off"));
						}
					}

				} else {
					PolarMeterObject p = new PolarMeterObject(mac, enabled,
							plus, id, label, this);
					map.put(mac, p);
					btn = p.getButton(label, enabled);
					ll.addView(btn);
				}

			}
			ll.invalidate();

		} catch (JSONException e) {
			Log.e("tim", "failed to create JsonObject");
		}

	}

	@SuppressWarnings("deprecation")
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		m = this;

		map = new HashMap<String, PolarMeterObject>();

		ll = new LinearLayout(this);
		ll.setOrientation(LinearLayout.VERTICAL);
		ll.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT,
				LayoutParams.FILL_PARENT));
		ll.setGravity(Gravity.CENTER);

		rr = new LinearLayout(this);
		rr.setOrientation(LinearLayout.HORIZONTAL);
		rr.setGravity(Gravity.TOP);

		refresh = new Button(this);
		refresh.setOnClickListener(this);
		refresh.setText("Refresh");

		rr.addView(refresh);

		ll.addView(rr);

		setContentView(ll);

		try {
			new RequestTask(this)
					.execute("http://www.polarmeter.com/sensors.json");
		} catch (Exception e) {
			e.printStackTrace();
		}

		m_handler = new Handler();

		m_statusChecker.run();

	}

	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	public void onClick(View v) {

		Button button = (Button) v;
		Toast.makeText(getApplicationContext(), button.getText().toString(), 2)
				.show();

		if (v == refresh) {
			try {
				new RequestTask(this)
						.execute("http://www.polarmeter.com/sensors.json");
			} catch (Exception e) {
				e.printStackTrace();
			}

		} else {
			try {
				for (PolarMeterObject p : map.values()) {
					if (p.b == v) {
						new SendTask(this, !p.enabled)
								.execute("http://www.polarmeter.com/sensors/"
										+ p.id);

					}
				}

			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}

	Runnable m_statusChecker = new Runnable() {
		public void run() {
			try {
				new RequestTask(m)
						.execute("http://www.polarmeter.com/sensors.json");
			} catch (Exception e) {
				e.printStackTrace();
			}
			m_handler.postDelayed(m_statusChecker, 1000);
		}
	};

}

class PolarMeterObject {

	boolean enabled;
	boolean plus;
	int id;
	String label;
	Button b;
	MainActivity m;
	String mac;

	public PolarMeterObject(String mac, boolean enabled, boolean plus, int id,
			String label, MainActivity m) {
		this.enabled = enabled;
		this.plus = plus;
		this.id = id;
		this.label = label;
		this.m = m;
		this.mac = mac;
		b = null;
	}

	Button getButton(String label, boolean enabled) {
		if (b == null) {
			b = new Button(m);
			b.setText(label + ": " + (enabled ? "On" : "Off"));
			b.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT,
					LayoutParams.WRAP_CONTENT));

			b.setOnClickListener(m);
		}
		return b;
	}

}

class RequestTask extends AsyncTask<String, String, String> {
	MainActivity m;

	public RequestTask(MainActivity mainActivity) {
		m = mainActivity;
	}

	protected String doInBackground(String... uri) {
		HttpClient httpclient = new DefaultHttpClient();
		HttpResponse response;
		String responseString = "";
		try {
			response = httpclient.execute(new HttpGet(uri[0]));
			StatusLine statusLine = response.getStatusLine();
			if (statusLine.getStatusCode() == HttpStatus.SC_OK) {
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				response.getEntity().writeTo(out);
				out.close();
				responseString = out.toString();
			} else {
				// Closes the connection.
				response.getEntity().getContent().close();
				throw new IOException(statusLine.getReasonPhrase());
			}
		} catch (ClientProtocolException e) {
			Log.e("tim", "ClientProtocolException in get");
		} catch (IOException e) {
			Log.e("tim", "IOException in get");
		}
		return responseString;
	}

	protected void onPostExecute(String result) {
		// super.onPostExecute(result);
		m.refreshLayout(result);
	}
}

class SendTask extends AsyncTask<String, String, String> {
	MainActivity m;
	boolean enabled;
	int id;

	public SendTask(MainActivity mainActivity, boolean enabled) {
		m = mainActivity;
		this.enabled = enabled;
	}

	protected String doInBackground(String... uri) {
		// Create a new HttpClient and Post Header
		HttpClient httpclient = new DefaultHttpClient();

		HttpPut httpPut = new HttpPut(uri[0]);
		try {
			// Add your data
			JSONObject j = new JSONObject();
			JSONObject i = new JSONObject();
			try {
				i.put("enabled", this.enabled);
				j.put("sensor", i);

			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			StringEntity se = new StringEntity(j.toString());
			se.setContentType(new BasicHeader(HTTP.CONTENT_TYPE,
					"application/json"));
			httpPut.setEntity(se);

			// Execute HTTP Post Request
			httpclient.execute(httpPut);

		} catch (ClientProtocolException e) {
			Log.e("tim", "ClientProtocolException in PUT");

		} catch (IOException e) {
			Log.e("tim", "IOException in Put");

		}
		return "";
	}

	protected void onPostExecute(String result) {
		try {
			new RequestTask(m)
					.execute("http://www.polarmeter.com/sensors.json");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
