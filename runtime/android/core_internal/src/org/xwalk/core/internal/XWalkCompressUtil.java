package org.xwalk.core.internal;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;

import android.content.Context;
import android.content.res.Resources.NotFoundException;
import android.util.Log;

import SevenZip.Compression.LZMA.Decoder;

public class XWalkCompressUtil {
    private static final String TAG = "XWalkCompressUtil";

    public static boolean decompressNativeLibrary(Context context,
            String[] libraries, String appDataDir) {
        for (String library : libraries) {
            String libraryName = library.split("\\.")[0];
            int resId = context.getResources().getIdentifier(
                    libraryName, "raw", context.getPackageName());
            try {
                if (resId != 0) {
                    InputStream input = context.getResources().openRawResource(resId);
                    BufferedOutputStream output = new BufferedOutputStream(
                            new FileOutputStream(new File(appDataDir, library)));

                    if (!decodeLzma(input, output)) return false;
                }
            } catch (NotFoundException e) {
                Log.w(TAG, "R.w" + libraryName + " can't be found.");
                return false;
            } catch (IOException e) {
                Log.w(TAG, "IO Exception while decompressing.");
                return false;
            }
        }
        return true;
    }

    private static boolean decodeLzma(InputStream input,
            OutputStream output) throws IOException {
        int propSize = 5;
        byte[] properties = new byte[propSize];
        if (input.read(properties, 0, propSize) != propSize) {
            Log.w(TAG, "input .lzma file is too short");
            return false;
        }

        Decoder decoder = new Decoder();
        if (!decoder.SetDecoderProperties(properties)) {
            Log.w(TAG,"Incorrect stream properties");
        }

        long outSize = 0;
        for (int i = 0; i < 8; i++) {
            int v = input.read();
            if (v < 0) {
               Log.w(TAG, "Can't read stream size");
            }
            outSize |= ((long)v) << (8 * i);
        }
        if (!decoder.Code(input, output, outSize)) {
            Log.w(TAG, "Error in data stream");
        }
        output.flush();
        output.close();
        input.close();
        return true;
    }
}
