package org.cloudfoundry.samples.music.domain;

import org.junit.Test;
import static org.junit.Assert.*;

public class RandomIdGeneratorTest {

    @Test
    public void testGenerateIdReturnsNonNull() {
        RandomIdGenerator generator = new RandomIdGenerator();
        String id = generator.generateId();
        assertNotNull(id);
    }

    @Test
    public void testGenerateIdReturnsUUIDFormat() {
        RandomIdGenerator generator = new RandomIdGenerator();
        String id = generator.generateId();
        // UUID format: 8-4-4-4-12 hex characters
        assertTrue(id.matches("[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}"));
    }

    @Test
    public void testGenerateIdReturnsUniqueValues() {
        RandomIdGenerator generator = new RandomIdGenerator();
        String id1 = generator.generateId();
        String id2 = generator.generateId();
        assertNotEquals(id1, id2);
    }

    @Test
    public void testGenerateViaHibernateInterface() {
        RandomIdGenerator generator = new RandomIdGenerator();
        // Test the Hibernate IdentifierGenerator interface method
        Object id = generator.generate(null, null);
        assertNotNull(id);
        assertTrue(id instanceof String);
    }
}
