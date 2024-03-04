package ibb.api.geneservice.utils;

public class SpeciesHelper {

    public static boolean isSameSpecies(String speciesA, String speciesB) {
        return speciesWithoutPriorityValue(speciesA)
            .toLowerCase()
            .equals(speciesWithoutPriorityValue(speciesB).toLowerCase());
    }

    public static String speciesWithoutPriorityValue(String species) {
        String[] parts = species.split("\\.", 2);
        if (parts.length == 2) {
            return parts[1];
        } else {
            return species;
        }
    }
}
