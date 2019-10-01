package fr.jmini.asciidoctorj.docascode.helper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DocAsCodeHelper {

    private static final String ROOT_COMMENT_PREFIX = "// {root} must point to the ";
    private static final String INIT_COMMENT = "// init this page in case of standalone display:";
    private static final String IMGS_COMMENT = "// init {imgs} in case of standalone display:";

    private static String relativizeAndNormalize(Path path, Path other) {
        return path.relativize(other)
                .toString()
                .replace('\\', '/');
    }

    public static void sanitizeHeaderInFiles(Path docsFolder, String initExpression, String imgsExpression) throws IOException {
        sanitizeHeaderInFiles(docsFolder, initExpression, imgsExpression, " end header");
    }

    public static void sanitizeHeaderInFiles(Path docsFolder, String initExpression, String imgsExpression, String headerEnd) throws IOException {
        Pattern headerEndPattern = Pattern.compile("//" + headerEnd);

        Files.walk(docsFolder)
                .filter(f -> f.toFile()
                        .isFile()
                        && f.toFile()
                                .getName()
                                .endsWith("adoc"))
                .forEach(f -> replaceHeader(docsFolder, f, initExpression, imgsExpression, headerEnd, headerEndPattern));
    }

    private static void replaceHeader(Path docsFolder, Path file, String initExpression, String imgsExpression, String headerEnd, Pattern headerEndPattern) {
        String content = readFile(file);

        AdocFile adocFile = createAdocFile(headerEndPattern, file, content);

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

        int endHeaderStartPosition = adocFile.getEndHeaderStartPosition();

        String newContent;
        if (endHeaderStartPosition > 0) {
            //Keep existing comments:
            String[] lines = content.substring(0, endHeaderStartPosition)
                    .split("\\r?\\n");
            Arrays.stream(lines)
                    .filter(s -> !s.startsWith(ROOT_COMMENT_PREFIX))
                    .filter(s -> !Objects.equals(INIT_COMMENT, s))
                    .filter(s -> !Objects.equals(IMGS_COMMENT, s))
                    .filter(s -> s.startsWith("//"))
                    .forEach(c -> {
                        sb.append(c + "\n");
                    });

            newContent = sb.toString() + content.substring(endHeaderStartPosition);
        } else {
            sb.append("//" + headerEnd + "\n\n");
            newContent = sb.toString() + content;
        }

        writeFile(file, newContent);
    }

    static AdocFile createAdocFile(Pattern pattern, Path file, String content) {
        Matcher matcher = pattern.matcher(content);
        if (!matcher.find()) {
            return new AdocFile(file, 0);
        }
        int start = matcher.start();

        return new AdocFile(file, start);
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
