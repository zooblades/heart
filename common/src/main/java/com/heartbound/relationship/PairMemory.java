package com.heartbound.relationship;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * What one mob remembers about one player: the first time each significant event happened (in order),
 * and a few counters. Pure Java.
 */
public final class PairMemory {

    public enum Counter {
        GIFTS,
        TALKS,
        GESTURES,
        MORNINGS
    }

    public record Entry(EventType type, long day, long pos, int extra) {
    }

    private final List<Entry> entries = new ArrayList<>();
    private final int[] counters = new int[Counter.values().length];

    public boolean has(EventType type) {
        for (Entry entry : entries) {
            if (entry.type() == type) {
                return true;
            }
        }
        return false;
    }

    /** Records the event unless it already happened. Returns true if it was new. */
    public boolean record(EventType type, long day, long pos, int extra) {
        if (type == null || has(type)) {
            return false;
        }
        entries.add(new Entry(type, day, pos, extra));
        return true;
    }

    public List<Entry> entries() {
        return Collections.unmodifiableList(entries);
    }

    public int count(Counter counter) {
        return counters[counter.ordinal()];
    }

    public void setCount(Counter counter, int value) {
        counters[counter.ordinal()] = Math.max(0, value);
    }

    public void increment(Counter counter) {
        if (counters[counter.ordinal()] < Integer.MAX_VALUE) {
            counters[counter.ordinal()]++;
        }
    }
}
