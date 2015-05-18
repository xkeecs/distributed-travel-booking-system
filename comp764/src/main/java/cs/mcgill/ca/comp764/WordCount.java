package cs.mcgill.ca.comp764;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Date;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.opencv.core.Core;
import org.opencv.core.Mat;

import cs.mcgill.ca.comp764.utils.WholeFileInputFormat;

public class WordCount
{
	@SuppressWarnings("deprecation")
	public static void main(String[] args) throws IOException, InterruptedException, ClassNotFoundException,
			URISyntaxException
	{
		Path inputPath = new Path(args[0]);
		Path outputDir = new Path(args[1]);
		Mat mat;
		// Create configuration
		Configuration conf = new Configuration(true);

		// Create job
		Job job = new Job(conf, "WordCount");
		job.setJarByClass(WordCountMapper.class);

		// Configuration conf2 = job.getConfiguration();
		// DistributedCache.createSymlink(conf2);
		// DistributedCache.addFileToClassPath(new Path("/user/kai/libraries/opencv-248.jar"), conf2);
		// conf2.set("mapred.map.child.java.opts", "-Djava.library.path=.");
		// conf2.set("mapred.map.child.java.opts", "-Djava.library.path=/user/kai/libraries/");

		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		// System.loadLibrary("libopencv_core.so");
		// Setup MapReduce
		job.setMapperClass(WordCountMapper.class);
		job.setReducerClass(WordCountReducer.class);

		// job.setNumReduceTasks(15);
		// Specify key / value
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(IntWritable.class);

		// Input
		FileInputFormat.addInputPath(job, inputPath);
		job.setInputFormatClass(WholeFileInputFormat.class);

		// Output
		FileOutputFormat.setOutputPath(job, outputDir);
		job.setOutputFormatClass(TextOutputFormat.class);

		// Delete output if exists
		FileSystem hdfs = FileSystem.get(conf);
		if (hdfs.exists(outputDir))
			hdfs.delete(outputDir, true);

		long start = new Date().getTime();
		int code = job.waitForCompletion(true) ? 0 : 1;
		long end = new Date().getTime();
		System.out.println("Job took " + (end - start) + " milliseconds");
		// Execute job
		// int code = job.waitForCompletion(true) ? 0 : 1;

		System.exit(code);
	}
}