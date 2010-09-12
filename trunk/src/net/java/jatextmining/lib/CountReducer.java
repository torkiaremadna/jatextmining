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

package net.java.jatextmining.lib;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.LinkedHashMap;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.filecache.DistributedCache;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

import static net.java.jatextmining.ConstantsClass.CONF_PATH;

/**
 * The Reducer implementation class for counting words of Japanese document.
 * @author kimura
 */
public class CountReducer
    extends Reducer<Text, DoubleWritable, Text, DoubleWritable> {

    /** The DoubleWritable instance for using output value. */
    private DoubleWritable value = new DoubleWritable();

    /** Specify the minmum number for saving value. */
    private int countMinNum = 1;

    /** The LinkedHashMap instantce for saving DF. */
    private LinkedHashMap<String, Double> dfMap;

    /**
     * The method for finding part file on HDFS.
     * @param regex Specify the patter for searching file on HDFS.
     * @return If found return true, not found return false.
     */
    private FilenameFilter getFileRegexFilter(final String regex) {
        return new FilenameFilter() {
            public boolean accept(File file, String name) {
                boolean rv = name.matches(regex);
                return rv;
            }
        };
    }

    /**
     * Loading the DF database which is distributed cache.
     * @param cachePath Specify the DF database path.
     * @param context Specify the Hadoop Context object.
     * @throws IOException Exception for IO(DF database).
     */
    private void loadCacheFile(Path cachePath, Context context)
    throws IOException {
        File cacheFile = new File(cachePath.toString());
        File[] caches =
            cacheFile.listFiles(getFileRegexFilter("part-.*-[0-9]*"));
        for (File cache : caches) {
            BufferedReader wordReader =
                new BufferedReader(new FileReader(cache.getAbsolutePath()));
            String line;
            while ((line = wordReader.readLine()) != null) {
                String[] wordCountBuf = line.split("\t");
                dfMap.put(wordCountBuf[0],
                        Double.valueOf(wordCountBuf[1]));
            }
        }
        Configuration conf = context.getConfiguration();
        FileSystem hdfs    = FileSystem.get(conf);
    }

    @Override
    public final void setup(Context context) {
        Configuration conf = context.getConfiguration();
        if (conf.getBoolean("weighting", false)) {
            String confPath = conf.get(CONF_PATH);
            conf.addResource(confPath);
            dfMap = new LinkedHashMap<String, Double>(
                    Integer.valueOf(conf.get("jatextmining.dfHashSize")));
            try {
                Path[] cacheFiles = DistributedCache.getLocalCacheFiles(conf);
                if (cacheFiles != null) {
                    for (Path cachePath : cacheFiles) {
                        loadCacheFile(cachePath, context);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        countMinNum = Integer.valueOf(conf.get("jatextmining.counterMimunNum"));
    }

    @Override
    public final void reduce(Text key , Iterable<DoubleWritable> values,
                             Context context)
        throws IOException, InterruptedException {

        Configuration conf = context.getConfiguration();
        double sum = 0.0;
        if (conf.getBoolean("weighting", false)) {
            String stringKey = key.toString();
            if (dfMap.containsKey(stringKey)) {
                double df = dfMap.get(stringKey);
                for (DoubleWritable val : values) {
                    sum += val.get() / df;
                }
            }
        } else {
            for (DoubleWritable val : values) {
                sum += val.get();
            }
        }
        if (sum >= countMinNum) {
            value.set(sum);
            context.write(key, value);
        }
    }
}
