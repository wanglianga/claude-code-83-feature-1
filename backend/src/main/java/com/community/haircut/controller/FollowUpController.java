package com.community.haircut.controller;

import com.community.haircut.common.Result;
import com.community.haircut.entity.FollowUp;
import com.community.haircut.enums.FollowUpStatus;
import com.community.haircut.enums.Role;
import com.community.haircut.security.SecurityUtils;
import com.community.haircut.service.FollowUpService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/follow-ups")
public class FollowUpController {

    private final FollowUpService followUpService;

    public FollowUpController(FollowUpService followUpService) {
        this.followUpService = followUpService;
    }

    @GetMapping
    public Result<List<FollowUp>> list(@RequestParam(required = false) String status,
                                       @RequestParam(required = false) Long elderId) {
        return Result.ok(followUpService.list(status == null ? null : FollowUpStatus.valueOf(status), elderId));
    }

    @PostMapping
    public Result<FollowUp> create(@RequestBody FollowUp followUp) {
        SecurityUtils.requireRole(Role.STAFF, Role.ADMIN, Role.GRID);
        return Result.ok(followUpService.create(followUp));
    }

    public record CompleteRequest(String result, boolean needExtraCare) {
    }

    @PostMapping("/{id}/complete")
    public Result<Void> complete(@PathVariable Long id, @RequestBody CompleteRequest request) {
        SecurityUtils.requireRole(Role.STAFF, Role.ADMIN, Role.GRID, Role.VOLUNTEER);
        followUpService.complete(id, request.result(), request.needExtraCare());
        return Result.ok();
    }
}
