package com.hethongtrongbanking.nienluancosonganh.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.hethongtrongbanking.nienluancosonganh.service.DataSimulatorService;

import lombok.RequiredArgsConstructor;

import java.util.Map;

/**
 * API dieu khien Data Simulator
 * Thay cho auto-start, gio day dung Postman de kich hoat/dung
 */
@RestController
@RequestMapping("/api/simulator")
@RequiredArgsConstructor
public class SimulatorController {

    private final DataSimulatorService dataSimulatorService;

    // Bat dau simulator
    // delay (ms) la tuychon, mac dinh 200ms = 5 GD/giay
    @PostMapping("/start")
    public ResponseEntity<Map<String, String>> start(
            @RequestParam(defaultValue = "200") int delay) {
        String result = dataSimulatorService.startSimulation(delay);
        return ResponseEntity.ok(Map.of("message", result));
    }

    // Dung simulator
    @PostMapping("/stop")
    public ResponseEntity<Map<String, String>> stop() {
        String result = dataSimulatorService.stopSimulation();
        return ResponseEntity.ok(Map.of("message", result));
    }

    // Xem trang thai
    @GetMapping("/status")
    public ResponseEntity<Map<String, String>> status() {
        String result = dataSimulatorService.getStatus();
        return ResponseEntity.ok(Map.of("status", result));
    }
}
