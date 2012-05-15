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
package de.toth.executer.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashSet;
import java.util.StringTokenizer;

import android.os.Environment;
import de.toth.executer.R;

public class FileUtils {
	
	final private static String[] emptyStringArray = new String[0];

	public static String toHumanReadable(final long bytes) {
		if (bytes < 1024L) {
			if(bytes == 1) {
				return "1 Byte";
			}
			return Long.toString(bytes) + " Bytes";
		}
		long kb = bytes / 1024L;
		if (kb < 1024) {
			return Long.toString(kb) + " kb";
		}
		long mb = bytes / 1024L;
		return Long.toString(mb) + " mb";
	}
	
	public static String[] getMountPoints() {
		HashSet<String> mountPoints = new HashSet<String>();
		
        if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
        	mountPoints.add(Environment.getExternalStorageDirectory().getAbsolutePath());
        }
        
        File voldFile = new File("/system/etc/vold.fstab");        
        
        if(voldFile.exists()) {	              
            HashSet<String> voldList = new HashSet<String>();

            try {
				BufferedReader reader = new BufferedReader(new FileReader(voldFile));
				String line = null;
				while((line = reader.readLine()) != null) {
					if(line.startsWith("dev_mount") || line.startsWith("usb_mount")) {
						StringTokenizer tokenizer = new StringTokenizer(line, " ");
						tokenizer.nextToken();
						tokenizer.nextToken();
						voldList.add(tokenizer.nextToken());
					}
				}
				reader.close();
			} catch (Exception e) {
	        	e.printStackTrace();
			}
            
            try {
            	BufferedReader reader = new BufferedReader(new FileReader(new File("/proc/mounts")));
    			String line = null;
    			while((line = reader.readLine()) != null) {
    				StringTokenizer tokenizer = new StringTokenizer(line, " ");
    				tokenizer.nextToken();
    				String mp = tokenizer.nextToken();
    				if(voldList.contains(mp)) {
    					mountPoints.add(mp);
    				}
    			}
            	reader.close();
            } catch (Exception e) {
            	e.printStackTrace();
            }
        }

        return mountPoints.toArray(FileUtils.emptyStringArray);
	}

	public static int getStorageDrawable(File mountPoint) {
		if(mountPoint != null) {
			String path = mountPoint.getAbsolutePath();
			if(path.equals(Environment.getExternalStorageDirectory().getAbsolutePath())) {
				return R.drawable.media_intern;
			}
			
			path = path.toLowerCase();
			
			if(path.contains("usb")) {
				return R.drawable.media_usb;
			}
			if(path.contains("sd") || path.contains("mmc")) {
				return R.drawable.media_sdcard;
			}
		}
		return R.drawable.media_unknown;
	}
}
