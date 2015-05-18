package cs.mcgill.ca.comp764.TextureOpenCV;

import java.io.IOException;
import java.util.Date;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.opencv.core.Core;

import cs.mcgill.ca.comp764.utils.WholeFileInputFormat;

public class TextureMain
{
	public static void main(String[] args) throws IOException, InterruptedException, ClassNotFoundException
	{
		Path inputPath = new Path(args[0]);
		Path outputDir = new Path(args[1]);

		// Create configuration
		Configuration conf = new Configuration(true);

		// Create job
		Job job = new Job(conf, "TextureMain");
		job.setJarByClass(TextureMapper.class);
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

		// Setup MapReduce
		job.setMapperClass(TextureMapper.class);
		job.setReducerClass(TextureReducer.class);
		job.setNumReduceTasks(1);

		// Specify key / value
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(Text.class);

		// Input
		// FileInputFormat.setInputPathFilter(job, RegExFilter.class);
		FileInputFormat.addInputPath(job, inputPath);
		job.setInputFormatClass(WholeFileInputFormat.class);

		// Output
		FileOutputFormat.setOutputPath(job, outputDir);
		job.setOutputFormatClass(TextOutputFormat.class);

		// Delete output if exists
		FileSystem hdfs = FileSystem.get(conf);
		if (hdfs.exists(outputDir))
			hdfs.delete(outputDir, true);

		// Execute job
		long start = new Date().getTime();
		int code = job.waitForCompletion(true) ? 0 : 1;
		long end = new Date().getTime();
		System.out.println("Job took " + (end - start) + " milliseconds");
		System.exit(code);
	}
}
