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
 * The Mapper implementation for counting Noun-Adj co-occurrence.
 * @author kimura
 */
public final class CoOccurrenceNAMapper
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
        String doc = value.toString();
        doc = net.java.jatextmining.util.Util.normalize(doc);
        EnumSet<ExtractType> pos = EnumSet.of(Tokenizer.ExtractType.Noun);
        pos.add(Tokenizer.ExtractType.Adj);
        tokenizer.extractToken(doc);
        String[] adjs  = tokenizer.getAdj();
        String[] nouns = tokenizer.getNoun();

        HashSet<String> uniqNouns = new HashSet<String>();
        for (int i = 0; i < nouns.length; i++) {
            if (nouns[i].length() < 2) {
                continue;
            }
            uniqNouns.add(nouns[i]);
        }

        HashSet<String> uniqAdjs = new HashSet<String>();
        for (int i = 0; i < adjs.length; i++) {
            if (adjs[i].length() < 2) {
                continue;
            }
            uniqAdjs.add(adjs[i]);
        }
        for (String noun : uniqNouns) {
            rKey.set(noun);
            context.write(rKey, RVAL);
            for (String adj : uniqAdjs) {
                rKey.set(noun + "\t" + adj);
                context.write(rKey, RVAL);
            }
        }
        for (String adj : uniqAdjs) {
            rKey.set(adj);
            context.write(rKey, RVAL);
        }
    }
}
