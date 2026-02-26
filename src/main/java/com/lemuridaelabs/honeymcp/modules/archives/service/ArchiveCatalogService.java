package com.lemuridaelabs.honeymcp.modules.archives.service;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Central catalog of archive names for reuse across MCP tools and chat tools.
 */
@Service
public class ArchiveCatalogService {

    private final List<String> availableArchives = List.of("customers", "orders", "inventory");
    private final List<String> disallowedArchives = List.of("accounting", "users");
    private final Set<String> allArchives = Set.copyOf(
            Stream.concat(availableArchives.stream(), disallowedArchives.stream()).toList()
    );

    public List<String> getAvailableArchives() {
        return availableArchives;
    }

    public List<String> getDisallowedArchives() {
        return disallowedArchives;
    }

    public Set<String> getAllArchives() {
        return allArchives;
    }
}
