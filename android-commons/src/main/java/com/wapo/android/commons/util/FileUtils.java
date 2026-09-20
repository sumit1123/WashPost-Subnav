/*
 *
 *   Copyright (C) 2014 . The Washington Post. All rights reserved.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package com.wapo.android.commons.util;

import android.content.Context;

import java.io.*;

public class FileUtils {
    /**
     * Return the size of a directory in bytes
     *
     * @ dir - directory name
     */
    public static long dirSize(File dir) {
        try {
            if (dir.exists()) {
                long result = 0L;
                File[] fileList = dir.listFiles();
                for (int i = 0; i < fileList.length; i++) {
                    // Recursive call if it's a directory
                    if (fileList[i].isDirectory()) {
                        result += dirSize(fileList[i]);
                    } else {
                        // Sum the file size in bytes
                        result += fileList[i].length();
                    }
                }
                return result; // return the file size
            }
            return 0L;
        } catch (Exception e) {
            return 0L;
        }
    }

    public static byte[] getBytesFromFile(File file) {
        FileInputStream fileInputStream = null;

        byte[] bFile = new byte[(int) file.length()];

        try {
            //convert file into array of bytes
            fileInputStream = new FileInputStream(file);
            fileInputStream.read(bFile);
            fileInputStream.close();

            return bFile;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }


    public static String getContentsFromFile(File file) {
        StringBuilder out = new StringBuilder();
        try {
            InputStream in = new FileInputStream(file);

            BufferedReader reader = new BufferedReader(new InputStreamReader(in));

            String line;
            while ((line = reader.readLine()) != null) {
                out.append(line);
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return out.toString();
    }

    public static String getContentsFromFile(InputStream inputStream) {
        StringBuilder out = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                out.append(line);
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return out.toString();
    }

    public static double getPrintEditionSizeInDisk(Context context, String archiveFolderName) {
        long bytes = dirSize(getArchivesFilePath(context, archiveFolderName));
        return (bytes / (1024D * 1024D));
    }

    public static File getArchivesFilePath(Context context, String folderName) {
        File extDir = context.getExternalFilesDir(null);
        File pdfRootDir = new File(extDir, folderName);
        return pdfRootDir;
    }
}
