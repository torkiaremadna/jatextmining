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

import org.apache.hadoop.util.ProgramDriver;

/**
 * A driver for an jatextmining classes.
 * @author kimura
 */
public final class JaTextminingDriver {

    /** A default constructor. */
    private JaTextminingDriver() { }

    /**
     * A main method for this class.
     * @param argv A arguments from command-line.
     */
    public static void main(String[] argv) {
        int exitCode = -1;
        ProgramDriver driver = new ProgramDriver();
        try {
            driver.addClass("wordcount", JaWordCounter.class, "A map/reduce "
                    + "program that counts the words from Japnese document.");
            driver.addClass("cooccurrence", JaCoOccurrence.class, "A map/reduce"
                    + " program that count the co-occurrence word.");
            driver.driver(argv);
            exitCode = 0;
        } catch (Throwable e) {
            e.printStackTrace();
        }
        System.exit(exitCode);
    }
}
