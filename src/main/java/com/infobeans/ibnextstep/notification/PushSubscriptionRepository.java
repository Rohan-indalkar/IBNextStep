package com.infobeans.ibnextstep.notification;


import org.springframework.data.mongodb.repository.MongoRepository;
 
import java.util.List;
import java.util.Optional;
 
public interface PushSubscriptionRepository extends MongoRepository<PushSubscription, String> {
    List<PushSubscription> findByUserId(String userId);
    Optional<PushSubscription> findByEndpoint(String endpoint);
    void deleteByEndpoint(String endpoint);
}
 


