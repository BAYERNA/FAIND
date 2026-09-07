package com.faind.integration.notification.dto;

import java.util.UUID;

public record RiskWarningWebhookDto(UUID incidentId, String message) {}
