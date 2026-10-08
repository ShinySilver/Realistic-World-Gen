package rwg.registry;

import java.util.EnumSet;

public enum TerrainCategory {

    PLAIN,
    HILLS,
    PLATEAU,
    MOUNTAIN,
    VALLEY,
    CANYON,
    SWAMP,
    SMALL_ISLAND,
    MEDIUM_ISLAND,
    LARGE_ISLAND,
    RIVER,
    CLIFF,
    WETLANDS,
    SHALLOW_OCEAN,
    DEEP_OCEAN;

    /** Builds a category set for biome registrations that are valid in more than one terrain category. */
    public EnumSet<TerrainCategory> or(TerrainCategory... others) {
        EnumSet<TerrainCategory> categories = EnumSet.of(this);
        for (TerrainCategory other : others) categories.add(other);
        return categories;
    }
}
