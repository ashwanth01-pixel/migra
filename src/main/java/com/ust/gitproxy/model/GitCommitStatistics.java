package com.ust.gitproxy.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author Valeriy Kucherenko
 * @since 13.11.2022
 */
@Data
public class GitCommitStatistics {
    /**
     * Number of lines added.
     */
    @Schema(description = "Number of lines added.")
    private int additions;

    /**
     * Number of lines deleted.
     */
    @Schema(description = "Number of lines deleted.")
    private int deletions;

    /**
     * Number of total changed lines.
     */
    @Schema(description = "Number of total changed lines.")
    private int total;
}
