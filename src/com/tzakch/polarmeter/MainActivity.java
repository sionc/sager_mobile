package com.tzakch.polarmeter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.AsyncTask;
import android.os.Bundle;
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

	public void refreshLayout(String response){
		JSONObject jObject = null;
		try {
			jObject = new JSONObject(response);
			JSONArray sensorsArray = jObject.getJSONArray("sensors");
			for(int i =0; i< sensorsArray.length(); i++){
				Button btn = new Button(this);
				btn.setText(sensorsArray.getJSONObject(i).get("label").toString());
				btn.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT,
						LayoutParams.WRAP_CONTENT));
				
				ll.addView(btn);
			}
			ll.invalidate();

		} catch (JSONException e) {
			e.printStackTrace();
		}
		
	}

	@SuppressWarnings("deprecation")
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		

		ll = new LinearLayout(this);
		ll.setOrientation(LinearLayout.VERTICAL);
		ll.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT,
				LayoutParams.FILL_PARENT));
		ll.setGravity(Gravity.CENTER);

		
		
		setContentView(ll);
		
		try {
			new RequestTask(this).execute("http://www.polarmeter.com/sensors.json");
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	public void onClick(View v) {

		Button button = (Button) v;
		Toast.makeText(getApplicationContext(), button.getText().toString(), 2)
				.show();

		Button btn3 = new Button(this);
		btn3.setText("Wooshaaa");
		btn3.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT,
				LayoutParams.WRAP_CONTENT));

		this.ll.addView(btn3);
		this.ll.removeView(v);
		this.ll.invalidate();

	}

}

class RequestTask extends AsyncTask<String, String, String>{
	MainActivity m;
	
    public RequestTask(MainActivity mainActivity) {
    	m = mainActivity;
	}
    
	protected String doInBackground(String... uri) {
        HttpClient httpclient = new DefaultHttpClient();
        HttpResponse response;
        String responseString = null;
        try {
            response = httpclient.execute(new HttpGet(uri[0]));
            StatusLine statusLine = response.getStatusLine();
            if(statusLine.getStatusCode() == HttpStatus.SC_OK){
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                response.getEntity().writeTo(out);
                out.close();
                responseString = out.toString();
            } else{
                //Closes the connection.
                response.getEntity().getContent().close();
                throw new IOException(statusLine.getReasonPhrase());
            }
        } catch (ClientProtocolException e) {
        	Log.e("tim","ClientProtocolException");
        } catch (IOException e) {
        	Log.e("tim","IOException");        }
        return responseString;
    }

    protected void onPostExecute(String result) {
        //super.onPostExecute(result);
    	m.refreshLayout(result);
    }
}

