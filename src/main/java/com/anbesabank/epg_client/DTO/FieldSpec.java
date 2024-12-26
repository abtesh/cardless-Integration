package com.anbesabank.epg_client.DTO;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class FieldSpec {
    String type;
    int length;
            FieldSpec(String type) {
            this.type = type;
        }
}
