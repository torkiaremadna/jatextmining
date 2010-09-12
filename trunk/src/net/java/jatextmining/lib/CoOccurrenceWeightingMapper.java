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
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

import static net.java.jatextmining.ConstantsClass.CONF_PATH;

/**
 * The Mapper implementation class for weghting co-occurrence words.
 * @author kimura
 */
public final class CoOccurrenceWeightingMapper
    extends Mapper<Object, Text, Text, Text> {

    /** The Text object for setting the result key. */
    private Text rToken = new Text();

    /** The Text object for setting the result value. */
    private Text rValue = new Text();

    /** The HashMap for saving DF database. */
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
                if (wordCountBuf.length == 3) {
                        dfMap.put(wordCountBuf[0] + "\t"
                                  + wordCountBuf[1],
                                  Double.valueOf(wordCountBuf[2]));
                } else {
                    dfMap.put(wordCountBuf[0],
                    Double.valueOf(wordCountBuf[1]));
                }
            }
        }
    }

    @Override
    public void setup(Context context) {
        Configuration conf = context.getConfiguration();
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

    @Override
    public void map(Object key, Text value, Context context)
        throws IOException, InterruptedException {
        Configuration conf = context.getConfiguration();
        String type = conf.get("type");
        long inputNum = 0;
        inputNum = conf.getLong("docNum", inputNum);
        String[] input = value.toString().split("\t");
        if (input.length > 2) {
            String w1 = input[0];
            String w2 = input[1];
            System.err.println("\t" + w1 + "\t" + w2);
            double a = Double.valueOf(input[2]);
            double w1Score = 0;
            double w2Score = 0;
            if (dfMap.containsKey(w1)) {
                w1Score = dfMap.get(w1);
            }
            if (dfMap.containsKey(w2)) {
                w2Score = dfMap.get(w2);
            }
            if (w1Score != 0 || w2Score != 0) {
                double b   = w2Score - a;
                double c   = w1Score - a;
                double d = inputNum - (w1Score + w2Score - a);
                System.err.println("\t" + a + "\t" + b + "\t" + c + "\t" + d);
                double score = 0.0;
                if (type.equals("chi")) {
                    double tmp = (a * d) - (b * c);
                    if (tmp == 0) {
                        score = 0;
                    } else {
                        double chiA = inputNum * Math.pow(tmp , 2);
                        double chiB = (a + b) * (a + c) * (c + d) * (b + d);
                        score = chiA / chiB;
                        }
                    System.err.println("\tchi:" + score);
                } else if (type.equals("mi")) {
                    score = Math.log((a * inputNum) / (a + b) * (a + c));
                } else if (type.equals("freaq")) {
                    score = a;
                }
                rToken.set(input[0]);
                rValue.set(input[1] + "\t" + String.valueOf(score));
                context.write(rToken,  rValue);
            }
        }
    }
}
