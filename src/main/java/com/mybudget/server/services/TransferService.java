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
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional
    public TransferResponse updateTransfer(String transferId, TransferRequest request){
        User currentUser = userUtils.getCurrentAuthenticatedUser();
        // check if transfer exists
        Transfer existedTransfer = transferRepository.findById(transferId)
                .orElseThrow(() -> new ResourceNotFoundException("Transfer not found"));
        //  find the account's bodys
        String sourceAccountId = request.getSourceAccount().getId();
        String destinationAccountId = request.getDestinationAccount().getId();;
        Account sourceAccount = accountRepository.findById(sourceAccountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
        Account destinationAccount = accountRepository.findById(destinationAccountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
        // update the account's bodys totalBalances
        String response = accountService.updateAccountsWithUpadateTransfer(sourceAccount, destinationAccount, existedTransfer, request);

        if(response.equals("success")) {
            existedTransfer.setAmountSent(request.getAmountSent());
            existedTransfer.setAmountReceived(request.getAmountReceived());
            existedTransfer.setExChangeRate(request.getExChangeRate());
            existedTransfer.setDescription(request.getDescription());
            return mapToTransferResponse(transferRepository.save(existedTransfer));
        } else {
            throw new ResourceNotFoundException("Transfer not found or access denied");
        }
    }

    public TransferResponse getTransferById(String transferId){
        User currentUser = userUtils.getCurrentAuthenticatedUser();
        Transfer transfer = transferRepository.findById(transferId)
                .orElseThrow(() -> new ResourceNotFoundException("Transfer not found or access denied"));
        if(!transfer.getUser().getId().equals(currentUser.getId())){
            throw new ResourceNotFoundException("Transfer not found or access denied");
            } else {
            return mapToTransferResponse(transfer);
        }
    }

    public String deleteTransfer(String transferId){
        User currentUser = userUtils.getCurrentAuthenticatedUser();
        Transfer transfer = transferRepository.findById(transferId)
                .orElseThrow(()-> new ResourceNotFoundException("Transfer is not found..."));
        if (transfer.getUser().getId().equals(currentUser.getId())){
            String response = accountService.updateAccountsTotalBalanceWithDeleteTransfer(transfer);
            if (response.equals("success")){
                transferRepository.delete(transfer);
            } else {
                throw new ResourceNotFoundException("Transfer not found or access denied");
            }
        }
        return "Transfer deleted successfully";
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
