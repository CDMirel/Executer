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

import java.io.InputStream;
import java.io.OutputStream;

import android.os.Message;

public class SUKiller extends Thread {

	private String pid = null;
	
	public SUKiller(String pid) {
		super();
		
		this.pid = pid;
	}
	
	public void run() {
		if(this.pid == null) {
			return;			
		}
		
		try {			
			Process process = new ProcessBuilder().command("su").redirectErrorStream(true).start();

			InputStream stdout = process.getInputStream();
			OutputStream stdin = process.getOutputStream();

			SUKillerOutputThread stdoutThread = new SUKillerOutputThread();
			stdoutThread.setInputStream(stdout);
			stdoutThread.start();
			
			String kill = "kill -9 " + this.pid + "\n";
			stdin.write(kill.getBytes(), 0, kill.length());
			stdin.write("exit\n".getBytes(), 0, "exit\n".length());			
			stdin.flush();
			
			process.waitFor();
			
			stdoutThread.interrupt();
			
			try {
				process.destroy();
			} catch(Exception e) {				
			}
			
			Message msg = Message.obtain(ExecActivity.handler, 3);
			ExecActivity.handler.sendMessage(msg);
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
}
