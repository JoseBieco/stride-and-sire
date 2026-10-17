package com.josebieco.strideandsire.command;

import com.josebieco.strideandsire.genetics.Zygosity;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SetCoatCommandTest {

    @Test
    void parseZygosityMapsExactGeneticTokens() throws CommandSyntaxException {
        assertEquals(Zygosity.HOMOZYGOUS_DOMINANT, SetCoatCommand.parseZygosity("AA"));
        assertEquals(Zygosity.HETEROZYGOUS, SetCoatCommand.parseZygosity("Aa"));
        assertEquals(Zygosity.HOMOZYGOUS_RECESSIVE, SetCoatCommand.parseZygosity("aa"));
    }

    @Test
    void parseZygosityRejectsAnyOtherToken() {
        assertThrows(CommandSyntaxException.class, () -> SetCoatCommand.parseZygosity("aA"));
        assertThrows(CommandSyntaxException.class, () -> SetCoatCommand.parseZygosity("AAA"));
        assertThrows(CommandSyntaxException.class, () -> SetCoatCommand.parseZygosity(""));
    }
}
