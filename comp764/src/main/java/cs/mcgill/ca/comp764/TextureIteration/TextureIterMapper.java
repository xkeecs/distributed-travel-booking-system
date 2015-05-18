package cs.mcgill.ca.comp764.TextureOpenCV;

import java.io.IOException;
import java.util.ArrayList;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

public class TextureMapper extends Mapper<Text, Mat, Text, Text>
{
	/*
	 * public void map(Object key, Text value, Context context) throws IOException, InterruptedException {
	 * 
	 * String[] csv = value.toString().split(","); }
	 */
	public static final int PATH_LEN = 1024;
	public static final int FN_LEN = 24;
	public static final int LBPBINCOUNT = 16;

	public void map(Text key, Mat value, Context context) throws IOException, InterruptedException
	{

		Mat lbp_hist = new Mat();
		Mat ret = img_texHist(value, lbp_hist);
		System.out.println("lbp_hist" + ret.dump());
		context.write(key, new Text(ret.dump()));
	}

	public static Mat img_texHist(Mat img, Mat lbp_hist)
	{
		// Mat img_gray = new Mat();
		Mat lbp = new Mat();

		Imgproc.GaussianBlur(img, img, new Size(3, 3), 0, 0, Imgproc.BORDER_DEFAULT);
		// Imgproc.cvtColor(img, img_gray, Imgproc.COLOR_RGB2GRAY);
		// System.out.println("IMAGE -------------- " + img.dump());
		float range[] = { 0, 256 };
		lbp = OLBP(img, lbp);
		// final float histRange[][] = { range };
		// boolean uniform = true;
		boolean accumulate = false;

		MatOfInt hist_size = new MatOfInt(LBPBINCOUNT);
		ArrayList<Mat> list = new ArrayList<Mat>();
		list.add(lbp);

		MatOfInt ch = new MatOfInt(0);
		Mat mMaskMat = new Mat();

		MatOfFloat range_new = new MatOfFloat(range);
		Imgproc.calcHist(list, ch, mMaskMat, lbp_hist, hist_size, range_new, accumulate);
		System.out.println("lbp:" + lbp_hist.dump());
		Core.normalize(lbp_hist, lbp_hist, 1, 0, Core.NORM_L2, -1, new Mat());

		return lbp_hist;
	}

	private static <T extends Comparable<T>> Mat OLBP(Mat src, Mat dst)
	{
		dst = Mat.zeros(src.rows() - 2, src.cols() - 2, CvType.CV_8UC1);
		for (int i = 1; i < src.rows() - 1; i++)
		{
			for (int j = 1; j < src.cols() - 1; j++)
			{
				double center = src.get(i, j)[0];
				byte code = 0;
				code |= (src.get(i - 1, j - 1)[0] > center ? 1 : 0) << 7;
				code |= (src.get(i - 1, j)[0] > center ? 1 : 0) << 6;
				code |= (src.get(i - 1, j + 1)[0] > center ? 1 : 0) << 5;
				code |= (src.get(i, j + 1)[0] > center ? 1 : 0) << 4;
				code |= (src.get(i + 1, j + 1)[0] > center ? 1 : 0) << 3;
				code |= (src.get(i + 1, j)[0] > center ? 1 : 0) << 2;
				code |= (src.get(i + 1, j - 1)[0] > center ? 1 : 0) << 1;
				code |= (src.get(i, j - 1)[0] > center ? 1 : 0) << 0;
				dst.put(i - 1, j - 1, code);
			}
		}
		return dst;
	}
}
