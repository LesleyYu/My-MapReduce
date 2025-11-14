
/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.lang.System.*;
import java.io.IOException;
import java.util.StringTokenizer;
import java.util.HashSet;
import java.util.Set;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

public class WordCount {

  public static class TokenizerMapper extends Mapper<Object, Text, Text, IntWritable> {

    private final static IntWritable one = new IntWritable(1);
    private Text bigram = new Text();

    // Target bigrams to count
    private static final Set<String> TARGET_BIGRAMS = new HashSet<String>();

    static {
      TARGET_BIGRAMS.add("computer science");
      TARGET_BIGRAMS.add("information retrieval");
      TARGET_BIGRAMS.add("power politics");
      TARGET_BIGRAMS.add("los angeles");
      TARGET_BIGRAMS.add("bruce willis");
    }

    public void map(Object key, Text value, Context context) throws IOException, InterruptedException {

      String[] splitted = null;

      try {
        String doc = value.toString().toLowerCase();

        splitted = doc.split("\t", 2);

        if (splitted.length < 2) {
          throw new IllegalArgumentException("parts.length < 2");
        }
      } catch (IllegalArgumentException e) {
        System.out.println("File does not have docID");
        return;
      }

      String docID = splitted[0];
      String content = splitted[1];

      String[] tokens = content
          .replaceAll("[^a-z0-9]", " ")
          .split(" ");

      // Generate bigrams and check if they match target bigrams
      for (int i = 0; i < tokens.length - 1; i++) {
        String token1 = tokens[i];
        String token2 = tokens[i + 1];

        if (token1.length() > 0 && token2.length() > 0) {
          String bigramStr = token1 + " " + token2;

          if (TARGET_BIGRAMS.contains(bigramStr)) {
            // Output format: "(bigram, docID)" as the key
            String outputKey = "(" + bigramStr + ", " + docID + ")";
            bigram.set(outputKey);
            context.write(bigram, one);
          }
        }
      }
    }
  }

  public static class IntSumReducer
      extends Reducer<Text, IntWritable, Text, IntWritable> {
    private IntWritable result = new IntWritable();

    public void reduce(Text key, Iterable<IntWritable> values,
        Context context) throws IOException, InterruptedException {
      int sum = 0;
      for (IntWritable val : values) {
        sum += val.get();
      }
      result.set(sum);
      // Output format: "(bigram, docID)" and count
      context.write(key, result);
    }
  }

  public static void main(String[] args) throws Exception {
    Configuration conf = new Configuration();
    Job job = Job.getInstance(conf, "word count");
    job.setJarByClass(WordCount.class);
    job.setMapperClass(TokenizerMapper.class);
    job.setCombinerClass(IntSumReducer.class);
    job.setReducerClass(IntSumReducer.class);

    // explicit map output classes (help avoid runtime type-mismatch errors)
    job.setMapOutputKeyClass(Text.class);
    job.setMapOutputValueClass(IntWritable.class);

    job.setOutputKeyClass(Text.class);
    job.setOutputValueClass(IntWritable.class);
    FileInputFormat.addInputPath(job, new Path(args[0]));
    FileOutputFormat.setOutputPath(job, new Path(args[1]));
    System.exit(job.waitForCompletion(true) ? 0 : 1);
  }
}