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

package net.java.jatextmining;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.Counters;
import org.apache.hadoop.filecache.DistributedCache;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

import net.java.jatextmining.lib.CountMapper;
import net.java.jatextmining.lib.CountReducer;
import static net.java.jatextmining.ConstantsClass.CONF_PATH;

/**
 * A MapReduce class that counts the words of Japanese document.
 * @author kimura
 */
public final class JaWordCounter extends Configured implements Tool {

    /** Input file path (on HDFS). */
    private String in = null;

    /** Output file path (on HDFS). */
    private String out = null;

    /** DF file path (on HDFS). */
    private String dfIn = null;

    /** Specify a pos(noun, compNoun, adj, verb) that will be extracted. */
    private String pos = null;

    /** Specify a boolean(true/false) if you want re-use DF data. */
    private boolean dfReuse = false;

    /** Specify a boolean(true/false) wheter weighting the word counts. */
    private boolean weightingFlag = false;

    /** The HashSet buffer for storing pos. */
    private HashSet<String> posSet = null;

    /** Specify a minimum num of argument. */
    private final int argNum = 3;

    /** The consructor for JaWordCounter. */
    JaWordCounter() {
        posSet = new HashSet<String>();
        posSet.add("noun");
        posSet.add("compNoun");
        posSet.add("adj");
        posSet.add("verb");
    }

    /** Printing the usage about this class. */
    private void printUsage() {
        System.err.println("");
        System.err.println("Usage:");
        System.err.println("  wordcount -i in -o out");
        System.err.println("");
        System.err.println("Option:");
        System.err.println("  -w df_doc: Spefify the document path for"
                + " document freaquency database.");
        System.err.println("  -p [noun|compNoun|adj]: Specify"
                + " the extracting pos.");
        System.err.println("");
        System.err.println("Example:");
        System.err.println("  hadoop jar jatextmining-*.* wordcount"
                + " -i input -o output");
        System.err.println("  hadoop jar jatextmining-*.* wordcount"
                + " -i input -o output -w df_doc -p noun");
        System.err.println("");

        System.exit(1);
    }

    /**
     * Printing the DF database error.
     * @param conf Specify the hadoop configuration object.
     */
    private void printDfdbError(Configuration conf) {
        String dfdbPath = conf.get("jatextmining.dfdb");
        System.err.println("");
        System.err.println("[error]");
        System.err.println("[error] dfdb is not exist. please make dfdb.");
        System.err.println("[error] " + dfdbPath + "is not exists.");
        System.err.println("[error]");
        printUsage();
    }

    /**
     * Processing the arguments from command-line.
     * @param args Specify the arguments from command-line.
     */
    private void procArgs(final String[] args) {
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
            } else if (elem.equals("-w")) {
                weightingFlag = true;
                dfIn = args[++i];
            } else if (elem.equals("-wr")) {
                weightingFlag = true;
                dfReuse = true;
            } else if (elem.equals("-p")) {
                pos = args[++i];
            } else {
                printUsage();
            }
        }
    }

    /**
     * Creating the DF database from Japanese documents.
     * @param conf Specify the Hadoop Configuration object.
     * @param dfdb Specify the saving path for DF database.
     * @return If success return true, it not success return false.
     * @throws IOException Exception for IO.
     * @throws URISyntaxException Exception for DF database URI.
     * @throws InterruptedException Exception for waitForCompletion().
     * @throws ClassNotFoundException Exception for waitForCompletion().
     */
    private boolean runCreateDFDB(Configuration conf, String dfdb)
        throws IOException, URISyntaxException,
            InterruptedException, ClassNotFoundException {
        String reducerNum = conf.get("jatextmining.JaWordCounterReducerNum");
        Job job = new Job(conf);
        job.setJarByClass(JaWordCounter.class);
        TextInputFormat.addInputPath(job, new Path(dfIn));
        FileOutputFormat.setOutputPath(job, new Path(dfdb));
        FileSystem fs = FileSystem.get(new URI(dfdb), conf);
        FileStatus[] status = fs.listStatus(new Path(dfdb));
        if (status != null) {
            fs.delete(new Path(dfdb), true);
        }
        fs.close();
        job.setMapperClass(CountMapper.class);
        job.setReducerClass(CountReducer.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(DoubleWritable.class);
        job.setNumReduceTasks(Integer.valueOf(reducerNum));
        boolean rv = job.waitForCompletion(true);
        if (rv) {
            Counters counters = job.getCounters();
            long docNum =
                counters.findCounter("org.apache.hadoop.mapred.Task$Counter",
                                     "MAP_INPUT_RECORDS").getValue();
            FileSystem hdfs = FileSystem.get(conf);
            String docNumPath = conf.get("jatextmining.docNum");
            FSDataOutputStream stream = hdfs.create(new Path(docNumPath));
            stream.writeUTF(String.valueOf((int) docNum));
            stream.close();
        }

        return rv;
    }

    /**
     * Setting the pos for configuration that will be extracted.
     * @param conf Specify the Hadoop configuration object.
     */
    private void setPos(Configuration conf) {
        if (pos == null) {
            pos = conf.get("jatextmining.wordCountposSet");
        }
        if (pos == null) {
            conf.setBoolean("compNoun", true);
        } else {
            String[] poses = pos.split(",");
            for (String posBuf : poses) {
                String posCleared = posBuf.replaceAll(" ", "");
                if (posSet.contains(posCleared)) {
                    conf.setBoolean(posCleared, true);
                } else {
                    System.err.println("[error] unknown pos: " + posCleared);
                    System.exit(-1);
                }
            }
        }
    }

    /**
     * Clear the pos from configuration.
     * @param conf Specify the Hadoop configuration object.
     */
    private void clearPos(Configuration conf) {
        for (String posBuf : posSet) {
            conf.setBoolean("posBuf", false);
        }
    }

    /**
     * The implements for start counting the words from document with MapReduce.
     * @param conf Specify the Hadoop configuration object.
     * @return If success return true, not sucess return fales.
     * @throws IOException Exception for IO.
     * @throws InterruptedException Exception for threads.
     * @throws ClassNotFoundException Exception for finding class.
     */
    private boolean runCount(Configuration conf)
        throws IOException,
        InterruptedException, ClassNotFoundException {
        String reducerNum = conf.get("jatextmining.JaWordCounterReducerNum");
        conf.setBoolean("df", true);
        if (weightingFlag) {
            conf.setBoolean("weighting", true);
        }
        Job job = new Job(conf);
        job.setJarByClass(JaWordCounter.class);
        TextInputFormat.addInputPath(job, new Path(in));
        FileOutputFormat.setOutputPath(job, new Path(out));
        job.setMapperClass(CountMapper.class);
        job.setMapOutputKeyClass(Text.class);
        job.setMapOutputValueClass(DoubleWritable.class);
        job.setReducerClass(CountReducer.class);
        job.setOutputKeyClass(Text.class);
        job.setNumReduceTasks(Integer.valueOf(reducerNum));

        return job.waitForCompletion(true);
    }

    @Override
    public int run(String[] args) throws Exception {
        Configuration conf = getConf();
        conf.addResource(CONF_PATH);
        procArgs(args);
        setPos(conf);
        String dfdb = conf.get("jatextmining.dfdb");
        if (weightingFlag && !dfReuse) {
            runCreateDFDB(conf, dfdb);
        }
        if (weightingFlag) {
            FileSystem fs = FileSystem.get(new URI(dfdb), conf);
            FileStatus[] status = fs.listStatus(new Path(dfdb));
            if (status == null) {
                printDfdbError(conf);
            }
            DistributedCache.addCacheFile(new URI(dfdb), conf);
        }
        runCount(conf);
        clearPos(conf);

        return 0;
    }

    /**
     * The main method for JaWordCounter class.
     * @param args Specify the arguments from command-line.
     * @throws Exception Exception for run().
     */
    public static void main(final String[] args)
    throws Exception {
        int rv = ToolRunner.run(new JaWordCounter(), args);
        System.exit(rv);
    }
}
