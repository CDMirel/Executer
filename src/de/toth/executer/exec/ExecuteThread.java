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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.os.Message;

public class ExecuteThread extends Thread {

	private String script = "";
	private String process = "";
	static public Process theProcess = null;
	static public String PID = null;
	
	private boolean destroyMode = false;
	
	public void run() {
		if (this.destroyMode == true) {
			try {
				ExecuteThread.theProcess.destroy();
			} catch (Exception e) {
			}
			return;
		}
		
		try {
			ExecuteThread.theProcess = Runtime.getRuntime().exec(this.process);
			ExecuteThread.PID = null;
			
			OutputStream stdin = ExecuteThread.theProcess.getOutputStream();
			InputStream stdout = ExecuteThread.theProcess.getInputStream();
			InputStream stderr = ExecuteThread.theProcess.getErrorStream();
			
			if(this.process.equals("su")) {
				ExecuteThread.PID = getPID(stdin, stdout);
			}
			
			stdin.write(this.script.getBytes(), 0, this.script.length());
			stdin.write("exit\n".getBytes(), 0, "exit\n".length());			
			stdin.flush();
			
			ProcessOutputThread stdoutThread = new ProcessOutputThread();
			stdoutThread.setInputStream(stdout);
			stdoutThread.start();

			ProcessOutputThread stderrThread = new ProcessOutputThread();
			stderrThread.setInputStream(stderr);
			stderrThread.start();
			
			ExecuteThread.theProcess.waitFor();
			
	        stdoutThread.interrupt();
	        stderrThread.interrupt();
	         
			while(stdoutThread.isAlive() || stderrThread.isAlive()) {
				try {
					Thread.sleep(50);
				} catch(InterruptedException e) {					
				}
			}
			
			stderr.close();
			stdout.close();
			stdin.close();
			
			try {
				ExecuteThread.theProcess.destroy();
			} catch(Exception e) {
				e.printStackTrace();
			}
			
			Message msg = Message.obtain(ExecActivity.handler, 2);
			ExecActivity.handler.sendMessage(msg);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		ExecuteThread.theProcess = null;
	}
	
	public void setScript(String s) {
		this.script = s;
	}
	
	public void setProcess(String p) {
		this.process = p;
	}

	public void destroy() {
		this.destroyMode = true;
		start();
	}
	
	private String getPID(OutputStream stdin, InputStream stdout) throws IOException {
		stdin.write("echo $$\n".getBytes(), 0, 8);
		
		byte[] pidBuffer = new byte[6];
		int pidC = 0;
		int b = -1;
		while((b = stdout.read()) != 10) {
			pidBuffer[pidC++] = (byte)b;
		}
		
		return new String(pidBuffer, 0, pidC);			
	}
}
