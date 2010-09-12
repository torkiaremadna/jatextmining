/**
 * Copyright 2010 Shunya KIMURA <brmtrain@gmail.com>
 *
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package net.java.jatextmining.util;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.compress.CompressionCodec;
import org.apache.hadoop.io.compress.GzipCodec;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

/**
 * The compressor class. compress the plain text files on HDFS.
 * @author kimura
 */
public final class Compressor extends Configured implements Tool {

    /**
     * The Mapper implementation of compressor class.
     * @author kimura
     */
    public static class CompressorMapper
    extends Mapper<Object, Text, Text, NullWritable> {

        /** The NullWritable instance for using value for reducer. */
        private final NullWritable nullVal = NullWritable.get();

        @Override
        public final void map(Object key, Text value, Context context) {
            try {
                context.write(value, nullVal);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    /** Specify the minmum number of arugments from command-line. */
    private final int argNum = 3;

    /** Input file path (on HDFS). */
    private String in;

    /** Output file path (on HDFS). */
    private String out;

    /** Specify the compress type. */
    private String codec;

    /** Printing the usage about this class. */
    private void printUsage() {
        System.err.println("");
        System.err.println("Compressor : A compressor for data on HDFS");
        System.err.println();
        System.err.println("\t[usage] hadoop jar net.broomie.libnakameguro."
                + "utils.Compressor -i input -o output -c codec");
        System.err.println("\t-i, --input=file\tspecify"
                + " the input file on HDFS.");
        System.err.println("\t-o, --output=directory\tspecify"
                + " the output dir on HDFS.");
        System.err.println("\t-c, --codec=type\tspecify the codec type.");
        System.err.println("\tcodec type = [deflate | gzip | bzip2 | lzo]");
        System.err.println();
        System.exit(-1);
    }

    /**
     * Processing the arguments from command-line.
     * @param args Specify the arguments from command-line.
     */
    private void procArgs(final String[] args) {
        System.err.println("length:" + args.length);
        if (args.length < argNum) {
            printUsage();
        }
        for (int i = 0; i < args.length; i++) {
            String elem = args[i];
            if (elem.equals("-i")) {
                in = args[++i];
            } else if (elem.matches("^--input=.*")) {
                int idx = elem.indexOf("=");
                in = elem.substring(idx + 1, elem.length());
            } else if (elem.equals("-o")) {
                out = args[++i];
            } else if (elem.matches("^--output=.*")) {
                int idx = elem.indexOf("=");
                out = elem.substring(idx + 1, elem.length());
            } else if (elem.equals("-c")) {
                codec = args[++i];
            } else if (elem.matches("^--codec=.*")) {
                int idx = elem.indexOf("=");
                codec = elem.substring(idx + 1, elem.length());
            } else {
                printUsage();
            }
        }
    }

    /**
     * Run the MapReduce compress files of HDFS.
     * @param conf Specify the Hadoop Configuration object.
     * @return If success return true, if not success return false.
     * @throws IOException Exception for IO.
     * @throws InterruptedException Exception for threads(waitForComletion()).
     * @throws ClassNotFoundException Exception for waitForComletion().
     */
    private boolean runCompressor(Configuration conf)
    throws IOException, InterruptedException, ClassNotFoundException {
        conf.setBoolean("mapred.output.compress", true);
        conf.setClass("mapred.output.compression.codec", GzipCodec.class,
                CompressionCodec.class);
        Job job = new Job(conf);
        job.setJarByClass(Compressor.class);
        TextInputFormat.addInputPath(job, new Path(in));
        FileOutputFormat.setOutputPath(job, new Path(out));
        job.setMapperClass(CompressorMapper.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(NullWritable.class);

        return job.waitForCompletion(true);
    }

    @Override
    public int run(final String[] args) {
        procArgs(args);
        if (in == null || out == null) {
            printUsage();
        }
        Configuration conf = getConf();
        boolean rvBuf = true;
        try {
            rvBuf = runCompressor(conf);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        int rv = rvBuf ? 0 : 1;
        return rv;
    }

    /**
     * The main method for this class.
     * @param args The arguments from command-line.
     * @throws Exception Exception for run().
     */
    public static void main(final String[] args) throws Exception {
        int rv = ToolRunner.run(new Compressor(), args);
        System.exit(rv);
    }

}
