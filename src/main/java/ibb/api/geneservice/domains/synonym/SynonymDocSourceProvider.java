package ibb.api.geneservice.domains.synonym;

import java.io.File;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Stream;

import ibb.api.geneservice.domains.synonym.parser.FlyBaseGeneRNAProteinMapParser;
import ibb.api.geneservice.domains.synonym.parser.FlyBaseSynonymParser;
import ibb.api.geneservice.domains.synonym.parser.GFF3SynonymParser;
import ibb.api.geneservice.es.ESDocSource;
import ibb.api.geneservice.es.ESDocSourceProvider;
import ibb.api.geneservice.parser.TextParser;
import ibb.api.geneservice.utils.DataLoader;
import ibb.api.geneservice.utils.FileTypeHelper;
import ibb.api.geneservice.utils.SpeciesHelper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class SynonymDocSourceProvider implements ESDocSourceProvider<Synonym> {

    @Inject
    DataLoader dataLoader;

    @Override
    public Stream<ESDocSource<Synonym>> provideDocSources() {
        return dataLoader.listSpeciesDirs()
            .map(this::findDocSources)
            .flatMap(s -> s);
    }

	private Stream<ESDocSource<Synonym>> findDocSources(File speciesDir) {
        String species = speciesDir.getName();
        return Arrays.stream(speciesDir.listFiles(File::isFile))
            .map(file -> {
                TextParser<Synonym> parser = null;
                if (SpeciesHelper.isSameSpecies(species, "Dmel")) {
                    if (isFlyBaseSynonymFile(file)) {
                        parser = new FlyBaseSynonymParser(species);
                    } else if (isFlyBaseGeneRNAProteinMapFile(file)) {
                        parser = new FlyBaseGeneRNAProteinMapParser(species);
                    }
                }
                if (FileTypeHelper.isGFFFile(file)) {
                    parser = new GFF3SynonymParser(species);
                }
                if (parser != null) {
                    return new ESDocSource<>(file, parser);
                }
                return null;
            })
            .filter(Objects::nonNull);
    }

    private boolean isFlyBaseSynonymFile(File file) {
        return file.getName().toLowerCase().startsWith("fb_synonym");
    }

    private boolean isFlyBaseGeneRNAProteinMapFile(File file) {
        return file.getName().toLowerCase().startsWith("fbgn_fbtr_fbpp");
    }
}
