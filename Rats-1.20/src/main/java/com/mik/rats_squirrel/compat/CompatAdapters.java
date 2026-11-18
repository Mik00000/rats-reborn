package com.mik.rats_squirrel.compat;

import com.mik.rats_squirrel.fruit.FruitAdapter;
import net.neoforged.fml.ModList;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds the adapter chain from the optional mods that are actually loaded.
 *
 * <p>No optional-mod classes are imported directly. Adapters use registry ids, block tags and
 * block-state properties so that a missing mod can never cause a class-load failure.</p>
 */
public final class CompatAdapters {
    private CompatAdapters() {
    }

    /** Returns the adapter chain in most-specific to most-generic order. */
    public static List<FruitAdapter> createDefaults() {
        List<FruitAdapter> adapters = new ArrayList<>();

        if (loaded("croptopia")) {
            adapters.add(new CroptopiaFruitAdapter());
        }
        if (loaded("fruitsdelight")) {
            adapters.add(new FruitsDelightAdapter());
        }
        if (loaded("culturaldelights")) {
            adapters.add(new CulturalDelightsAdapter());
        }
        if (loaded("create_winery")) {
            adapters.add(new CreateWineryAdapter());
        }
        if (loaded("cocktailsdelight")) {
            adapters.add(new CocktailsDelightAdapter());
        }

        adapters.add(new VanillaFruitAdapter());
        adapters.add(new GenericFruitAdapter());
        return List.copyOf(adapters);
    }

    /** Null-safe mod-loaded check. */
    public static boolean loaded(String modId) {
        ModList modList = ModList.get();
        return modList != null && modList.isLoaded(modId);
    }
}
