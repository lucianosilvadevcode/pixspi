package com.example.pacs008.controller;

import com.example.pacs008.dto.PaymentRequestDto;
import com.example.pacs008.service.Pacs008Service;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller REST para expor o serviço de geração de mensagens pacs.008.
 */
@RestController
@RequestMapping("/api/pix/payments")
@RequiredArgsConstructor
public class Pacs008Controller {

    private final Pacs008Service pacs008Service;

    /**
     * Endpoint para criar uma nova ordem de pagamento Pix (pacs.008).
     * Recebe os dados do pagamento em formato JSON, processa-os e retorna
     * a mensagem XML completa e formatada.
     *
     * @param request DTO com os dados do pagamento.
     * @return Uma ResponseEntity contendo a string XML da mensagem pacs.008
     *         ou uma mensagem de erro em caso de falha.
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> createPixPayment(@RequestBody PaymentRequestDto request) {
        try {
            String pacs008Xml = pacs008Service.createAndProcessPacs008Message(request);
            return ResponseEntity.status(201).body(pacs008Xml);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("<error>Falha ao gerar a mensagem pacs.008: " + e.getMessage() + "</error>");
        }
    }
}