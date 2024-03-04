package ibb.api.geneservice.utils;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class SpeciesHelperTest {
    
    @ParameterizedTest
    @CsvSource({"Tcas,1.Tcas", "2.Dmel, Dmel"})
    public void testIsSameSpecies(String species1, String species2) {
        assert(SpeciesHelper.isSameSpecies(species1, species2));
    }
}
