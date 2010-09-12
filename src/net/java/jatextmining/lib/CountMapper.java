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
import java.util.TreeSet;

import net.java.jatextmining.util.Tokenizer;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

/**
 * The Mapper implementation class for counting words of Japanese document.
 * @author kimura
 *
 */
public final class CountMapper
    extends Mapper<Object, Text, Text, DoubleWritable> {

    /** The DoubleWritable instance for value of reducer. */
    private static final DoubleWritable RVAL = new DoubleWritable(1.0);

    /** The Text instance for key of reducer. */
    private Text token = new Text();

    /** The Tokenier instance. Morphological analyze for document. */
    private Tokenizer tokenizer;

    /**
     * The method for writing token to Context object.
     * @param context Specify the Hadoop Context object.
     * @param tokenBuf Specify the string token object.
     */
    private void writeContext(Context context, String tokenBuf) {
        token.set(tokenBuf);
        try {
            context.write(token, RVAL);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setup(Context context) {
        Configuration conf = context.getConfiguration();
        String senConf = conf.get("jatextmining.GoSen");
        tokenizer = new Tokenizer(senConf);
    }

    @Override
    public void map(Object key, Text value, Context context) {
        Configuration conf = context.getConfiguration();
        String doc         = value.toString();
        doc = net.java.jatextmining.util.Util.normalize(doc);
        tokenizer.extractToken(doc);
        if (conf.getBoolean("df", false)) {
            TreeSet<String> uniqWords = new TreeSet<String>();
            if (conf.getBoolean("noun", false)) {
                for (String buf : tokenizer.getNoun()) {
                    uniqWords.add(buf);
                }
            }
            if (conf.getBoolean("compNoun", false)) {
                for (String buf : tokenizer.getCompoundNoun()) {
                    uniqWords.add(buf);
                }
            }
            if (conf.getBoolean("adj", false)) {
                for (String buf : tokenizer.getAdj()) {
                    uniqWords.add(buf);
                }
            }
            for (String buf : uniqWords) {
                writeContext(context, buf);
            }
        } else {
            if (conf.getBoolean("noun", false)) {
                for (String buf : tokenizer.getNoun()) {
                    writeContext(context, buf);
                }
            }
            if (conf.getBoolean("compNoun", false)) {
                for (String buf : tokenizer.getCompoundNoun()) {
                    writeContext(context, buf);
                }
            }
            if (conf.getBoolean("adj", false)) {
                for (String buf : tokenizer.getAdj()) {
                    writeContext(context, buf);
                }
            }
        }
    }
}
