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
import java.util.List;

import net.java.sen.SenFactory;
import net.java.sen.StringTagger;
import net.java.sen.dictionary.Token;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

import static net.java.jatextmining.ConstantsClass.CONF_PATH;

/**
 * The TokenizerSample class. Tokenizing a Japanese plain text on HDFS.
 * @author kimura
 */
public final class TokenizerSample extends Configured implements Tool {

    /**
     * The Mapper implementation of TokenizerSample class.
     * @author kimura
     */
    public static class TokenizerSampleMapper
    extends Mapper<Object, Text, Text, IntWritable> {
    private Text contextKey = new Text();
    private IntWritable contextValue = new IntWritable(1);
    private StringTagger tagger;

    @Override
    public final void setup(Context context) {
    // Mapperのメンバ変数に形態素解析器を持たせる
        tagger = SenFactory.getStringTagger("/usr/local/GoSen/testdata/dictionary/dictionary.xml");
    }

    @Override
    public final void map(Object key, Text value,
            Context context) {
        List<Token> tokens = null;
        try {
        // テキストを形態素解析する
            tokens = tagger.analyze(value.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
        for (Token token : tokens) {
        // Tokenオブジェクトから、単語を取得する
            String word = token.getSurface();
            contextKey.set(word);
            try {
                context.write(contextKey, contextValue);
            } catch (IOException e1) {
                e1.printStackTrace();
            } catch (InterruptedException e1) {
                e1.printStackTrace();
            }
        }
    }
}


    /** Specify the minmum number of arugments from command-line. */
    private final int argNum = 3;

    /** Input file path (on HDFS). */
    private String in;

    /** Output file path (on HDFS). */
    private String out;

    /** Printing the usage about this class. */
    private void printUsage() {
        System.err.println("");
        System.err.println("TokenizerSampler :"
                + " A Tokenizer Sample for Japanese Document");
        System.err.println();
        System.err.println("\t[usage] hadoop jar net.broomie.libnakameguro."
                + "utils.TokenizerSample -i input -o output");
        System.err.println("\t-i, --input=file\tspecify"
                + " the input file on HDFS.");
        System.err.println("\t-o, --output=directory\tspecify"
                + " the output dir on HDFS.");
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
            } else {
                printUsage();
            }
        }
    }

    /**
     * Run the MapReduce tokenize for Japanese document.
     * @param conf Specify the Hadoop Configuration object.
     * @return If success return true, if not success return false.
     * @throws IOException Exception for IO.
     * @throws InterruptedException Exception for threads(waitForComletion()).
     * @throws ClassNotFoundException Exception for waitForComletion().
     */
    private boolean runTokenizerSample(Configuration conf)
    throws IOException, InterruptedException, ClassNotFoundException {
        Job job = new Job(conf);

        job.setJarByClass(TokenizerSample.class);
        TextInputFormat.addInputPath(job, new Path(in));
        FileOutputFormat.setOutputPath(job, new Path(out));
        job.setMapperClass(TokenizerSampleMapper.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(IntWritable.class);

        return job.waitForCompletion(true);
    }

    @Override
    public int run(final String[] args) {
        procArgs(args);
        if (in == null || out == null) {
            printUsage();
        }
        Configuration conf = getConf();
        conf.addResource(CONF_PATH);
        boolean rvBuf = true;
        try {
            rvBuf = runTokenizerSample(conf);
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
        int rv = ToolRunner.run(new TokenizerSample(), args);
        System.exit(rv);
    }

}
