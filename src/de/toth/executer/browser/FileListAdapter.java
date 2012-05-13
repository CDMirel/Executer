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

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import de.toth.executer.R;
import de.toth.executer.data.FileData;
import de.toth.executer.utils.FileUtils;

public class FileListAdapter extends BaseAdapter {

	private Context context = null;
	private ArrayList<FileData> files = new ArrayList<FileData>();
	private LayoutInflater inflater;
//	private Integer filenameColor = null;

	public FileListAdapter(Context c) {
		this.context = c;
		this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
//		this.filenameColor = this.context.getResources().getColor(R.color.filename);
	}

	public int getCount() {
		return this.files.size();
	}

	public Object getItem(int position) {
		return this.files.get(position);
	}

	public long getItemId(int position) {
		return position;
	}

	public View getView(int position, View convertView, ViewGroup parent) {
		if (convertView == null) {
			convertView = this.inflater.inflate(R.layout.listitem, parent, false);
		}

		// hole die Daten der Datei/Verzeichnis zur Position
		FileData fileData = this.files.get(position);
		File file = fileData.file;

		// setze Bild
		ImageView image = (ImageView) convertView.findViewById(R.id.listItemImage);
		if (fileData.drawable == -1) {
			if (file.isFile() == true) {
				if(file.getName().endsWith(".sh")) {
					image.setImageResource(R.drawable.script);
				}
				else {
					image.setImageResource(R.drawable.file);
				}
			} else {
				image.setImageResource(R.drawable.folder);
			}
		}
		else {
			image.setImageResource(fileData.drawable);
		}

		// setze Name der Datei/des Verzeichnisses
		TextView name = (TextView) convertView.findViewById(R.id.listItemName);
		if(fileData.alias == null) {
			name.setText(file.getName());
		}
		else {
			name.setText(fileData.alias);
		}
		
//		if(file.exists() == false) {
//			name.setTextColor(Color.RED);			
//		}
//		else {
//			name.setTextColor(this.filenameColor);			
//		}

		// setze Beschreibung
		TextView descr = (TextView) convertView.findViewById(R.id.listItemDescr);
		if (fileData.descr != null && fileData.descr.length() > 0) {
			descr.setText(fileData.descr);
		} else {
			if (file.isFile() == true) {
				String temp = FileUtils.toHumanReadable(file.length());
				fileData.descr = temp;
				descr.setText(temp);
			} else {
				descr.setText("");
			}
		}
		
		convertView.setTag(position);		
//		convertView.setOnClickListener((OnClickListener)parent.getContext());

		return convertView;
	}

	public void addFile(FileData fileData) {
		this.files.add(fileData);
	}

	public void clear() {
		this.files.clear();
	}

}
