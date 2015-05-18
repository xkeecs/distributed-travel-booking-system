package cs.mcgill.ca.comp764;

import java.io.IOException;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;
import org.opencv.core.Mat;

public class WordCountReducer extends Reducer<Object, Mat, Object, Mat>
{
	public void reduce(Text text, Mat values, Context context) throws IOException, InterruptedException
	{
		int sum = 0;
		/*
		 * for (IntWritable value : values) { sum += value.get(); }
		 */

		context.write(text, values);
	}
}