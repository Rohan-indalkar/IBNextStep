package com.infobeans.ibnextstep.notification;


import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import nl.martijndwars.webpush.Subscription;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.security.Security;
import java.util.List;
import java.util.Map;

/**
 * Sends OS-level browser notifications via the Web Push protocol (VAPID),
 * so the recipient sees them even if the app/tab isn't open — as long as
 * their browser has previously been granted permission on this site.
 *
 * This is *in addition to* the existing WebSocket push (works only while
 * the tab is open) and email (always sent) — none of that changes.
 * If a user has no push subscriptions, this is a silent no-op.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WebPushService {

    private final PushSubscriptionRepository pushSubscriptionRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.push.vapid.public-key}")
    private String vapidPublicKey;

    @Value("${app.push.vapid.private-key}")
    private String vapidPrivateKey;

    @Value("${app.push.vapid.subject:mailto:admin@ibnextstep.com}")
    private String vapidSubject;

    private PushService pushService;

    static {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
    }

    private PushService pushService() throws Exception {
        if (pushService == null) {
            pushService = new PushService(vapidPublicKey, vapidPrivateKey, vapidSubject);
        }
        return pushService;
    }

    /**
     * Fire-and-forget so a slow/failing push provider never blocks the
     * notification compose request — same philosophy as EmailService.
     */
    @Async
    public void sendToUser(String userId, String title, String message) {
        List<PushSubscription> subscriptions = pushSubscriptionRepository.findByUserId(userId);
        if (subscriptions.isEmpty()) {
            return;
        }

        String payload;
        try {
            payload = objectMapper.writeValueAsString(Map.of("title", title, "body", message));
        } catch (Exception e) {
            log.warn("Failed to serialize push payload for user {}: {}", userId, e.getMessage());
            return;
        }

        for (PushSubscription sub : subscriptions) {
            try {
                Subscription.Keys keys = new Subscription.Keys(sub.getP256dh(), sub.getAuth());
                Subscription subscription = new Subscription(sub.getEndpoint(), keys);
                Notification notification = new Notification(subscription, payload);

                var response = pushService().send(notification);
                int status = response.getStatusLine().getStatusCode();

                // 404/410 = the browser subscription is dead (uninstalled,
                // permissions revoked, cache cleared) — clean it up so we
                // stop trying it every time.
                if (status == 404 || status == 410) {
                    pushSubscriptionRepository.deleteByEndpoint(sub.getEndpoint());
                }
            } catch (Exception e) {
                log.warn("Push send failed for subscription {}: {}", sub.getId(), e.getMessage());
            }
        }
    }
}