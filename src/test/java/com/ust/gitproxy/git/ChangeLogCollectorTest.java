package com.ust.gitproxy.git;

import com.ustri.digital.edgeops.storage.GitStorage;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.NoSuchElementException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Valeriy Kucherenko
 * @since 03.04.2023
 */
class ChangeLogCollectorTest extends AbstractGitRepositoryAwareTest {

    @Test
    void changelog() throws Exception {
        try (GitStorage storage = openStorage()) {
            ChangeLogCollector collector = new ChangeLogCollector(storage);

            Collection<String> changelog = collector.changelog("tag1", "tag2", "^(VALKUC\\-.+):");
            assertThat(changelog, is(hasSize(1)));

            changelog = collector.changelog("tag1", "tag2", "^(asd):");
            assertThat(changelog, is(hasSize(0)));

            changelog = collector.changelog("tag1", "tag2", "");
            assertThat(changelog, is(hasSize(1)));
        }
    }

    @Test
    void changelog_WrongTag() throws Exception {
        try (GitStorage storage = openStorage()) {
            ChangeLogCollector collector = new ChangeLogCollector(storage);
            NoSuchElementException e = assertThrows(NoSuchElementException.class, () -> collector.changelog("asd", "tag2", null));
            assertThat(e.getMessage(), is(containsString("'From'")));

            e = assertThrows(NoSuchElementException.class, () -> collector.changelog("tag1", "asd", null));
            assertThat(e.getMessage(), is(containsString("'To'")));
        }
    }
}
