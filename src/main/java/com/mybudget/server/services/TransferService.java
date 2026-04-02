package com.mybudget.server.services;

import com.mybudget.server.dto.transfer.TransferRequest;
import com.mybudget.server.dto.transfer.TransferResponse;
import com.mybudget.server.exeptions.ResourceNotFoundException;
import com.mybudget.server.modules.Account;
import com.mybudget.server.modules.Transfer;
import com.mybudget.server.modules.User;
import com.mybudget.server.repositories.AccountRepository;
import com.mybudget.server.repositories.TransferRepository;
import com.mybudget.server.util.UserUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TransferService {
    private final AccountRepository accountRepository;
    private final UserUtils userUtils;
    private final TransferRepository transferRepository;
    private  final AccountService accountService;




    public TransferResponse executeTransfet(TransferRequest request){
        User user = userUtils.getCurrentAuthenticatedUser();

        Account sourceAccount = accountRepository.findById(request.getSourceAccount().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
        Account destinationAccount = accountRepository.findById(request.getDestinationAccount().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        Double receivedAmount = request.getAmountSent() * request.getExChangeRate();

        Transfer transfer = new Transfer();
        accountService.updateTotalBalanceWithSemdTransfer(sourceAccount, request.getAmountSent());
        accountService.updateTotalBalanceWithReceivedTransfer(destinationAccount, receivedAmount);
        transfer.setUser(user);
        transfer.setDescription(request.getDescription());
        transfer.setExChangeRate(request.getExChangeRate());
        transfer.setAmountSent(request.getAmountSent());
        transfer.setAmountReceived(receivedAmount);
        transfer.setSourceAccount(sourceAccount);
        transfer.setDestinationAccount(destinationAccount);
        transfer.setCurrency(sourceAccount.getCurrency());

        return  mapToTransferResponse(transferRepository.save(transfer));
    }


    private TransferResponse mapToTransferResponse(Transfer transfer) {
        return new TransferResponse(
                transfer.getId(),
                transfer.getSourceAccount(),
                transfer.getDestinationAccount(),
                transfer.getAmountSent(),
                transfer.getExChangeRate(),
                transfer.getAmountReceived(),
                transfer.getDescription(),
                transfer.getCreatedAt()
        );
    }
}
