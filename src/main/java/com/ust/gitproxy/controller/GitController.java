package com.ust.gitproxy.controller;

import com.ust.gitproxy.model.Credentials;
import com.ust.gitproxy.model.GitBranch;
import com.ust.gitproxy.model.GitCommit;
import com.ust.gitproxy.service.GitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.errors.NoRemoteRepositoryException;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.ServletWebRequest;

import java.io.FileNotFoundException;
import java.net.ConnectException;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * @author Valeriy Kucherenko
 * @since 11.11.2022
 */
@Slf4j
@RequiredArgsConstructor
@SuppressWarnings("java:S112") // disable warning because controller exceptions are handled by exception handlers
@RestController
@RequestMapping("**/_git")
public class GitController {
    private final GitService gitService;

    @GetMapping("/prepare")
    @Operation(summary = "Checkout or update repository. Must be invoked before any other endpoints.")
    public void prepare(Credentials credentials,
                        @Parameter(description = "Repository URL.")
                        @RequestParam String url) throws Exception {
        gitService.prepare(url, credentials.getGit());
    }

    @GetMapping("/list-commits")
    @Operation(summary = "List repository commits. The repository must be prepared before using this endpoint.")
    public List<GitCommit> listCommits(Credentials credentials,
                                       @Parameter(description = "Repository URL.")
                                       @RequestParam String url,
                                       @Parameter(description = "Branch for which to collect commits. If omitted - all branches.")
                                       @RequestParam(required = false) String branch,
                                       @Parameter(description = "Commit ID at which the listing starts.")
                                       @RequestParam(required = false) String startCommitId,
                                       @Parameter(description = "Collect commit related branches.")
                                       @RequestParam(required = false) boolean includeBranches,
                                       @RequestParam(required = false, defaultValue = "0") int skip,
                                       @RequestParam(required = false, defaultValue = "0") int size) throws Exception {
        return gitService.listCommits(url, credentials.getGit(), branch, startCommitId, includeBranches, skip, size);
    }

    @GetMapping("/list-branches")
    @Operation(summary = "List repository branches. The repository must be prepared before using this endpoint.")
    public List<GitBranch> listBranches(Credentials credentials,
                                        @Parameter(description = "Repository URL.")
                                        @RequestParam String url) throws Exception {
        return gitService.listBranches(url, credentials.getGit());
    }

    @GetMapping("/changelog")
    @Operation(summary = "Generate changelog. The repository must be prepared before using this endpoint.")
    public Collection<String> changelog(Credentials credentials,
                                        @Parameter(description = "Repository URL.")
                                        @RequestParam String url,
                                        @Parameter(description = "Start tag name.", example = "DAgility-5.3.10.0")
                                        @RequestParam String from,
                                        @Parameter(description = "End tag name.", example = "DAgility-5.3.11.0")
                                        @RequestParam String to,
                                        @Parameter(description = "Regular expression patter that can be used to specify commit message transformation logic.", example = "^(EO-.+):")
                                        @RequestParam(required = false) String pattern) throws Exception {
        return gitService.changelog(url, credentials.getGit(), from, to, pattern);
    }

    @GetMapping("/default-branch-details")
    @Operation(summary = "Return default branch name and last commit id. The repository must be prepared before using this endpoint.")
    public List<GitBranch> defaultBranchDetails(Credentials credentials,
                                                @Parameter(description = "Repository URL.")
                                                @RequestParam String url) throws Exception {
        return gitService.defaultBranchDetails(url, credentials.getGit());
    }

    @ExceptionHandler({
            FileNotFoundException.class,
            NoSuchElementException.class,
            NoRemoteRepositoryException.class,
            MissingObjectException.class,
            IncorrectObjectTypeException.class
    })
    ResponseEntity<String> handleException(Exception e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
    }

    @ExceptionHandler({TransportException.class})
    ResponseEntity<String> handleException(TransportException e, ServletWebRequest request) {
        Throwable ex = NestedExceptionUtils.getMostSpecificCause(e);
        if (ex.getMessage() != null) {
            if (ex.getMessage().endsWith(": not authorized") || ex.getMessage().contains(" not permitted ")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
            }
            if (checkTooManyRequests(request, ex.getMessage())) {
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(e.getMessage());
            }
        } else if (ex instanceof ConnectException) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(e.getMessage());
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
    }

    private boolean checkTooManyRequests(ServletWebRequest request, String message) {
        String url = request.getParameter("url");
        return url != null && message.startsWith(String.format("%s: 429 ", url));
    }
}
