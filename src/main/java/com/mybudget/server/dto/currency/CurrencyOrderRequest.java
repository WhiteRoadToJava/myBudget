package com.mybudget.server.dto.currency;


import lombok.Data;

import java.util.List;



// every code of the list, in the order the admin wants them
@Data
public class CurrencyOrderRequest {
private List<String> codes;
}
