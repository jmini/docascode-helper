package fr.jmini.asciidoctorj.docascode.helper;

import java.nio.file.Path;

public class AdocFile {

    private Path file;
    private String title;
    private int titleStartPosition;

    public AdocFile(Path file, String title, int titleStartPosition) {
        this.file = file;
        this.title = title;
        this.titleStartPosition = titleStartPosition;
    }

    public Path getFile() {
        return file;
    }

    public String getTitle() {
        return title;
    }

    public int getTitleStartPosition() {
        return titleStartPosition;
    }
}
