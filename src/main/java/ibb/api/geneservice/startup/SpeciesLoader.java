package ibb.api.geneservice.startup;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import ibb.api.geneservice.domains.genomic.GenomicLocation;
import ibb.api.geneservice.domains.genomic.GenomicLocationParser;
import ibb.api.geneservice.domains.sequence.Sequence;
import ibb.api.geneservice.domains.sequence.SequenceParser;
import ibb.api.geneservice.domains.sequence.SequenceType;
import ibb.api.geneservice.domains.synonym.FlyBaseGeneRNAProteinMapParser;
import ibb.api.geneservice.domains.synonym.FlyBaseSynonymParser;
import ibb.api.geneservice.domains.synonym.GFF3SynonymParser;
import ibb.api.geneservice.domains.synonym.Synonym;
import ibb.api.geneservice.index.DocumentSource;
import ibb.api.geneservice.index.IndexManager;
import ibb.api.geneservice.index.IndexType;
import ibb.api.geneservice.parser.GFF3GeneIDFinder;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class SpeciesLoader {
    
    @Inject
    IndexManager indexManager;

    public void load(String species, File dir) {
        File[] files = dir.listFiles(File::isFile);

        indexManager.loadAllIfNotExists(
            getGenomicLocationSources(species, files),
            IndexType.GENOMIC_LOCATION,
            species);

        indexManager.loadAllIfNotExists(
            getSequenceSources(species, files),
            IndexType.SEQUENCE,
            species);
        
        indexManager.loadAllIfNotExists(
            getSynonymSources(species, files),
            IndexType.SYNONYM,
            species);
    }

    private List<DocumentSource<GenomicLocation>> getGenomicLocationSources(String species, File[] files) {
        return Arrays.stream(files)
            .filter(FileTypes::isGFFFile)
            .map(file -> {
                GenomicLocationParser parser;
                if (Objects.equals(species, "tribolium_castaneum")) {
                    parser = new GenomicLocationParser(GFF3GeneIDFinder.byTCLocusTag());
                } else {
                    parser = new GenomicLocationParser();
                }
                return new DocumentSource<>(file, parser);
            }).toList();
    }

    private List<DocumentSource<Sequence>> getSequenceSources(String species, File[] files) {
        return Arrays.stream(files)
            .map(file -> {
                SequenceParser parser = null;
                if (FileTypes.isCDSFile(file)) {
                    parser = new SequenceParser(SequenceType.CDS);
                } else if (FileTypes.isRNAFile(file)) {
                    parser = new SequenceParser(SequenceType.RNA);
                } else if (FileTypes.isProteinFile(file)) {
                    parser = new SequenceParser(SequenceType.PROTEIN);
                }
                if (parser == null) {
                    return null;
                } else {
                    return new DocumentSource<>(file, parser);
                }
            })
            .filter(Objects::nonNull)
            .toList();
    }

    private List<DocumentSource<Synonym>> getSynonymSources(String species, File[] files) {
        return Arrays.stream(files)
            .map(file -> {
                if (Objects.equals("drosophila_melanogaster", species)) {
                    if (FileTypes.isFlyBaseSynonymFile(file)) {
                        return new DocumentSource<>(file, new FlyBaseSynonymParser());
                    } else if (FileTypes.isFlyBaseGeneRNAProteinMapFile(file)) {
                        return new DocumentSource<>(file, new FlyBaseGeneRNAProteinMapParser());
                    }
                }
                if (FileTypes.isGFFFile(file)) {
                    GFF3SynonymParser parser;
                    if (Objects.equals(species, "tribolium_castaneum")) {
                        parser = new GFF3SynonymParser(GFF3GeneIDFinder.byTCLocusTag());
                    } else {
                        parser = new GFF3SynonymParser();
                    }
                    return new DocumentSource<>(file, parser);
                }
                return null;
            })
            .filter(Objects::nonNull)
            .toList();
    }
}
