package com.teleconnect.subscriber.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.List;

@Data
@AllArgsConstructor
public class AccountListResponseDTO {
    private List<AccountResponseDTO> subscribers;
    private Integer totalCount;
}