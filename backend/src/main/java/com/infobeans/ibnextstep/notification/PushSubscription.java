package com.infobeans.ibnextstep.notification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * One document per browser/device a user has granted notification
 * permission on. A user can have several (phone + laptop + a second
 * browser) — all of them get pushed to.
 *
 * endpoint/p256dh/auth come straight from the browser's
 * PushSubscription.toJSON() on the frontend; they're opaque to us,
 * just passed through to the web-push library at send time.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "push_subscriptions")
public class PushSubscription {

    @Id
    private String id;

    @Indexed
    private String userId;

    @Indexed(unique = true)
    private String endpoint;

    private String p256dh;
    private String auth;

    private Instant createdAt;
}