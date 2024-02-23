package ibb.api.geneservice.startup;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class SpeciesLoader {
    
    // @Inject
    // SourceIndexManager indexManager;

    // public void load(String species, File dir) {
    //     File[] files = dir.listFiles(File::isFile);

    //     indexManager.loadAllIfNotExists(
    //         getGenomicLocationSources(species, files),
    //         SourceIndexType.GENOMIC_LOCATION,
    //         species);

    //     indexManager.loadAllIfNotExists(
    //         getSequenceSources(species, files),
    //         SourceIndexType.SEQUENCE,
    //         species);
        
    //     indexManager.loadAllIfNotExists(
    //         getSynonymSources(species, files),
    //         SourceIndexType.SYNONYM,
    //         species);
    // }

    // private List<DocumentSource<GenomicLocation>> getGenomicLocationSources(String species, File[] files) {
    //     return Arrays.stream(files)
    //         .filter(FileTypes::isGFFFile)
    //         .map(file -> {
    //             GenomicLocationParser parser;
    //             if (Objects.equals(species, "tribolium_castaneum")) {
    //                 parser = new GenomicLocationParser(GFF3GeneIDFinder.byTCLocusTag());
    //             } else {
    //                 parser = new GenomicLocationParser();
    //             }
    //             return new DocumentSource<>(file, parser);
    //         }).toList();
    // }

    // private List<DocumentSource<Sequence>> getSequenceSources(String species, File[] files) {
    //     return Arrays.stream(files)
    //         .map(file -> {
    //             SequenceParser parser = null;
    //             if (FileTypes.isCDSFile(file)) {
    //                 parser = new SequenceParser(SequenceType.CDS);
    //             } else if (FileTypes.isRNAFile(file)) {
    //                 parser = new SequenceParser(SequenceType.RNA);
    //             } else if (FileTypes.isProteinFile(file)) {
    //                 parser = new SequenceParser(SequenceType.PROTEIN);
    //             }
    //             if (parser == null) {
    //                 return null;
    //             } else {
    //                 return new DocumentSource<>(file, parser);
    //             }
    //         })
    //         .filter(Objects::nonNull)
    //         .toList();
    // }

    // private List<DocumentSource<Synonym>> getSynonymSources(String species, File[] files) {
    //     return Arrays.stream(files)
    //         .map(file -> {
    //             if (Objects.equals("drosophila_melanogaster", species)) {
    //                 if (FileTypes.isFlyBaseSynonymFile(file)) {
    //                     return new DocumentSource<>(file, new FlyBaseSynonymParser());
    //                 } else if (FileTypes.isFlyBaseGeneRNAProteinMapFile(file)) {
    //                     return new DocumentSource<>(file, new FlyBaseGeneRNAProteinMapParser());
    //                 }
    //             }
    //             if (FileTypes.isGFFFile(file)) {
    //                 GFF3SynonymParser parser;
    //                 if (Objects.equals(species, "tribolium_castaneum")) {
    //                     parser = new GFF3SynonymParser(GFF3GeneIDFinder.byTCLocusTag());
    //                 } else {
    //                     parser = new GFF3SynonymParser();
    //                 }
    //                 return new DocumentSource<>(file, parser);
    //             }
    //             return null;
    //         })
    //         .filter(Objects::nonNull)
    //         .toList();
    // }
}
