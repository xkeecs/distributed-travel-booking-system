package cs.mcgill.ca.comp764.Motion;

import java.io.IOException;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;
import org.opencv.core.Mat;

/**
 * org.apache.hadoop.mapreduce.Reducer<KEYIN,VALUEIN,KEYOUT,VALUEOUT>
 * 
 * @author kai
 * 
 */
public class MotionReducer extends Reducer<Text, Mat, Text, Text>
{

	public void reduce(Text text, Text values, Context context) throws IOException, InterruptedException
	{
		/*
		 * int sum = 0; for (IntWritable value : values) { sum += value.get(); }
		 */
		context.write(text, values);
	}
}
