package fr.jmini.asciidoctorj.docascode.helper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import fr.jmini.utils.substringfinder.Range;
import fr.jmini.utils.substringfinder.SubstringFinder;

public class DocAsCodeHelper {

    private static final Pattern TITLE_REGEX = Pattern.compile("={1,5}(.+)");
    private static final Pattern ALIAS_REGEX = Pattern.compile("// *ALIAS:(.+)");

    private static final String ROOT_COMMENT = "// {root} must point to the `docs/` folder:";
    private static final String INIT_COMMENT = "// init this page in case of standalone display:";
    private static final String IMGS_COMMENT = "// init {imgs} in case of standalone display:";

    private static final String REFERENCES_START = "//references start";
    private static final String REFERENCES_END = "//references end";

    public static void updateInitFiles(Path docsFolder, List<InitInput> initInputs) throws IOException {
        List<AdocFile> adocFiles = Files.walk(docsFolder)
                .filter(f -> f.toFile()
                        .isFile()
                        && f.toFile()
                                .getName()
                                .endsWith("adoc"))
                .map(f -> DocAsCodeHelper.createAdocFile(f))
                .filter(a -> a.isPresent())
                .map(Optional::get)
                .collect(Collectors.toList());
        initInputs.forEach(i -> replaceReferences(docsFolder, i, adocFiles));
    }

    private static void replaceReferences(Path docsFolder, InitInput initInput, List<AdocFile> adocFiles) {
        String content = DocAsCodeHelper.readFile(initInput.getInitFile());

        SubstringFinder finder = SubstringFinder.define(REFERENCES_START, REFERENCES_END);
        Optional<Range> findRange = finder.nextRange(content);
        if (findRange.isPresent()) {
            Range range = findRange.get();

            String referencesContent = createReferences(docsFolder, adocFiles, initInput.getMode());
            String newContent = content.substring(0, range.getRangeStart()) + referencesContent + content.substring(range.getRangeEnd());
            DocAsCodeHelper.writeFile(initInput.getInitFile(), newContent);
        } else {
            throw new IllegalStateException("Could not find range delimited by '" + REFERENCES_START + "' and '" + REFERENCES_END + "' ");
        }
    }

    static String createReferences(Path docsFolder, List<AdocFile> files, InitInput.Mode mode) {
        StringBuilder sb = new StringBuilder();
        sb.append(REFERENCES_START + "\n");
        Map<String, List<TitleAndFileHolder>> titles = files.stream()
                .flatMap(a -> {
                    return a.getAllTitles()
                            .stream()
                            .map(t -> new TitleAndFileHolder(t, a.getFile()));
                })
                .collect(Collectors.groupingBy(h -> DocAsCodeHelper.toAttributeName(h.title)));

        titles.entrySet()
                .stream()
                .sorted(Comparator.comparing(Map.Entry::getKey))
                .forEach(entry -> {
                    List<TitleAndFileHolder> list = entry.getValue()
                            .stream()
                            .sorted(Comparator.comparing(TitleAndFileHolder::getTitle)
                                    .thenComparing(TitleAndFileHolder::getFile))
                            .collect(Collectors.toList());
                    int i = 0;
                    for (TitleAndFileHolder element : list) {
                        Path subPath = docsFolder.relativize(element.file);
                        String key;
                        switch (i) {
                        case 0:
                            key = entry.getKey();
                            break;
                        case 1:
                            key = "_" + entry.getKey();
                            break;
                        default:
                            key = "_" + "_" + entry.getKey() + "_" + i;
                            break;
                        }
                        String value;
                        switch (mode) {
                        case TEXT:
                            value = element.title;
                            break;
                        default:
                        case LINK:
                            value = "<<{root}" + subPath + "#, " + element.title + ">>";
                            break;
                        }
                        sb.append(toReferenceDefinition(key, value));
                        i = i + 1;
                    }
                });
        sb.append(REFERENCES_END);
        return sb.toString();
    }

    private static String toReferenceDefinition(String key, String value) {
        return ":" + key + ": " + value + "\n";
    }

    public static void sanitizeHeaderInFiles(Path docsFolder) throws IOException {
        Files.walk(docsFolder)
                .filter(f -> f.toFile()
                        .isFile()
                        && f.toFile()
                                .getName()
                                .endsWith("adoc"))
                .forEach(f -> replaceHeader(docsFolder, f));
    }

    private static void replaceHeader(Path docsFolder, Path file) {
        String content = readFile(file);

        Optional<AdocFile> adocFile = createAdocFile(file, content);
        if (adocFile.isPresent()) {
            adocFile.get();

            Path relPathToDocs = file.getParent()
                    .relativize(docsFolder);
            String rootDef = (relPathToDocs.toString()
                    .isEmpty()) ? "" : relPathToDocs.toString() + "/";
            StringBuilder sb = new StringBuilder();
            sb.append(ROOT_COMMENT + "\n");
            sb.append("ifndef::root[]\n");
            sb.append(":root: " + rootDef + "\n");
            sb.append("endif::[]\n");
            sb.append("\n");
            sb.append(INIT_COMMENT + "\n");
            sb.append("ifndef::init[]\n");
            sb.append("include::{root}_init.adoc[]\n");
            sb.append("endif::[]\n");
            sb.append("\n");
            sb.append(IMGS_COMMENT + "\n");
            sb.append("ifndef::imgs[]\n");
            sb.append(":imgs: {root}imgs/\n");
            sb.append("endif::[]\n");
            sb.append("\n");
            if (!adocFile.get()
                    .getAliases()
                    .isEmpty()) {
                for (String alias : adocFile.get()
                        .getAliases()) {
                    sb.append("//ALIAS: " + alias + "\n");
                }
                sb.append("\n");
            }

            int titleStartPosition = adocFile.get()
                    .getTitleStartPosition();

            //Keep existing comments:
            String[] lines = content.substring(0, titleStartPosition)
                    .split("\\r?\\n");
            List<String> comments = Arrays.stream(lines)
                    .filter(s -> !Objects.equals(ROOT_COMMENT, s))
                    .filter(s -> !Objects.equals(INIT_COMMENT, s))
                    .filter(s -> !Objects.equals(IMGS_COMMENT, s))
                    .filter(s -> !s.startsWith("//ALIAS"))
                    .filter(s -> s.startsWith("//"))
                    .collect(Collectors.toList());
            for (String c : comments) {
                sb.append(c + "\n");
            }

            String newContent = sb.toString() + content.substring(titleStartPosition);

            writeFile(file, newContent);
        }
    }

    static Optional<AdocFile> createAdocFile(Path file) {
        String content = readFile(file);

        return createAdocFile(file, content);
    }

    static Optional<AdocFile> createAdocFile(Path file, String content) {
        Matcher titleMatcher = TITLE_REGEX.matcher(content);
        if (!titleMatcher.find()) {
            return Optional.empty();
        }
        String title = titleMatcher.group(1)
                .trim();
        int titleStart = titleMatcher.start();

        List<String> aliases = new ArrayList<>();
        Matcher aliasMatcher = ALIAS_REGEX.matcher(content);
        while (aliasMatcher.find() && aliasMatcher.start() < titleStart) {
            String alias = aliasMatcher.group(1)
                    .trim();
            aliases.add(alias);
        }

        return Optional.of(new AdocFile(file, title, aliases, titleStart));
    }

    static String toAttributeName(String title) {
        String name = title;
        name = name.replace("&", "");
        name = name.replace("-", "");
        name = name.replace("'", "");
        name = name.replace("(", " ");
        name = name.replace(")", " ");
        name = name.replace("/", " ");
        name = name.replace("\"", " ");
        name = name.replaceAll(" +", " ");
        name = name.trim();
        name = name.replace(" ", "-");
        return name.toLowerCase();
    }

    static String readFile(Path file) {
        String content;
        try {
            content = new String(Files.readAllBytes(file));
        } catch (IOException e) {
            e.printStackTrace();
            throw new IllegalStateException("Could not read file: " + file, e);
        }
        return content;
    }

    static void writeFile(Path file, String content) {
        try {
            Files.write(file, content.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            e.printStackTrace();
            throw new IllegalStateException("Could not write file: " + file, e);
        }
    }

    private static class TitleAndFileHolder {
        private String title;
        private Path file;

        public TitleAndFileHolder(String title, Path file) {
            this.title = title;
            this.file = file;
        }

        public String getTitle() {
            return title;
        }

        public Path getFile() {
            return file;
        }

    }
}
