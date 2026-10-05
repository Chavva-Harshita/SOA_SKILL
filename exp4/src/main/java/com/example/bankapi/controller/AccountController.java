package com.example.bankapi.controller;

import java.math.BigDecimal;

import com.example.bankapi.model.AccountDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/account")
public class AccountController {

	@GetMapping("/details")
	public AccountDetails getAccountDetails() {
		return new AccountDetails("XXXXXX1234", "Test User", new BigDecimal("50000.00"));
	}
}