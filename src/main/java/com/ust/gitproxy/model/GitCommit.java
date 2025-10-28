package com.ust.gitproxy.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import java.util.concurrent.Future;

/**
 * @author Valeriy Kucherenko
 * @since 13.11.2022
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GitCommit {
    private String id;
    private String message;
    private String author;
    private String email;

    /**
     * Commit timestamp in seconds.
     */
    @Schema(name = "Commit timestamp in seconds.")
    private int timestamp;

    /**
     * List of branches to which this commit belongs to.
     */
    @Schema(name = "List of branches to which this commit belongs to.")
    private List<String> branches;

    private GitCommitStatistics statistics = new GitCommitStatistics();

    /**
     * For internal use only.
     */
    @Schema(hidden = true)
    @JsonIgnore
    Future<?> future;
}
