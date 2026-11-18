package fr.monlogcompta.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidationTest {
    @Test
    void siretAcceptsFourteenDigitsWithOrWithoutSpaces() {
        assertTrue(Validation.isValidSiret("41234567890123"));
        assertTrue(Validation.isValidSiret("412 345 678 90123"));
        assertEquals("41234567890123", Validation.normalizeSiret("412 345 678 90123"));
        assertFalse(Validation.isValidSiret("1234"));
        assertFalse(Validation.isValidSiret("4123456789012A"));
    }

    @Test
    void emailAndPasswordRules() {
        assertTrue(Validation.isValidEmail("a@b.fr"));
        assertFalse(Validation.isValidEmail("a@b"));
        assertNotNull(Validation.checkPassword("court"));
        assertNull(Validation.checkPassword("assezlong1"));
    }

    @Test
    void completeClientIsAccepted() {
        assertNull(Validation.checkClient("Ma Société", "41234567890123", "1 rue X", "x@y.fr", "motdepasse1"));
        assertNotNull(Validation.checkClient("", "41234567890123", "1 rue X", "x@y.fr", "motdepasse1"));
    }
}
