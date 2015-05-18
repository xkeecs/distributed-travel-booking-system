package cs.mcgill.ca.comp764.utils;

import java.io.IOException;
import java.util.ArrayList;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.JobContext;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.opencv.core.Mat;

public class MotionThreeFileInputFormat extends FileInputFormat<Text, ArrayList<Mat>>
{

	@Override
	public RecordReader<Text, ArrayList<Mat>> createRecordReader(InputSplit split, TaskAttemptContext context)
			throws IOException, InterruptedException
	{
		RecordReader<Text, ArrayList<Mat>> recordReader = new MotionThreeFileRecordReader();
		recordReader.initialize(split, context);
		return recordReader;
	}

	protected boolean isSplitable(JobContext context, Path file)
	{
		return false;
	}
}
