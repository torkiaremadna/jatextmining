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
import java.util.EnumSet;
import java.util.HashSet;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

import net.java.jatextmining.util.Tokenizer;
import net.java.jatextmining.util.Tokenizer.ExtractType;

/**
 * The Mapper implementation class for counting co-occurrence words.
 * @author kimura
  */
public final class CoOccurrenceMapper
    extends Mapper<Object, Text, Text, DoubleWritable> {

    /** The Tokenizer instance. Morphological analyze for document. */
    private Tokenizer tokenizer;

    /** The DoubleWritable instance for value of reducer. */
    private static final DoubleWritable RVAL = new DoubleWritable(1.0);

    /** The Text instance for key of reducer. */
    private Text rKey = new Text();

    @Override
    public void setup(Context context) {
        Configuration conf = context.getConfiguration();
        String senConf = conf.get("jatextmining.GoSen");
        tokenizer = new Tokenizer(senConf);
    }

    @Override
    public void map(Object key, Text value, Context context)
    throws IOException, InterruptedException {
        Configuration conf = context.getConfiguration();
        String doc = value.toString();
        doc = net.java.jatextmining.util.Util.normalize(doc);
        EnumSet<ExtractType> pos = EnumSet.noneOf(ExtractType.class);
        if (conf.getBoolean("noun", false)) {
            pos.add(Tokenizer.ExtractType.Noun);
        }
        if (conf.getBoolean("adj", false)) {
            pos.add(Tokenizer.ExtractType.Adj);
        }
        if (conf.getBoolean("compNoun", false)) {
            pos.add(Tokenizer.ExtractType.CompNoun);
        }
        String[] tokens = tokenizer.getToken(doc, pos);
        HashSet<String> uniqTokens = new HashSet<String>();
        for (int i = 0; i < tokens.length; i++) {
            if (tokens[i].length() < 2) {
                continue;
            }
            uniqTokens.add(tokens[i]);
            for (int j = 0; j < tokens.length; j++) {
                if (tokens[j].length() < 2) {
                    continue;
                }
                if (tokens[i].equals(tokens[j])) {
                    continue;
                }
                if (i != j) {
                    String compoundToken = tokens[i] + "\t" + tokens[j];
                    uniqTokens.add(compoundToken);
                }
            }
        }
         for (String token : uniqTokens) {
             rKey.set(token);
             context.write(rKey, RVAL);
         }
    }
}
