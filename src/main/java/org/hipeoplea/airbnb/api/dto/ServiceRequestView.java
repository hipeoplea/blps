package org.hipeoplea.airbnb.api.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServiceRequestView {

    private UUID id;
    private String guestId;
    private String hostId;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private List<ExtraServiceRequestView> extraServiceRequests;
    private List<ChatMessageView> chat;
}
