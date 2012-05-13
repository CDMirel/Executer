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
import java.util.ArrayList;

import android.os.Handler;
import android.os.Message;
import de.toth.executer.ApplicationContext;
import de.toth.executer.R;
import de.toth.executer.data.FileData;

public class FileCounterThread extends Thread {

	public ArrayList<FileData> messages = null;
	public int stillToCount = 0;
	public Handler handler = null;
	private static int count[] = new int[2];
	
	// started = true wenn der Thread gestartet wurde
	public boolean started = false;
	public boolean stop = false;

	public FileCounterThread() {
		this.messages = new ArrayList<FileData>();
	}

	public void run() {
		while (true) {
			synchronized (this) {
				try {
					// setze true, da Thread gestartet wurde
					this.started = true;
					
					// warte auf notify
					wait();
				} catch (InterruptedException e) {
					if(this.stop == true) {
						break;
					}
				}
			}

			boolean finished = false;
			while (finished == false) {
				FileData msg = null;
				// hole die nächste Nachricht an FileCounter-Thread
				// ist sychronized, da nebenläufiger Zugriff auf messages-Objekt
				synchronized (this) {
					if (this.messages.size() > 0) {
						msg = this.messages.remove(0);
					}
				}

				boolean msgProcessed = false;
				if (msg != null && msg.descr == null) {					
					File file = msg.file;
					
					// zähle Anzahl der Verzeichnisse und Dateien
					countFiles(file);

					int dirCount = FileCounterThread.count[0];
					int fileCount = FileCounterThread.count[1];

					// Setze Beschreibung zum aktuellen Verzeichniss
					String temp = Integer.toString(fileCount);
					if (fileCount == 1) {
						temp = temp + " " + ApplicationContext.getInstance().getResources().getText(R.string.file).toString() + ", ";
					} else {
						temp = temp + " " + ApplicationContext.getInstance().getResources().getText(R.string.files).toString() + ", ";
					}
					temp = temp + Integer.toString(dirCount);
					if (dirCount == 1) {
						temp = temp + " " + ApplicationContext.getInstance().getResources().getText(R.string.directory).toString();
					} else {
						temp = temp + " " + ApplicationContext.getInstance().getResources().getText(R.string.directories).toString();
					}
					msg.descr = temp;

					// Nachricht wurde bearbeitet und wird daher an BrowserActivity-Thread zurück geschickt
					Message sendMsg = new Message();
					sendMsg.obj = msg;
					this.handler.sendMessage(sendMsg);
					
					// Nachricht wurde bearbeitet
					msgProcessed = true;
				}

				// wurde Nachricht verarbeitet, dann stillToCount runter zählen
				// synchronized, da nebenläufiger Zugriff auf stillToCount
				synchronized (this) {
					if (msgProcessed == true) {
						this.stillToCount = this.stillToCount - 1;
					}
					if (this.stillToCount == 0) {
						// schleife beenden, da keine Nachrichten mehr vorhanden
						finished = true;
					}
				}

				try {
					// Wenn die BrowserActivity beschäftigt ist, also gescrollt wird, dann 100ms warten
					if (BrowserActivity.busy == true) {
						Thread.sleep(100);
					} else {
						// yield();
					}
				} catch (InterruptedException e) {
				}
			}
		}
	}

	/**
	 * Zählt die Anzahl der Dateien und Unterverzeichnisse eines Verzeichnisses 
	 * @param directory
	 */
	private void countFiles(final File directory) {
		File[] files = directory.listFiles();
		if (files == null) {
			return;
		}
		int filesCount = 0;
		int dirCount = 0;
		for (int c = 0; c < files.length; c++) {
			if (files[c].isDirectory() == true) {
				dirCount = dirCount + 1;
			} else {
				filesCount = filesCount + 1;
			}
		}
		FileCounterThread.count[0] = dirCount;
		FileCounterThread.count[1] = filesCount;
	}

}
