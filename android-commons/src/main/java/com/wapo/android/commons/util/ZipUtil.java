package com.wapo.android.commons.util;

import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ZipUtil {
    public static byte[] extractFileFromZip(String fileName, InputStream is) throws IOException {
        ZipInputStream zipIs = null;
        fileName = fileName.startsWith("/") ? fileName : "/" + fileName;
        try {
            zipIs = new ZipInputStream(is);
            ZipEntry entry;
            while((entry = zipIs.getNextEntry()) != null) {
                if (entry.isDirectory())
                    continue;
                String name = entry.getName();
                name = name.startsWith("/") ? name : "/" + name;
                if (fileName.equals(name)) {
                    ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
                    byte[] buff = new byte[4096];
                    int len;
                    while((len = zipIs.read(buff)) > 0) {
                        bytesOut.write(buff, 0, len);
                    }
                    return bytesOut.toByteArray();
                }
            }
        } finally {
            if (zipIs != null)
                zipIs.close();
        }

        return null;
    }

    public static byte[] extractFileFromZip(String fileName, File file) throws IOException {
        byte[] result = extractFileFromZip(fileName, new BufferedInputStream(new FileInputStream(file)));
        if (result == null) {
            throw new FileNotFoundException(String.format("File %s not found in %s", fileName, file.getName()));
        }
        return result;
    }

    public static void extract(File archiveFile, File targetFolder) throws IOException {
        FileInputStream fileInputStream = new FileInputStream(archiveFile);
        BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);
        ZipInputStream zipIs = new ZipInputStream(bufferedInputStream);
        ZipEntry entry;
        while((entry = zipIs.getNextEntry()) != null) {
            File target = new File(targetFolder, adjustFileName(entry.getName()));
            if (entry.isDirectory()) {
                continue;
            }

            File parent = target.getParentFile();
            if (!parent.exists() && !parent.mkdirs()) {
                Logger.e("extract", "unable to create path: " + parent.getPath());
                continue;
            }

            OutputStream os = null;
            try {
                os = new BufferedOutputStream(new FileOutputStream(target));
                byte[] buff = new byte[4096];
                int len;
                while((len = zipIs.read(buff)) > 0) {
                    os.write(buff, 0, len);
                }
            } catch (FileNotFoundException e) {
                Logger.e("extract", "unable to create file: " + target.getAbsolutePath(), e);
            } finally {
                if (os != null) {
                    os.close();
                }
            }
        }
        fileInputStream.close();
        bufferedInputStream.close();
        zipIs.close();
    }

    public static String adjustFileName(String path) {
        return path.replaceAll("\\?+", "");
    }
}
