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

public class SUKillerOutputThread extends Thread {

	private InputStream input = null;

	public void run() {
		try {
			boolean stop = false;
			byte[] buffer = new byte[1024];
			while (stop == false) {
				while (input.available() > 0) {
					int count = input.read(buffer, 0, 1024);
					if (count == -1) {
						break;
					}
				}

				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					try {
						while (input.available() > 0) {
							int count = input.read(buffer, 0, 1024);
							if (count == -1) {
								break;
							}
						}
					} catch (IOException e2) {
						e2.printStackTrace();
					}

					stop = true;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void setInputStream(InputStream is) {
		this.input = is;
	}
}
