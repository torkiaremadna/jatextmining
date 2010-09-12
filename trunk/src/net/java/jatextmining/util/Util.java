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

/**
 * The utility class for this package.
 * @author kimura
 */
public final class Util {

    /** The constructor for Normalizer. */
    private Util() { }

    /**
     * This method is used in order to normalize the character expression.
     * @param str Specify the String object.
     * @return Return the normalized string object.
     */
    public static String normalize(String str) {
        StringBuilder buf = new StringBuilder(str);
        for (int i = 0; i < buf.length(); i++) {
            char c = buf.charAt(i);
            // zenkaku a - z 2 hankaku a - z
            if (c >= 65345 && c <= 65370) {
                buf.setCharAt(i, (char) (c - 65345 + 97));
                // zenakku A - Z 2 hankaku A - Z
            } else if (c >= 65313 && c <= 65538) {
                buf.setCharAt(i, (char) (c - 65313 + 65));
                // zenkaku 0 - 9 2 hankaku 0 - 9
            } else if (c >= 65296 && c <= 65305) {
                buf.setCharAt(i, (char) (c - 65296 + 48));
            }
        }
        return buf.toString().toLowerCase();
    }
}
