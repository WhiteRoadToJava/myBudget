package com.mybudget.server.mapMappers;

import com.mybudget.server.dto.Transaction;

import java.util.ArrayList;
import java.util.List;

public final class CancellationRows {
    private CancellationRows() {

    }

    public static List<Transaction> withReversals(List<Transaction> rows) {
        List<Transaction> result = new ArrayList<>(rows);
        for (Transaction row : rows) {
            if (row.isCancelled() && !row.isReversal() && row.getCancelledAt() != null) {
                result.add(reversalOf(row));
            }
        }
        return result;
    }


    public static Transaction reversalOf(Transaction row) {
        Transaction reversal = new Transaction(row.getId(), row.getAmount(), row.getCategory(),
                row.getSourceAccount(), row.getDestinationAccount(), row.getType(),
                row.getCancelledAt(), row.getDescription(), row.getImage());
        reversal.setDebtId(row.getDebtId());
        reversal.setCurrency(row.getCurrency());
        reversal.setCancelled(row.isCancelled());
        reversal.setCancelledAt(row.getCancelledAt());
        reversal.setCancelReason(row.getCancelReason());
        reversal.setNumber(row.getNumber());
        reversal.setReversal(true);

        return reversal;
    }
}

