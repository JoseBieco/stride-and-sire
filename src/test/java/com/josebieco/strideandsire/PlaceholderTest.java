package com.josebieco.strideandsire;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class PlaceholderTest {
    @Test
    void modIdIsStrideAndSire() {
        assertEquals("strideandsire", StrideAndSire.MODID);
    }
}
