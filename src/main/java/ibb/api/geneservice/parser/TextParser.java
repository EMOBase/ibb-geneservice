package ibb.api.geneservice.parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PushbackInputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;


public interface TextParser<T> {

    /**
     * @implNote The returned stream must have a close handler that closes the underlying file.
     */
    Stream<T> parse(Path path) throws IOException;

    default Stream<String> parseText(Path path) throws IOException {
        var inputStream = getInputStream(path.toString());
        var br = new BufferedReader(new InputStreamReader(inputStream));
        return br.lines().onClose(() -> {
            try {
                br.close();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    private static InputStream getInputStream(String localPath) throws IOException {
        InputStream input = Files.newInputStream(Paths.get(localPath));
        return tryDecompress(input);
    }

    private static InputStream tryDecompress(InputStream input) throws IOException {
        PushbackInputStream pb = new PushbackInputStream(input, 2);
        byte[] signature = new byte[2];
        int len = pb.read(signature);

        if (len > 0) {
            pb.unread(signature, 0, len);
        }

        if (isGzipped(signature)) {
            return new GZIPInputStream(pb);
        } else {
            return pb;
        }
    }

    private static boolean isGzipped(byte[] data) {
        return data != null && data.length >= 2 && (data[0] & 0xff
                | (data[1] << 8) & 0xff00) == GZIPInputStream.GZIP_MAGIC;
    }
}
