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

import java.io.IOException;

import net.java.jatextmining.util.MyPriorityQueue;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

/**
 * The Reducer implementation class for counting co-occurrence words.
 * @author kimura
 */
public class CoOccurrenceReducer
    extends Reducer<Text, Text, Text, Text> {

    /** The Text object for reducer value. */
    private Text coWords = new Text();

    @Override
    public final void setup(Context context) {
        
    }

    @Override
    public final void reduce(Text key , Iterable<Text> values, Context context)
        throws IOException, InterruptedException {
        Configuration conf = context.getConfiguration();
        MyPriorityQueue queue = new MyPriorityQueue(
                Integer.valueOf(conf.get("jatextmining.numofaroundword")));
        for (Text coTokenBuf : values) {
            String[] coToken = coTokenBuf.toString().split("\t");
            queue.add(coToken[0], Double.valueOf(coToken[1]));
        }
        StringBuilder resultBuf = new StringBuilder();
        MyPriorityQueue.Entity ent;
        while ((ent = queue.poll()) != null) {
            if (!ent.getKey().equals(key.toString())) {
                if (resultBuf.length() > 0) {
                    resultBuf.insert(0, "\t");
                   }
                String resultScore = String.format("%.4f", ent.getVal());
                resultBuf.insert(0, ent.getKey() + resultScore);
                }
          }
        coWords.set(resultBuf.toString());
        context.write(key, coWords);
    }
 }
