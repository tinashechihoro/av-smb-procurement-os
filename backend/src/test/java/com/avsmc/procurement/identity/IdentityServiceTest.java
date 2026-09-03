package com.avsmc.procurement.identity;

import com.avsmc.procurement.identity.dto.AuthRequest;
import com.avsmc.procurement.identity.dto.AuthResponse;
import com.avsmc.procurement.identity.service.IdentityService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class IdentityServiceTest {

    @Autowired
    private IdentityService identityService;

    @Test
    void login_withValidCredentials_returnsToken() {
        AuthRequest req = new AuthRequest();
        req.setEmail("admin@avmotors.com");
        req.setPassword("Admin@123");

        AuthResponse res = identityService.login(req);

        assertNotNull(res.getAccessToken());
        assertNotNull(res.getRefreshToken());
        assertEquals("admin@avmotors.com", res.getEmail());
        assertEquals("AV Motors", res.getOrganisationName());
        assertEquals("BUYER", res.getOrgType());
        assertEquals("AV_SYS_ADMIN", res.getRoleCode());
        assertFalse(res.getPermissions().isEmpty());
    }

    @Test
    void login_withWrongPassword_throwsException() {
        AuthRequest req = new AuthRequest();
        req.setEmail("admin@avmotors.com");
        req.setPassword("wrong-password");

        assertThrows(BadCredentialsException.class, () -> identityService.login(req));
    }

    @Test
    void login_withNonexistentUser_throwsException() {
        AuthRequest req = new AuthRequest();
        req.setEmail("nonexistent@test.com");
        req.setPassword("password");

        assertThrows(BadCredentialsException.class, () -> identityService.login(req));
    }

    @Test
    void login_smbAdmin_returnsCorrectOrg() {
        AuthRequest req = new AuthRequest();
        req.setEmail("admin@smbprocurement.com");
        req.setPassword("Admin@123");

        AuthResponse res = identityService.login(req);

        assertEquals("SMB Procurement", res.getOrganisationName());
        assertEquals("SUPPLIER", res.getOrgType());
        assertEquals("SMB_ADMIN", res.getRoleCode());
    }
}
