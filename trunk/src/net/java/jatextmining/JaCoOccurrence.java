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
import org.apache.hadoop.fs.FSDataInputStream;
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

import net.java.jatextmining.lib.CoOccurrenceWeightingMapper;
import net.java.jatextmining.lib.CoOccurrenceReducer;
import net.java.jatextmining.lib.CoOccurrenceMapper;
import net.java.jatextmining.lib.CountReducer;
import static net.java.jatextmining.ConstantsClass.CONF_PATH;

/**
 * A MapReduce class that count the Co-Occurrence words of Japanes document.
 * @author kimura
 */
public final class JaCoOccurrence extends Configured implements Tool {

    /** Input file path (on HDFS). */
    private String in = null;

    /** Output file path (on HDFS). */
    private String out = null;

    /** Specify the weighting method type(freaq|chi|mi). */
    private String type = null;

    /** Specify the Extracting pos types(noun, verb, adj...). */
    private String pos = null;

    /** Specify the distributed cached file name. */
    private String cacheName = null;

    /** Specify a boolean(true/false) if you want re-use DF data. */
    private boolean reUse = false;

    /** The HashSet buffer for storing pos. */
    private HashSet<String> posSet = null;

    /** Saving the num of line of input file. */
    private int inputNum = 0;

    /** The constructor for JaCoOccurrence. */
    JaCoOccurrence() {
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
        System.err.println("  cooccurrence -i in -o out -t [chi|mi|freaq]");
        System.err.println("");
        System.err.println("Option:");
        System.err.println("  -t [chi|mi|freaq]: Spefify the method for"
                + " weighting co-occurrence.");
        System.err.println("  -p [noun|compNoun|adj]: Sepecify the"
                + " extracting words. The default is compNoun");
        System.err.println("  -r: if given this option,"
                + " reuse intermediate file.");
        System.err.println("");
        System.err.println("Example:");
        System.err.println("  hadoop jar jatextmining-*.* cooccurrence"
                + " -i input -o output -t chi");
        System.err.println("  hadoop jar jatextmining-*.* cooccurrence"
                + " -i input -o output -t chi -p noun");
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
     * Extractig argument from argument buffer.
     * @param argsElem argument from command-line.
     * @return Argument elemet.
     */
    private String extractArg(String argsElem) {
        int idx = argsElem.indexOf("=");
        return argsElem.substring(idx + 1, argsElem.length());
    }

    /** The argument checker. */
    private void argsCheck() {
        if (in == null) {
            System.err.println("error: please check [-i in] option");
            printUsage();
        } else if (out == null) {
            System.err.println("error: please check [-o out] option");
            printUsage();
        } else if (type == null) {
            System.err.println("error: please check"
                    + " -t [chi | mi | freaq] option");
            printUsage();
        }
    }

    /**
     * Processing the arguments from command-line.
     * @param args Specify the arguments from command-line.
     */
    private void procArgs(final String[] args) {
        if (args.length == 0) {
            printUsage();
        }
        for (int i = 0; i < args.length; i++) {
            String elem = args[i];
            if (elem.equals("-i")) {
                in = args[++i];
            } else if (elem.matches("^--input=.*")) {
                in = extractArg(elem);
            } else if (elem.equals("-o")) {
                out = args[++i];
            } else if (elem.matches("^--output=.*")) {
                out = extractArg(elem);
            } else if (elem.equals("-t")) {
                type = args[++i];
            } else if (elem.matches("^--type=.*")) {
                type = extractArg(elem);
            } else if (elem.equals("-p")) {
                pos = args[++i];
            } else if (elem.equals("-r")) {
                reUse = true;
            } else {
                printUsage();
            }
        }
        argsCheck();
    }

    /**
     * Tne implementation for start counting the co-occurrence words.
     * @param conf Specify the Hadoop Configuration object.
     * @param cache Specify the distributed cache file path.
     * @return If success return true, not success return false.
     * @throws IOException Exception for IO.
     * @throws URISyntaxException Exception for distributed cache file path.
     * @throws InterruptedException Exception for threads, waitForComletion().
     * @throws ClassNotFoundException Exception for waitForCompletion().
     */
    private boolean runJaCoOccurrence(Configuration conf, String cache)
        throws IOException, URISyntaxException,
               InterruptedException, ClassNotFoundException {
        String reducerNum = conf.get("jatextmining.JaWordCounterReducerNum");
        conf.setBoolean("df", true);
        Job job = new Job(conf);
        job.setJarByClass(JaCoOccurrence.class);
        TextInputFormat.addInputPath(job, new Path(in));
        FileOutputFormat.setOutputPath(job, new Path(cache));
        FileSystem fs = FileSystem.get(new URI(cache), conf);
        FileStatus[] status = fs.listStatus(new Path(cache));
        if (status != null) {
            fs.delete(new Path(cache), true);
        }
        fs.close();
        job.setMapperClass(CoOccurrenceMapper.class);
        job.setReducerClass(CountReducer.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(DoubleWritable.class);
        job.setNumReduceTasks(Integer.valueOf(reducerNum));
        boolean rv = job.waitForCompletion(true);
        if (rv) {
            writeDocNumFile(conf, job);
        }

        return rv;
    }

    /**
     * Writing the numf of line of input file on HDFS.
     * @param conf Spefity the Hadoop Configuration object.
     * @param job Specify the Hadoop Job object.
     * @return if success return true, not success return false.
     * @throws IOException Exception for IO.
     */
    private boolean writeDocNumFile(Configuration conf, Job job)
    throws IOException {
        Counters counters = job.getCounters();
        inputNum = (int) counters.findCounter(
                "org.apache.hadoop.mapred.Task$Counter",
        "MAP_INPUT_RECORDS").getValue();
        if (inputNum == 0) {
            return false;
        }
        FileSystem hdfs = FileSystem.get(conf);
        String docNumPath = conf.get("jatextmining.docNumPath");
        if (docNumPath == null) {
            return false;
        }
        FSDataOutputStream stream = hdfs.create(new Path(docNumPath));
        stream.writeUTF(String.valueOf((int) inputNum));
        stream.close();

        return true;
    }

    /**
     * Reading the num of line of input file.
     * @param conf Specify the Hadoop Configuration object.
     * @return If sucess return true, not success return false.
     * @throws IOException Exception for IO.
     */
    private boolean readDocNumFile(Configuration conf) throws IOException {
        FileSystem hdfs = FileSystem.get(conf);
        String docNumPath = conf.get("jatextmining.docNumPath");
        if (docNumPath == null) {
            return false;
        }
        FSDataInputStream stream = hdfs.open(new Path(docNumPath));
        String docNumBuf = stream.readUTF();
        if (docNumBuf == null) {
            return false;
        }
        inputNum = Integer.valueOf(docNumBuf);

        return true;
    }

    /**
     * Weighting the value of each co-occurrence words.
     * @param conf Specify the Hadoop Configuration object.
     * @param cache Specify the distributed cache file path.
     * @return if success return true, not success return false.
     * @throws IOException Exception for IO.
     * @throws URISyntaxException Exception for URI.
     * @throws InterruptedException Exception for threads, waitForCompletion().
     * @throws ClassNotFoundException Exception for waitForCompletion().
     */
    private boolean runJaCoOccurrenceWeighting(Configuration conf,
            String cache) throws IOException, URISyntaxException,
               InterruptedException, ClassNotFoundException {
        String reducerNum =
            conf.get("jatextmining.JaCoOccurrenceCounterReducerNum");
        if (type.equals("chi") || type.equals("mi") || type.equals("freaq")) {
            conf.set("type", type);
        } else {
            System.err.println("error type: [" + type + "]");
            printUsage();
        }
        readDocNumFile(conf);
        conf.setLong("docNum", inputNum);
        Job job = new Job(conf);
        job.setJarByClass(JaCoOccurrence.class);
        TextInputFormat.addInputPath(job, new Path(cache));
        FileOutputFormat.setOutputPath(job, new Path(out));
        job.setMapperClass(CoOccurrenceWeightingMapper.class);
        job.setReducerClass(CoOccurrenceReducer.class);
        job.setMapOutputKeyClass(Text.class);
        job.setMapOutputValueClass(Text.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);
        job.setNumReduceTasks(Integer.valueOf(reducerNum));

        return job.waitForCompletion(true);
    }

    @Override
    public int run(String[] args) throws Exception {
        Configuration conf = getConf();
        conf.addResource(CONF_PATH);
        procArgs(args);
        setPos(conf);
        cacheName = conf.get("jatextmining.cache");
        if (!reUse) {
            runJaCoOccurrence(conf, cacheName);
        }
        FileSystem fs = FileSystem.get(new URI(cacheName), conf);
        FileStatus[] status = fs.listStatus(new Path(cacheName));
        if (status == null) {
            printDfdbError(conf);
        }
        DistributedCache.addCacheFile(new URI(cacheName), conf);
        runJaCoOccurrenceWeighting(conf, cacheName);

        return 0;
    }

    /**
     * The main method for JaCoOccurrence class.
     * @param args Input arguments from command-line.
     * @throws Exception Exception for run().
     */
    public static void main(final String[] args) throws Exception {
        int rv = ToolRunner.run(new JaCoOccurrence(), args);
        System.exit(rv);
    }
}
