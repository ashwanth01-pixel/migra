package com.ust.gitproxy.git;

import com.ustri.digital.edgeops.storage.GitStorage;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.utils.IOUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.io.TempDir;

import java.io.*;

/**
 * @author Valeriy Kucherenko
 * @since 09.02.2023
 */
abstract class AbstractGitRepositoryAwareTest {
    @TempDir
    static File repoDir;

    @BeforeAll
    static void beforeAll() throws Exception {
        try (InputStream is = StandardCommitCollectorTest.class.getResourceAsStream("/repo.zip")) {
            extractZip(is, repoDir);
        }
    }

    static void extractZip(InputStream is, File destinationFilePath) throws IOException, ArchiveException {
        try (ArchiveInputStream in = new ArchiveStreamFactory().createArchiveInputStream("zip", is)) {
            ZipArchiveEntry entry;
            while ((entry = (ZipArchiveEntry) in.getNextEntry()) != null) {
                File f = new File(destinationFilePath, entry.getName());
                if (entry.isDirectory()) {
                    if (!f.mkdir()) {
                        throw new IOException("Unable to create directory " + f.getAbsolutePath());
                    }
                    continue;
                }
                try (OutputStream out = new FileOutputStream(f)) {
                    IOUtils.copy(in, out);
                }
            }
        }
    }

    static GitStorage openStorage() throws IOException {
        GitStorage storage = GitStorage.local(repoDir.getAbsolutePath());
        storage.open();
        return storage;
    }
}
