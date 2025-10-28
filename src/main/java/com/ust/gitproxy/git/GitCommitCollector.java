package com.ust.gitproxy.git;

import com.ust.gitproxy.model.GitCommit;

import java.util.List;

/**
 * @author Valeriy Kucherenko
 * @since 01.02.2023
 */
public interface GitCommitCollector {
    List<GitCommit> listCommits(String branch, String startCommitId, int skip, int maxCount);
}
