package fr.jmini.asciidoctorj.docascode.helper;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

class DocAsCodeHelperTest {

    private static final Path DOCS = Paths.get("src/test/resources/example-setup/docs");
    private static final Path INDEX_FILE = DOCS.resolve("index.adoc");
    private static final Path LOREM_FILE = DOCS.resolve("concepts/lorem.adoc");
    private static final Path IPSUM_FILE = DOCS.resolve("concepts/ipsum.adoc");
    private static final Path DOLOR_FILE = DOCS.resolve("concepts/dolor.adoc");
    private static final Path SITAMET_FILE = DOCS.resolve("concepts/sitamet.adoc");
    private static final Path OTHER = Paths.get("src/test/resources/other");
    private static final Path OTHER_FILE = OTHER.resolve("index.adoc");
    private static final Path HEADER_MISSING = Paths.get("src/test/resources/header-missing");
    private static final Path HEADER_MISSING_FILE = HEADER_MISSING.resolve("index.adoc");

    private static final Pattern PATTERN = Pattern.compile("// end header");

    @Test
    void testCreateAdocFile() {
        AdocFile lorem = createAdocFile(LOREM_FILE);
        assertThat(lorem).isNotNull();
        assertThat(lorem.getFile()).isEqualTo(LOREM_FILE);

        AdocFile ipsum = createAdocFile(IPSUM_FILE);
        assertThat(ipsum).isNotNull();
        assertThat(ipsum.getFile()).isEqualTo(IPSUM_FILE);

        AdocFile dolor = createAdocFile(DOLOR_FILE);
        assertThat(dolor).isNotNull();
        assertThat(dolor.getFile()).isEqualTo(DOLOR_FILE);

        AdocFile sitamet = createAdocFile(SITAMET_FILE);
        assertThat(sitamet).isNotNull();
        assertThat(sitamet.getFile()).isEqualTo(SITAMET_FILE);
    }

    private AdocFile createAdocFile(Path file) {
        String content = DocAsCodeHelper.readFile(file);
        return DocAsCodeHelper.createAdocFile(PATTERN, file, content);
    }

    @Test
    void testSanitizeHeaderInFiles() throws Exception {
        String indexOriginal = DocAsCodeHelper.readFile(INDEX_FILE);
        String loremOriginal = DocAsCodeHelper.readFile(LOREM_FILE);
        String ipsumOriginal = DocAsCodeHelper.readFile(IPSUM_FILE);
        String dolorOriginal = DocAsCodeHelper.readFile(DOLOR_FILE);
        String sitametOriginal = DocAsCodeHelper.readFile(SITAMET_FILE);

        DocAsCodeHelper.sanitizeHeaderInFiles(DOCS, "include::{root}../_init.adoc[]", null);

        String index = DocAsCodeHelper.readFile(INDEX_FILE);
        String lorem = DocAsCodeHelper.readFile(LOREM_FILE);
        String ipsum = DocAsCodeHelper.readFile(IPSUM_FILE);
        String dolor = DocAsCodeHelper.readFile(DOLOR_FILE);
        String sitamet = DocAsCodeHelper.readFile(SITAMET_FILE);

        assertThat(index).isEqualTo(indexOriginal);
        assertThat(lorem).isEqualTo(loremOriginal);
        assertThat(ipsum).isEqualTo(ipsumOriginal);
        assertThat(dolor).isEqualTo(dolorOriginal);
        assertThat(sitamet).isEqualTo(sitametOriginal);
    }

    @Test
    void testSanitizeHeaderInFilesOther() throws Exception {
        String indexOriginal = DocAsCodeHelper.readFile(OTHER_FILE);

        DocAsCodeHelper.sanitizeHeaderInFiles(OTHER, ":init: OK", ":imgs: test", "END-HEADER");

        String index = DocAsCodeHelper.readFile(OTHER_FILE);

        assertThat(index).isEqualTo(indexOriginal);
    }

    @Test
    void testSanitizeHeaderInFilesHeaderMissing() throws Exception {
        String indexOriginal = DocAsCodeHelper.readFile(HEADER_MISSING_FILE);

        String content = "== Test\n" +
                "\n" +
                "This is a test\n";
        DocAsCodeHelper.writeFile(HEADER_MISSING_FILE, content);

        DocAsCodeHelper.sanitizeHeaderInFiles(HEADER_MISSING, "include::{root}../_init.adoc[]", null);

        String index = DocAsCodeHelper.readFile(HEADER_MISSING_FILE);

        assertThat(index).isEqualTo(indexOriginal);
    }
}
