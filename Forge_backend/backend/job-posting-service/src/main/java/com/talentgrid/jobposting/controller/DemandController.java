package com.talentgrid.jobposting.controller;

import com.talentgrid.jobposting.dto.response.DemandResponse;
import com.talentgrid.jobposting.service.DemandService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/demands")
@RequiredArgsConstructor
public class DemandController {

    private final DemandService demandService;

    @GetMapping
    public ResponseEntity<List<DemandResponse>> getAll() {
        return ResponseEntity.ok(demandService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<DemandResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(demandService.getById(id));
    }

    @GetMapping("/by-demand-id/{demandId}")
    public ResponseEntity<DemandResponse> getByDemandId(@PathVariable Long demandId) {
        return ResponseEntity.ok(demandService.getByDemandId(demandId));
    }
}
