package com.mybudget.server.modules;

import com.mybudget.server.modules.enums.FeeDirection;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;


@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "transfers")
public class Transfer {
    @Id
    private String id;
    @DBRef
    private Account sourceAccount;
    @DBRef
    private Account destinationAccount;
    @DBRef
    private User user;
    private Double amountSent;
    private double amountReceived ;

    private String description;
    @CreatedDate
    private LocalDateTime createdAt;
    private Boolean cancelled;
    private LocalDateTime cancelledAt;
    private String cancelReason;
    private Long number;     // the reference number shown to people and in the statement (ReferenceNumbers)
    private String currency;
    private String type = "transfer";
    private double exChangeRate;
    private Map<String, String> image;

    // Fees. Missing values mean "no fee". The fee account is stored explicitly
    // (the source / destination when the user does not choose another one), so a
    // null account with a fee means that account was deleted.
    // multi-currency (filled by scripts/migration; the services switch to it in the next phases)
    private String sourceCurrency;
    private String destinationCurrency;
    private String sendFeeCurrency;
    private String receiveFeeCurrency;

    private Double sendFee;
    private FeeDirection sendFeeDirection;
    @DBRef
    private Account sendFeeAccount;
    private Double receiveFee;
    private FeeDirection receiveFeeDirection;
    @DBRef
    private Account receiveFeeAccount;
}
