package com.project.ewallet.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.ewallet.CommonConstants;
import com.project.ewallet.WalletUpdateStatus;
import com.project.ewallet.model.Transaction;
import com.project.ewallet.model.TransactionStatus;
import com.project.ewallet.repository.TransactionRepository;
import lombok.extern.slf4j.Slf4j;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
public class TransactionService implements UserDetailsService {

    @Autowired
    TransactionRepository transactionRepository;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    ObjectMapper objectMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        //somehow this method have to return UserDetails.

        JSONObject requestedUser = getUserFromUserService(username);
        List<LinkedHashMap<String, String>> requestedAuthorities =
                (List<LinkedHashMap<String, String>>) requestedUser.get("authorities");
        List<SimpleGrantedAuthority> authorities = requestedAuthorities.stream()
                .map(x-> x.get("authority"))
                .map(x-> new SimpleGrantedAuthority(x))
                .collect(Collectors.toList());
        return new User((String)requestedUser.get("username"), (String)requestedUser.get("password"), authorities);
    }

    public String initiateTransaction(String sender, String receiver, String purpose, String amount) throws JsonProcessingException {

        Transaction transaction = Transaction.builder()
                .sender(sender)
                .receiver(receiver)
                .purpose(purpose)
                .transactionId(UUID.randomUUID().toString())
                .amount(amount)
                .transactionStatus(TransactionStatus.PENDING)
                .build();

        transactionRepository.save(transaction);

        //TODO: publish kafka event.
        ObjectMapper objectMapper = new ObjectMapper();
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("sender", transaction.getSender());
        jsonObject.put("receiver", transaction.getReceiver());
        jsonObject.put("amount", transaction.getAmount());
        jsonObject.put("transactionId", transaction.getTransactionId());

        kafkaTemplate.send(CommonConstants.TRANSACTION_CREATION_TOPIC, objectMapper.writeValueAsString(jsonObject));

        return transaction.getTransactionId();
    }

    @KafkaListener(topics = CommonConstants.WALLET_UPDATED_TOPIC, groupId = "123")
    public void updateTxn(String message) throws JsonProcessingException, ParseException {
        JSONObject jsonObject = (JSONObject) new JSONParser().parse(message);
        Double amount = (Double) jsonObject.get("amount");
        String transactionId = (String) jsonObject.get("transactionId");
        String sender = (String) jsonObject.get("sender");
        String receiver = (String) jsonObject.get("receiver");
        WalletUpdateStatus walletUpdateStatus = WalletUpdateStatus.valueOf((String) jsonObject.get("walletUpdateStatus"));
        
        JSONObject senderObj = getUserFromUserService(sender);
        String senderEmail = (String) senderObj.get("email");
        String receiverEmail = null;

        if(walletUpdateStatus == WalletUpdateStatus.SUCCESS){
            JSONObject receiverObj = getUserFromUserService(receiver);
            receiverEmail = (String) receiverObj.get("email");
            transactionRepository.updateTransaction(transactionId, TransactionStatus.SUCCESS);
        }else{
            transactionRepository.updateTransaction(transactionId, TransactionStatus.FAILED);
        }

        //Send transaction status emails to sender and receiver.
        String senderMessage = "Hi, your transaction with Id: " + transactionId + " is " + walletUpdateStatus;
        JSONObject senderEmailObj = new JSONObject();
        senderEmailObj.put("email", senderEmail);
        senderEmailObj.put("message", senderMessage);

        kafkaTemplate.send(CommonConstants.TRANSACTION_COMPLETED_TOPIC, objectMapper.writeValueAsString(senderEmailObj));

        if(walletUpdateStatus == WalletUpdateStatus.SUCCESS){
            String receiverMessage = "Hi, you have received Rs."+amount+" from "
                    +sender+" in your wallet linked with phoneNumber "+receiver;
            JSONObject receiverEmailObj = new JSONObject();
            receiverEmailObj.put("email" ,receiverEmail);
            receiverEmailObj.put("message", receiverMessage);

            kafkaTemplate.send(CommonConstants.TRANSACTION_COMPLETED_TOPIC, objectMapper.writeValueAsString(receiverEmailObj));
        }
    }

    private JSONObject getUserFromUserService(String username) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setBasicAuth("txn_service", "txn123");
        HttpEntity request = new HttpEntity<>(httpHeaders);

        return restTemplate
                .exchange("http://localhost:6001/admin/user/"+username, HttpMethod.GET,request, JSONObject.class)
                .getBody();
    }
}
