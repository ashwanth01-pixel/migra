package com.ust.gitproxy.git;

import com.ust.gitproxy.model.GitCommit;
import com.ustri.digital.edgeops.storage.GitStorage;
import org.eclipse.jgit.api.errors.CanceledException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Valeriy Kucherenko
 * @since 09.02.2023
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BranchAwareCommitCollectorTest extends AbstractGitRepositoryAwareTest {

    private ExecutorService executorService;

    @BeforeEach
    void setUp() {
        executorService = Executors.newSingleThreadExecutor();
    }

    @AfterEach
    void tearDown() {
        executorService.shutdownNow();
    }

    @Test
    void commitBranchesTest() throws Exception {
        try (GitStorage storage = openStorage()) {
            GitCommitCollector collector = new BranchAwareCommitCollector(storage, executorService);
            List<GitCommit> commits = collector.listCommits("", "", 0, 1);
            assertThat(commits, is(hasSize(1)));

            GitCommit commit = commits.get(0);
            assertThat(commit.getId(), is("81dd39b523cc306c8e259a43635d14251c432e27"));
            assertThat(commit.getBranches(), is(notNullValue()));
            assertThat(commit.getBranches(), is(hasSize(2)));
            assertThat(commit.getBranches(), is(contains("master", "test1")));
        }
    }

    @Test
    void executorExecutionExceptionTest() throws Exception {
        try (GitStorage storage = openStorage()) {
            GitCommitCollector collector = new BranchAwareCommitCollector(storage, executorService) {
                @Override
                protected GitCommit transform(RevCommit commit) throws IOException {
                    GitCommit c = super.transform(commit);
                    c.setFuture(executorService.submit(() -> {
                        throw new CanceledException("test");
                    }));
                    return c;
                }
            };

            assertThrows(CanceledException.class, () -> {
                collector.listCommits("", "", 0, 1);
            });
        }
    }
}
