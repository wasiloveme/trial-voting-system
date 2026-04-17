package com.votingsystem.dto;
import lombok.Data;
@Data
public class VoteReceiptDto { 
    private String receiptHash; 

    public String getReceiptHash() { return receiptHash; }
    public void setReceiptHash(String receiptHash) { this.receiptHash = receiptHash; }
}
