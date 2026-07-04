package com.bank.aiassistant.util;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class FileHashUtilsTest {

    @Test
    void sha256ShouldCalculateStableHashFromInputStream() {
        String hash = FileHashUtils.sha256(new ByteArrayInputStream("abc".getBytes(StandardCharsets.UTF_8)));

        assertThat(hash).isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
    }

    @Test
    void sha256ShouldCalculateStableHashFromMultipartFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                "abc".getBytes(StandardCharsets.UTF_8));

        String hash = FileHashUtils.sha256(file);

        assertThat(hash).isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
    }
}
