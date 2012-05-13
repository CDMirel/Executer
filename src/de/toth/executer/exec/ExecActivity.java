/*  Executer
    Copyright (C) 2012  Alfred Toth

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>. */
package de.toth.executer.exec;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import de.toth.executer.ApplicationContext;
import de.toth.executer.R;

public class ExecActivity extends Activity {

	private EditText terminal = null;
//	private WakeLock wakeLock = null;

	private static ExecuteThread executeThread = null;
	public static Handler handler = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.exec);

		this.terminal = (EditText) findViewById(R.id.terminal);
		this.terminal.setVerticalScrollBarEnabled(true);

		ExecActivity.handler = new Handler() {
			public void handleMessage(Message msg) {
				if (msg.what == 1) {
					String line = (String) msg.obj;
					String text = ExecActivity.this.terminal.getText().toString() + line;
					ExecActivity.this.terminal.setText(text);
				}
				else if (msg.what == 2) {
					View v = ExecActivity.this.findViewById(R.id.cancelItem);
					if(v != null) {
						v.setVisibility(View.INVISIBLE);
					}
					disableWakeLock();
				}
				else if (msg.what == 3) {
					destroyExecuteThread();
				}
			}
		};

		if (ExecActivity.executeThread == null || ExecActivity.executeThread.isAlive() == false) {
			Intent intent = getIntent();
			String scriptFilename = intent.getStringExtra("file");
			boolean su = intent.getBooleanExtra("su", false);

			String script = "";

			try {
				StringBuffer buffer = new StringBuffer();
				BufferedReader reader = new BufferedReader(new FileReader(new File(scriptFilename)));
				String line = null;
				while ((line = reader.readLine()) != null) {
					buffer.append(line).append("\n");
				}
				script = buffer.toString();
			} catch (Exception e) {
				e.printStackTrace();
			}

			ExecActivity.executeThread = new ExecuteThread();
			ExecActivity.executeThread.setScript(script);
			if (su == true) {
				ExecActivity.executeThread.setProcess("su");
			} else {
				ExecActivity.executeThread.setProcess("sh");
			}
			ExecActivity.executeThread.start();

//			PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
//			this.wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, this.getClass().getName());
//			this.wakeLock.acquire();
		}
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		disableWakeLock();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.exec, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();

		switch (id) {
		case R.id.cancelItem:
			try {
				if(ExecuteThread.theProcess != null) {
					if(ExecuteThread.PID != null) {
						new SUKiller(ExecuteThread.PID).start();
					}
					else {
						destroyExecuteThread();
					}
				}
			} catch(Exception e) {
				e.printStackTrace();
			}
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
    	if(keyCode == KeyEvent.KEYCODE_BACK) {
    		if (ExecActivity.executeThread == null || ExecActivity.executeThread.isAlive() == true) {
    			Toast toast = Toast.makeText(this, ApplicationContext.getInstance().getResources().getText(R.string.script_is_still_running).toString(), Toast.LENGTH_LONG);
    			toast.show();
    			return true;
			}
		}
    	return super.onKeyDown(keyCode, event);
    }
    
    private void disableWakeLock() {
//		if(this.wakeLock != null) {
//			this.wakeLock.release();			
//		}
    }
    
    private void destroyExecuteThread() {
		ExecuteThread.theProcess.destroy();
		Toast toast = Toast.makeText(this, ApplicationContext.getInstance().getResources().getText(R.string.script_is_stopped).toString(), Toast.LENGTH_LONG);
		toast.show();
    }
}
