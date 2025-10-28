package com.ust.gitproxy.git;

import com.ustri.digital.edgeops.storage.GitStorage;
import org.eclipse.jgit.api.LogCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.slf4j.helpers.MessageFormatter;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.NoSuchElementException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Valeriy Kucherenko
 * @since 03.04.2023
 */
public class ChangeLogCollector {

    protected final GitStorage storage;

    public ChangeLogCollector(GitStorage storage) {
        this.storage = storage;
    }

    public Collection<String> changelog(String fromRev, String toRev, String pattern)
            throws IOException, GitAPIException {

        Ref fromId = storage.getGit().getRepository().findRef(fromRev);
        if (fromId == null) {
            throw new NoSuchElementException(MessageFormatter.format(
                    "'From' object reference '{}' was not resolved", fromRev).getMessage());
        }
        Ref toId = storage.getGit().getRepository().findRef(toRev);
        if (toId == null) {
            throw new NoSuchElementException(MessageFormatter.format(
                    "'To' object reference '{}' was not resolved", toRev).getMessage());
        }

        LogCommand logCommand = storage.getGit().log();
        logCommand.addRange(
                fromId.getPeeledObjectId() != null ? fromId.getPeeledObjectId() : fromId.getObjectId(),
                toId.getPeeledObjectId() != null ? toId.getPeeledObjectId() : toId.getObjectId()
        );

        Collection<String> changelog = new LinkedHashSet<>();
        try (RevWalk commits = (RevWalk) logCommand.call()) {
            Pattern rePattern = null;
            if (StringUtils.hasText(pattern)) {
                rePattern = Pattern.compile(pattern);
            }

            for (RevCommit commit : commits) {
                String msg = commit.getFullMessage();
                if (rePattern != null) {
                    Matcher m = rePattern.matcher(msg);
                    if (m.find() && m.groupCount() > 0) {
                        msg = m.group(1);
                        changelog.add(msg);
                    }
                } else {
                    changelog.add(msg);
                }
            }
        }
        return changelog;
    }
}
