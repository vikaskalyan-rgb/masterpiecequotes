package com.masterpiece.quotes.controller;

import com.masterpiece.quotes.dto.BackupDto;
import com.masterpiece.quotes.dto.RestoreResultDto;
import com.masterpiece.quotes.service.BackupService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/backup")
public class BackupController {

    private final BackupService backupService;

    public BackupController(BackupService backupService) {
        this.backupService = backupService;
    }

    @GetMapping
    public BackupDto export() {
        return backupService.export();
    }

    @PostMapping("/restore")
    public RestoreResultDto restore(@RequestBody BackupDto backup,
                                    @RequestParam(defaultValue = "false") boolean includeSettings) {
        return backupService.restore(backup, includeSettings);
    }
}
