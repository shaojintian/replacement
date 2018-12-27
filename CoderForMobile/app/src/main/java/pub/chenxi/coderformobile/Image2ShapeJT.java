package pub.chenxi.coderformobile;

import android.graphics.Bitmap;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import pub.chenxi.cv.IImageTransfer;
import pub.chenxi.cv.entity.GRectangle;
import pub.chenxi.cv.entity.ShapeFile;
import pub.chenxi.cv.entity.ShapeType;

public class Image2ShapeJT implements IImageTransfer {

    private Mat gray =new Mat();
    private Mat binary =new  Mat();
   // private ImageGui Imgviewer =null;
    private Mat result =new Mat();
    private ArrayList<MatOfPoint>  contours=new ArrayList<>();
    private ArrayList<MatOfPoint2f> newcontours=new ArrayList<>();
    private Mat hierarchy =new Mat();
    private double epsilon;
    private MatOfPoint2f approx= new MatOfPoint2f();
    private Scalar color_green= new Scalar(0,255,0);
    private int corners;
    private String shape_type ;
    private int count;
    private HashMap<String, Integer> shapes = new HashMap<String,Integer>();
    private Moments mm;
    private int cx,cy;


    Bitmap bitmap;
    public Image2ShapeJT(Bitmap _bitmap) {

        bitmap  = _bitmap;
    }

    List<GRectangle> wordShapeList;

    @Override
    public List<ShapeFile> img2Shapes() {
        wordShapeList = new ArrayList<>();
        List<ShapeFile> shapeFiles = new ArrayList<>();

        Mat src = new Mat(bitmap.getHeight(), bitmap.getWidth(), CvType.CV_8UC4);
        Utils.bitmapToMat(bitmap, src);

        Imgproc.cvtColor(src, gray,Imgproc.COLOR_BGR2GRAY);//灰度
        Imgproc.threshold(gray, binary, 100,200, Imgproc.THRESH_BINARY_INV );


        //轮廓
        Imgproc.findContours(binary, contours,hierarchy,Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
        //转换matOfpoint->matofpoint2f
        for(MatOfPoint point :contours) {

            MatOfPoint2f newpoint =new MatOfPoint2f(point.toArray());
            newcontours.add(newpoint);

        }

        int k = 0;
        for (int i=0;i<contours.size();i++) {
            //提取与绘制轮廓
            Imgproc.drawContours(result, contours, i, color_green,2);//绿色轮廓

            //轮廓逼近
            epsilon = 0.01 * Imgproc.arcLength(newcontours.get(i), true);
            Imgproc.approxPolyDP(newcontours.get(i), approx,epsilon, true);

            //分析几何形状״
            corners = approx.rows();


            //初始化shapes字典

            shapes.put("rectangle", 0);
            shapes.put("ploygons", 0);
            shapes.put("circle", 0);

            //周长
            double p = Imgproc.arcLength(newcontours.get(i), true);
            double w_and_h=p/2;
            double area = Imgproc.contourArea(contours.get(i));
            double width=(w_and_h+Math.sqrt(Math.pow(w_and_h, 2)-4*area))/2;
            double height=(w_and_h-Math.sqrt(Math.pow(w_and_h, 2)-4*area))/2;


            //求中心位置
            mm = Imgproc.moments(contours.get(i));
            cx = (int)((mm.m10 / mm.m00)-width/2);
            cy = (int)((mm.m01 / mm.m00)-height/2);


            if (corners == 4) {
                shapeFiles.add(new ShapeFile(""+k,ShapeType.Rect,new GRectangle((int)cx,(int)cy,(int)width,(int)height)));
                k++;
                System.out.println("矩形周长："+String.format("%.2f", p)+
                        "面积："+ String.format("%.2f", area)+
                        "长度："+String.format("%.2f", height)+
                        "宽度："+String.format("%.2f", width));

            }
            if (4 < corners &&corners<10) {
                shapeFiles.add(new ShapeFile(""+k,ShapeType.RoundRect,new GRectangle((int)cx,(int)cy,(int)width,(int)height)));
                k++;

                System.out.println("圆角矩形周长："+String.format("%.2f", p)+
                        "面积："+ String.format("%.2f", area)+
                        "长度："+String.format("%.2f", height)+
                        "宽度："+String.format("%.2f", width));
            }
            if(corners>=10) {
                shapeFiles.add(new ShapeFile(""+k,ShapeType.Circle,new GRectangle((int)cx,(int)cy,(int)width,(int)height)));
                k++;

                System.out.println("圆周长："+String.format("%.2f", p)+
                        "面积："+ String.format("%.2f", area) +
                        "半径"+String.format("%.2f", p/(2*3.1415)));

            }



            Imgproc.circle(result, new Point(cx,cy), 2, new Scalar(0, 0, 255), -1);





        }


        //sjt：预处理
        dilation=preprocess(gray);
        //sjt:查找和筛选文字区域
        region=findTextRegion(dilation);

        int flag = contours.size();

        //sjt:画轮廓
        for (RotatedRect rect : region)
        {
            Rect r = rect.boundingRect();
            int area = (r.width*r.height);
            Log.i(TAG, "img2Shapes: "+area);
            if(area<30000) continue;

            Point[] P =new Point[4];
            GRectangle rectangle = new GRectangle(r.x,r.y,r.width,r.height);
            shapeFiles.add(new ShapeFile(""+flag,ShapeType.Rect,rectangle));
            wordShapeList.add(rectangle);
            rect.points(P);
            for (int j = 0; j <= 3; j++)
            {
                Imgproc.line(result, P[j], P[(j + 1) % 4], new Scalar(0,255,0), 2);
            }
            flag++;
        }

        return shapeFiles;
    }

    private static final String TAG = "Image2ShapeJT";
    public List<GRectangle> getWordShape(){
        return  wordShapeList;
    }

    private Mat dilation =new Mat();
    private Mat erosion =new Mat();
    private Mat dilation2=new Mat();
    private ArrayList<RotatedRect> region =new ArrayList<>();

    //sjt
    public Mat preprocess(Mat frame) {
        //Sobel 算子
        Mat sobel =new Mat();
        Mat element1= new Mat();
        Mat element2=new Mat();
        Mat edges =new Mat();
        Imgproc.Sobel(gray,sobel,3, 1, 0);


        //图像二值化
        Imgproc.threshold(sobel, binary, 0,255, Imgproc.THRESH_BINARY);
        //腐蚀核函数
        element1=Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(30,9));
        //膨胀核函数
        element2=Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(24,4));


        //膨胀
        Imgproc.dilate(binary, dilation, element2,new Point(-1,-1),1);
        //腐蚀
        Imgproc.erode(dilation, erosion, element1, new Point(-1,-1), 1);
        //再次膨胀
        Imgproc.dilate(erosion, dilation2, element2, new Point(-1,-1),3);

       /* Imgcodecs.imwrite("dilatan.jpg", dilation);
        Imgcodecs.imwrite("erosion.jpg", erosion);
        Imgcodecs.imwrite("dilation2.jpg", dilation2);
*/


        return dilation2;
    }
    //sjt
    public ArrayList<RotatedRect> findTextRegion(Mat frame){
        frame.convertTo(frame, CvType.CV_32SC1);
        ArrayList<MatOfPoint>  contours1=new ArrayList<>();
        ArrayList<MatOfPoint2f>  newcontours1=new ArrayList<>();
        Imgproc.findContours(frame, contours1, hierarchy, Imgproc.RETR_FLOODFILL, Imgproc.CHAIN_APPROX_SIMPLE);
        //筛选轮廓
        //转换matOfpoint->matofpoint2f
        for(MatOfPoint point :contours1) {

            MatOfPoint2f newpoint =new MatOfPoint2f(point.toArray());
            newcontours1.add(newpoint);

        }

        for (int i=0;i<contours1.size();i++) {
            double area=Imgproc.contourArea(contours1.get(i));
            RotatedRect rect =new RotatedRect();
            Mat box=new Mat();
            //double height,width;
            //筛选
            if(area<100) continue;
            //轮廓逼近
            epsilon = 0.01 * Imgproc.arcLength(newcontours1.get(i), true);
            Imgproc.approxPolyDP(newcontours1.get(i), approx,epsilon, true);
            //找到最小的矩形
            rect=Imgproc.minAreaRect(newcontours1.get(i));
            //计算高和宽
            int m_width = rect.boundingRect().width;
            int m_height = rect.boundingRect().height;



            //符合条件的rect添加到rects集合中
            region.add(rect);






        }


        return region;
    }


    @Override
    public List<ShapeFile> optimizeShapes(List<ShapeFile> list) {
        List<ShapeFile> newList = new ArrayList<>();

        int area =100000;

        //过滤面积比较小的
        for (ShapeFile curr : list) {

            if(curr.getPositon().getArea()>area){

                newList.add(curr);
            }
        }


        //去掉被包含关系的去掉，如果面积相差一半就保留
//        HashMap<Integer,Integer> maps = new HashMap<>();
//        for (int i = 0; i < newList.size()-1; i++) {
//            ShapeFile curr= newList.get(i);
//
//            if(maps.containsKey(i)) continue;
//            for (int j = i+1; j < newList.size(); j++) {
//                if(maps.containsKey(i)) continue;
//
//                ShapeFile next= newList.get(i);
//                int compare = curr.getPositon().getContainer(next.getPositon());
//                if(compare==0 || compare==1){//如果两个相等,或者包含了第二个，第二个被标记
//                    maps.put(j,j);
//                }else if(compare==2){
//                    maps.put(i,i);
//                    break;
//                }
//
//            }
//        }
//
//        List<ShapeFile> newList2 = new ArrayList<>();
//        for (int i = 0; i < newList.size(); i++) {
//            if(!maps.containsKey(i)){
//                newList2.add(newList.get(i));
//            }
//        }
//        return newList2;
        return newList;
    }
}
