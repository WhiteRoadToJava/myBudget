package com.mybudget.server.services.transactions;

import com.mybudget.server.dto.transfer.TransferRequest;
import com.mybudget.server.dto.transfer.TransferResponse;
import com.mybudget.server.exeptions.ResourceNotFoundException;
import com.mybudget.server.modules.Account;
import com.mybudget.server.modules.Transfer;
import com.mybudget.server.modules.User;
import com.mybudget.server.modules.enums.CashFlow;
import com.mybudget.server.modules.enums.FeeDirection;
import com.mybudget.server.repositories.AccountRepository;
import com.mybudget.server.repositories.TransferRepository;
import com.mybudget.server.services.account.AccountBalances;
import com.mybudget.server.services.account.AccountService;
import com.mybudget.server.util.UserUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TransferService {
    private final AccountRepository accountRepository;
    private final UserUtils userUtils;
    private final TransferRepository transferRepository;
    private final AccountService accountService;

    public TransferResponse executeTransfer(TransferRequest request) {
        return create(request, userUtils.getCurrentAuthenticatedUser());
    }

    // used by the schedules, which run without a logged-in user
    public TransferResponse executeTransfer(TransferRequest request, User user) {
        return create(request, user);
    }

    public TransferResponse updateTransfer(String transferId, TransferRequest request) {
        User user = userUtils.getCurrentAuthenticatedUser();
        Transfer transfer = findOwnedTransfer(transferId, user);

        // what the transfer did before the change, so it can be undone exactly
        List<TransferEffects.Effect> before = TransferEffects.of(transfer);
        fill(transfer, request, user);
        if (request.getCreatedAt() != null) {
            transfer.setCreatedAt(request.getCreatedAt());
        }
        List<TransferEffects.Effect> after = TransferEffects.of(transfer);

        apply(before, -1);
        apply(after, +1);
        try {
            return mapToTransferResponse(transferRepository.save(transfer));
        } catch (RuntimeException e) {
            apply(after, -1);
            apply(before, +1);
            throw e;
        }
    }

    public TransferResponse getTransferById(String transferId) {
        User currentUser = userUtils.getCurrentAuthenticatedUser();
        return mapToTransferResponse(findOwnedTransfer(transferId, currentUser));
    }

    public String deleteTransfer(String transferId) {
        User currentUser = userUtils.getCurrentAuthenticatedUser();
        Transfer transfer = findOwnedTransfer(transferId, currentUser);
        List<TransferEffects.Effect> effects = TransferEffects.of(transfer);

        apply(effects, -1);
        try {
            transferRepository.delete(transfer);
        } catch (RuntimeException e) {
            apply(effects, +1);
            throw e;
        }
        return "Transfer deleted successfully";
    }

    // ---------- create / fill ----------

    private TransferResponse create(TransferRequest request, User user) {
        Transfer transfer = new Transfer();
        transfer.setUser(user);
        fill(transfer, request, user);

        List<TransferEffects.Effect> effects = TransferEffects.of(transfer);
        apply(effects, +1);
        try {
            return mapToTransferResponse(transferRepository.save(transfer));
        } catch (RuntimeException e) {
            apply(effects, -1);
            throw e;
        }
    }

    // validates the request and copies it into the transfer; changes no balance
    private void fill(Transfer transfer, TransferRequest request, User user) {
        validate(request);
        Account source = findOwnedAccount(request.getSourceAccount(), user);
        Account destination = findOwnedAccount(request.getDestinationAccount(), user);
        // a currency of each account; none given = the account's first currency
        String sourceCurrency = AccountBalances.resolve(source, request.getSourceCurrency());
        String destinationCurrency = AccountBalances.resolve(destination, request.getDestinationCurrency());
        // the same account is fine in two currencies (an exchange), not twice the same pocket
        if (source.getId().equals(destination.getId()) && sourceCurrency.equals(destinationCurrency)) {
            throw new IllegalArgumentException("A transfer needs two different accounts or two different currencies");
        }

        transfer.setSourceAccount(source);
        transfer.setDestinationAccount(destination);
        transfer.setAmountSent(request.getAmountSent());
        transfer.setExChangeRate(request.getExChangeRate());
        transfer.setAmountReceived(round(request.getAmountSent() * request.getExChangeRate()));
        transfer.setSourceCurrency(sourceCurrency);
        transfer.setDestinationCurrency(destinationCurrency);
        transfer.setCurrency(sourceCurrency);
        transfer.setDescription(request.getDescription());
        transfer.setImage(request.getImage());

        double sendFee = feeValue(request.getSendFee());
        transfer.setSendFee(sendFee);
        transfer.setSendFeeDirection(direction(request.getSendFeeDirection()));
        Account sendFeeAccount = sendFee > 0 ? feeAccount(request.getSendFeeAccount(), source, user) : null;
        transfer.setSendFeeAccount(sendFeeAccount);
        transfer.setSendFeeCurrency(sendFeeAccount == null ? null
                : feeCurrency(sendFeeAccount, request.getSendFeeCurrency(), source, sourceCurrency));

        double receiveFee = feeValue(request.getReceiveFee());
        transfer.setReceiveFee(receiveFee);
        transfer.setReceiveFeeDirection(direction(request.getReceiveFeeDirection()));
        Account receiveFeeAccount = receiveFee > 0 ? feeAccount(request.getReceiveFeeAccount(), destination, user) : null;
        transfer.setReceiveFeeAccount(receiveFeeAccount);
        transfer.setReceiveFeeCurrency(receiveFeeAccount == null ? null
                : feeCurrency(receiveFeeAccount, request.getReceiveFeeCurrency(), destination, destinationCurrency));
    }

    private void validate(TransferRequest request) {
        if (request.getAmountSent() == null || request.getAmountSent() <= 0) {
            throw new IllegalArgumentException("The amount sent must be greater than zero");
        }
        if (request.getExChangeRate() == null || request.getExChangeRate() <= 0) {
            throw new IllegalArgumentException("The exchange rate must be greater than zero");
        }
        if (request.getSendFee() != null && request.getSendFee() < 0
                || request.getReceiveFee() != null && request.getReceiveFee() < 0) {
            throw new IllegalArgumentException("A fee can not be negative");
        }
    }

    // a fee on its side's account is in that side's currency unless another one is chosen;
    // a fee on another account is in the chosen currency, or that account's first one
    private String feeCurrency(Account feeAccount, String requested, Account sideAccount, String sideCurrency) {
        boolean onSide = feeAccount.getId().equals(sideAccount.getId());
        if (onSide && (requested == null || requested.isBlank())) {
            return sideCurrency;
        }
        return AccountBalances.resolve(feeAccount, requested);
    }

    // the chosen account, or the account on the fee's side when none is chosen
    private Account feeAccount(Account requested, Account sideAccount, User user) {
        if (requested == null || requested.getId() == null || requested.getId().isBlank()) {
            return sideAccount;
        }
        return findOwnedAccount(requested, user);
    }

    private double feeValue(Double fee) {
        return fee == null ? 0.0 : round(fee);
    }

    private FeeDirection direction(FeeDirection direction) {
        return direction == null ? FeeDirection.CHARGE : direction;
    }

    // sign +1 applies the effects, -1 undoes them; each account is read fresh
    private void apply(List<TransferEffects.Effect> effects, int sign) {
        for (TransferEffects.Effect effect : effects) {
            accountRepository.findById(effect.account().getId())
                    .ifPresent(account -> accountService.applyCashFlow(account, effect.currency(), CashFlow.IN, sign * effect.amount()));
        }
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    // ---------- mapping and lookups ----------

    private TransferResponse mapToTransferResponse(Transfer transfer) {
        return new TransferResponse(
                transfer.getId(),
                transfer.getSourceAccount(),
                transfer.getDestinationAccount(),
                transfer.getAmountSent(),
                transfer.getExChangeRate(),
                transfer.getAmountReceived(),
                transfer.getDescription(),
                transfer.getCreatedAt(),
                transfer.getImage(),
                transfer.getSendFee(),
                transfer.getSendFeeDirection(),
                transfer.getSendFeeAccount(),
                transfer.getReceiveFee(),
                transfer.getReceiveFeeDirection(),
                transfer.getReceiveFeeAccount(),
                TransferEffects.sourceCurrency(transfer),
                TransferEffects.destinationCurrency(transfer),
                TransferEffects.sendFeeCurrency(transfer),
                TransferEffects.receiveFeeCurrency(transfer)
        );
    }

    private Account findOwnedAccount(Account requested, User user) {
        if (requested == null || requested.getId() == null || user == null) {
            throw new ResourceNotFoundException("Account not found");
        }
        Account account = accountRepository.findByIdAndUser(requested.getId(), user);
        if (account == null) {
            throw new ResourceNotFoundException("Account not found");
        }
        return account;
    }

    private Transfer findOwnedTransfer(String transferId, User user) {
        Transfer transfer = transferRepository.findById(transferId)
                .orElseThrow(() -> new ResourceNotFoundException("Transfer not found"));
        if (transfer.getUser() == null || !transfer.getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Transfer not found");
        }
        return transfer;
    }
}
