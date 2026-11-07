package com.healthcare.exception;

import java.time.Instant;
import java.util.Map;

/** Uniform error body returned by every failing endpoint. */
public record ErrorResponse(Instant timestamp, int status, String code, String message, String path,
                            Map<String, String> fieldErrors) {}
