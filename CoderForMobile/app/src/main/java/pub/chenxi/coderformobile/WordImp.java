package pub.chenxi.coderformobile;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.util.Log;

import com.googlecode.tesseract.android.TessBaseAPI;

import org.apache.commons.lang3.StringUtils;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Range;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import pub.chenxi.cv.IWordTransfer;
import pub.chenxi.cv.entity.GRectangle;
import pub.chenxi.cv.entity.ShapeFile;
import pub.chenxi.cv.entity.WordFile;

import static android.support.constraint.Constraints.TAG;

public class WordImp implements IWordTransfer {
    Context context;
    Bitmap bitmap;
    public WordImp(Context _context, Bitmap _bitmap) {
        context=_context;
        bitmap  = _bitmap;
    }

    @Override
    public List<WordFile> shapeRange2Word(List<ShapeFile> list) {

        List<WordFile> words = new ArrayList<>();
        //buildWordImg(list);

        for(ShapeFile shapeFile:list){
            Bitmap lvBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.g297_2_2);
            String name = getContent(lvBitmap);


            GRectangle position = shapeFile.getPositon();
            WordFile wordStr = new WordFile(name,position);
            words.add(wordStr);
        }

        return words;
    }

    public List<WordFile> shapeRange2Word2(List<GRectangle> list) {

        List<WordFile> words = new ArrayList<>();
        List<String> listImgPath= buildWordImg(list);

        for(int i =0;i<list.size();i++){
            GRectangle shapeFile= list.get(i);
            String path = listImgPath.get(i);
            BitmapFactory.Options options = new BitmapFactory.Options();
            Bitmap lvBitmap =BitmapFactory.decodeFile(path, options);
            //Bitmap lvBitmap =readBitmapFromFile(path,)

                    //Bitmap lvBitmap =BitmapFactory.decodeResource(context.getResources(), R.drawable.g297_2_2);
            String name = getContent(lvBitmap);

            if(StringUtils.isBlank(name)){
                continue;
            }

            //GRectangle position = shapeFile.getPositon();
            WordFile wordStr = new WordFile(name,shapeFile);
            words.add(wordStr);
        }

        return words;
    }

    private static final String DATAPATH = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator;
    private Mat gray =new Mat();
    private List<String> buildWordImg(List<GRectangle> list){
        List<String> listPath = new ArrayList<>();
        Mat src = new Mat(bitmap.getHeight(), bitmap.getWidth(), CvType.CV_8UC4);
        Utils.bitmapToMat(bitmap, src);

        String grayName = DATAPATH+"gray.png";
        saveImg(bitmap,grayName);
        Imgproc.cvtColor(src, gray,Imgproc.COLOR_BGR2GRAY);

        try{
            for(int i =0;i<list.size();i++){
                GRectangle rectangle= list.get(i);

                String fileName3 = DATAPATH+i+"_new.png";

//            Mat m = gray.rowRange(new Range(rectangle.getX(),rectangle.getX()+rectangle.getWidth()));
//            Mat m2 = m.rowRange(new Range(rectangle.getY(),rectangle.getY()+rectangle.getHeight()));
                int padding = 0;
                int paddingStart = 0;

                if(rectangle.getArea()>700000){
                    padding = 100;
                    paddingStart = 30;
                }
                saveImg( Bitmap.createBitmap(bitmap,rectangle.getX()+paddingStart,rectangle.getY()+paddingStart,rectangle.getWidth()-padding,rectangle.getHeight()-padding),fileName3);
                listPath.add(fileName3);
            }
        }catch (Exception ex){
            Log.e(TAG,"buildWordImg",ex);
        }

       return listPath;


    }



    private Bitmap matToBitMap(Mat result) {
        //Create Result
        Bitmap bmp_result = Bitmap.createBitmap(result.cols(), result.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(result, bmp_result);
        return bmp_result;
    }


    private void saveImg(Bitmap bmp,String filename){
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(filename);
            bmp.compress(Bitmap.CompressFormat.PNG, 100, out); // bmp is your Bitmap instance
            // PNG is a lossless format, the compression factor (100) is ignored
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private String getContent(Bitmap lvBitmap){
        TessBaseAPI lvBaseAPI = new TessBaseAPI();
        lvBaseAPI.init(Environment.getExternalStorageDirectory().getPath(), "chi_sim");
        lvBaseAPI.setPageSegMode(TessBaseAPI.PageSegMode.PSM_AUTO);


        lvBaseAPI.setImage(lvBitmap);

        String result = lvBaseAPI.getUTF8Text();
        Log.v(TAG,"getContent::::::: "+result);

        return result;
    }
}
