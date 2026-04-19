package org.cloudfoundry.samples.music.domain;

import org.junit.Test;
import static org.junit.Assert.*;

public class ApplicationInfoTest {

    @Test
    public void testConstructorAndGetters() {
        String[] profiles = {"mysql", "redis"};
        String[] services = {"db-service", "cache-service"};

        ApplicationInfo info = new ApplicationInfo(profiles, services);

        assertArrayEquals(profiles, info.getProfiles());
        assertArrayEquals(services, info.getServices());
    }

    @Test
    public void testSetProfiles() {
        ApplicationInfo info = new ApplicationInfo(new String[]{}, new String[]{});
        String[] newProfiles = {"postgres"};
        info.setProfiles(newProfiles);
        assertArrayEquals(newProfiles, info.getProfiles());
    }

    @Test
    public void testSetServices() {
        ApplicationInfo info = new ApplicationInfo(new String[]{}, new String[]{});
        String[] newServices = {"my-service"};
        info.setServices(newServices);
        assertArrayEquals(newServices, info.getServices());
    }

    @Test
    public void testEmptyArrays() {
        ApplicationInfo info = new ApplicationInfo(new String[]{}, new String[]{});
        assertEquals(0, info.getProfiles().length);
        assertEquals(0, info.getServices().length);
    }
}
