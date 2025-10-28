package com.ust.gitproxy.service;

import com.ust.gitproxy.config.GitProperties;
import com.ustri.digital.edgeops.storage.GitStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.File;
import java.io.FileNotFoundException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * @author Valeriy Kucherenko
 * @since 03.04.2023
 */
class GitStorageFactoryTest {

    GitStorageFactory gitStorageFactory;

    @BeforeEach
    void setUp() {
        GitProperties properties = new GitProperties();

        gitStorageFactory = new GitStorageFactory(properties);
    }

    @Test
    void getStorage_WrongUrl() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () ->
                gitStorageFactory.getStorage("", ""));
        assertThat(e.getMessage(), containsString("URL must not be empty"));
    }

    @Test
    void getStorage_NotExist() {
        try (MockedStatic<GitStorage> gitStorageMockStatic = mockStatic(GitStorage.class)) {
            File fileMock = Mockito.mock(File.class);
            when(fileMock.exists()).thenReturn(false);

            GitStorage gitStorageMock = Mockito.mock(GitStorage.class, Answers.RETURNS_DEEP_STUBS);
            when(gitStorageMock.getDirectory()).thenReturn(fileMock);

            //noinspection resource
            gitStorageMockStatic.when(() -> GitStorage.withToken(any(), any(), any())).thenReturn(gitStorageMock);

            FileNotFoundException e = assertThrows(FileNotFoundException.class, () ->
                    gitStorageFactory.getStorageChecked("url", ""));
            assertThat(e.getMessage(), containsString("clone the repository"));
        }
    }

    @Test
    void getStorage_Exists() throws Exception {
        try (MockedStatic<GitStorage> gitStorageMockStatic = mockStatic(GitStorage.class)) {
            File fileMock = Mockito.mock(File.class);
            when(fileMock.exists()).thenReturn(true);

            GitStorage gitStorageMock = Mockito.mock(GitStorage.class, Answers.RETURNS_DEEP_STUBS);
            when(gitStorageMock.getDirectory()).thenReturn(fileMock);

            //noinspection resource
            gitStorageMockStatic.when(() -> GitStorage.withToken(eq("u%20r%20l"), any(), eq("token"))).thenReturn(gitStorageMock);

            GitStorage storage = gitStorageFactory.getStorageChecked("u r l", "token");
            assertThat(storage, notNullValue());

            //noinspection resource
            gitStorageMockStatic.when(() -> GitStorage.withLoginPassword(eq("test"), any(), eq("user"), eq("pass")))
                    .thenReturn(gitStorageMock);

            storage = gitStorageFactory.getStorageChecked("test", "user pass");
            assertThat(storage, notNullValue());
        }
    }
}
