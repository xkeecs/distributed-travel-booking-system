package cs.mcgill.ca.comp764;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.Size;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;

public class testClass
{
	public static final int PATH_LEN = 1024;
	public static final int FN_LEN = 24;
	public static final int LBPBINCOUNT = 16;

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
		System.out.println(lbp.dump());
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
		// System.out.println(dst.dump());
		return dst;
	}

	public static void main(String[] args) throws IOException
	{
		System.out.println("IN INITIALIZE!");

		String s = "/usr/local/hadoop/1_1_Y/I-Frames1.jpeg";
		String ss = "hdfs://localhost:9000/user/kai/input/I-Frames1.jpeg";
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		// Mat mat = Highgui.imread(ss);
		// System.out.println(Core.NATIVE_LIBRARY_NAME);
		File file = new File(s);
		DataInputStream in;
		ByteArrayOutputStream out = new ByteArrayOutputStream();

		try
		{
			in = new DataInputStream(new FileInputStream(s));

			// byte[] content = new byte[100];
			// in.readFully(content);
			// int len = 100;
			// IOUtils.readFully(in, content, 0, len);
			// Mat m = new Mat();
			// m.put(0, 0, content);

			// System.out.println("The path is " + file.toString());
			// System.out.println("TEST!!!" + Highgui.CV_CAP_ANDROID);
			System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
			/*
			 * byte[] buffer = new byte[4096]; int bytesRead; while ((bytesRead = in.read(buffer)) != -1) {
			 * out.write(buffer, 0, bytesRead); } in.close(); Mat encoded = new Mat(1, out.size(), CvType.CV_8UC4);
			 * encoded.put(0, 0, out.toByteArray()); out.close();
			 * 
			 * Mat decoded = Highgui.imdecode(encoded, 0); encoded.release();
			 */
			Mat decoded = Highgui.imread(s);
			// System.out.println("The matrix is " + m);
			Mat lbp_hist = new Mat();
			Mat ret = img_texHist(decoded, lbp_hist);
			System.out.println("lbp_hist" + ret.dump());
			// System.out.println("MAT??? " + decoded.dump());
		}
		catch (FileNotFoundException e)
		{
			System.out.println("Exception@@@@@@@@@@@");

			// Mat mat = Highgui.imread(fileSplit.getPath().toString());

		}
	}
}
