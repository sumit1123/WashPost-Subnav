package com.wapo.android.commons.logger;

import android.test.AndroidTestCase;

import com.wapo.android.commons.util.Compress;

import java.io.File;
import java.io.FileWriter;

/**
 * Created by saneeshc on 10/17/14.
 */
public class CompressTest extends AndroidTestCase {
    File srcFile;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        srcFile = new File(getContext().getFilesDir().getAbsolutePath(), "/" + "test.log");
        if (!srcFile.isFile()) {
            srcFile.createNewFile();
        }

        FileWriter fw = new FileWriter(srcFile, true);
        fw.write("Compression Test \n");
        fw.close();
    }

    public void testS3UploadRightKey() throws Exception {
        String destPath = getContext().getFilesDir().getAbsolutePath() + "/" + "compressed.log";
        new Compress().gZipFile(srcFile.getAbsolutePath(), destPath);

        File file = new File(destPath);
        assertTrue(file.exists());

        file.delete();
    }
}
