package com.anbesabank.epg_client.services;

import com.solab.iso8583.IsoMessage;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

@Service
@RequiredArgsConstructor
public class TcpClientService {

    private static final Logger logger = LoggerFactory.getLogger(TcpClientService.class);
    private final Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;

    @PostConstruct
    public void init() throws IOException {
        // Initialize I/O streams once
        out = new PrintWriter(clientSocket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

        // Log when connection is established
        if (clientSocket.isConnected()) {
            logger.info("Connection established with {}:{}", clientSocket.getInetAddress(), clientSocket.getPort());
        } else {
            logger.warn("Failed to establish connection.");
        }
    }

    @PreDestroy
    public void cleanup() {
        try {
            if (clientSocket != null && !clientSocket.isClosed()) {
                clientSocket.close();
            }
            if (out != null) {
                out.close();
            }
            if (in != null) {
                in.close();
            }
            logger.info("Connection closed and resources cleaned up.");
        } catch (IOException e) {
            logger.error("Error during cleanup", e);
        }
    }

    public int sendBinaryMessage(IsoMessage isoMessage) {
        try {
            // Log the connection status before sending the message
            logger.info("Socket connected: {}", clientSocket.isConnected());
            logger.info("Socket closed: {}", clientSocket.isClosed());

            // 1. Get the entire ISO message data in binary format
            byte[] messageData = isoMessage.writeData();

            // 2. Define MTI in hexadecimal format as per requirement
            String mtiHex = "30313030";  // "0100" in hex
            logger.info("MTI Hex: {}", mtiHex);

            // 3. Extract the 32 bytes starting from the 5th byte (bytes 5-36) as ASCII
            byte[] first32Bytes = Arrays.copyOfRange(messageData, 4, 36);
            String first32BytesStr = new String(first32Bytes, StandardCharsets.US_ASCII);
            logger.info("First 32 Bytes (ASCII Part): {}", first32BytesStr);

            // 4. Convert the remaining data (starting from byte 37 onward) to hex
            String remainingDataHex = bytesToHex(Arrays.copyOfRange(messageData, 36, messageData.length));
            logger.info("Remaining Data (Converted to Hex): {}", remainingDataHex);

            // 5. Combine MTI, ASCII part, and hex part to form the full message
            String fullMessage = mtiHex + first32BytesStr + remainingDataHex;
            byte[] messageBytes = hexStringToByteArray(fullMessage);
            logger.info("Full Message: {}", fullMessage);

            // Send the message
            clientSocket.getOutputStream().write(messageBytes);
            clientSocket.getOutputStream().flush();
            logger.info("Binary message sent successfully.");

            // Log the connection status again after sending the message
            logger.info("Socket connected: {}", clientSocket.isConnected());
            logger.info("Socket closed: {}", clientSocket.isClosed());

            // Optionally read response
            if (in != null) {
                String response = in.readLine();
                logger.info("Received response: {}", response);
            }

            return 0;
        } catch (IOException e) {
            logger.error("Error sending binary message", e);
            return -1;
        }
    }


    // Helper method to convert a byte array to a hex string
    public String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            hexString.append(String.format("%02X", b));
        }
        return hexString.toString();
    }
    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

}

