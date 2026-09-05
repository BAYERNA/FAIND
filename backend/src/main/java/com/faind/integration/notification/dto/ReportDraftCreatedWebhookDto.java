package com.faind.integration.notification.dto;

import java.util.List;
import java.util.UUID;

public record ReportDraftCreatedWebhookDto(UUID incidentId, List<UUID> responderIds, List<UUID> reportIds) {}
