package com.griefcraft.cache;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

public class MethodCounter {

    /**
     * A map of the counts
     */
    private final Map<String, VariableInteger> counts = new HashMap<String, VariableInteger>();

    /**
     * Increment a method in the counts
     *
     * @param method
     */
    public void increment(String method) {
        deltaMethod(method, 1);
    }

    /**
     * Decrement a method in the cache
     *
     * @param method
     */
    public void decrement(String method) {
        deltaMethod(method, -1);
    }

    /**
     * Get the counts for a method
     *
     * @param method
     * @return
     */
    public int get(String method) {
        return counts.containsKey(method) ? counts.get(method).value : 0;
    }

    /**
     * Sorts the method counts by the value and returns an unmodifiable map for it
     *
     * @return
     */
    public Map<String, Integer> sortByValue() {
        HashMap<String, Integer> out = new HashMap<>();
        for (Entry<String, VariableInteger> e : counts.entrySet()) {
            out.put(e.getKey(), e.getValue().value);
        }
        return Collections.unmodifiableMap(sortByComparator(out, false));
    }

    /**
     * Handle increment / decrement
     *
     * @param method
     * @param delta
     */
    private void deltaMethod(String method, int delta) {
        VariableInteger value = counts.get(method);
        if (value == null) {
            counts.put(method, new VariableInteger(delta));
            return;
        }

        value.value = value.value + delta;
    }

    /**
     * Sort a hashmap by the values
     * <p/>
     * credits: http://stackoverflow.com/questions/8119366/sorting-hashmap-by-values/13913206#13913206
     *
     * @param unsortMap
     * @param order
     * @return
     */
    private static Map<String, Integer> sortByComparator(Map<String, Integer> unsortMap, final boolean order) {
        ArrayList<Map.Entry<String, Integer>> list = new ArrayList<Map.Entry<String, Integer>>(unsortMap.entrySet());

        // Sorting the list based on values
        Collections.sort(list, new Comparator<Map.Entry<String, Integer>>() {
            public int compare(Map.Entry<String, Integer> o1,
                    Map.Entry<String, Integer> o2) {
                if (order) {
                    return o1.getValue().compareTo(o2.getValue());
                } else {
                    return o2.getValue().compareTo(o1.getValue());

                }
            }
        });

        // Maintaining insertion order with the help of LinkedList
        Map<String, Integer> sortedMap = new LinkedHashMap<String, Integer>();
        for (Map.Entry<String, Integer> entry : list) {
            sortedMap.put(entry.getKey(), entry.getValue());
        }

        return sortedMap;
    }

    private class VariableInteger {
        public int value;

        public VariableInteger(int value) {
            this.value = value;
        }
    }
}
