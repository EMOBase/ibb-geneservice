package ibb.api.geneservice.utils;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonValue;

public class Species {

    private String fourLetterCode;

    /**
     * Create a new Species object from a raw value. The raw value must be in the format 'Genus_species' or 'Genus species' or 4-letter code.
     * @param rawValue
     * @return
     * @throws IllegalArgumentException if the raw value is not in the correct format
     */
    public static Species of(String rawValue) {
        return new Species(rawValue);
    }

    public static Species ofGene(String gene) {
        String[] parts = gene.split(":", 2);
        if (parts.length != 2) {
            throw new IllegalArgumentException("Gene must be in the format 'Species:geneId'");
        }
        return new Species(parts[0]);
    }

    public Species(String rawValue) {
        Objects.requireNonNull(rawValue);
        if (rawValue.length() < 4) {
            throw new IllegalArgumentException("Species must be at least 4 characters long");
        } else if (rawValue.length() > 4) {
            String[] parts = rawValue.replace(" ", "_").split("_");
            if (parts.length != 2) {
                throw new IllegalArgumentException("Species must be in the format 'Genus_species' or 'Genus species' or 4-letter code");
            }
            fourLetterCode = parts[0].substring(0, 1).toUpperCase() + parts[1].substring(0, 3).toLowerCase();
        } else {
            fourLetterCode = capitalize(rawValue);
        }
    }

    private static String capitalize(String str) {
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }

    public String createGeneId(String gene) {
        return fourLetterCode + ":" + gene;
    }

    public boolean isGeneFromSpecies(String gene) {
        return gene.startsWith(fourLetterCode + ":");
    }

    public String removeSpeciesFromGene(String gene) {
        return gene.replace(fourLetterCode + ":", "");
    }

    @JsonValue
    @Override
    public String toString() {
        return fourLetterCode;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (obj instanceof Species) {
            Species other = (Species) obj;
            return Objects.equals(fourLetterCode, other.fourLetterCode);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(fourLetterCode);
    }
}
