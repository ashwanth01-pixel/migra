package com.ust.gitproxy.git;

import com.ust.gitproxy.model.GitCommit;
import com.ustri.digital.edgeops.storage.GitStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.List;
import java.util.NoSuchElementException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Valeriy Kucherenko
 * @since 09.02.2023
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StandardCommitCollectorTest extends AbstractGitRepositoryAwareTest {

    @Test
    void commitValidationTest() throws Exception {
        try (GitStorage storage = openStorage()) {
            GitCommitCollector collector = new StandardCommitCollector(storage);
            List<GitCommit> commits = collector.listCommits("", "", 0, 1);
            assertThat(commits, is(hasSize(1)));

            GitCommit commit = commits.get(0);
            assertThat(commit.getId(), is("81dd39b523cc306c8e259a43635d14251c432e27"));
            assertThat(commit.getMessage(), is("VALKUC-75-00: Initial commit"));
            assertThat(commit.getAuthor(), is("Edge Opsx"));
            assertThat(commit.getEmail(), is("edgeops@ust-global.com"));
            assertThat(commit.getTimestamp(), is(1661509829));
            assertThat(commit.getBranches(), is(nullValue()));
            assertThat(commit.getStatistics(), is(notNullValue()));
            assertThat(commit.getStatistics().getAdditions(), is(1033));
            assertThat(commit.getStatistics().getDeletions(), is(0));
            assertThat(commit.getStatistics().getTotal(), is(1033));
        }
    }

    @Test
    void startCommitNoBranchTest() throws Exception {
        try (GitStorage storage = openStorage()) {
            GitCommitCollector collector = new StandardCommitCollector(storage);
            List<GitCommit> commits = collector.listCommits("", "2c3d32f98294c00b26d8f4a55d7265c1f1820518", 0, 0);
            assertThat(commits, is(hasSize(4)));
            assertThat(commits.get(0).getId(), is("6e6b55fdf716d46d93905f9fbd087dc50ba082b2"));
            assertThat(commits.get(1).getId(), is("8c5c3256a4f3b183e7b2cb6d02bf7f0799bf2559"));
        }
    }

    @Test
    void startCommitWithBranchTest() throws Exception {
        try (GitStorage storage = openStorage()) {
            GitCommitCollector collector = new StandardCommitCollector(storage);
            List<GitCommit> commits = collector.listCommits("master", "6e0639c5a0698b29f5a3d0c4daed5d0a451ff4b2", 0, 0);
            assertThat(commits, is(hasSize(4)));
            assertThat(commits.get(0).getId(), is("854cdbe4c9e0d307333207277a6ab7f67ffa9239"));
        }
    }

    @Test
    void paginationTest() throws Exception {
        try (GitStorage storage = openStorage()) {
            GitCommitCollector collector = new StandardCommitCollector(storage);
            List<GitCommit> commits1 = collector.listCommits("", "", 0, 2);
            assertThat(commits1, is(hasSize(2)));
            assertThat(commits1.get(0).getId(), is("81dd39b523cc306c8e259a43635d14251c432e27"));
            assertThat(commits1.get(1).getTimestamp(), is(greaterThan(commits1.get(0).getTimestamp())));

            List<GitCommit> commits2 = collector.listCommits("", "", 1, 1);
            assertThat(commits2, is(hasSize(1)));

            assertThat(commits1.get(1), is(equalTo(commits2.get(0))));
        }
    }

    @Test
    void commitsPerBranchTest() throws Exception {
        try (GitStorage storage = openStorage()) {
            GitCommitCollector collector = new StandardCommitCollector(storage);
            List<GitCommit> commits = collector.listCommits("", "", 0, 0);
            assertThat(commits, is(hasSize(19)));

            commits = collector.listCommits("master", "", 0, 0);
            assertThat(commits, is(hasSize(17)));

            commits = collector.listCommits("test1", "", 0, 0);
            assertThat(commits, is(hasSize(16)));
        }
    }

    @Test
    void skipAndMaxCountTest() throws Exception {
        try (GitStorage storage = openStorage()) {
            GitCommitCollector collector = new StandardCommitCollector(storage);
            List<GitCommit> commits = collector.listCommits("", "", 0, 2);
            assertThat(commits, is(hasSize(2)));
            assertThat(commits.get(0).getId(), is("81dd39b523cc306c8e259a43635d14251c432e27"));
            assertThat(commits.get(1).getId(), is("1e49aa9c45017ee3e4aca216953b10784eaa2dff"));

            commits = collector.listCommits("", "", 1, 2);
            assertThat(commits, is(hasSize(2)));
            assertThat(commits.get(0).getId(), is("1e49aa9c45017ee3e4aca216953b10784eaa2dff"));
            assertThat(commits.get(1).getId(), is("6cef00d348866f88a0cd033c3d93a50ffb61eec9"));
        }
    }

    @Test
    void wrongParamsTest() throws Exception {
        try (GitStorage storage = openStorage()) {
            GitCommitCollector collector = new StandardCommitCollector(storage);
            IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> {
                collector.listCommits("", "", -1, 0);
            });
            assertThat(e.getMessage(), is(containsString("Skip")));

            e = assertThrows(IllegalArgumentException.class, () -> {
                collector.listCommits("", "", 0, -1);
            });
            assertThat(e.getMessage(), is(containsString("MaxCount")));
        }
    }

    @Test
    void wrongStartCommitTest() throws Exception {
        try (GitStorage storage = openStorage()) {
            GitCommitCollector collector = new StandardCommitCollector(storage);
            NoSuchElementException e = assertThrows(NoSuchElementException.class, () -> {
                collector.listCommits("master", "YAHOO!!!", 0, 0);
            });
            assertThat(e.getMessage(), is(containsString("Object reference")));
        }
    }
}
