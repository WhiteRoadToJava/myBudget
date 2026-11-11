package com.mybudget.server.services.transactions;

import com.mybudget.server.modules.Counter;
import com.mybudget.server.modules.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;


/**
 * The reference number of a transaction (64320 in "تحويل 64320"): 1, 2, 3... per user, shared
 * by incomes, expenses, transfers and debt movements, never reused. It is what a customer quotes
 * on the phone and what the account statement shows.
 *
 * One atomic $inc in MongoDB, so two transactions saved at the same moment never get the same
 * number. A number taken by a save that then fails is simply skipped (a gap, never a duplicate).
 */



@Service
@RequiredArgsConstructor
public class ReferenceNumbers {
    private final MongoTemplate mongoTemplate;

    private static String counterId(String userId){
        return "transactions:" + userId;
    }

    public long next(User user) {
        Query query = new Query(Criteria.where("_id").is(counterId(user.getId())));
        Update update = new Update().inc("seq", 1);
        Counter counter = mongoTemplate.findAndModify(query, update,
                FindAndModifyOptions.options().returnNew(true).upsert(true), Counter.class);
        return counter.getSeq();
    }
}
