package com.ust.gitproxy.git;

import com.ust.gitproxy.model.GitBranch;
import com.ust.gitproxy.model.GitCommit;
import com.ustri.digital.edgeops.storage.GitStorage;
import lombok.SneakyThrows;
import org.eclipse.jgit.api.ListBranchCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.revwalk.RevCommit;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

/**
 * @author Valeriy Kucherenko
 * @since 01.02.2023
 */
public class BranchAwareCommitCollector extends StandardCommitCollector {

    private final ExecutorService executorService;

    public BranchAwareCommitCollector(GitStorage storage, ExecutorService executorService) {
        super(storage);
        this.executorService = executorService;
    }

    @SneakyThrows
    @Override
    public List<GitCommit> listCommits(String branch, String startCommitId, int skip, int maxCount) {
        List<GitCommit> result = super.listCommits(branch, startCommitId, skip, maxCount);

        try {
            // wait for all futures to be completed
            for (GitCommit gitCommit : result) {
                gitCommit.getFuture().get();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            Throwable ex = e.getCause();
            if (ex.getClass().equals(RuntimeException.class) && ex.getCause() != null) {
                ex = ex.getCause();
            }
            throw ex;
        }

        return result;
    }

    @Override
    protected GitCommit transform(RevCommit commit) throws IOException {
        GitCommit gitCommit = super.transform(commit);

        ListBranchCommand listBranchCommand = storage.getGit().branchList().setContains(commit.getId().getName());

        gitCommit.setFuture(executorService.submit(() -> {
            try {
                gitCommit.setBranches(listBranchCommand.call().stream()
                        .map(x -> new GitBranch(x).getName())
                        .collect(Collectors.toList()));
            } catch (GitAPIException e) {
                throw new RuntimeException(e); // NOSONAR
            }
        }));

        return gitCommit;
    }
}
