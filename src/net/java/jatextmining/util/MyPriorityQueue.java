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

import java.util.Queue;
import java.util.PriorityQueue;
import java.util.Comparator;

/**
 * The Priority Queue class for libnakamegruo.
 * @author kimura
 */
public class MyPriorityQueue {

    /** Queue object for priority queue. */
    private Queue<Entity> queue;

    /** Saving the max key size of the queue. */
    private int maxKeyNum;

    /** THe Comparator instance. */
    private MyComparator comparator;

    /**
     * The entity object for priority queue.
     * @author kimura
     */
    public class Entity {

        /** The key object for priority queue entity. */
        private String key;

        /** The value object for priority queue entity. */
        private double val;

        /**
         * The constructor for Entity class.
         * @param key Specify the String object for the Entity key.
         * @param val Specify the double value for the Entity val.
         */
        public Entity(String key, double val) {
            this.key = key;
            this.val = val;
        }

        /**
         * This method is used in order to access key object of the Entity.
         * @return The String object of the key.
         */
        public final String getKey() {
            return key;
        }

        /**
         * This method is used in order to access value object of the Entity.
         * @return The double value of the Entity.
         */
        public final double getVal() {
            return val;
        }
    }

    /**
     * This class is Comparator for Entity class.
     * using value of the each Entity.
     * @author kimura
     */
    public  class MyComparator implements Comparator<Entity> {
        /**
         * This method is used in order to compare each Entity.
         * @param ent1 Specify the Entity object.
         * @param ent2 Specify the Entity object.
         * @return Return 1 if ent1 is bigger, return -1 if ent2 is bigger.
         */
        @Override
        public final int compare(Entity ent1, Entity ent2) {

            if (ent1.getVal() > ent2.getVal()) {
                return 1;
            } else if (ent1.getVal() < ent2.getVal()) {
                return -1;
            } else {
                return 0;
            }
        }
    }

    /**
     * The constructor for MyPriorityQueue.
     * @param maxNum Specify the max key size.
     */
    public MyPriorityQueue(int maxNum) {
        comparator = new MyComparator();
        queue = new PriorityQueue<Entity>(maxNum, comparator);
        maxKeyNum = maxNum;
    }

    /**
     * This method is used in order to get queue size.
     * @return queue size.
     */
    public final int getSize() {
        return queue.size();
    }

    /**
     * This method is used in order to add Entity to the queue.
     * @param key Specify the String object for key.
     * @param val Specify the value of the key.
     */
    public final void add(String key, double val) {
        Entity newEnt = new Entity(key, val);
        Entity peekEnt = queue.peek();
        if (peekEnt == null) {
            queue.add(newEnt);
            return;
        }
        if (queue.size() < maxKeyNum) {
            queue.add(newEnt);
        } else {
            int result = comparator.compare(newEnt, peekEnt);
            if (result > 0) {
                if (queue.size() >= maxKeyNum) {
                    queue.poll();
                }
                queue.add(newEnt);
            }
        }
    }

    /**
     * This method is used in order to poll Entity from queue.
     * @return The top Entity object.
     */
    public final Entity poll() {
        if (queue.size() > 0) {
            return queue.poll();
        } else {
            return null;
        }
    }
}
