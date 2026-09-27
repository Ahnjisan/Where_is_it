package com.whereisit.backend.search.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/** API-14 요청. notificationEmail 미전송 시 회원 이메일을 쓴다. */
public record ActivateTrackingRequest(@Email @Size(max = 254) String notificationEmail) {
}
