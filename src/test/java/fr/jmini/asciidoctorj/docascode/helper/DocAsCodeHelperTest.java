package fr.jmini.asciidoctorj.docascode.helper;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import org.junit.jupiter.api.Test;

class DocAsCodeHelperTest {

    private static final Path DOCS = Paths.get("src/test/resources/example-setup/docs");
    private static final Path INDEX_FILE = DOCS.resolve("index.adoc");
    private static final Path LOREM_FILE = DOCS.resolve("concepts/lorem.adoc");
    private static final Path IPSUM_FILE = DOCS.resolve("concepts/ipsum.adoc");
    private static final Path OTHER = Paths.get("src/test/resources/other");
    private static final Path OTHER_FILE = OTHER.resolve("index.adoc");

    @Test
    void testCreateAdocFile() {
        Optional<AdocFile> optLorem = DocAsCodeHelper.createAdocFile(LOREM_FILE);
        assertThat(optLorem).isPresent();

        AdocFile lorem = optLorem.get();
        assertThat(lorem).isNotNull();
        assertThat(lorem.getFile()).isEqualTo(LOREM_FILE);
        assertThat(lorem.getTitle()).isEqualTo("Lorem");

        Optional<AdocFile> optIpsum = DocAsCodeHelper.createAdocFile(IPSUM_FILE);
        assertThat(optIpsum).isPresent();

        AdocFile ipsum = optIpsum.get();
        assertThat(ipsum).isNotNull();
        assertThat(ipsum.getFile()).isEqualTo(IPSUM_FILE);
        assertThat(ipsum.getTitle()).isEqualTo("Ipsum");
    }

    @Test
    void testSanitizeHeaderInFiles() throws Exception {
        String indexOriginal = DocAsCodeHelper.readFile(INDEX_FILE);
        String loremOriginal = DocAsCodeHelper.readFile(LOREM_FILE);
        String ipsumOriginal = DocAsCodeHelper.readFile(IPSUM_FILE);

        DocAsCodeHelper.sanitizeHeaderInFiles(DOCS, "include::{root}../_init.adoc[]", null);

        String index = DocAsCodeHelper.readFile(INDEX_FILE);
        String lorem = DocAsCodeHelper.readFile(LOREM_FILE);
        String ipsum = DocAsCodeHelper.readFile(IPSUM_FILE);

        assertThat(index).isEqualTo(indexOriginal);
        assertThat(lorem).isEqualTo(loremOriginal);
        assertThat(ipsum).isEqualTo(ipsumOriginal);
    }

    @Test
    void testSanitizeHeaderInFilesOther() throws Exception {
        String indexOriginal = DocAsCodeHelper.readFile(OTHER_FILE);

        DocAsCodeHelper.sanitizeHeaderInFiles(OTHER, ":init: OK", ":imgs: test");

        String index = DocAsCodeHelper.readFile(OTHER_FILE);

        assertThat(index).isEqualTo(indexOriginal);
    }
}
