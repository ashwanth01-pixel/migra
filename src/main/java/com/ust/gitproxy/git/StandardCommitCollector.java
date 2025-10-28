package com.ust.gitproxy.git;

import com.ust.gitproxy.model.GitCommit;
import com.ustri.digital.edgeops.storage.GitStorage;
import lombok.SneakyThrows;
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.jgit.api.LogCommand;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.patch.HunkHeader;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevSort;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.util.io.NullOutputStream;
import org.slf4j.helpers.MessageFormatter;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Supplier;

/**
 * @author Valeriy Kucherenko
 * @since 06.11.2022
 */
public class StandardCommitCollector implements GitCommitCollector {
    protected final GitStorage storage;

    public StandardCommitCollector(GitStorage storage) {
        this.storage = storage;
    }

    @SneakyThrows
    @Override
    public List<GitCommit> listCommits(String branch, String startCommitId, int skip, int maxCount) {
        if (skip < 0) {
            throw new IllegalArgumentException("Skip parameter must be greater or equals to zero");
        }
        if (maxCount < 0) {
            throw new IllegalArgumentException("MaxCount parameter must be greater or equals to zero");
        }

        LogCommand logCommand = createLogCommand(branch, startCommitId);

        List<GitCommit> result = new ArrayList<>();
        try (RevWalk commits = (RevWalk) logCommand.call()) {
            commits.sort(RevSort.REVERSE, true);

            // logCommand.setSkip and logCommand.setMaxCount can not be used together with RevSort.REVERSE
            // as they produce wrong results: skip from the end of the list / maxCount from the beginning of the list

            boolean startFound = true;
            ObjectId startCommit = null;
            if (StringUtils.hasText(startCommitId) && !StringUtils.hasText(branch)) {
                startCommit = resolveRev(startCommitId, () -> "Object reference was not found for startCommitId '{}'");
                startFound = false;
            }

            int skipped = 0;
            Iterator<RevCommit> it = commits.iterator();
            while (it.hasNext() && (maxCount == 0 || result.size() < maxCount)) {
                RevCommit commit = it.next();

                if (!startFound) {
                    if (commit.getId().equals(startCommit)) {
                        startFound = true;
                    }
                    continue;
                }

                if (skip > 0 && skipped < skip) {
                    skipped++;
                    continue;
                }

                result.add(transform(commit));
            }
        } catch (NoHeadException e) {
            // exception is thrown when the repository is new and does not have any commit or branch - just return empty list.
        }

        return result;
    }

    protected LogCommand createLogCommand(String branch, String startCommitId) throws IOException {
        LogCommand logCommand = storage.getGit().log();

        if (StringUtils.hasText(branch)) {
            ObjectId to = resolveRev(branch, () -> "Object reference was not found for branch '{}'");

            if (StringUtils.hasText(startCommitId)) {
                ObjectId from = resolveRev(startCommitId, () -> "Object reference was not found for startCommitId '{}'");
                logCommand.addRange(from, to);
            } else {
                logCommand.add(to);
            }
        } else {
            logCommand.all();
        }

        return logCommand;
    }

    protected ObjectId resolveRev(String revstr, Supplier<String> errorSupplier) throws IOException {
        ObjectId id = storage.getGit().getRepository().resolve(revstr);
        if (id == null) {
            throw new NoSuchElementException(MessageFormatter.format(errorSupplier.get(), revstr).getMessage());
        }
        return id;
    }

    protected GitCommit transform(RevCommit commit) throws IOException {
        GitCommit item = new GitCommit();

        item.setId(commit.getName().trim());
        item.setMessage(commit.getFullMessage().trim());
        item.setEmail(commit.getAuthorIdent().getEmailAddress().trim().toLowerCase());
        item.setAuthor(commit.getAuthorIdent().getName().trim());
        item.setTimestamp(commit.getCommitTime());

        Pair<Integer, Integer> changes = calculateChanges(commit);

        item.getStatistics().setAdditions(changes.getLeft());
        item.getStatistics().setDeletions(changes.getRight());
        item.getStatistics().setTotal(changes.getLeft() + changes.getRight());

        return item;
    }

    /**
     * @return Pair first - additions. Pair second - deletions.
     */
    private Pair<Integer, Integer> calculateChanges(RevCommit commit) throws IOException {
        int added = 0;
        int deleted = 0;

        try (DiffFormatter formatter = new DiffFormatter(NullOutputStream.INSTANCE)) {
            formatter.setRepository(storage.getGit().getRepository());
            RevCommit prevCommit = commit.getParentCount() > 0 ? commit.getParent(0) : null;
            List<DiffEntry> entries = formatter.scan(prevCommit, commit);
            for (DiffEntry entry : entries) {
                for (HunkHeader hh : formatter.toFileHeader(entry).getHunks()) {
                    for (Edit edit : hh.toEditList()) {
                        switch (edit.getType()) {
                            case INSERT:
                                added += edit.getLengthB();
                                break;
                            case DELETE:
                                deleted += edit.getLengthA();
                                break;
                            case REPLACE:
                                added += edit.getLengthB();
                                deleted += edit.getLengthA();
                                break;
                            default:
                                break;
                        }
                    }
                }
            }
        }

        return Pair.of(added, deleted);
    }
}
