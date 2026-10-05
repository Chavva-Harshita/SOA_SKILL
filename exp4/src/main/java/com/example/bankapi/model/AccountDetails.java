package com.example.bankapi.model;

import java.math.BigDecimal;

public record AccountDetails(String accountNumber, String accountHolder, BigDecimal balance) {
}