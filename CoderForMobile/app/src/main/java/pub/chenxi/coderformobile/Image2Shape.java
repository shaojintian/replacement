package pub.chenxi.coderformobile;



import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.constraint.solver.widgets.Rectangle;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import pub.chenxi.cv.IImageTransfer;
import pub.chenxi.cv.entity.GRectangle;
import pub.chenxi.cv.entity.ShapeFile;
import pub.chenxi.cv.entity.ShapeType;

public class Image2Shape implements IImageTransfer {

    private Mat srcGray;


    private int threshold = 100;

    private volatile ArrayList<ShapeFile> _shapes;
    private volatile ArrayList<ShapeFile> _circleshp ;

    Bitmap bitmap;
    public Image2Shape(Bitmap _bitmap) {
        srcGray = new Mat();// = new Mat()
        bitmap  = _bitmap;
    }

    @Override
    public List<ShapeFile> img2Shapes() {
        //Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.gall, options);
        Mat src = new Mat(bitmap.getHeight(), bitmap.getWidth(), CvType.CV_8UC4);
        Utils.bitmapToMat(bitmap, src);


        Imgproc.cvtColor(src, srcGray, Imgproc.COLOR_BGR2GRAY);
        // Set up the content pane.

        Mat cannyOutput = new Mat();
        Imgproc.Canny(srcGray, cannyOutput, threshold, threshold * 2);
        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(cannyOutput, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);
//		readMatOfPoint(contours);//读取顶点集中的点数据
//        List <MatOfPoint2f> newContours = (List<MatOfPoint2f>) new MatOfPoint2f();
//		write2TXT(cannyOutput,"canny");
        List<ShapeFile> _shapes = List2Shape(contours);
        List<ShapeFile> _circleshp = FindCircle(src);
        //把_circleshp与_shapes合并，或者不要circleshp，直接在FindCirCle函数中把圆形加到_shapes中
        for(int i=0;i<_circleshp.size();i++) {
            _shapes.add(_circleshp.get(i));
        }
        for (int i =0;i<_shapes.size();i++) {
            ShapeFile shape = _shapes.get(i);
            shape.setId(""+i);
        }
        // 测试用：重绘外包矩形
        if (_shapes != null) {
            for (int i = 0; i < _shapes.size(); i++) {
                ShapeFile sha = _shapes.get(i);// 取出shape图形
                System.out.println(sha.toString());

//				switch (sha.getShapeType()) {
//				case ShapeType.Rect:
//					Rectangle tmprec = sha.getPositon();
//					System.out.println(sha.toString());
//					break;
//				case ShapeType.Triangle:
//					break;
//				case ShapeType.RoundRect:
//					break;
//				case ShapeType.Circle:
//					Rectangle cir = sha.getPositon();
//					System.out.println(sha.toString());
//
//					break;
//				}
            }
        }
//		update(g);
        return _shapes;
    }//~ img2Shapes

    @Override
    public List<ShapeFile> optimizeShapes(List<ShapeFile> list) {
        List<ShapeFile> newList = new ArrayList<>();


       //int area =100000;

        //过滤面积比较小的
        for (ShapeFile curr : list) {

          //  if(curr.getPositon().getArea()>area){

                newList.add(curr);
          //  }
        }


        //去掉被包含关系的去掉，如果面积相差一半就保留
        HashMap<Integer,Integer> maps = new HashMap<>();
        for (int i = 0; i < newList.size()-1; i++) {
            ShapeFile curr= newList.get(i);

            if(maps.containsKey(i)) continue;
            for (int j = i+1; j < newList.size(); j++) {
                if(maps.containsKey(i)) continue;

                ShapeFile next= newList.get(i);
                int compare = curr.getPositon().getContainer(next.getPositon());
                if(compare==0 || compare==1){//如果两个相等,或者包含了第二个，第二个被标记
                    maps.put(j,j);
                }else if(compare==2){
                    maps.put(i,i);
                    break;
                }

            }
        }

        List<ShapeFile> newList2 = new ArrayList<>();
        for (int i = 0; i < newList.size(); i++) {
            if(!maps.containsKey(i)){
                newList2.add(newList.get(i));
            }
        }
        return newList2;
    }



    /**
     * 寻找圆形，返回圆心坐标和半径
     * @param cannyOutput
     * @return
     */
    private List<ShapeFile> FindCircle(Mat cannyOutput) {

        Mat gray = new Mat();

        Imgproc.cvtColor(cannyOutput, gray, Imgproc.COLOR_BGR2GRAY);

        Imgproc.medianBlur(gray, gray, 5);
        Mat circles = new Mat();
        // 直接输入灰度化后的Mat矩阵，输出也是灰度化后的Mat矩阵
        Imgproc.HoughCircles(gray, circles, Imgproc.HOUGH_GRADIENT, 1.0, (double) gray.rows() / 16, 100.0, 30.0, 5,
                200);// change this value
        // to detect circles
        // with different
        // distances to each
        // other
        // change the last two parameters
        // (min_radius & max_radius) to detect larger circles
        for (int x = 0; x < circles.cols(); x++) {
            double[] c = circles.get(0, x);
            Point center = new Point(Math.round(c[0]), Math.round(c[1]));
            // circle center
            Imgproc.circle(cannyOutput, center, 1, new Scalar(0, 100, 100), 3, 8, 0);
            // circle outline
            int radius = (int) Math.round(c[2]);
            Imgproc.circle(cannyOutput, center, radius, new Scalar(255, 0, 255), 3, 8, 0);
//			System.out.println("圆形位置"+center+","+radius);
            _circleshp.add(new ShapeFile("0", ShapeType.Circle, new GRectangle((int)center.x-radius, (int)center.y-radius, 2*radius, 2*radius)));
        }
//		HighGui.imshow("可视化圆形检测结果", cannyOutput);
//		HighGui.waitKey();
//		System.exit(0);
        return _circleshp;
    }

    /**
     * 由轮廓点集转几何形状列表
     * @param contours
     * @return
     */
    private List<ShapeFile> List2Shape(List<MatOfPoint> contours) {
        initShapeList();
        MatOfPoint2f newContours = new MatOfPoint2f();
        int cou = 0;// 测试用，用于计算几何图形个数
        // 对于Canny算子检测出的每一个边缘进行形状判断
        for (MatOfPoint point : contours) {
//        MatOfPoint2f newPoint = new MatOfPoint2f(point.toArray());
            newContours.fromArray(point.toArray());// 从MatOfPoint得到MatOfPoint2f对象
            MatOfPoint2f approxCurve = new MatOfPoint2f();
            double eps = 0.01 * Imgproc.arcLength(newContours, true);

            Imgproc.approxPolyDP(newContours, approxCurve, eps, true);
            int corn = approxCurve.toArray().length;
            if (corn == 4) {
                System.out.println("矩形");
//				System.out.println("approxCurve_size:" + cou + ":" + approxCurve.size());
                Rect rect = Imgproc.boundingRect(point);
//				readMatOfPoint2f(approxCurve);
//				System.out.println(new Point(rect.x, rect.y) + ":" + rect.width + ":" + rect.height);
                _shapes.add(
                        new ShapeFile(0 + "", ShapeType.Rect, new GRectangle(rect.x, rect.y, rect.width, rect.height)));
//				System.out.println(new Point(rect.x, rect.y) + ":" + rect.width + ":" + rect.height);

                cou++;

            } else if (corn == 3) {
                System.out.println("三角形");
                System.out.println("approxCurve_size:" + cou + ":" + approxCurve.size());
                Rect rect = Imgproc.boundingRect(point);

                _shapes.add(
                        new ShapeFile(0 + "", ShapeType.Triangle, new GRectangle(rect.x, rect.y, rect.width, rect.height)));
                // readMatOfPoint2f(approxCurve);
                cou++;

            }else if (corn == 8) {
                System.out.println("其他多边形");
//				FurtherJudge(newContours.toArray());// 进一步判断 Point[]
//				FurtherJudge(newContours);// 进一步判断MatOfPoint2f newContours
                Rect rect = Imgproc.boundingRect(point);
                System.out.println(corn + "," + rect.area());
//				System.out.println(rect.toString());
                if (rect.area() >=5000) {
                    _shapes.add(new ShapeFile(0 + "", ShapeType.RoundRect,
                            new GRectangle(rect.x, rect.y, rect.width, rect.height)));
                    cou++;
                }

            } else if (corn > 4) {
                System.out.println("其他多边形");
//				FurtherJudge(newContours.toArray());//进一步判断
//				readMatOfPoint2f(approxCurve);
                cou++;

            }

        }
//		readArrayList(_shapes);

        // _shapes值为null时，嘤嘤嘤无法输出
        return _shapes;
    }

    /*
     * 進一步判斷是否是圓角矩形，未实现
     */
    private void FurtherJudge(Point[] array) {

        for (int i = 0; i < array.length; i++) {
            System.out.println(i + "点:" + array[i]);
        }
//		g.drawRoundRect(100, 100, 200, 200, 200, 200);//java自带画圆角矩形的函数，弧宽=宽=长时画出的为圆形
    }

    /**
     * 测试用
     *
     * @param contours：MatOfPoint数组
     */
    public void readMatOfPoint(List<MatOfPoint> contours) {
        System.out.println("contour输出");
        int count = 0;
        for (int i = 0; i < contours.size(); i++) {
            List lst = contours.get(i).toList();
            if (4 < lst.size() && lst.size() < 10) {
                System.out.println("第" + count + "个List：" + lst.size() + ":");
                for (int j = 0; j < lst.size(); j++) {
                    System.out.println(lst.get(j).toString());
                }
                count++;

            }

        }
    }

    /**
     * 测试用
     *
     * @param contours
     */
    public void readMatOfPoint2f(MatOfPoint2f contours) {
        System.out.println("contour输出");
        List lst = contours.toList();

//			if(4<lst.size()&&lst.size()<10) {
//		System.out.println("第" + count + "个List：" + lst.size() + ":");
        for (int j = 0; j < lst.size(); j++) {
            System.out.println(lst.get(j).toString());
        }

//			}

    }

    private void initShapeList() {
        _shapes = new ArrayList<ShapeFile>();
        _circleshp = new ArrayList<>();
    }

    /**
     * 測試用
     *
     * @param _shapes2
     */
    private void readArrayList(ArrayList<ShapeFile> _shapes2) {
        System.out.println("ShapeFile数组大小:" + _shapes2.size());
        for (int i = 0; i < _shapes2.size(); i++) {
            ShapeFile shape = _shapes2.get(i);
//			System.out.println(shapes.get(i))
            System.out.println(shape.toString());
        }
    }
}//~
