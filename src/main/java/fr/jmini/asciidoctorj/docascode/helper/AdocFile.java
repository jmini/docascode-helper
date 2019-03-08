package fr.jmini.asciidoctorj.docascode.helper;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class AdocFile {

    private Path file;
    private String title;
    private List<String> aliases;
    private int titleStartPosition;

    public AdocFile(Path file, String title, List<String> aliases, int titleStartPosition) {
        this.file = file;
        this.title = title;
        this.aliases = aliases;
        this.titleStartPosition = titleStartPosition;
    }

    public Path getFile() {
        return file;
    }

    public String getTitle() {
        return title;
    }

    public List<String> getAliases() {
        return aliases;
    }

    public List<String> getAllTitles() {
        List<String> list = new ArrayList<>();
        list.add(title);
        list.addAll(aliases);
        return list;
    }

    public int getTitleStartPosition() {
        return titleStartPosition;
    }
}
