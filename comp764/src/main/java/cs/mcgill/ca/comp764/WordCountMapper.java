package cs.mcgill.ca.comp764;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import org.opencv.core.Mat;

public class WordCountMapper extends Mapper<Object, Mat, Object, Mat>
{

	private final IntWritable ONE = new IntWritable(1);
	private Text word = new Text();
	public static final Log LOG = LogFactory.getLog(WordCountMapper.class);

	public void map(Object key, Mat value, Context context) throws IOException, InterruptedException
	{

		// System.out.println(value.toString());
		String[] csv = value.toString().split(",");
		LOG.info("Hello?????????????????????????????????????????????");
		LOG.info("Map value " + value.dump());
		System.out.println(value);
		System.out.println(value.dump());
		/*
		 * for (String str : csv) { word.set(str); context.write(word, ONE); }
		 */
	}
}