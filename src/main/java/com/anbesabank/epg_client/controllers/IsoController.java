package com.anbesabank.epg_client.controllers;


import com.anbesabank.epg_client.DTO.IsoRequest;
import com.anbesabank.epg_client.services.ClientRunner;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/iso")
public class IsoController {
    private final ClientRunner clientRunner;

    @PostMapping("/send")
    public ResponseEntity<String> sendIsoMessage(@Valid @RequestBody IsoRequest request) {
        try {
            // Use CompletableFuture to handle async task and respond immediately
            CompletableFuture<String> responseFuture = clientRunner.sendIsoMessage(request.getAmount(), request.getAccountNumber());

            // Return HTTP accepted response while processing happens asynchronously
            return ResponseEntity.accepted().body("Request is being processed. You will get the result shortly.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to send ISO message: " + e.getMessage());
        }
    }
}
