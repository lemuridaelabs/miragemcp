package com.lemuridaelabs.miragemcp;

import com.lemuridaelabs.miragemcp.modules.notifications.dao.VapidKeyRepository;
import com.lemuridaelabs.miragemcp.modules.notifications.service.VapidKeyInitializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class VapidKeyInitializerTest {

    @Autowired
    private VapidKeyRepository repository;

    @Autowired
    private VapidKeyInitializer initializer;

    @BeforeEach
    void resetDatabase() {
        // Ensure the initializer runs from a clean state.
        repository.deleteAll();
    }

    @Test
    void generatesKeysWhenMissing() throws Exception {
        // When no keys exist, the initializer should create a valid key pair.
        initializer.init();

        assertThat(repository.count()).isEqualTo(1);
        var key = repository.findById(1L).orElseThrow();
        assertThat(key.getPublicKey()).isNotBlank();
        assertThat(key.getPrivateKey()).isNotBlank();
    }
}
