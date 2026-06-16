package com.talentgrid.jobposting.kafka;

import com.talentgrid.jobposting.event.PortalConfirmationEvent;
import com.talentgrid.jobposting.service.PortalConfirmationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PortalConfirmationConsumer {

    private final PortalConfirmationService confirmationService;

    @KafkaListener(
            topics = "${portal.kafka.topic.confirmations:portal-confirmations}",
            groupId = "job-posting-group",
            containerFactory = "portalContainerFactory"
    )
    public void consume(ConsumerRecord<String, PortalConfirmationEvent> record, Acknowledgment ack) {
        PortalConfirmationEvent event = record.value();

        log.info("Portal confirmation received | eventType={} | jobPostingId={}",
                event != null ? event.getEventType() : null,
                event != null && event.getPayload() != null ? event.getPayload().getJobPostingId() : null);

        try {
            confirmationService.handle(event);
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Failed to handle portal confirmation for key={}: {}", record.key(), e.getMessage(), e);
            ack.acknowledge(); // ack to avoid infinite retry — add DLQ in production
        }
    }
}
