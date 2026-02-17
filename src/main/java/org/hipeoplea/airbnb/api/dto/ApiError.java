package org.hipeoplea.airbnb.api.dto;

import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiError {

    private OffsetDateTime timestamp;
    private int status;
    private String error;
    private String message;
}
