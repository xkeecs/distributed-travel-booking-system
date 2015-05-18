package cs.mcgill.ca.comp764.TextureIteration;

import java.io.IOException;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;
import org.opencv.core.Mat;

public class TextureIterReducer extends Reducer<Text, Mat, Text, Text>
{
	/**
	 * Compute average of historgrams
	 * 
	 * @param text
	 * @param values
	 * @param context
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public void reduce(Text text, Text values, Context context) throws IOException, InterruptedException
	{
		context.write(text, values);
	}
}
