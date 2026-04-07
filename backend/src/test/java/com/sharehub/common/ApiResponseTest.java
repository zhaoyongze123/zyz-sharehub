package com.sharehub.common;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ApiResponseTest {

    @Test
    void failIncludesCode() {
        ApiResponse<Void> response = ApiResponse.fail("ERROR", "Bad request");
        assertEquals(false, response.success());
        assertEquals("ERROR", response.code());
        assertEquals("Bad request", response.message());
    }

    @Test
    void failUsesDefaultCode() {
        ApiResponse<Void> response = ApiResponse.fail("ERR_DEFAULT");
        assertEquals("ERR_DEFAULT", response.code());
        assertEquals("ERR_DEFAULT", response.message());
    }

    @Test
    void okHasOkCode() {
        ApiResponse<String> response = ApiResponse.ok("payload");
        assertEquals(true, response.success());
        assertEquals("payload", response.data());
        assertEquals("OK", response.code());
    }
}
