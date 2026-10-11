package com.waste.management.service;

import com.waste.management.exception.RateLimitExceededException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimiterService {

    private static final int MAX_REQUESTS_PER_EMAIL = 3;
    private static final long EMAIL_WINDOW_SECONDS = 15 * 60; // 15 minutes

    private static final int MAX_REQUESTS_PER_IP = 10;
    private static final long IP_WINDOW_SECONDS = 15 * 60; // 15 minutes

    private final ConcurrentHashMap<String, List<Long>> emailAttempts = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, List<Long>> ipAttempts = new ConcurrentHashMap<>();

    public synchronized void checkRateLimit(String email, String clientIp) {
        long now = Instant.now().getEpochSecond();

        // 1. Check IP rate limit
        if (StringUtils.hasText(clientIp)) {
            List<Long> timestamps = ipAttempts.computeIfAbsent(clientIp, k -> new ArrayList<>());
            purgeOldTimestamps(timestamps, now, IP_WINDOW_SECONDS);
            if (timestamps.size() >= MAX_REQUESTS_PER_IP) {
                throw new RateLimitExceededException("Too many password reset requests from your IP. Please try again in 15 minutes.");
            }
            timestamps.add(now);
        }

        // 2. Check Email rate limit
        if (StringUtils.hasText(email)) {
            String normalizedEmail = email.toLowerCase().trim();
            List<Long> timestamps = emailAttempts.computeIfAbsent(normalizedEmail, k -> new ArrayList<>());
            purgeOldTimestamps(timestamps, now, EMAIL_WINDOW_SECONDS);
            if (timestamps.size() >= MAX_REQUESTS_PER_EMAIL) {
                throw new RateLimitExceededException("Too many password reset attempts for this email. Please try again in 15 minutes.");
            }
            timestamps.add(now);
        }
    }

    private void purgeOldTimestamps(List<Long> timestamps, long now, long windowSeconds) {
        Iterator<Long> iter = timestamps.iterator();
        while (iter.hasNext()) {
            Long ts = iter.next();
            if (now - ts > windowSeconds) {
                iter.remove();
            }
        }
    }

    public synchronized void clearForTest() {
        emailAttempts.clear();
        ipAttempts.clear();
    }
}
