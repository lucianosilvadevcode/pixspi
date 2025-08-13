package com.example.pacs008.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * DTO para receber os dados de uma solicitação de pagamento do frontend.
 */
@Data
public class PaymentRequestDto {
    private String payerName;
    private String payerCpfCnpj;
    private String payerIspb;
    private String payerAgency;
    private String payerAccount;
    private String payerAccountType; // CACC, SLRY, SVGS, TRAN

    private String receiverName;
    private String receiverCpfCnpj;
    private String receiverIspb;
    private String receiverAgency;
    private String receiverAccount;
    private String receiverAccountType;
    private String receiverPixKey;

    private BigDecimal amount;
    private String description;
}