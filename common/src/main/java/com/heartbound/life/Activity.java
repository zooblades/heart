package com.heartbound.life;

import com.heartbound.gesture.Personality;

import java.util.List;

/** Small things a mob does near a player it knows. Pure data, chosen by the mob's character. */
public enum Activity {
    WANDER,
    STAY_CLOSE,
    PLAY,
    WATCH_FROM_AFAR,
    PATROL;

    public static List<Activity> forPersonality(Personality personality) {
        return switch (personality) {
            case SHY -> List.of(WATCH_FROM_AFAR, STAY_CLOSE);
            case NEUTRAL -> List.of(STAY_CLOSE, WANDER);
            case BOLD -> List.of(PATROL, WANDER);
            case PLAYFUL -> List.of(PLAY, WANDER);
        };
    }
}
