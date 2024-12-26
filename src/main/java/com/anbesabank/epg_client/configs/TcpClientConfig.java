package com.anbesabank.epg_client.configs;


import com.anbesabank.epg_client.DTO.FieldSpec;
import com.solab.iso8583.IsoMessage;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;


@Configuration
@RequiredArgsConstructor
public class TcpClientConfig {
    private static final Logger logger = LoggerFactory.getLogger(TcpClientConfig.class);
    IsoMessage isoMessage;

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
                System.out.println("hex data: " + hexData);
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
            String allAscii = mti + asciiPart + isoMessageData;
            System.out.println("All Ascii: " + allAscii);

            try {
                String mtiNew = allAscii.substring(0, 4);
                System.out.println("MTI New: " + mtiNew);
                String remainingData = allAscii.substring(36);

                // Field specifications for ISO 8583
                Map<Integer, FieldSpec> fieldSpecs = new LinkedHashMap<>();
                fieldSpecs.put(3, new FieldSpec("NUMERIC", 6));
                fieldSpecs.put(4, new FieldSpec("NUMERIC", 12));
                fieldSpecs.put(7, new FieldSpec("DATE10", 10));
                fieldSpecs.put(11, new FieldSpec("NUMERIC", 6));
                fieldSpecs.put(12, new FieldSpec("TIME", 12));
                fieldSpecs.put(18, new FieldSpec("NUMERIC", 4));
                fieldSpecs.put(34, new FieldSpec("NUMERIC", 12));  // Field 34 is defined here
                fieldSpecs.put(37, new FieldSpec("ALPHA", 12));
                fieldSpecs.put(38, new FieldSpec("NUMERIC", 6));
                fieldSpecs.put(39, new FieldSpec("NUMERIC", 2));
                fieldSpecs.put(41, new FieldSpec("ALPHA", 8));
                fieldSpecs.put(42, new FieldSpec("ALPHA", 15));
                fieldSpecs.put(49, new FieldSpec("NUMERIC", 3));
                fieldSpecs.put(102, new FieldSpec("LLVAR", 19));

                // Parse fields
                Map<Integer, String> fieldValues = parseFields(remainingData, fieldSpecs);

                // Log parsed fields
                fieldValues.forEach((key, value) -> {
                    System.out.println("Field " + key + ": " + value);
                    // Check if the field is 34
                    if (key == 34) {
                        System.out.println("Field 34: " + value); // Log Field 34 specifically
                    }
                });

            } catch (Exception e) {
                return "Error processing response: " + e.getMessage();
            }

            // Combine results and return
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
    private Map<Integer, String> parseFields(String data, Map<Integer, FieldSpec> fieldSpecs) {
        Map<Integer, String> fieldValues = new LinkedHashMap<>();
        int currentIndex = 0;

        for (Map.Entry<Integer, FieldSpec> entry : fieldSpecs.entrySet()) {
            int fieldNumber = entry.getKey();
            FieldSpec spec = entry.getValue();

            if (spec.getType().equals("LLVAR")) {
                // Handle LLVAR: First 2 characters indicate length
                int length = Integer.parseInt(data.substring(currentIndex, currentIndex + 2));
                currentIndex += 2;
                String value = data.substring(currentIndex, currentIndex + length);
                fieldValues.put(fieldNumber, value);
                currentIndex += length;
            } else {
                // Handle fixed-length fields
                String value = data.substring(currentIndex, currentIndex + spec.getLength());
                fieldValues.put(fieldNumber, value);
                currentIndex += spec.getLength();
            }
        }
        return fieldValues;
    }

//    // Helper class to define field specifications
//    static class FieldSpec {
//        String type;
//        int length;
//
//        FieldSpec(String type, int length) {
//            this.type = type;
//            this.length = length;
//        }
//
//        FieldSpec(String type) {
//            this.type = type;
//        }
//    }
}
