package org.hipeoplea.airbnb.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateServiceRequestDto {

    @NotBlank
    private String guestId;

    @NotBlank
    private String hostId;

    @NotBlank
    private String message;
}
