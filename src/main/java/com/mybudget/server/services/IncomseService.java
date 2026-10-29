package com.mybudget.server.services;


import com.mybudget.server.dto.incomse.IncomseRequset;
import com.mybudget.server.dto.incomse.IncomseResponse;
import com.mybudget.server.exeptions.ResourceNotFoundException;
import com.mybudget.server.modules.Account;
import com.mybudget.server.modules.Incomse;
import com.mybudget.server.modules.User;
import com.mybudget.server.modules.enums.CashFlow;
import com.mybudget.server.repositories.AccountRepository;
import com.mybudget.server.repositories.DebtMovementRepository;
import com.mybudget.server.repositories.IncomseRepository;
import com.mybudget.server.services.account.AccountService;
import com.mybudget.server.util.UserUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class IncomseService {
    private final UserUtils userUtils;
    private final AccountService accountService;
    private final IncomseRepository incomseRepository;
    private final AccountRepository accountRepository;
    private final DebtMovementRepository debtMovementRepository;

    @Transactional
    public IncomseResponse addInccmse(IncomseRequset incomseRequest) {
        User currentUser = userUtils.getCurrentAuthenticatedUser();
        Incomse incomse = new Incomse();
        incomse.setAmount(incomseRequest.getAmount());
        incomse.setCategory(incomseRequest.getCategory());
        incomse.setUser(currentUser);

        String accountId = incomseRequest.getAccount().getId();
        Account account = accountRepository.findByIdAndUser(accountId, currentUser);
        if (account == null) {
            throw new ResourceNotFoundException("Account not found");
        }
        incomse.setAccount(account);
        accountService.updateTotalBalanceWithIncome(account, incomseRequest.getAmount());

        return mapToIncomseResponse(incomseRepository.save(incomse));
    }

    // this method is used with schedule
    @Transactional
    public IncomseResponse excuteIncomse(IncomseRequset requset, User user) {
        Incomse incomse = new Incomse();
        incomse.setAmount(requset.getAmount());
        incomse.setCategory(requset.getCategory());
        incomse.setUser(user);

        String accountId = requset.getAccount().getId();
        Account account = accountRepository.findByIdAndUser(accountId, user);
        if (account == null) {
            throw new ResourceNotFoundException("Account not found");
        }
        incomse.setAccount(account);
        accountService.updateTotalBalanceWithIncome(account, requset.getAmount());

        return mapToIncomseResponse(incomseRepository.save(incomse));
    }

    public IncomseResponse getIncomseById(String incomseId) {
        User currentUser = userUtils.getCurrentAuthenticatedUser();
        Incomse incomse = incomseRepository.findById(incomseId)
                .orElseThrow(() -> new ResourceNotFoundException("Incomse not found or access denied"));

        if (!incomse.getUser().getId().equals(currentUser.getId())) {
            throw new ResourceNotFoundException("Incomse not found or access denied");
        } else {
            return mapToIncomseResponse(incomse);
        }
    }

    @Transactional
    public IncomseResponse updateIncomse(String incomseId, IncomseRequset requset) {
        User currentUser = userUtils.getCurrentAuthenticatedUser();
        Incomse incomse = findOwnedIncomse(incomseId, currentUser);
        Account newAccount = findOwnedAccount(requset.getAccount(), currentUser);
        ensureNotDebtMirror(incomseId);

        Account oldAccount = incomse.getAccount();
        double oldAmount = incomse.getAmount();
        ;
        double newAmount = requset.getAmount();
        boolean sameAccount = oldAccount != null && oldAccount.getId().equals(newAccount.getId());
        if (sameAccount) {
            accountService.applyCashFlow(newAccount, CashFlow.IN, newAmount - oldAmount);
        } else {
            if (oldAccount != null) {
                accountService.revertCashFlow(oldAccount, CashFlow.IN, oldAmount);
            }
            accountService.applyCashFlow(newAccount, CashFlow.IN, newAmount);
        }
        try {
            incomse.setAccount(newAccount);
            incomse.setAmount(newAmount);
            incomse.setCategory(requset.getCategory());
            incomse.setImage(requset.getImage());
            return mapToIncomseResponse(incomseRepository.save(incomse));
        } catch (RuntimeException e) {
            if (sameAccount) {
                accountService.applyCashFlow(newAccount, CashFlow.IN, oldAmount - newAmount);
            } else {
                accountService.revertCashFlow(newAccount, CashFlow.IN, newAmount);
                if (oldAccount != null) {
                    accountService.applyCashFlow(oldAccount, CashFlow.IN, oldAmount);
                }
            }
            throw e;
        }
    }

    private Account findOwnedAccount(Account requesstAccount, User currentUser) {
        if (requesstAccount == null || requesstAccount.getId() == null) {
            throw new ResourceNotFoundException("Account not found");
        }
        Account account = accountRepository.findByIdAndUser(requesstAccount.getId(), currentUser);
        if (account == null) {
            throw new ResourceNotFoundException("Account not found");
        }
        return account;
    }

    @Transactional
    public String deleteIncomse(String incomseId) {
        User currentUser = userUtils.getCurrentAuthenticatedUser();
        Incomse incomse = findOwnedIncomse(incomseId, currentUser);
        accountService.updateTotalBalanceWithIncome(incomse.getAccount(), incomse.getAmount());
        incomseRepository.delete(incomse);
        return "Income deleted successfully";
    }
    private void ensureNotDebtMirror(String recordId){
        if (debtMovementRepository.existsByMirroredTransactionId(recordId)){
            throw new ResourceNotFoundException("This income was created by a debt movement. Edit or delete it from the debt");
        }
    }

    private Incomse findOwnedIncomse(String incomseId, User currentUser) {
        Incomse incomse = incomseRepository.findById(incomseId)
                .orElseThrow(() -> new ResourceNotFoundException("Income not found"));
        if (incomse.getUser() == null || !incomse.getUser().getId().equals(currentUser.getId())) {
            throw new ResourceNotFoundException("Income not found");
        }
        return incomse;
    }

    public List<IncomseResponse> getAllIncomseByAccount(Account account) {
        List<Incomse> incomseResponses = incomseRepository.findAllByAccount(account);
        return incomseResponses.stream().map(this::mapToIncomseResponse).toList();
    }


    public List<IncomseResponse> getAllIncomseByUser() {
        User user = userUtils.getCurrentAuthenticatedUser();
        List<Incomse> incomseResponses = incomseRepository.findAllByUser(user);
        return incomseResponses.stream().map(this::mapToIncomseResponse).toList();
    }


    private IncomseResponse mapToIncomseResponse(Incomse incomse) {
        String type;
        if (incomse.getType() == null) {
            type = "inocmse";
        } else type = incomse.getType();
        LocalDateTime date = incomse.getCreatedAt();
        if (incomse.getCreatedAt() == null)
            date = date.now();
        Map<String, String> image = incomse.getImage();
        Map<String, String> imageUrl = new HashMap<>();
        if (image != null && image.get("url") != null) {
            imageUrl.put("url", image.get("url"));
            imageUrl.put("filename", image.get("filename"));
        } else {
            imageUrl.put("url", "");
            imageUrl.put("filename", "");
        }
        return new IncomseResponse(
                incomse.getId(),
                incomse.getAmount(),
                incomse.getCategory(),
                incomse.getUser(),
                incomse.getAccount(),
                type,
                date,
                image
        );
    }

}
