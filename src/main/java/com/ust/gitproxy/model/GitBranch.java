package com.ust.gitproxy.model;

import lombok.Data;
import lombok.NonNull;
import org.eclipse.jgit.lib.Ref;

import java.util.Objects;

/**
 * @author Valeriy Kucherenko
 * @since 16.11.2022
 */
@Data
public class GitBranch {
    private static final String REFS_PREFIX = "refs/heads/";

    private String lastCommitId;
    private String name;

    public GitBranch(@NonNull Ref branch) {
        lastCommitId = Objects.requireNonNull(branch.getObjectId(), "Branch does not have objectId").getName();
        name = branch.getName();
        if (name.startsWith(REFS_PREFIX)) {
            name = name.substring(REFS_PREFIX.length());
        }
    }
}
