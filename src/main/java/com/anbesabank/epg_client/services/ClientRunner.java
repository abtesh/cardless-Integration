package com.anbesabank.epg_client.services;

import com.solab.iso8583.IsoMessage;
import com.solab.iso8583.IsoType;
import com.solab.iso8583.MessageFactory;
import com.solab.iso8583.parse.ConfigParser;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
public class ClientRunner{

    private static final Logger logger = LoggerFactory.getLogger(ClientRunner.class);
    private final TcpClientService tcpClientService;

//    @Override
//    public void run(String... args) throws Exception {
//        // Build the ISO 8583 message
//        IsoMessage isoMessage = createIsoMessage();
//
//        // Send the binary message
//        tcpClientService.sendBinaryMessage(isoMessage);
//    }
public CompletableFuture<String> sendIsoMessage(String amount, String accountNumber) throws IOException {
    // Build the ISO 8583 message
    IsoMessage isoMessage = createIsoMessage(amount, accountNumber);

    // Return a CompletableFuture that handles the TCP communication asynchronously
    return CompletableFuture.supplyAsync(() -> {
        int status = tcpClientService.sendBinaryMessage(isoMessage);
        if (status == 0) {
            // If status is 0, the message was sent successfully
            return "ISO message sent and response received successfully.";
        } else {
            // If status isn't 0, there was an issue sending the message
            return "Failed to send ISO message.";
        }
    });
}

    private IsoMessage createIsoMessage(String amount, String accountNumber) throws IOException {
        // Validate inputs
        if (accountNumber == null || accountNumber.isBlank()) {
            throw new IllegalArgumentException("Account number cannot be null or blank.");
        }
        if (amount == null || amount.isBlank()) {
            throw new IllegalArgumentException("Amount cannot be null or blank.");
        }

        // Create the ISO message
        MessageFactory<IsoMessage> messageFactory = ConfigParser.createDefault();
        IsoMessage isoMessage = messageFactory.newMessage(0x0100);

        // Set the message fields
        isoMessage.setValue(3, "520000", IsoType.NUMERIC, 6);
        isoMessage.setValue(4, amount, IsoType.NUMERIC, 12);
        isoMessage.setValue(7, generateField7Value(), IsoType.NUMERIC, 10); // Field 7: Transmission Date & Time
        String field11Value = generateRandomNumber();
        isoMessage.setValue(11, field11Value, IsoType.NUMERIC, 6);
        isoMessage.setValue(12, generateField12Value(), IsoType.NUMERIC, 12); // Field 12: Time, Local Transaction
        isoMessage.setValue(18, "6011", IsoType.NUMERIC, 4);
        isoMessage.setValue(41, "LICLT232", IsoType.ALPHA, 8);
        isoMessage.setValue(42, "LIONMERCL232323", IsoType.ALPHA, 15);
        isoMessage.setValue(48, "361009351940189803709", IsoType.LLLVAR, 21);
        isoMessage.setValue(49, "230", IsoType.NUMERIC, 3);
        isoMessage.setValue(102, "00138".concat(accountNumber), IsoType.LLVAR, 19);

        return isoMessage;
    }


    //"0013800310872732"

    private String generateRandomNumber() {
        SecureRandom random = new SecureRandom();
        int num = random.nextInt(900000) + 100000;
        return String.valueOf(num);
    }

    private String generateField7Value() {
        // Generate current date and time in MMDDhhmmss format
        LocalDateTime now = LocalDateTime.now(ZoneId.of("UTC"));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMddHHmmss");
        return now.format(formatter);
    }

    private String generateField12Value() {
        // Generate current date and time in YYMMDDhhmmss format
        LocalDateTime now = LocalDateTime.now(ZoneId.of("UTC"));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyMMddHHmmss");
        return now.format(formatter);
    }
}

