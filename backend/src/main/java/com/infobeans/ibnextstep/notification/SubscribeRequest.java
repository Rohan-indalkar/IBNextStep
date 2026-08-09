package com.infobeans.ibnextstep.notification;


import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** Mirrors what browser PushSubscription.toJSON() produces. */
@Data
public class SubscribeRequest {

   @NotBlank
   private String endpoint;

   @NotBlank
   private String p256dh;

   @NotBlank
   private String auth;
}
