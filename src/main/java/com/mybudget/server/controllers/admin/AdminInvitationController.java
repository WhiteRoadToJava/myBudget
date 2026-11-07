package com.mybudget.server.controllers.admin;

import com.mybudget.server.dto.admin.AdminAccount;
import com.mybudget.server.dto.admin.InviteAdminRequest;
import com.mybudget.server.services.admin.AdminInvitationService;
import com.mybudget.server.util.UserUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Admin only: SecurityConfig protects /api/admin/** with ROLE_ADMIN
@RestController
@RequestMapping("/api/admin/admins")
@RequiredArgsConstructor
public class AdminInvitationController {

    private final AdminInvitationService invitationService;
    private final UserUtils userUtils;

    @GetMapping
    public ResponseEntity<List<AdminAccount>> list() {
        return ResponseEntity.ok(invitationService.list());
    }

    // body: { "email", "firstName", "lastName", "phone", "language" }
    @PostMapping("/invite")
    public ResponseEntity<AdminAccount> invite(@RequestBody InviteAdminRequest request) {
        AdminAccount created = invitationService.invite(request, userUtils.getCurrentAuthenticatedUser());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/{adminId}/resend")
    public ResponseEntity<AdminAccount> resend(@PathVariable String adminId,
                                               @RequestParam(defaultValue = "ar") String language) {
        return ResponseEntity.ok(invitationService.resend(adminId, language, userUtils.getCurrentAuthenticatedUser()));
    }
}
