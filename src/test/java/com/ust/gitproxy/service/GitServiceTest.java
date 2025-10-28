package com.ust.gitproxy.service;

import com.ust.gitproxy.config.GitProperties;
import com.ust.gitproxy.git.BranchAwareCommitCollector;
import com.ust.gitproxy.git.ChangeLogCollector;
import com.ust.gitproxy.git.StandardCommitCollector;
import com.ust.gitproxy.model.GitCommit;
import com.ustri.digital.edgeops.storage.GitStorage;
import org.eclipse.jgit.api.FetchCommand;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * @author Valeriy Kucherenko
 * @since 10.02.2023
 */
@ExtendWith(MockitoExtension.class)
class GitServiceTest {

    @Mock
    GitStorageFactory gitStorageFactory;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    GitStorage gitStorage;

    @Mock
    File repoDirectory;

    GitService gitService;

    @BeforeEach
    void setUp() {
        GitProperties properties = new GitProperties();
        properties.setParallelism(1);

        gitService = new GitService(properties, gitStorageFactory);
        gitService.afterPropertiesSet();

        when(gitStorage.getDirectory()).thenReturn(repoDirectory);
        when(gitStorageFactory.getStorage(any(), any())).thenReturn(gitStorage);
    }

    @AfterEach
    void tearDown() {
        gitService.destroy();
    }

    @Test
    void prepareRepo_DirExists() throws Exception {
        when(repoDirectory.exists()).thenReturn(true);

        gitService.prepare("url", "");

        verify(gitStorage, times(1)).open();
        verify(gitStorage, times(1)).applySecurity(any(FetchCommand.class));
    }

    @Test
    void prepareRepo_DirNotExists() throws Exception {
        when(repoDirectory.exists()).thenReturn(false);

        gitService.prepare("test", "");

        //noinspection unchecked
        verify(gitStorage, times(1)).cloneRepository(any(Consumer.class));
    }

    @Test
    void listCommits_Standard() throws Exception {
        when(repoDirectory.exists()).thenReturn(true);
        when(gitStorageFactory.getStorageChecked(any(), any())).thenCallRealMethod();

        try (MockedConstruction<StandardCommitCollector> ccmc = mockConstruction(StandardCommitCollector.class, (ccMock, context) -> {
            Mockito.when(ccMock.listCommits("b", null, 1, 2))
                    .thenReturn(Collections.singletonList(new GitCommit()));
        })) {
            List<GitCommit> commits = gitService.listCommits("", "user pass", "b",
                    null, false, 1, 2);
            assertThat(commits, hasSize(1));
        }
    }

    @Test
    void listCommits_WithBranches() throws Exception {
        when(repoDirectory.exists()).thenReturn(true);
        when(gitStorageFactory.getStorageChecked(any(), any())).thenCallRealMethod();

        try (MockedConstruction<BranchAwareCommitCollector> ccmc = mockConstruction(BranchAwareCommitCollector.class, (ccMock, context) -> {
            Mockito.when(ccMock.listCommits("", null, 1, 2))
                    .thenReturn(Collections.singletonList(new GitCommit()));
        })) {
            List<GitCommit> commits = gitService.listCommits("", "", "",
                    null, true, 1, 2);
            assertThat(commits, hasSize(1));
        }
    }

    @Test
    void listBranches() throws Exception {
        when(repoDirectory.exists()).thenReturn(true);
        when(gitStorageFactory.getStorageChecked(any(), any())).thenCallRealMethod();

        gitService.listBranches("", "");

        verify(gitStorage, times(1)).getBranches();
    }

    @Test
    void changelog() throws Exception {
        when(repoDirectory.exists()).thenReturn(true);
        when(gitStorageFactory.getStorageChecked(any(), any())).thenCallRealMethod();

        try (MockedConstruction<ChangeLogCollector> ccmc = mockConstruction(ChangeLogCollector.class, (ccMock, context) -> {
            Mockito.when(ccMock.changelog("tag1", "tag2", ""))
                    .thenReturn(Collections.singleton("test"));
        })) {
            Collection<String> changelog = gitService.changelog("", "", "tag1", "tag2", "");
            assertThat(changelog, hasSize(1));
        }
    }

    @Test
    void defaultBranchDetails() throws Exception {
        when(repoDirectory.exists()).thenReturn(true);
        when(gitStorageFactory.getStorageChecked(any(), any())).thenCallRealMethod();
        when(gitStorage.getGit().getRepository().getFullBranch()).thenReturn("main");

        gitService.defaultBranchDetails("", "");

        verify(gitStorage, times(1)).getBranches();
    }
}
