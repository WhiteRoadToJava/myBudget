package com.mybudget.server.services;

import com.mybudget.server.dto.transfer.TransferRequest;
import com.mybudget.server.dto.transfer.TransferResponse;
import com.mybudget.server.exeptions.ResourceNotFoundException;
import com.mybudget.server.modules.Account;
import com.mybudget.server.modules.Transfer;
import com.mybudget.server.modules.User;
import com.mybudget.server.repositories.AccountRepository;
import com.mybudget.server.repositories.TransferRepository;
import com.mybudget.server.services.account.AccountService;
import com.mybudget.server.util.UserUtils;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TransferService {
    private final AccountRepository accountRepository;
    private final UserUtils userUtils;
    private final TransferRepository transferRepository;
    private final AccountService accountService;


    public TransferResponse excuteTransfer(TransferRequest request) {
        User user = userUtils.getCurrentAuthenticatedUser();

        Account sourceAccount = findOwnedAccount(request.getSourceAccount(), user);
        Account destinationAccount = findOwnedAccount(request.getDestinationAccount(), user);
        double receivedAmount = request.getAmountSent() * request.getExChangeRate();

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
        transfer.setImage(request.getImage());

        return mapToTransferResponse(transferRepository.save(transfer));
    }

    // This method is used with schedule
    @Transactional
    public TransferResponse excuteTransfer(TransferRequest request, User user) {
        Account sourceAccount = findOwnedAccount(request.getSourceAccount(), user);
        Account destinationAccount = findOwnedAccount(request.getDestinationAccount(), user);
        double receivedAmount = request.getAmountSent() * request.getExChangeRate();

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


        return mapToTransferResponse(transferRepository.save(transfer));
    }


    @Transactional
    public TransferResponse updateTransfer(String transferId, TransferRequest request) {
        User currentUser = userUtils.getCurrentAuthenticatedUser();
        // check if transfer exists
        Transfer existedTransfer = findOwnedTransfer(transferId, currentUser);
        Account sourceAccount = findOwnedAccount(request.getSourceAccount(), currentUser);
        Account destinationAccount = findOwnedAccount(request.getDestinationAccount(), currentUser);
        // update the account's bodys totalBalances
        String response = accountService.updateAccountsWithUpdateTransfer(sourceAccount, destinationAccount, existedTransfer, request);

        if (response.equals("success")) {
            existedTransfer.setAmountSent(request.getAmountSent());
            existedTransfer.setAmountReceived(request.getAmountReceived());
            existedTransfer.setExChangeRate(request.getExChangeRate());
            existedTransfer.setDescription(request.getDescription());
            existedTransfer.setCreatedAt(request.getCreatedAt());
            existedTransfer.setImage(request.getImage());
            return mapToTransferResponse(transferRepository.save(existedTransfer));
        } else {
            throw new ResourceNotFoundException("Transfer not found or access denied");
        }
    }

    public TransferResponse getTransferById(String transferId) {
        User currentUser = userUtils.getCurrentAuthenticatedUser();
        return mapToTransferResponse(findOwnedTransfer(transferId, currentUser));
    }

    public String deleteTransfer(String transferId) {
        User currentUser = userUtils.getCurrentAuthenticatedUser();
        Transfer transfer = findOwnedTransfer(transferId, currentUser);
        String response = accountService.updateAccountsTotalBalanceWithDeleteTransfer(transfer);
        if (!response.equals("success")){
            throw new ResourceNotFoundException("transfer not found or access denied");
        }
        transferRepository.save(transfer);
        return "Transfer deleted successfully";
    }


    private TransferResponse mapToTransferResponse(Transfer transfer) {
        Map<String, String> image = transfer.getImage();
        Map<String, String> imageUrl = new HashMap<>();
        if (image != null && image.get("url") != null) {
            imageUrl.put("url", image.get("url"));
            imageUrl.put("filename", image.get("filename"));
        } else {
            imageUrl.put("url", "");
            imageUrl.put("filename", "");
        }
        return new TransferResponse(
                transfer.getId(),
                transfer.getSourceAccount(),
                transfer.getDestinationAccount(),
                transfer.getAmountSent(),
                transfer.getExChangeRate(),
                transfer.getAmountReceived(),
                transfer.getDescription(),
                transfer.getCreatedAt(),
                transfer.getImage()
        );
    }

    private Account findOwnedAccount(Account requested, User user){
        if (requested == null|| requested.getId() == null || user == null){
            throw new ResourceNotFoundException("Account not found");
        }
        Account account = accountRepository.findByIdAndUser(requested.getId(), user);
        if (account == null){
            throw new ResourceNotFoundException("account not found");
        }
        return account;
    }
    private Transfer findOwnedTransfer(String transferId, User user){
        Transfer transfer = transferRepository.findById(transferId)
                .orElseThrow(()-> new ResourceNotFoundException("Transfer not found"));
        if (transfer.getUser() == null || !transfer.getUser().getId().equals(user.getId())){
            throw new ResourceNotFoundException("Transfer not found");
        }
        return transfer;
    }
}
