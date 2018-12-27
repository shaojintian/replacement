package pub.chenxi.coderformobile;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;

import org.apache.commons.lang3.StringUtils;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import pub.chenxi.csvfile.UITransferImpl;
import pub.chenxi.cv.entity.CSVFile;
import pub.chenxi.cv.entity.CSVFileItem;
import pub.chenxi.cv.entity.CodeTypeEnum;
import pub.chenxi.cv.entity.CtlTypeEnum;
import pub.chenxi.cv.entity.GRectangle;
import pub.chenxi.cv.entity.ImageFile;
import pub.chenxi.cv.entity.ShapeFile;
import pub.chenxi.cv.entity.WordFile;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    static {
        try {
            if (!OpenCVLoader.initDebug()) {
                Log.d(TAG, "打开library失败！");


            }
        } catch (Exception ex2) {
            Log.e(TAG, "静态初始化异常:", ex2);
        }

    }


    /**
     * TessBaseAPI初始化用到的第一个参数，是个目录。
     */
    private static final String DATAPATH = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator;
    /**
     * 在DATAPATH中新建这个目录，TessBaseAPI初始化要求必须有这个目录。
     */
    private static final String tessdata = DATAPATH + File.separator + "tessdata";
    /**
     * TessBaseAPI初始化测第二个参数，就是识别库的名字不要后缀名。
     */
    private static final String DEFAULT_LANGUAGE = "chi_sim";
    /**
     * assets中的文件名
     */
    private static final String DEFAULT_LANGUAGE_NAME = DEFAULT_LANGUAGE + ".traineddata";
    /**
     * 保存到SD卡中的完整文件名
     */
    private static final String LANGUAGE_PATH = tessdata + File.separator + DEFAULT_LANGUAGE_NAME;


    ImageView imageView;
    Bitmap bitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        FileHelper.copyToSD(this,LANGUAGE_PATH, DEFAULT_LANGUAGE_NAME);

        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                    checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
            }
        }

        BitmapFactory.Options options = new BitmapFactory.Options();
        bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.test_bl, options);
        imageView = (ImageView) this.findViewById(R.id.imageView);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        new AsyncTask<Void, Void, Bitmap>() {
            private long startTime, endTime;
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                startTime = System.currentTimeMillis();
            }

            @Override
            protected Bitmap doInBackground(Void... params) {
               // Image2Shape image2Shape = new Image2Shape(bitmap);
                String pName = "用户";

                Image2ShapeJT image2Shape = new Image2ShapeJT(bitmap);

                List<ShapeFile> shapes =  image2Shape.img2Shapes();
                StringBuilder builder = new StringBuilder();
                for (ShapeFile shapeFile : shapes) {
                    GRectangle rectange = shapeFile.getPositon();
                    builder.append(""+shapeFile.getId()+" "+shapeFile.getShapeName()+" "+shapeFile.getShapeType()+" "+rectange.toString()+" S:"+rectange.getArea()+" \r\n");
                }
                Log.i(TAG, "doInBackground: "+builder.toString());
                //shapes = image2Shape.optimizeShapes(shapes);

                //List<GRectangle> wordShape= image2Shape.getWordShape();

                shapes = image2Shape.optimizeShapes(shapes);

                List<GRectangle> wordShape= new ArrayList<>();

                for(int i=0;i<shapes.size();i++){
                    GRectangle rect = shapes.get(i).getPositon();

                    wordShape.add(new GRectangle(rect.getX(),rect.getY(),rect.getWidth(),rect.getHeight()));

                }

                List<String> ctlRecord = new ArrayList<>();

                ctlRecord.add("用户登录 10 0");
                ctlRecord.add("用户信息 10 0");
                ctlRecord.add("用户 10 0");
                ctlRecord.add("用户名 10 10");
                ctlRecord.add("密码 10 12");
                ctlRecord.add("登录 10 30");

/*

                Name,Alias,DataType,CtlType
                用户 User
                用户登录 UserName String label
                账号 UserName String text
                密码 Password String password
                登陆 Submit String button
                [End]


                 */


                // 识别文字
                WordImp wordImp = new WordImp(MainActivity.this,bitmap);
                List<WordFile> words = wordImp.shapeRange2Word2(wordShape);

                // 识别UI
                  UITransferImpl trans = new UITransferImpl();

               //输出为特征文件
                ImageFile imgFile = new ImageFile(bitmap.getWidth(),bitmap.getHeight(),pName);
                CSVFile csvFile= trans.shape2csv(imgFile,shapes,words,ctlRecord);


                List<String> csvString = new ArrayList<>();
                csvString.add("Name,Alias,DataType,CtlType");

                csvString.add(csvFile.getName()+" "+toPinyin(csvFile.getAlias()));
                List<CSVFileItem> contents = csvFile.getContents();
                for (CSVFileItem content : contents) {
                    csvString.add(csvFile.getName()+" "+toPinyin(csvFile.getAlias())+" "+toDataType(content.getDataType())+" "+toCtlType(content.getCtlType()));
                }
                csvString.add("[End]");


                String returnToServer = StringUtils.join(csvString,"\n");

                return matToBitMap(processShapeFile(bitmap,shapes));
                //cx给原来特征匹配用的 return findSamePart();
            }

            @Override
            protected void onPostExecute(Bitmap bitmap) {
                super.onPostExecute(bitmap);

                imageView.setImageBitmap(bitmap);

            }
        }.execute();
    }

    private String toPinyin(String name){
        return name;
    }


    private String toDataType(int type){
        if(CodeTypeEnum.STR==type){
            return "String";
        }else  if(CodeTypeEnum.INT==type){
            return "int";
        }else{
            return "String";
        }

    }

    private String toCtlType(int type){

        if(CtlTypeEnum.Label==type){
            return "label";
        }else  if(CtlTypeEnum.Input==type){
            return "text";
        }else  if(CtlTypeEnum.InputPwd==type){
            return "password";
        }else  if(CtlTypeEnum.Button==type){
            return "button";
        }else  if(CtlTypeEnum.Radio==type){
            return "radio";
        }else  if(CtlTypeEnum.Check==type){
            return "checkbox";
        }else{
            return "label";
        }
    }



    private Bitmap matToBitMap(Mat result) {
        //Create Result
        Bitmap bmp_result = Bitmap.createBitmap(result.cols(), result.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(result, bmp_result);
        return bmp_result;
    }

    private Random r = new Random();
    private Mat processShapeFile(Bitmap bitmap,List<ShapeFile> list){
        Mat mat_bmp = new Mat(bitmap.getHeight(), bitmap.getWidth(), CvType.CV_8UC4);
        Utils.bitmapToMat(bitmap, mat_bmp);


        for (ShapeFile shapeFile : list) {
            GRectangle rect = shapeFile.getPositon();
            Log.i(TAG, "processShapeFile.Area: "+rect.getArea());

            int padding = 10;
            Scalar scalar= new Scalar(r.nextInt(255), r.nextInt(255), r.nextInt(255),255);
            Imgproc.putText(mat_bmp,shapeFile.getId(),new Point( rect.getX()-padding+r.nextInt(50) , rect.getY()-padding+r.nextInt(50)), Core.FONT_HERSHEY_COMPLEX,4.0,scalar,5 );
            Imgproc.rectangle(mat_bmp, new Point( rect.getX()-padding , rect.getY()-padding), new Point(rect.getX()+rect.getWidth()+padding, rect.getY() + rect.getHeight()+padding), scalar, 4);

        }


        return mat_bmp;
    }

    private Mat process(Bitmap bitmap, boolean isDraw) {
        Mat mat_bmp = new Mat(bitmap.getHeight(), bitmap.getWidth(), CvType.CV_8UC4);
        Utils.bitmapToMat(bitmap, mat_bmp);

        //边缘
        Mat mat_edge = processEdge(bitmap);

        return mat_edge;
    }

    private Mat processEdge(Bitmap bitmap) {
        Mat mat_bmp = new Mat(bitmap.getHeight(), bitmap.getWidth(), CvType.CV_8UC4);
        Utils.bitmapToMat(bitmap, mat_bmp);
         Mat mat_gray;

        //灰化
        mat_gray = toGray(mat_bmp);
        //边缘
        Mat mat_edge = toEdge(mat_gray, 10, 100);


        return mat_edge;
    }

    public Mat toGray(Mat mat) {
        //将彩色Mat对象转成单通道的灰度Mat.
        Mat mat_gray = new Mat();
        Imgproc.cvtColor(mat, mat_gray, Imgproc.COLOR_BGRA2GRAY, 1);

        //由于最后将mat转成ARGB_8888型的Bitmap，输入必须是4通道的.
        //因而这里要将单通道转成4通道
        //Mat gray4 = new Mat(mat_gray.rows(), mat_gray.cols(), CvType.CV_8UC4);
        //Imgproc.cvtColor(mat_gray, gray4, Imgproc.COLOR_GRAY2BGRA, 4);

        return mat_gray;
    }

    public Mat toEdge(Mat grayMat, double threshold1, double threshold2) {
        Mat cannyEdges = new Mat();
        Imgproc.Canny(grayMat,cannyEdges,threshold1,threshold2);
        return cannyEdges;
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,int[] grantResults) {
        Log.i(TAG, "onRequestPermissionsResult: " + grantResults[0]);
        switch (requestCode) {
            case 1:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.i(TAG, "onRequestPermissionsResult.copy: ");
                   FileHelper.copyToSD(this,LANGUAGE_PATH, DEFAULT_LANGUAGE_NAME);
                }
                break;
            default:
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
