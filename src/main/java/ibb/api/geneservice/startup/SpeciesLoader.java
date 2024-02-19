package ibb.api.geneservice.startup;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import ibb.api.geneservice.domains.genomic.GenomicIndex;
import ibb.api.geneservice.domains.sequence.SequenceIndex;
import ibb.api.geneservice.domains.sequence.SequenceType;
import ibb.api.geneservice.domains.synonym.FlyBaseGeneRNAProteinMapParser;
import ibb.api.geneservice.domains.synonym.FlyBaseSynonymParser;
import ibb.api.geneservice.domains.synonym.SynonymIndex;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class SpeciesLoader {
    
    @Inject
    GenomicIndex geneIndex;

    @Inject
    SequenceIndex sequenceIndex;

    @Inject
    SynonymIndex synonymIndex;

    public void load(String species, File dir) {
        File[] files = dir.listFiles(File::isFile);

        Optional.of(Arrays.stream(files).filter(FileTypes::isGFFFile).toList())
            .filter(gffs -> !gffs.isEmpty())
            .ifPresent(gffs -> loadGFFs(species, gffs));

        Optional.of(Arrays.stream(files).filter(FileTypes::isCDSFile).toList())
            .filter(cds -> !cds.isEmpty())
            .ifPresent(cds -> loadSequences(species, SequenceType.CDS, cds));
        
        Optional.of(Arrays.stream(files).filter(FileTypes::isRNAFile).toList())
            .filter(rnas -> !rnas.isEmpty())
            .ifPresent(rnas -> loadSequences(species, SequenceType.RNA, rnas));

        Optional.of(Arrays.stream(files).filter(FileTypes::isProteinFile).toList())
            .filter(proteins -> !proteins.isEmpty())
            .ifPresent(proteins -> loadSequences(species, SequenceType.PROTEIN, proteins));

        if (Objects.equals(species, "drosophila_melanogaster")) {
            Optional.of(
                Arrays.stream(files)
                    .filter(file -> FileTypes.isFlyBaseSynonymFile(file) || FileTypes.isFlyBaseGeneRNAProteinMapFile(file))
                    .toList()
            )
                .filter(synonyms -> !synonyms.isEmpty())
                .ifPresent(synonyms -> loadFlyBaseSynonyms(species, synonyms));
        }
    }

    private void loadGFFs(String species, List<File> files) {
        try {
            if (!geneIndex.exists(species)) {
                geneIndex.createIndex(species);
                for (var file : files) {
                    geneIndex.loadFromGFF(species, file.toPath());
                };
            } else {
                Log.infov("Gene index for species {0} already exists", species);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void loadSequences(String species, SequenceType type, List<File> files) {
        try {
            if (!sequenceIndex.exists(species, type)) {
                sequenceIndex.createIndex(species, type);
                for (var file : files) {
                    sequenceIndex.loadFromFasta(species, type, file.toPath());
                };
            } else {
                Log.infov("{0} index for species {1} already exists", type, species);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void loadFlyBaseSynonyms(String species, List<File> files) {
        try {
            if (!synonymIndex.exists(species)) {
                synonymIndex.createIndex(species);
            
                for (var file : files) {
                    if (FileTypes.isFlyBaseSynonymFile(file)) {
                        synonymIndex.loadSynonyms(species, file.toPath(), new FlyBaseSynonymParser());
                    } else if (FileTypes.isFlyBaseGeneRNAProteinMapFile(file)) {
                        synonymIndex.loadSynonyms(species, file.toPath(), new FlyBaseGeneRNAProteinMapParser());
                    }
                }
            } else {
                Log.infov("Synonym index for species {0} already exists", species);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
