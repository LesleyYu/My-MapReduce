
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
import java.util.HashMap;
import java.util.Map;

import java.io.IOException;
import java.util.StringTokenizer;

import javax.naming.Context;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;

public class WordCount {

  public static class TokenizerMapper extends Mapper<Object, Text, Text, Text> {

    private Text word = new Text();
    private Text docIDText = new Text();

    public void map(Object key, Text value, Context context) throws IOException, InterruptedException {

      // Read each document store 'docID' and process its content
      String[] parts = null;
      try {
        String doc = value.toString().toLowerCase();

        // Split by tab to get docID and content
        parts = doc.split("\t", 2);

        if (parts.length < 2) {
          throw new IllegalArgumentException("parts.length < 2");
          // return; // Skip lines without tab delimiter
        }
      } catch (IllegalArgumentException e) {
        System.out.println("File does not have docID.");
        return; // Exit early if parsing fails
      }

      String docID = parts[0];
      String content = parts[1];

      // System.out.println("This is docID: " + docID);

      // Split content on multiple delimiters: whitespace, punctuation and special
      // characters
      String[] tokens = content
          .replaceAll("[^a-z0-9]", " ") // Replace all non-alphanumeric with spaces
          .split("\\s+"); // Split on whitespace

      for (String token : tokens) {
        if (token.length() > 0) { // Skip empty strings
          word.set(token);
          docIDText.set(docID);
          context.write(word, docIDText);
        }
      }
    }
  }

  public static class IntSumReducer extends Reducer<Text, Text, Text, Text> {
    private Text result = new Text();

    public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
      // Use HashMap to store docID and count pairs
      HashMap<String, Integer> docIDCountMap = new HashMap<>();

      // Count occurrences of each docID for the current word
      for (Text val : values) {
        String docID = val.toString();
        docIDCountMap.put(docID, docIDCountMap.getOrDefault(docID, 0) + 1);
      }

      // Output each (word, docID:count) pair
      for (Map.Entry<String, Integer> entry : docIDCountMap.entrySet()) {
        String output = entry.getKey() + ":" + entry.getValue();
        result.set(output);
        context.write(key, result);
      }
    }
  }

  public static void main(String[] args) throws Exception {
    Configuration conf = new Configuration();
    // String[] otherArgs = new GenericOptionsParser(conf, args).getRemainingArgs();
    // if (otherArgs.length < 2) {
    // System.err.println("Usage: wordcount <in> [<in>...] <out>");
    // System.exit(2);
    // }
    Job job = Job.getInstance(conf, "word count");
    job.setJarByClass(WordCount.class);
    job.setMapperClass(TokenizerMapper.class);
    // Don't set a combiner class - we need all docIDs to reach the reducer for
    // proper counting
    job.setReducerClass(IntSumReducer.class);
    job.setOutputKeyClass(Text.class);
    job.setOutputValueClass(Text.class);

    FileInputFormat.addInputPath(job, new Path(args[0]));
    FileOutputFormat.setOutputPath(job, new Path(args[1]));
    System.exit(job.waitForCompletion(true) ? 0 : 1);
  }
}
