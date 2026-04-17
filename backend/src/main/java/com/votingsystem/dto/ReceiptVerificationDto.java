package com.votingsystem.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReceiptVerificationDto {
    private String receiptHash;

    public String getReceiptHash() { return receiptHash; }
    public void setReceiptHash(String receiptHash) { this.receiptHash = receiptHash; }
}
