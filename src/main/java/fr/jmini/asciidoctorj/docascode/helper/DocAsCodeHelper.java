package fr.jmini.asciidoctorj.docascode.helper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class DocAsCodeHelper {

    private static final Pattern TITLE_REGEX = Pattern.compile("/{0,2}?={1,5}(.*)");
    private static final String ROOT_COMMENT_PREFIX = "// {root} must point to the ";
    private static final String INIT_COMMENT = "// init this page in case of standalone display:";
    private static final String IMGS_COMMENT = "// init {imgs} in case of standalone display:";

    private static String relativizeAndNormalize(Path path, Path other) {
        return path.relativize(other)
                .toString()
                .replace('\\', '/');
    }

    private static String toReferenceDefinition(String key, String value) {
        return ":" + key + ": " + value + "\n";
    }

    public static void sanitizeHeaderInFiles(Path docsFolder, String initExpression, String imgsExpression) throws IOException {
        Files.walk(docsFolder)
                .filter(f -> f.toFile()
                        .isFile()
                        && f.toFile()
                                .getName()
                                .endsWith("adoc"))
                .forEach(f -> replaceHeader(docsFolder, f, initExpression, imgsExpression));
    }

    private static void replaceHeader(Path docsFolder, Path file, String initExpression, String imgsExpression) {
        String content = readFile(file);

        Optional<AdocFile> adocFile = createAdocFile(file, content);
        if (adocFile.isPresent()) {
            adocFile.get();

            String relPathToDocs = relativizeAndNormalize(file.getParent(), docsFolder);
            String rootDef = (relPathToDocs.toString()
                    .isEmpty()) ? "" : relPathToDocs.toString() + "/";
            StringBuilder sb = new StringBuilder();
            sb.append(ROOT_COMMENT_PREFIX + "`" + docsFolder.getFileName() + "/` folder:\n");
            sb.append("ifndef::root[]\n");
            sb.append(":root: " + rootDef + "\n");
            sb.append("endif::[]\n");
            sb.append("\n");
            if (initExpression != null) {
                sb.append(INIT_COMMENT + "\n");
                sb.append("ifndef::init[]\n");
                sb.append(initExpression + "\n");
                sb.append("endif::[]\n");
                sb.append("\n");
            }
            if (imgsExpression != null) {
                sb.append(IMGS_COMMENT + "\n");
                sb.append("ifndef::imgs[]\n");
                sb.append(imgsExpression + "\n");
                sb.append("endif::[]\n");
                sb.append("\n");
            }

            int titleStartPosition = adocFile.get()
                    .getTitleStartPosition();

            //Keep existing comments:
            String[] lines = content.substring(0, titleStartPosition)
                    .split("\\r?\\n");
            List<String> comments = Arrays.stream(lines)
                    .filter(s -> !s.startsWith(ROOT_COMMENT_PREFIX))
                    .filter(s -> !Objects.equals(INIT_COMMENT, s))
                    .filter(s -> !Objects.equals(IMGS_COMMENT, s))
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

        return Optional.of(new AdocFile(file, title, titleStart));
    }

    static String readFile(Path file) {
        String content;
        try {
            content = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
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
