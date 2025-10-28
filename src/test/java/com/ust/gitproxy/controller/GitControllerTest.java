package com.ust.gitproxy.controller;

import com.ust.gitproxy.model.Credentials;
import com.ust.gitproxy.model.GitBranch;
import com.ust.gitproxy.model.GitCommit;
import com.ust.gitproxy.service.GitService;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectIdRef;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.net.ConnectException;
import java.util.Collections;
import java.util.NoSuchElementException;

import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Valeriy Kucherenko
 * @since 09.02.2023
 */
@WebMvcTest(controllers = GitController.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GitControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GitService gitService;

    @Test
    void prepare() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/http/example.com/_git/prepare?url=https://example.com/1.git")
                        .header(Credentials.HEADER, "ewogICJnaXQiOiAidXNlciBwYXNzIiwKICAicHJveHkiOiAiQXV0aG9yaXphdGlvbiBCZWFyZXIgdG9rZW4iCn0="))
                .andDo(print())
                .andExpect(status().isOk());

        verify(gitService, times(1)).prepare("https://example.com/1.git", "user pass");
    }

    @Test
    void listBranches() throws Exception {
        when(gitService.listBranches("https://example.com/1.git", "user pass"))
                .thenReturn(Collections.singletonList(new GitBranch(new ObjectIdRef.Unpeeled(null, "refs/heads/test", new ObjectId(1, 1, 1, 1, 1)))));

        mockMvc.perform(MockMvcRequestBuilders
                        .get("/http/example.com/_git/list-branches?url=https://example.com/1.git")
                        .header(Credentials.HEADER, "ewogICJnaXQiOiAidXNlciBwYXNzIiwKICAicHJveHkiOiAiQXV0aG9yaXphdGlvbiBCZWFyZXIgdG9rZW4iCn0="))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name", is("test")));
    }

    @Test
    void listCommits() throws Exception {
        GitCommit commit = new GitCommit();
        commit.setId("1");
        when(gitService.listCommits("https://example.com/1.git", "user pass", null, null, false, 0, 0))
                .thenReturn(Collections.singletonList(commit));

        mockMvc.perform(MockMvcRequestBuilders
                        .get("/http/example.com/_git/list-commits?url=https://example.com/1.git")
                        .header(Credentials.HEADER, "ewogICJnaXQiOiAidXNlciBwYXNzIiwKICAicHJveHkiOiAiQXV0aG9yaXphdGlvbiBCZWFyZXIgdG9rZW4iCn0="))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id", is("1")));
    }

    @Test
    void listCommits_CheckNotFound() throws Exception {
        when(gitService.listCommits("https://example.com/1.git", null, null, null, false, 0, 0))
                .thenThrow(new NoSuchElementException("error"));

        mockMvc.perform(MockMvcRequestBuilders
                        .get("/http/example.com/_git/list-commits?url=https://example.com/1.git"))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    @Test
    void listCommits_CheckTransportErrorUnauthorized() throws Exception {
        when(gitService.listCommits("https://example.com/1.git", null, null, null, false, 0, 0))
                .thenThrow(new TransportException("YOU: not authorized"));

        mockMvc.perform(MockMvcRequestBuilders
                        .get("/http/example.com/_git/list-commits?url=https://example.com/1.git"))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    void listCommits_CheckTransportErrorUnableToConnect() throws Exception {
        when(gitService.listCommits("https://example.com/1.git", null, null, null, false, 0, 0))
                .thenThrow(new TransportException("unable to connect", new ConnectException()));

        mockMvc.perform(MockMvcRequestBuilders
                        .get("/http/example.com/_git/list-commits?url=https://example.com/1.git"))
                .andDo(print())
                .andExpect(status().isBadGateway());
    }

    @Test
    void listCommits_CheckTransportErrorOther() throws Exception {
        when(gitService.listCommits("https://example.com/1.git", null, null, null, false, 0, 0))
                .thenThrow(new TransportException("", new UnsupportedOperationException()));

        mockMvc.perform(MockMvcRequestBuilders
                        .get("/http/example.com/_git/list-commits?url=https://example.com/1.git"))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    void changelog() throws Exception {
        when(gitService.changelog("https://example.com/1.git", "user pass", "tag1", "tag2", null))
                .thenReturn(Collections.singleton("result"));

        mockMvc.perform(MockMvcRequestBuilders
                        .get("/http/example.com/_git/changelog?url=https://example.com/1.git&from=tag1&to=tag2")
                        .header(Credentials.HEADER, "ewogICJnaXQiOiAidXNlciBwYXNzIiwKICAicHJveHkiOiAiQXV0aG9yaXphdGlvbiBCZWFyZXIgdG9rZW4iCn0="))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]", is("result")));
    }

    @Test
    void defaultBranchDetails() throws Exception {
        when(gitService.defaultBranchDetails("https://example.com/1.git", "user pass"))
                .thenReturn(Collections.singletonList(new GitBranch(new ObjectIdRef.Unpeeled(null, "main", new ObjectId(1, 1, 1, 1, 1)))));

        mockMvc.perform(MockMvcRequestBuilders
                        .get("/http/example.com/_git/default-branch-details?url=https://example.com/1.git")
                        .header(Credentials.HEADER, "ewogICJnaXQiOiAidXNlciBwYXNzIiwKICAicHJveHkiOiAiQXV0aG9yaXphdGlvbiBCZWFyZXIgdG9rZW4iCn0="))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name", is("main")));
    }
}
