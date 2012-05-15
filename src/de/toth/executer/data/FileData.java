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
package de.toth.executer.data;

import java.io.File;

public class FileData {
	public File file = null;
	public String descr = null;
	public int position = -1;
	public int drawable = -1;
	public String alias = null;
	
	public FileData(File f, int pos) {
		this.file = f;
		this.descr = null;
		this.position = pos;
	}
}
