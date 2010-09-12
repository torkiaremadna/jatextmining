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

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import net.java.sen.StringTagger;
import net.java.sen.SenFactory;
import net.java.sen.dictionary.Token;

/**
 * The Tokenizer abstarct class. Morphological analyze for Japane document.
 * @author kimura
 */
public class Tokenizer {
    /** Noun string definition. */
    private final String nounDef = "名詞";

    /** Verb string definition. */
    private final String verbDef = "動詞";

    /** Adjective string definition. */
    private final String adjDef = "形容";

    /** Unknown string definition. */
    private final String unkDef = "未知";

    /**
     *
     * @author kimura
     *
     */
    public static enum ExtractType {
        /** Noun type definition. */
        Noun,
        /** Compound Noun type definition. */
        CompNoun,
        /** Verb type definition. */
        Verb,
        /** Adjective type definition .*/
        Adj,
        /** Unknown type definition. */
        Unk,
    }

    /** The buffer for saving noun words. */
    private ArrayList<String> nounArray;

    /** The buffer for saving compound noun words. */
    private ArrayList<String> compNounArray;

    /** The buffer for saving verb words. */
    private ArrayList<String> verbArray;

    /** The buffer for saving adjective words. */
    private ArrayList<String> adjArray;

    /** The buffer for saving unknown words. */
    private ArrayList<String> unkArray;

    /** The Tagger object for extract token from Japanese document. */
    private StringTagger tagger;

    /**
     * The constructor for Tokenizer class.
     * @param senConfPath Specify the path for GoSen configuration.
     */
    public Tokenizer(String senConfPath) {
        try {
            tagger = SenFactory.getStringTagger(senConfPath);
        } catch (Exception e) {
            e.printStackTrace();
        }
        nounArray     = new ArrayList<String>(100);
        compNounArray = new ArrayList<String>(100);
        verbArray     = new ArrayList<String>(100);
        adjArray      = new ArrayList<String>(100);
        unkArray      = new ArrayList<String>(100);
    }

    /** Clearing the words from buffer. */
    private void clear() {
        nounArray.clear();
        compNounArray.clear();
        verbArray.clear();
        adjArray.clear();
        unkArray.clear();
    }

    /**
     * Creating compound nouns from noun seaquence.
     * @param nouns String array object including nouns.
     * @return compound noun.
     */
    private String createCompoundNoun(ArrayList<String> nouns) {
        StringBuilder cmpNoun = new StringBuilder();
        for (String noun : nouns) {
            cmpNoun.append(noun);
        }
        return cmpNoun.toString();
    }

    /**
     * Extracting target words and save buffer.
     * @param str Specify the japanese document.
     */
    public final void extractToken(String str) {
        clear();
        ArrayList<String> compoundNoun = new ArrayList<String>();
        try {
            List<Token> tokens = tagger.analyze(str);
            if (tokens != null) {
                for (Token token : tokens) {
                    String pos = token.getMorpheme().toString().substring(0, 2);
                    String baseToken = token.getMorpheme().getBasicForm();
                    if (pos.equals(nounDef)) {
                        nounArray.add(baseToken);
                        compoundNoun.add(baseToken);
                    } else if (pos.equals(verbDef)) {
                        verbArray.add(baseToken);
                        if (compoundNoun.size() > 0) {
                            compNounArray.add(createCompoundNoun(compoundNoun));
                            compoundNoun.clear();
                        }
                    } else if (pos.equals(adjDef)) {
                        adjArray.add(baseToken);
                        if (compoundNoun.size() > 0) {
                            compNounArray.add(createCompoundNoun(compoundNoun));
                            compoundNoun.clear();
                        }
                    } else if (pos.equals(unkDef)) {
                        unkArray.add(baseToken);
                        if (compoundNoun.size() > 0) {
                            compNounArray.add(createCompoundNoun(compoundNoun));
                            compoundNoun.clear();
                        }
                    } else {
                        if (compoundNoun.size() > 0) {
                            compNounArray.add(createCompoundNoun(compoundNoun));
                            compoundNoun.clear();
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Getting nouns from nouns buffer.
     * @return return String array including nouns.
     */
    public final String[] getNoun() {
        return (String[]) nounArray.toArray(new String[0]);
    }

    /**
     * Getting compound nouns from nouns buffer.
     * @return return String array including compound nouns.
     */
    public final String[] getCompoundNoun() {
        return (String[]) compNounArray.toArray(new String[0]);
    }

    /**
     * Getting nouns from verbs buffer.
     * @return return String array including verbs.
     */
    public final String[] getVerb() {
        return (String[]) verbArray.toArray(new String[0]);
    }

    /**
     * Getting nouns from adjectives buffer.
     * @return return String array including adjectives.
     */
    public final String[] getAdj() {
        return (String[]) adjArray.toArray(new String[0]);
    }

    /**
     * Getting nouns from unknown words buffer.
     * @return return String array including unknown words.
     */
    public final String[] getUnk() {
        return (String[]) unkArray.toArray(new String[0]);
    }

    /**
     * Extract tokens by spcifing pos(noun, adjective...).
     * @param str The string object for extracting tokens.
     * @param type The extracting type. Tokenizer.Noun etc...
     * @return The String array including tokens.
     */
    public final String[] getToken(String str, EnumSet<ExtractType> type) {
        ArrayList<String> result = new ArrayList<String>();
        ArrayList<String> compoundNoun = new ArrayList<String>();
        try {
            List<Token> tokens = tagger.analyze(str);
            if (tokens != null) {
                for (Token token : tokens) {
                    String pos = token.getMorpheme().toString().substring(0, 2);
                    String baseToken = token.getMorpheme().getBasicForm();
                    if (pos.equals(nounDef)) {
                        if (type.contains(ExtractType.Noun)) {
                            result.add(baseToken);
                        }
                        if (type.contains(ExtractType.CompNoun)) {
                            compoundNoun.add(baseToken);
                        }
                    } else if (pos.equals(verbDef)) {
                        if (type.contains(ExtractType.Verb)) {
                            result.add(baseToken);
                        }
                        if (compoundNoun.size() > 1) {
                            result.add(createCompoundNoun(compoundNoun));
                            compoundNoun.clear();
                        }
                    } else if (pos.equals(adjDef)) {
                        if (type.contains(ExtractType.Adj)) {
                            result.add(baseToken);
                        }
                        if (compoundNoun.size() > 1) {
                            result.add(createCompoundNoun(compoundNoun));
                            compoundNoun.clear();
                        }
                    } else if (pos.equals(unkDef)) {
                        if (type.contains(ExtractType.Unk)) {
                            result.add(baseToken);
                        }
                        if (compoundNoun.size() > 1) {
                            result.add(createCompoundNoun(compoundNoun));
                            compoundNoun.clear();
                        }
                    } else {
                        if (compoundNoun.size() > 1) {
                            result.add(createCompoundNoun(compoundNoun));
                        }
                        compoundNoun.clear();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return (String[]) result.toArray(new String[0]);
    }
}
