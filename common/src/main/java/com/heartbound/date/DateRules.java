package com.heartbound.date;

import com.heartbound.talk.PartnerTalkRules.Period;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Rules for dates: what kinds there are, when the partner suggests which, how long they last and what
 * counts as a success. Pure Java, no Minecraft dependencies; the lines are in the language files.
 */
public final class DateRules {

    public static final int WALK_TICKS = 1800;
    public static final int STAY_TICKS = 600;
    public static final int TIMEOUT_TICKS = 6000;
    public static final int TOGETHER_PERCENT = 70;
    public static final double TOGETHER_DISTANCE = 12.0D;
    public static final double WAIT_DISTANCE = 14.0D;
    public static final double ABANDON_DISTANCE = 40.0D;
    public static final double ARRIVE_MOB_DISTANCE = 6.0D;
    public static final double ARRIVE_PLAYER_DISTANCE = 9.0D;
    public static final int WALK_LINES = 3;

    private static final String PREFIX = "date.heartbound.";

    private DateRules() {
    }

    public enum DateType {
        /** Walks next to the player for a while. */
        WALK,
        /** Leads the player to a viewpoint nearby. */
        PLACE,
        /** Goes to the home point and stays there with the player. */
        HOME;

        public String key() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public enum FailReason {
        /** The player went away. */
        ABANDONED,
        /** The partner could not get there or something got in the way. */
        LOST,
        /** The partner was hurt. */
        HURT;

        public String key() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    /**
     * What the partner suggests, or null if nothing fits (outdoors only by day in the Overworld without
     * rain, at home if there is a home point and it is evening, night or raining).
     *
     * @param roll a random number from 0 to 99
     */
    public static DateType chooseType(Period period, boolean raining, boolean hasHome, boolean overworld, int roll) {
        boolean outdoors = overworld && !raining && (period == Period.MORNING || period == Period.DAY);
        if (hasHome && (!outdoors || roll < 25)) {
            return DateType.HOME;
        }
        if (!outdoors) {
            return null;
        }
        return roll < 60 ? DateType.WALK : DateType.PLACE;
    }

    public static int successGain(DateType type) {
        return type == DateType.PLACE ? 12 : 10;
    }

    public static int failGain(FailReason reason) {
        return reason == FailReason.ABANDONED ? -2 : 0;
    }

    /** A walk counts if the player stayed near the partner for most of the time. */
    public static boolean walkSucceeded(int togetherTicks, int totalTicks) {
        return totalTicks > 0 && togetherTicks * 100L >= (long) totalTicks * TOGETHER_PERCENT;
    }

    public static String startKey(DateType type) {
        return PREFIX + "start." + type.key();
    }

    public static String arriveKey(DateType type) {
        return PREFIX + "arrive." + type.key();
    }

    public static String walkKey(int index) {
        return PREFIX + "walk." + index;
    }

    public static String successKey(DateType type) {
        return PREFIX + "success." + type.key();
    }

    public static String failKey(FailReason reason) {
        return PREFIX + "fail." + reason.key();
    }

    /** Every key that must exist in the language files. */
    public static List<String> allKeys() {
        List<String> keys = new ArrayList<>();
        for (DateType type : DateType.values()) {
            keys.add(startKey(type));
            keys.add(successKey(type));
            if (type != DateType.WALK) {
                keys.add(arriveKey(type));
            }
        }
        for (int i = 0; i < WALK_LINES; i++) {
            keys.add(walkKey(i));
        }
        for (FailReason reason : FailReason.values()) {
            keys.add(failKey(reason));
        }
        keys.add("event.heartbound.first_date");
        return keys;
    }
}
