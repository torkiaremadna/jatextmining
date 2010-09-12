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

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;

/**
 * The test class. running tokenizer with single process.
 * @author kimura
  */
public final class SingleTokenizer {

    /** The default constructor. */
    private SingleTokenizer() { }

    /**
     * Tokenize Japanese document.
     * @param args The arguments from command-line.
     * @throws IOException io exception.
     */
    public static void main(String[] args) throws IOException {
        Tokenizer tokenizer;
        String tokenizerConf =
            "/usr/local/GoSen/testdata/dictionary/dictionary.xml";
        tokenizer = new Tokenizer(tokenizerConf);
        BufferedReader in =
            new BufferedReader(new InputStreamReader(
                    new FileInputStream(args[0])));
        String line;
        HashMap<String, Integer> map = new HashMap<String, Integer>(1000000);
        while ((line = in.readLine()) != null) {
            String[] rv = tokenizer.getToken(line,
                    EnumSet.of(Tokenizer.ExtractType.Noun));
            for (String token : rv) {
                if (!map.containsKey(token)) {
                    map.put(token, 1);
                } else {
                    map.put(token, map.get(token).intValue() + 1);
                }
            }
        }
        Iterator<String> itr = map.keySet().iterator();
        while (itr.hasNext()) {
            Object token = itr.next();
            System.out.println(token + "\t" + map.get(token));
        }
    }

}
