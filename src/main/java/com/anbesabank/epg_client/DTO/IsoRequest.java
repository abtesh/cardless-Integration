package com.anbesabank.epg_client.DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class IsoRequest {
    @NotBlank(message = "Account number is required.")
    @Pattern(regexp = "\\d{1,19}", message = "Account number must be numeric and up to 19 digits.")
    private String accountNumber;

    @NotBlank(message = "Amount is required.")
    @Pattern(regexp = "\\d+(\\.\\d{1,2})?", message = "Amount must be a valid numeric value with up to two decimal places.")
    private String amount;
}
