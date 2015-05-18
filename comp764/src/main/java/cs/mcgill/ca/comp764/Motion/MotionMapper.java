package cs.mcgill.ca.comp764.Motion;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;
import org.opencv.video.Video;

// org.apache.hadoop.mapreduce.Mapper<KEYIN,VALUEIN,KEYOUT,VALUEOUT>
public class MotionMapper extends Mapper<Text, Mat, Text, Text>
{
	public static final int PATH_LEN = 1024;
	public static final int FN_LEN = 24;
	public static final int OTHERANGLE = 4;
	public static final int HISTSIZE = 16;
	public static final double CV_PI = 3.1415926535897932384626433832795;
	String base = "hdfs://localhost:9000/user/kai/input/I-Frames";
	String dir = "hdfs://localhost:9000/user/kai/input/";

	public void map(Text key, Mat value, Context context) throws IOException, InterruptedException
	{
		// String key = value.toString();

		FileSystem fs = FileSystem.get(context.getConfiguration());
		FileStatus[] status = fs.listStatus(new Path(dir));

		Mat mot_hist = new Mat();
		Mat avg_motion_hist = new Mat();
		Mat prev = new Mat();
		Mat next = new Mat();
		Mat flow = new Mat();

		for (int i = 1; i <= status.length; i++)
		{

			String filename = base + i + ".jpeg";
			Path path = new Path(filename);
			FSDataInputStream in = null;
			ByteArrayOutputStream out = new ByteArrayOutputStream();

			// System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
			try
			{
				in = fs.open(path);

				byte[] buffer = new byte[4096];
				int bytesRead;
				while ((bytesRead = in.read(buffer)) != -1)
				{

					out.write(buffer, 0, bytesRead);
				}
				in.close();
				byte[] data = out.toByteArray();

				Mat encoded = new Mat(1, out.size(), CvType.CV_8UC1);

				encoded.put(0, 0, data);
				out.close();
				Mat decoded = Highgui.imdecode(encoded, 0);
				if (i > 1)
				{
					next.copyTo(prev);
					int flags;
					if (i == 2)
					{
						flags = Video.OPTFLOW_FARNEBACK_GAUSSIAN;
					}
					else
					{
						flags = Video.OPTFLOW_USE_INITIAL_FLOW;
					}

					mot_hist = img_motHist(prev, next, flow, flags, mot_hist);
				}
				decoded.copyTo(next);
				if (i == 2)
				{
					mot_hist.copyTo(avg_motion_hist);
				}
				else if (i > 2)
				{
					Core.add(avg_motion_hist, mot_hist, avg_motion_hist);
				}
				System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

				// System.out.println("dump-----------" + decoded.dump());

				// System.out.println("The path is " + file.toString());
				// System.out.println("TEST!!!" + Highgui.CV_CAP_ANDROID);
				// Mat mat = Highgui.imread(fileSplit.getPath().toString());
				// Mat mat = Highgui.imdecode(m, 0);
				// System.out.println("The matrix is " + mat);
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}

		}
		for (int xx = 0; xx < avg_motion_hist.rows(); xx++)
			for (int yy = 0; yy < avg_motion_hist.cols(); yy++)
			{
				avg_motion_hist.put(xx, yy, avg_motion_hist.get(xx, yy)[0] * 1 / (double) (status.length - 1));
			}

		context.write(key, new Text(avg_motion_hist.dump()));
	}

	Mat img_motHist(Mat prev, Mat next, Mat flow, int flags, Mat mot_hist)
	{
		double pyr_scale = 0.5;
		int levels = 1;
		int winsize = 5;
		int iterations = 3;
		int poly_n = 5;
		double poly_sigma = 1.1;
		float mag_thresh = (float) 0.01;

		// Mat flow;
		Video.calcOpticalFlowFarneback(prev, next, flow, pyr_scale, levels, winsize, iterations, poly_n, poly_sigma,
				flags);
		Mat vecs_angle = new Mat(flow.size(), CvType.CV_32F);

		float mag;
		for (int i = 0; i < flow.rows(); i++)
		{
			for (int j = 0; j < flow.cols(); j++)
			{
				mag = (float) Math.sqrt(Math.pow(flow.get(i, j)[0], 2) + Math.pow(flow.get(i, j)[1], 2));
				if (mag > mag_thresh)
				{
					vecs_angle.put(i, j, (float) Math.atan2(flow.get(i, j)[1], flow.get(i, j)[0]));
				}
				else
				{
					vecs_angle.put(i, j, (float) OTHERANGLE);
				}
			}
		}

		float range[] = { (float) -CV_PI, (float) CV_PI };
		// MatOfInt histRange = new MatOfInt(range);
		boolean accumulate = false;
		int hist_size = HISTSIZE;

		MatOfInt ch = new MatOfInt(0);
		Mat mMaskMat = new Mat();

		MatOfFloat range_new = new MatOfFloat(range);
		List<Mat> list = new ArrayList<Mat>();
		list.add(vecs_angle);
		System.out.println("HELLO-----------------");
		Imgproc.calcHist(list, ch, mMaskMat, mot_hist, new MatOfInt(hist_size), range_new, accumulate);

		// Imgproc.calcHist(list, ch, mMaskMat, lbp_hist, hist_size, range_new, accumulate);

		Core.normalize(mot_hist, mot_hist, 1, 0, Core.NORM_L2, -1, new Mat());

		return mot_hist;
	}
}
