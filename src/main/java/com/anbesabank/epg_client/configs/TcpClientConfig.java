package com.anbesabank.epg_client.configs;

import ch.qos.logback.core.joran.event.SaxEventRecorder;
import com.anbesabank.epg_client.services.TcpClientService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

@Configuration
@RequiredArgsConstructor
public class TcpClientConfig {
    private static final Logger logger = LoggerFactory.getLogger(TcpClientConfig.class);
    @Bean
    public Socket clientSocket() throws IOException {
        Socket socket = new Socket("192.168.20.5", 9234); // Server IP and Port
        // Enable TCP keep-alive
        try {
            socket.setKeepAlive(true);
            new Thread(() -> handleClient(socket)).start();
        } catch (SocketException e) {
            throw new IOException("Failed to set keep-alive option", e);
        }

        // Optionally, you can also set other socket options here if needed
        // e.g., socket.setSoTimeout(60000); // Set read timeout to 60 seconds

        return socket;
    }

//    private void handleClient(Socket clientSocket) {
//        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.US_ASCII))) {
//
//            String response;
//            while ((response = in.readLine()) != null) {
//                // Log the raw response
//                System.out.println("Raw Response: " + response);
//
//                // Parse the response to match the structure
//                String parsedResponse = parseResponse(response);
//                System.out.println("Parsed Response: " + parsedResponse);
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
private void handleClient(Socket clientSocket) {
    try (InputStream in = clientSocket.getInputStream()) {
        byte[] buffer = new byte[1024];
        int bytesRead;

        while ((bytesRead = in.read(buffer)) != -1) {
            byte[] rawData = Arrays.copyOf(buffer, bytesRead);
            String hexData = bytesToHex(rawData);

            // Process the response
            String processedResponse = processResponse(hexData);

            System.out.println("Processed Response: " + processedResponse);
        }
    } catch (IOException e) {
        e.printStackTrace();
    }
}

    private String processResponse(String hexResponse) {
        try {
            // Split the response
            String mtiHex = hexResponse.substring(0, 8); // First 8 hex digits
            String asciiPart = hexResponse.substring(8, 40); // Next 32 characters
            String remainingHex = hexResponse.substring(40); // Remaining data

            // Convert MTI to ASCII
            String mti = hexToAscii(mtiHex);

            // Convert remaining data to ASCII
            String isoMessageData = hexToAscii(remainingHex);

            // Combine results
            return String.format("MTI: %s, ASCII Part: %s, ISO 8583 Data: %s", mti, asciiPart, isoMessageData);
        } catch (Exception e) {
            return "Error processing response: " + hexResponse;
        }
    }

    private String hexToAscii(String hex) {
        StringBuilder output = new StringBuilder();
        for (int i = 0; i < hex.length(); i += 2) {
            String str = hex.substring(i, i + 2);
            output.append((char) Integer.parseInt(str, 16));
        }
        return output.toString();
    }

    public String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            hexString.append(String.format("%02X", b));
        }
        return hexString.toString();
    }





//    private String parseResponse(String response) {
//        try {
//            if (response.length() < 40) { // Minimum expected length: MTI (8) + ASCII (32)
//                return "Response too short: " + response;
//            }
//
//            // MTI (First 8 characters)
//            String mtiHex = response.substring(0, 8);
//
//            // ASCII Part (Next 32 characters)
//            String asciiPart = response.substring(8, 40);
//
//            // Remaining Data (Hex-encoded)
//            String remainingData = bytesToHex(response.substring(40).getBytes(StandardCharsets.US_ASCII));
//
//            return String.format("MTI (Hex): %s, ASCII Part: %s, Remaining Data (Hex): %s", mtiHex, asciiPart, remainingData);
//        } catch (Exception e) {
//            logger.error("Error parsing response: {}", response, e);
//            return "Response format is incorrect: " + response;
//        }
//    }
}
