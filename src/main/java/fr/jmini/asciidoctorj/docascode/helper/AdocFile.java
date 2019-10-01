package fr.jmini.asciidoctorj.docascode.helper;

import java.nio.file.Path;

public class AdocFile {

    private Path file;
    private int endHeaderStartPosition;

    public AdocFile(Path file, int endHeaderStartPosition) {
        this.file = file;
        this.endHeaderStartPosition = endHeaderStartPosition;
    }

    public Path getFile() {
        return file;
    }

    public int getEndHeaderStartPosition() {
        return endHeaderStartPosition;
    }
}
