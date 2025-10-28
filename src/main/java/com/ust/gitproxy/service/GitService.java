package com.ust.gitproxy.service;

import com.ust.gitproxy.config.GitProperties;
import com.ust.gitproxy.git.BranchAwareCommitCollector;
import com.ust.gitproxy.git.ChangeLogCollector;
import com.ust.gitproxy.git.GitCommitCollector;
import com.ust.gitproxy.git.StandardCommitCollector;
import com.ust.gitproxy.model.GitBranch;
import com.ust.gitproxy.model.GitCommit;
import com.ustri.digital.edgeops.storage.GitStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.FetchCommand;
import org.eclipse.jgit.api.LsRemoteCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author Valeriy Kucherenko
 * @since 24.10.2022
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GitService implements InitializingBean, DisposableBean {

    private final GitProperties properties;
    private final GitStorageFactory gitStorageFactory;

    private ExecutorService executorService;

    @Override
    public void afterPropertiesSet() {
        int parallelism = properties.getParallelism();
        if (parallelism <= 0) {
            parallelism = Runtime.getRuntime().availableProcessors();
        }

        log.info("Creating work-stealing thread pool with parallelism level of {}", parallelism);
        executorService = Executors.newWorkStealingPool(parallelism);
    }

    @Override
    public void destroy() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(properties.getExecutorShutdownTimeout().toMillis(), TimeUnit.MILLISECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public void prepare(String url, String authentication) throws IOException, GitAPIException {
        try (GitStorage storage = gitStorageFactory.getStorage(url, authentication)) {
            if (storage.getDirectory().exists()) {
                storage.open();
                FetchCommand fetchCommand = storage.getGit().fetch();
                storage.applySecurity(fetchCommand);
                fetchCommand.setRemoveDeletedRefs(true);
                fetchCommand.call();
            } else {
                storage.cloneRepository(cfg -> {
                    cfg.setBare(true);
                    cfg.setCloneAllBranches(true);
                });
            }
        }
    }

    public List<GitCommit> listCommits(String url, String authentication, String branch,
                                       String startCommitId, boolean includeBranches, int skip, int maxCount)
            throws IOException, GitAPIException {

        try (GitStorage storage = gitStorageFactory.getStorageChecked(url, authentication)) {
            storage.open();

            checkAuthentication(storage);

            GitCommitCollector collector = includeBranches
                    ? new BranchAwareCommitCollector(storage, executorService)
                    : new StandardCommitCollector(storage);
            return collector.listCommits(branch, startCommitId, skip, maxCount);
        }
    }

    public Collection<String> changelog(String url, String authentication, String from, String to, String pattern)
            throws IOException, GitAPIException {
        try (GitStorage storage = gitStorageFactory.getStorageChecked(url, authentication)) {
            storage.open();

            checkAuthentication(storage);

            return new ChangeLogCollector(storage).changelog(from, to, pattern);
        }
    }

    public List<GitBranch> listBranches(String url, String authentication) throws IOException, GitAPIException {
        try (GitStorage storage = gitStorageFactory.getStorageChecked(url, authentication)) {
            storage.open();

            checkAuthentication(storage);

            return storage.getBranches().stream().map(GitBranch::new).collect(Collectors.toList());
        }
    }

    private void checkAuthentication(GitStorage storage) throws GitAPIException {
        LsRemoteCommand command = storage.getGit().lsRemote();
        storage.applySecurity(command);
        command.call();
    }

    public List<GitBranch> defaultBranchDetails(String url, String authentication) throws IOException, GitAPIException {
        try (GitStorage storage = gitStorageFactory.getStorageChecked(url, authentication)) {
            storage.open();

            checkAuthentication(storage);

            String defaultBranchName = storage.getGit().getRepository().getFullBranch();
            return storage.getBranches().stream().filter(branch -> branch.getName().equals(defaultBranchName)).map(GitBranch::new).collect(Collectors.toList());
        }
    }
}
