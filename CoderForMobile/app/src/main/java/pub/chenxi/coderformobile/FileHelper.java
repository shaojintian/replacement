package pub.chenxi.coderformobile;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class FileHelper {
    private static final String TAG = "FileHelper";
    /**
     * 将assets中的识别库复制到SD卡中
     *
     * @param path 要存放在SD卡中的 完整的文件名。这里是"/storage/emulated/0//tessdata/chi_sim.traineddata"
     * @param name assets中的文件名 这里是 "chi_sim.traineddata"
     */
    public static void copyToSD(Context context , String path, String name) {
        Log.i(TAG, "copyToSD: " + path);
        Log.i(TAG, "copyToSD: " + name);


        File f = new File(path);

        if (!f.exists()) {
            File p = new File(f.getParent());
            if (!p.exists()) {
                p.mkdirs();
            }
            try {
                f.createNewFile();
            } catch (IOException e) {
                Log.e(TAG, "copyToSD.createNewFile: ", e);
            }

            InputStream is = null;
            OutputStream os = null;
            try {
                is = context.getAssets().open(name);
                File file = new File(path);
                os = new FileOutputStream(file);
                byte[] bytes = new byte[2048];
                int len = 0;
                while ((len = is.read(bytes)) != -1) {
                    os.write(bytes, 0, len);
                }
                os.flush();
            } catch (IOException e) {
                Log.e(TAG, "copyToSD.copyFile: ", e);
            } finally {
                try {
                    if (is != null)
                        is.close();
                    if (os != null)
                        os.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e(TAG, "copyToSD.closeIO: ", e);
                }
            }
        }// ~ if file not exist
    }//copyToSD
}
