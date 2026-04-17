package com.artistas.api.web;

import com.artistas.api.dto.regional.RegionalDto;
import com.artistas.api.service.RegionalSyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Operações administrativas sobre a tabela local de regionais (sincronização com API externa GET).
 */
@RestController
@RequestMapping("/api/v1/admin/regionais")
@RequiredArgsConstructor
@Tag(name = "Admin — Regionais Argus")
@SecurityRequirement(name = "bearer-jwt")
public class RegionalAdminController {

    private final RegionalSyncService regionalSyncService;

    @PostMapping("/sync")
    @Operation(summary = "Sincronizar tabela regional com a API Argus")
    public ResponseEntity<RegionalSyncService.SyncResult> sync() {
        return ResponseEntity.ok(regionalSyncService.syncFromArgus());
    }

    @GetMapping
    @Operation(summary = "Listar regionais ativas (após sync)")
    public ResponseEntity<List<RegionalDto>> list() {
        return ResponseEntity.ok(regionalSyncService.listAtivas());
    }
}
