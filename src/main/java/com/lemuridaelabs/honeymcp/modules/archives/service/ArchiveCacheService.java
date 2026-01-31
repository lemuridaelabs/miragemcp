package com.lemuridaelabs.honeymcp.modules.archives.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.lemuridaelabs.honeymcp.modules.archives.dto.ArchiveFileCacheWrapper;
import com.lemuridaelabs.honeymcp.modules.archives.dto.ArchiveFileSummary;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class ArchiveCacheService {

    @Value("${honeymcp.archive.cache.size:4096}")
    private Integer cacheSize;

    @Value("${honeymcp.archive.cache.durationMins:120}")
    private Long cacheDurationMins;

    private static Cache<String, ArchiveFileCacheWrapper> archiveFileCache;

    private static Cache<String, ArchiveFileSummary> archiveSummaryCache;

    /**
     * Stores archive file records associated with a given source IP address into the cache.
     * For each file in the provided {@code ArchiveFileSummary}, a cache entry is created and
     * linked to the source IP. The entry contains the source IP, the file details, and a timestamp.
     *
     * @param remoteIp           The IP address associated with the archive file records. Must not be null.
     * @param archiveFileSummary The summary containing the list of archive file records to be stored. Must not be null.
     */
    public void storeArchiveFileSummary(@NotNull String remoteIp, @NotNull ArchiveFileSummary archiveFileSummary) {

        if (archiveFileSummary == null) {
            return;
        }

        var summaryKey = remoteIp + "::" + archiveFileSummary.getArchiveName() + "::" + archiveFileSummary.getCount();
        getSummaryCache().put(summaryKey, archiveFileSummary);

        if (archiveFileSummary.getFiles() != null) {
            archiveFileSummary.getFiles().forEach(archiveFileRecord -> {
                log.info("Caching Archive File Record, remoteIp={}, archiveFileRecord.id={}.",
                        remoteIp, archiveFileRecord.getId());
                getCache().put(archiveFileRecord.getId(), ArchiveFileCacheWrapper.builder().remoteIp(remoteIp)
                        .file(archiveFileRecord).timestamp(new Date()).build());
            });
        }
    }


    /**
     * Retrieves an {@link ArchiveFileCacheWrapper} from the cache based on the given ID.
     * If no record is found in the cache, it will return {@code null}.
     *
     * @param id The unique identifier of the archive file record to retrieve from the cache. Must not be null.
     * @return The {@code ArchiveFileCacheWrapper} associated with the provided ID, or {@code null} if not found.
     */
    public ArchiveFileCacheWrapper getArchiveFileRecord(String id) {
        log.info("Getting Archive File Record, id={}.", id);
        return getCache().getIfPresent(id);
    }


    /**
     * Retrieves an {@link ArchiveFileSummary} from the cache based on the given remote IP address
     * and archive name. If no summary is found in the cache, it will return {@code null}.
     *
     * @param remoteIp   The IP address associated with the archive file summary. Must not be null.
     * @param archiveName The name of the archive file. Must not be null.
     * @return The {@link ArchiveFileSummary} associated with the given remote IP address and archive name,
     *         or {@code null} if no matching entry is found in the cache.
     */
    public ArchiveFileSummary getArchiveFileSummary(String remoteIp, String archiveName, int count) {
        return getSummaryCache().getIfPresent(remoteIp + "::" + archiveName + "::" + count);
    }


    /**
     * Retrieves the cache instance used to store {@link ArchiveFileCacheWrapper} objects.
     * If the cache is not already initialized, a new cache is created with the configured
     * expiration duration and maximum size.
     *
     * @return The {@code Cache} instance used for storing {@code ArchiveFileCacheWrapper} objects.
     */
    private Cache<String, ArchiveFileCacheWrapper> getCache() {

        if (archiveFileCache != null) {
            return archiveFileCache;
        }

        archiveFileCache = Caffeine.newBuilder()
                .expireAfterWrite(cacheDurationMins, TimeUnit.MINUTES)
                .maximumSize(cacheSize)
                .build();

        return archiveFileCache;
    }

    private Cache<String, ArchiveFileSummary> getSummaryCache() {

        if (archiveSummaryCache != null) {
            return archiveSummaryCache;
        }

        archiveSummaryCache = Caffeine.newBuilder()
                .expireAfterWrite(cacheDurationMins, TimeUnit.MINUTES)
                .maximumSize(cacheSize)
                .build();

        return archiveSummaryCache;
    }

}
