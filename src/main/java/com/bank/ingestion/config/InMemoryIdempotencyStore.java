package com.bank.ingestion.config;

import com.bank.ingestion.domain.port.outbound.IdempotencyStore;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
public class InMemoryIdempotencyStore implements IdempotencyStore {

    private final Cache<UUID, Boolean> cache;

    public InMemoryIdempotencyStore(@Value("${app.ingestion.idempotency-ttl-hours:24}") long ttlHours) {
        this.cache = Caffeine.newBuilder()
                .expireAfterWrite(ttlHours, TimeUnit.HOURS)
                .maximumSize(100_000)
                .build();
    }

    @Override
    public boolean isAlreadyProcessed(UUID eventId) {
        return cache.getIfPresent(eventId) != null;
    }

    @Override
    public void markProcessed(UUID eventId) {
        cache.put(eventId, Boolean.TRUE);
    }
}
