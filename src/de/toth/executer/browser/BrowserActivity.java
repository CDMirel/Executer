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
package de.toth.executer.browser;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Hashtable;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;
import de.toth.executer.R;
import de.toth.executer.about.AboutActivity;
import de.toth.executer.data.FileData;
import de.toth.executer.exec.ExecActivity;
import de.toth.executer.thread.Threads;
import de.toth.executer.utils.FileUtils;

public class BrowserActivity extends Activity implements OnScrollListener, OnItemClickListener {
	
	final private static String PARENT_DIRECTORY = "parentDirectory";
	final private static String CURRENT_DIRECTORY = "currentDirectory";
	final private static String CURRENT_MOUNT_POINT = "currentMountPoint";
	final private static String SCRIPT_FILENAME_TO_EXEC = "scriptFilenameToExec";
	
	public static boolean busy = false;

	private FileListAdapter listViewAdapter = null;
	private ListView listView = null;

	private File parentDirectory = null;
	private File currentDirectory = null;

	private static Hashtable<File, Integer> scrollPositions = null;
	
	private String scriptFilenameToExec = null;
	
	private File currentMountPoint = null;
	
	final private static File homePath = new File("/");

	// verarbeitet Nachrichten des FileCounter-Threads an den
	// BrowserActivity-Thread
	private Handler handler = new Handler() {
		public void handleMessage(Message msg) {
			FileData fileData = (FileData) msg.obj;

			// hole ersten Eintrag der ListView welches gerade angezeigt wird
			int first = listView.getFirstVisiblePosition();
			// hole Anzahl der Einträge der ListView welche angezeigt werden
			int count = listView.getChildCount();

			// gehört die Nachricht zu einem aktuell angezeigtem Eintrag,
			// wenn ja, dann setze die Beschreibung (also Anzahl der
			// Unterverzeichnisse und Bilder)
			if (fileData.position >= first && fileData.position < first + count) {
				View v = listView.getChildAt(fileData.position - first);
				TextView descr = (TextView) v.findViewById(R.id.listItemDescr);
				descr.setText(fileData.descr);
			}
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		// FileListAdapter nimmt die Daten für die Liste mit
		// Dateien/Verzeichnissen der ListView
		this.listViewAdapter = new FileListAdapter(this);

		// ListView einrichten
		this.listView = (ListView) findViewById(R.id.fileListView);
		this.listView.setAdapter(this.listViewAdapter);
		this.listView.setEmptyView(findViewById(android.R.id.empty));
		this.listView.setOnScrollListener(this);
		this.listView.setOnItemClickListener(this);
		// this.listView.setOnKeyListener(this);

		Threads.getInstance().fileCounter.handler = this.handler;

		// warte bis der FileCounter-Thread gestartet wurde
		boolean stop = false;
		while (stop == false) {
			synchronized (Threads.getInstance().fileCounter) {
				if (Threads.getInstance().fileCounter.started == true) {
					stop = true;
				}
			}
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
			}
		}

		if(BrowserActivity.scrollPositions == null) {
			BrowserActivity.scrollPositions = new Hashtable<File, Integer>();
		}
		
		// zeige das Home-Directory an
		this.currentDirectory = BrowserActivity.homePath;

		if(savedInstanceState != null) {
			String temp = savedInstanceState.getString(BrowserActivity.CURRENT_DIRECTORY);
			if(temp != null) {
				this.currentDirectory = new File(temp);
			}
			
			temp = savedInstanceState.getString(BrowserActivity.PARENT_DIRECTORY);
			if(temp != null) {
				this.parentDirectory = new File(temp);
			}
			
			temp = savedInstanceState.getString(BrowserActivity.CURRENT_MOUNT_POINT);
			if(temp != null) {
				this.currentMountPoint = new File(temp);
			}
			
			this.scriptFilenameToExec = savedInstanceState.getString(BrowserActivity.SCRIPT_FILENAME_TO_EXEC);
		}
		
		goToDirectory(this.currentDirectory, false);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		if(this.parentDirectory != null) {
			outState.putString(BrowserActivity.PARENT_DIRECTORY, this.parentDirectory.getAbsolutePath());
		}
		else {
			outState.putString(BrowserActivity.PARENT_DIRECTORY, null);
		}
		if(this.currentDirectory != null) {
			outState.putString(BrowserActivity.CURRENT_DIRECTORY, this.currentDirectory.getAbsolutePath());
		}
		else {
			outState.putString(BrowserActivity.CURRENT_DIRECTORY, null);
		}
		if(this.currentMountPoint != null) {
			outState.putString(BrowserActivity.CURRENT_MOUNT_POINT, this.currentMountPoint.getAbsolutePath());
		}
		else {
			outState.putString(BrowserActivity.CURRENT_MOUNT_POINT, null);
		}
		outState.putString(BrowserActivity.SCRIPT_FILENAME_TO_EXEC, this.scriptFilenameToExec);
		
		super.onSaveInstanceState(outState);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();

		switch (id) {
		case android.R.id.home:
			goToDirectory(this.parentDirectory, true);
			return true;
		case R.id.aboutItem:
			Intent i = new Intent(BrowserActivity.this, AboutActivity.class);
			startActivity(i);			
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main, menu);
		return true;
	}
	
	private void goToDirectory(File directory, boolean back) {
		boolean showMountPoints = false;
		File[] files = null;
		
		if(directory == null) {
			return;
		}

		if (back == false) {
			// merke die Scroll-Position des noch aktuellen Verzeichnisses
			BrowserActivity.scrollPositions.put(this.currentDirectory, this.listView.getFirstVisiblePosition());

			// es wurde eine Ebene tiefer gewechselt und daher gibt es noch
			// keine gespeicherte Scroll-Position
			this.listView.setSelection(0);
		}

		if (directory != null) {
			this.currentDirectory = directory;

			if(directory.equals(BrowserActivity.homePath)) {
				this.currentMountPoint = null;
			}
			
			// setze auch das Eltern-Verzeichnis vom neuen Verzeichnis
			// wenn das neue Verzeichnis das Home-Verzeichnis ist,
			// dann ist das Eltern-Verzeichnis auch das Home-Verzeichnis (höher
			// wie das Homer-Verzeichnis geht nicht)
			if (directory.equals(this.currentMountPoint)) {
				this.parentDirectory = BrowserActivity.homePath;
			} else {
				this.parentDirectory = directory.getParentFile();
			}

			ActionBar actionBar = this.getActionBar();
			if (this.currentMountPoint != null && actionBar != null) {
				actionBar.setDisplayHomeAsUpEnabled(true);
			}
			else {
				actionBar.setDisplayHomeAsUpEnabled(false);
			}

			// lese alle Dateien des neuen aktuellen Verzeichnisses ein
			if(directory.equals(BrowserActivity.homePath)) {
				String[] mountPoints = FileUtils.getMountPoints();
				files = new File[mountPoints.length];
				for(int c=0; c<mountPoints.length; c++) {
					files[c] = new File(mountPoints[c]);
				}
				showMountPoints = true;
			}
			else {
				files = directory.listFiles();
			}

			setTitle(this.currentDirectory.getAbsolutePath());
		}

		// leere die ListView
		this.listViewAdapter.clear();

		if (files == null) {
			files = new File[0];
		}

		// sortiere nach Namen, Verzeichnisse kommen zuerst
		Arrays.sort(files, new Comparator<File>() {
			public int compare(File f1, File f2) {
				if (f1.isDirectory() == true && f2.isDirectory() == false) {
					return -1;
				} else if (f1.isDirectory() == false && f2.isDirectory() == true) {
					return 1;
				}
				return f1.getName().compareTo(f2.getName());
			}
		});

		// Block ist synchronized, da Nachrichten-Schlange des
		// FileCounter-Threads bearbeitet wird und Zugriff darauf nebenläufig
		// ist
		synchronized (Threads.getInstance().fileCounter) {
			// lösche Nachrichten-Schlange an FileCounter-Thread
			Threads.getInstance().fileCounter.messages.clear();
			Threads.getInstance().fileCounter.stillToCount = 0;

			// Anzahl der Verzeichnisse
			int dirCount = 0;
			// durchlaufe alle Datein/Verzeichnisse
			for (int c = 0; c < files.length; c++) {
				// für jede Datei/Verzeichnis gibts ein FileData-Objekt
				FileData fileData = new FileData(files[c], c);
				
				if(showMountPoints == true) {
					fileData.drawable = FileUtils.getStorageDrawable(files[c]);
					switch(fileData.drawable) {
					case R.drawable.media_intern:
						fileData.alias = getResources().getText(R.string.internal_storage).toString();						
						break;
					case R.drawable.media_sdcard:
						fileData.alias = getResources().getText(R.string.sd_card).toString();
						break;
					case R.drawable.media_usb:
						fileData.alias = getResources().getText(R.string.usb_storage).toString();
						break;
					default:
						fileData.alias = files[c].getAbsolutePath();
						break;						
					}
				}

				if (files[c].isDirectory() == true) {
					// aktuelles File ist ein Verzeichnis

					// FileData-Objekt zur ListView hinzufügen
					this.listViewAdapter.addFile(fileData);

					// Anzahl der Verzeichnisse erhöhen
					dirCount = dirCount + 1;

					// Nachricht an FileCounter-Thread dass ein neues
					// Verzeichnis verarbeitet werden muss
					Threads.getInstance().fileCounter.messages.add(fileData);
				} else {
					// File ist eine Datei
					this.listViewAdapter.addFile(fileData);
				}
			}

			// Daten der ListView haben sich geändert und Änderung wird bekannt
			// gegeben
			this.listViewAdapter.notifyDataSetChanged();

			// setze Anzahl der zu bearbeiten Nachrichten für den
			// FileCounter-Thread
			Threads.getInstance().fileCounter.stillToCount = dirCount;

			// wecke FileCounter-Thread auf
			Threads.getInstance().fileCounter.notify();

			if (back == true) {
				Integer scrollPosition = BrowserActivity.scrollPositions.get(directory);
				if (scrollPosition != null) {
					this.listView.setSelection(scrollPosition);
					BrowserActivity.scrollPositions.remove(directory);
				} else {
					this.listView.setSelection(0);
				}
			}
		}
	}

	@Override
	public void onScroll(AbsListView arg0, int arg1, int arg2, int arg3) {
	}

	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {
		switch (scrollState) {
		case OnScrollListener.SCROLL_STATE_IDLE:
			// ListView wird nicht gescrollt und daher ist die Activity nicht
			// beschäftigt
			BrowserActivity.busy = false;
			break;
		default:
			// ListView wird gescrollt und daher ist die Activity beschäftigt
			BrowserActivity.busy = true;
			break;
		}
	}

	@Override
	public Dialog onCreateDialog(int dialogID, Bundle bundle) {
		Dialog dialog = null;
		
		if (dialogID == 1) {
			final CharSequence[] items = { getResources().getText(R.string.run).toString(), getResources().getText(R.string.run_as_root).toString(), getResources().getText(R.string.view).toString() };
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(new File(this.scriptFilenameToExec).getName());
			builder.setItems(items, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int item) {
					switch (item) {
					case 0:
						Intent i = new Intent(BrowserActivity.this, ExecActivity.class);
						i.putExtra("file", BrowserActivity.this.scriptFilenameToExec);
						i.putExtra("su", false);
						startActivity(i);
						break;
					case 1:
						Intent i2 = new Intent(BrowserActivity.this, ExecActivity.class);
						i2.putExtra("file", BrowserActivity.this.scriptFilenameToExec);
						i2.putExtra("su", true);
						startActivity(i2);
						break;
					case 2:
		                Intent i3 = new Intent(android.content.Intent.ACTION_VIEW);
		                Uri data = Uri.fromFile(new File(BrowserActivity.this.scriptFilenameToExec));
		                i3.setDataAndType(data, "text/plain");
                        startActivity(i3);
						break;
					}
				}
			});
			dialog = builder.create();
		}

		return dialog;
	}
	
	@SuppressWarnings("deprecation")
	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {
		super.onPrepareDialog(id, dialog);
		dialog.setTitle(new File(this.scriptFilenameToExec).getName());
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		FileData fileData = (FileData) this.listViewAdapter.getItem(position);
		if(fileData != null) {
			if(fileData.file.isFile() && fileData.file.getName().endsWith(".sh")) {
				this.scriptFilenameToExec = fileData.file.getAbsolutePath();
				showDialog(1);
			}
			else if(fileData.file.isDirectory()) {
				if(this.currentMountPoint == null) {
					this.currentMountPoint = fileData.file;
				}
				goToDirectory(fileData.file, false);
			}
		}
	}
	
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
    	if(keyCode == KeyEvent.KEYCODE_BACK) {
    		if(this.currentDirectory.equals(BrowserActivity.homePath) == false) {
    			goToDirectory(this.parentDirectory, true);
    			return true;
    		}
    	}
    	return super.onKeyDown(keyCode, event);
    }

}
