package com.un1tz3r0.streamdownloader;

import android.app.*;
import android.content.*;
import android.net.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import com.soundcloud.api.*;
import java.io.*;
import java.lang.ref.*;
import org.apache.http.*;
import org.json.*;

public class ShareReceiver extends Activity
{
    //public final String mClientID = "912849b714109f12abefbf47096d864d";
	//public final String mClientSecret = "7c12d9cebc05ccd42b9570bbf675d613";


	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.receiver);

		final Button cancelbtn = (Button) findViewById(R.id.ButtonCancel);
		//cancelbtn.setEnabled(false);
		cancelbtn.setOnClickListener(
			new View.OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					//streamtask.cancel(true);
					finish();
				}
			}
		);



		try
		{
			Intent intent = getIntent();
			String action = intent.getAction();
			String type = intent.getType();

			if ((!(Intent.ACTION_SEND.equals(action))) || (type == null))
			{
				throw new RuntimeException("Unknown intent action or type");
			}

			if (!("text/plain".equals(type)))
			{
				throw new RuntimeException("Type is not text/plain");
			}

			String extra = intent.getStringExtra(Intent.EXTRA_TEXT);
			if (extra == null)
			{
				throw new RuntimeException("Cannot get shared text");
			}


			final DownloadStreamTask streamtask = new DownloadStreamTask(this);

			//	Once created, a task is executed very simply:
			streamtask.execute(extra);


		}
		catch (Exception e)
		{
			Toast.makeText(this, e.toString(), Toast.LENGTH_LONG).show();
		}
	}


	private class DownloadStreamTask extends AsyncTask<String, String, String[] > 
	{
		private final WeakReference<Context> mContext;
		private TextView text1;
		//private final ProgressDialog dialog

		@Override
		public DownloadStreamTask(Context _c)
		{
			mContext = new WeakReference<Context>(_c);
			//dialog = new ProgressDialog(myact.this);

			text1 = (TextView) findViewById(R.id.textView1);

			Button cancelbtn = (Button) findViewById(R.id.ButtonCancel);
			cancelbtn.setOnClickListener(
				new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						// Do something in response to button click 
						cancel(true);
					} 
				}
			);

			cancelbtn.setEnabled(true);
		}

		protected void doDownload(String url, String filename)
		{
			//String url = "url you want to download";
			DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
			request.setDescription(filename);
			request.setTitle("Stream Downloader");
            
			// in order for this if to run, you must use the android 3.2 to compile your app
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
				request.allowScanningByMediaScanner();
				request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
			}
			request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename);

            // get download service and enqueue file
			DownloadManager manager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
			manager.enqueue(request);
		}
		
		@Override
		protected void onPreExecute() 
		{
			//this.dialog.setMessage("Please Wait...");
			//this.dialog.show();
			// ... code which load at prefix time ...
		}

		protected String resolveURL(ApiWrapper wrapper, String url) throws IOException
		{
			String result = "";

			HttpResponse reso = wrapper.head(Request.to(Endpoints.RESOLVE).with("url", url));
			if (reso.getStatusLine().getStatusCode() != HttpStatus.SC_MOVED_TEMPORARILY)
				throw new CloudAPI.ResolverException("Invalid status code", reso);

			//Header location = reso.getFirstHeader("Location");
			if (!reso.containsHeader("Location"))
				throw new CloudAPI.ResolverException("No location header", reso);
			result = reso.getFirstHeader("Location").getValue();

			return result;
		}

		@Override
		protected String[] doInBackground(String... parms) 
		{
			String[] resultarr = new String[1];
			String intentstr = parms[0];

			//int count = urls.length;
			//long totalSize = 0; for (int i = 0; i < count; i++) { totalSize += Downloader.downloadFile(urls[i]); 
			//publishProgress((int) ((i / (float) count) * 100));
			// Escape early if cancel() is called
			//if (isCancelled())
			//		break;

			try
			{
				// Connect to API and authenticate
				publishProgress("Connecting and authenticating API session...");

				ApiWrapper wrapper;
				//wrapper = Api.wrapper; 
				wrapper = new ApiWrapper(Api.mClientID, Api.mClientSecret, null, null);
				wrapper.login("un1tz3r0", "Farscap3");
				
				publishProgress("Resolving url...");

				String resolvedurl = resolveURL(wrapper, intentstr);

				publishProgress("Getting metadata...");

				HttpResponse resp = wrapper.get(Request.to(resolvedurl));

				JSONObject jso = Http.getJSON(resp);
				//resultstr = jso.toString();

				if (jso.getString("kind").equals("track"))
				{
					if (jso.getBoolean("downloadable"))
					{
						publishProgress("Getting download redirect URL...");
						
						String dlrurl = wrapper.getURI( Request.to(jso.getString("download_url")).add("allow_redirect", false), false, false ).toString();
						HttpResponse dlrresp = wrapper.get( Request.to( dlrurl ) );
						String dlstr = dlrurl;
						if(dlrresp.getStatusLine().getStatusCode() == HttpStatus.SC_MOVED_TEMPORARILY)
						{
					        Header dlloch = dlrresp.getFirstHeader("location");
						    if((dlloch == null) || (dlloch.getValue() == null))
							    throw new RuntimeException("Download url HEAD response has no location header");
								
							dlstr = wrapper.getURI( Request.to( dlloch.getValue() ) , false, false).toString();
                        }
						else if(dlrresp.getStatusLine().getStatusCode() == HttpStatus.SC_OK)
						{
							dlstr = dlrurl;
						}
						else
						{
							throw new RuntimeException("Download url HEAD response has wrong status (" + String.valueOf(dlrresp.getStatusLine().getStatusCode()) + " " + dlrresp.getStatusLine().getReasonPhrase() + ")");
						}
						//String dlstr = Request.to( dlloch.getValue() ).add("CLIENT_ID", Api.mClientID).toString();

//												if(dlresp2.getStatusLine().getStatusCode() != HttpStatus.SC_MOVED_TEMPORARILY)
//														throw new RuntimeException("Download redirect url HEAD response has wrong status: " + dlresp2.getStatusLine().toString());
//												Header dlloc2 = dlresp2.getFirstHeader("location");
//												if((dlloc2 == null) || (dlloc2.getValue() == null))
//														throw new RuntimeException("Download redirect url HEAD response has no location header");
//											

						resultarr = new String[2];
						resultarr[1] = jso.getString("title").replaceAll("[^A-Za-z0-9 -]*", "") + "." + jso.getString("original_format");
						resultarr[0] = dlstr;
					}
					else
					{
						Stream st = wrapper.resolveStreamUrl(jso.getString("stream_url"), true);
						resultarr = new String[2];
						resultarr[1] = jso.getString("title").replaceAll("[^A-Za-z0-9 -]*", "").concat(".mp3");
						resultarr[0] = st.streamUrl;
					}
				}
			}
			catch (JSONException e)
			{
				resultarr = new String[1];
				resultarr[0] = e.toString();
			}
			catch (IOException e)
			{
				resultarr = new String[1];
				resultarr[0] = e.toString();
			}
			catch (Exception e)
			{
				resultarr = new String[1];
				resultarr[0] = e.toString();
			}

			return resultarr;
		}

		protected void onProgressUpdate(String... progress) 
		{ 
			//setProgressPercent(progress[0]);
			text1.setText(progress[0]);
		}

		protected void onPostExecute(String[] result)
		{ 
			//("Download complete: " + result + "\nComplete");
			
			if (result.length < 2)
			{
				text1.setText("Error: " + result[0]);

			}
			else
			{
				//text1.setText("Starting download " + result[1] + "... " + result[0]);

				doDownload(result[0], result[1]);

				finish();
				
				Toast.makeText(mContext.get(), "Downloading " + result[1] + "...", Toast.LENGTH_LONG).show();
			}
		}
	}

}
