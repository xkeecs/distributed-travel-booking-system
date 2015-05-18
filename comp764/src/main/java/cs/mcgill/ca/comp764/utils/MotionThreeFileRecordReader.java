package cs.mcgill.ca.comp764.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.JobContext;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.highgui.Highgui;

public class MotionThreeFileRecordReader extends RecordReader<Text, ArrayList<Mat>>
{

	private FileSplit fileSplit;
	private JobContext jobContext;
	private Text currentKey;
	private ArrayList<Mat> currentValue;
	private boolean finishConverting = false;
	String base = "hdfs://localhost:9000/user/kai/input/I-Frames";

	@Override
	public Text getCurrentKey() throws IOException, InterruptedException
	{
		return currentKey;
	}

	public ArrayList<Mat> getCurrentValue() throws IOException, InterruptedException
	{
		return currentValue;
	}

	@Override
	public void initialize(InputSplit split, TaskAttemptContext context) throws IOException, InterruptedException
	{
		this.fileSplit = (FileSplit) split;
		this.jobContext = context;
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		// System.out.println("INIT?????????????????????????????????????????????");

		context.getConfiguration().set("map.input.file", fileSplit.getPath().getName());
	}

	@Override
	public boolean nextKeyValue() throws IOException, InterruptedException
	{
		ArrayList<Mat> retList = new ArrayList<Mat>();
		Path file = fileSplit.getPath();
		String path = file.toString();

		int currentFrame = Integer.parseInt(path.substring(35, path.length()).replaceAll("[\\D]", ""));
		System.out.println(currentFrame);

		if (!finishConverting)
		{
			// currentValue = new BytesWritable();

			for (int i = 0; i < 3; i++)
			{
				int len = (int) fileSplit.getLength();
				byte[] content = new byte[len];

				System.out.println(file.toString());
				FileSystem fs = file.getFileSystem(jobContext.getConfiguration());
				FSDataInputStream in = null;
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				// System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
				try
				{
					in = fs.open(file);
					// IOUtils.readFully(in, content, 0, len);
					// Mat test = new Mat();
					// System.out.println("TEST!!!" + Highgui.CV_CAP_ANDROID);

					byte[] buffer = new byte[4096];
					int bytesRead;
					int counter = 0;
					while ((bytesRead = in.read(buffer)) != -1)
					{
						counter = counter + bytesRead;

						out.write(buffer, 0, bytesRead);
					}
					in.close();
					byte[] data = out.toByteArray();
					String filename = file.toString();

					Mat encoded = new Mat(1, out.size(), CvType.CV_8UC1);

					encoded.put(0, 0, data);
					out.close();

					Mat decoded = Highgui.imdecode(encoded, 0);

					encoded.release();
					System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

					// System.out.println("dump-----------" + decoded.dump());

					// System.out.println("The path is " + file.toString());
					// System.out.println("TEST!!!" + Highgui.CV_CAP_ANDROID);
					// Mat mat = Highgui.imread(fileSplit.getPath().toString());
					// Mat mat = Highgui.imdecode(m, 0);
					// System.out.println("The matrix is " + mat);
					currentKey = new Text(filename);
					currentValue = retList;
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
				finally
				{
					/*
					 * if (in != null) { IOUtils.closeStream(in); }
					 */
				}
			}

			finishConverting = true;
			return true;
		}
		return false;
	}

	@Override
	public float getProgress() throws IOException
	{
		float progress = 0;
		if (finishConverting)
		{
			progress = 1;
		}
		return progress;
	}

	@Override
	public void close() throws IOException
	{
		// TODO Auto-generated method stub

	}
}
