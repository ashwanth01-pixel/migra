package com.ust.gitproxy.service;

import com.ust.gitproxy.config.GitProperties;
import com.ustri.digital.edgeops.storage.GitStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriUtils;

import java.io.FileNotFoundException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author Valeriy Kucherenko
 * @since 03.04.2023
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GitStorageFactory {

    private final GitProperties properties;

    public GitStorage getStorage(String url, String authentication) {
        Path repoPath = getRepoPath(url);

        url = UriUtils.encodePath(url, Charset.defaultCharset());

        if (StringUtils.hasText(authentication)) {
            String[] authTokens = authentication.split(" ", 2);
            if (authTokens.length == 2) {
                return GitStorage.withLoginPassword(url, repoPath.toString(), authTokens[0], authTokens[1]);
            } else if (authTokens.length == 1) {
                return GitStorage.withToken(url, repoPath.toString(), authTokens[0]);
            } else {
                log.warn("Wrong value supplied for git credentials: {}", authentication);
            }
        }

        return GitStorage.withToken(url, repoPath.toString(), "");
    }

    public GitStorage getStorageChecked(String url, String authentication) throws FileNotFoundException {
        GitStorage storage = getStorage(url, authentication);
        if (!storage.getDirectory().exists()) {
            throw new FileNotFoundException("Local repository not found for [" + url + "]. Please clone the repository first.");
        }
        return storage;
    }

    private Path getRepoPath(String url) {
        if (!StringUtils.hasText(url)) {
            throw new IllegalArgumentException("URL must not be empty");
        }

        String file = FilenameUtils.getBaseName(url);
        return Paths.get(properties.getCheckoutPath(), file + "." + url.hashCode());
    }
}
