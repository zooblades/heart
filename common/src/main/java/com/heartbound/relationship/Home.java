package com.heartbound.relationship;

/** A home point: dimension id (e.g. "minecraft:overworld") and a packed block position. */
public record Home(String dimension, long pos) {
}
