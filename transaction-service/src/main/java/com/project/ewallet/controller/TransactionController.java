package com.project.ewallet.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.project.ewallet.service.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TransactionController {

    /**
     * senderId
     * receiverId
     * Reason
     * Amount
     */

    @Autowired
    TransactionService transactionService;

    @PostMapping("/transaction")
    public String initiateTransaction(@RequestParam("receiver") String receiver,
                                      @RequestParam("purpose") String purpose,
                                      @RequestParam("amount") String amount) throws JsonProcessingException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        return transactionService.initiateTransaction(userDetails.getUsername(), receiver, purpose, amount);

    }
}
