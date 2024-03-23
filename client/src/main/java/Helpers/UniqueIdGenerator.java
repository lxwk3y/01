package main.java.Helpers;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class  UniqueIdGenerator {
    private static Set<Integer> generatedIds;
    private Random random;

    public UniqueIdGenerator() {
        generatedIds = new HashSet<>();
        random = new Random();
    }

    public Integer generateUniqueId() {
        if (generatedIds.size() == 900) {
            throw new IllegalStateException("Alle 3 cijferige ID's zijn al gegenereerd");
        }

        int uniqueId;
        do {
            uniqueId = 100 + random.nextInt(900);
        } while (generatedIds.contains(uniqueId));

        generatedIds.add(uniqueId);
        return uniqueId;
    }

    public static Set<Integer> getGeneratedIds() {
        return generatedIds;
    }
}
