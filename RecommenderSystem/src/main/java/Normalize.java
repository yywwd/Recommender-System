import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class Normalize {

    public static class NormalizeMapper extends Mapper<LongWritable, Text, Text, Text> {

        // map method
        @Override
        public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {

            //movieA:movieB \t relation
            //collect the relationship list for movieA
            // outputKey = movieB
            // moutputValue = movieA=relation
            String[] movie_relation = value.toString().trim().split("\t");
            String movieA = movie_relation[0].split(":")[0];
            String movieB = movie_relation[0].split(":")[1];
            context.write(new Text(movieA), new Text(movieB + "=" + movie_relation));
        }
    }

    public static class NormalizeReducer extends Reducer<Text, Text, Text, Text> {
        // reduce method
        @Override
        public void reduce(Text key, Iterable<Text> values, Context context)
                throws IOException, InterruptedException {

            //key = movieA, value=<movieB:relation, movieC:relation...>
            //normalize each unit of co-occurrence matrix
            //outputKey = movieB
            //outputValue = movieA=relativeRelation
            int denominator = 0;
            Map<String, Integer> movie_relation_map = new HashMap<String, Integer>();
            for(Text value : values){
                String[] movieB_relation = value.toString().trim().split("=");
                int relation = Integer.parseInt(movieB_relation[1]);
                movie_relation_map.put(movieB_relation[0], relation);
                denominator += relation;
            }

            Iterator it = movie_relation_map.entrySet().iterator();
            while(it.hasNext()){
                Map.Entry<String, Integer> entry = (Map.Entry<String, Integer>) it.next();
                double relative_relation = (double) entry.getValue()/denominator;
                context.write(new Text(entry.getKey()), new Text(key + "=" + relative_relation));
            }
        }
    }

    public static void main(String[] args) throws Exception {

        Configuration conf = new Configuration();

        Job job = Job.getInstance(conf);
        job.setMapperClass(NormalizeMapper.class);
        job.setReducerClass(NormalizeReducer.class);

        job.setJarByClass(Normalize.class);

        job.setInputFormatClass(TextInputFormat.class);
        job.setOutputFormatClass(TextOutputFormat.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);

        TextInputFormat.setInputPaths(job, new Path(args[0]));
        TextOutputFormat.setOutputPath(job, new Path(args[1]));

        job.waitForCompletion(true);
    }
}
