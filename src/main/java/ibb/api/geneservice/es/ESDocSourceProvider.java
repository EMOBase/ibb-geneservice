package ibb.api.geneservice.es;

import java.util.stream.Stream;

public interface ESDocSourceProvider<T extends ESDoc> {
    Stream<ESDocSource<T>> provideDocSources();
}
