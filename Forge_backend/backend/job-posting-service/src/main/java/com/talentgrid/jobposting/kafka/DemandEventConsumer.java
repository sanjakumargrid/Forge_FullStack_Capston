package com.talentgrid.jobposting.kafka;

import com.talentgrid.jobposting.event.DemandEvent;
import com.talentgrid.jobposting.service.DemandService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DemandEventConsumer {

  private final DemandService demandService;

  @KafkaListener(
    topics = "demand-events",
    groupId = "job-posting-group",
    containerFactory = "kafkaListenerContainerFactory"
  )
  public void consume(ConsumerRecord<String, DemandEvent> record, Acknowledgment ack) {

    DemandEvent event = record.value();

    if (event == null) {
      log.error(
        "Skipping undeserializable message | partition={} | offset={}",
        record.partition(),
        record.offset()
      );
      ack.acknowledge();
      return;
    }

    log.info(
      "Kafka event received | key={} | partition={} | offset={} | eventType={} | demandId={}",
      record.key(),
      record.partition(),
      record.offset(),
      event != null ? event.getEventType() : null,
      event != null && event.getPayload() != null
        ? event.getPayload().getDemandId()
        : null
    );

    log.debug("Full Kafka Event Payload: {}", event);

    try {
      demandService.processKafkaEvent(event);

      log.info(
        "Successfully processed demandId={} eventType={}",
        event.getPayload().getDemandId(),
        event.getEventType()
      );

      ack.acknowledge();

      log.info(
        "Acknowledged Kafka message | partition={} | offset={}",
        record.partition(),
        record.offset()
      );

    } catch (Exception e) {

      log.error(
        "Failed to process Kafka event | key={} | demandId={} | error={}",
        record.key(),
        event != null && event.getPayload() != null
          ? event.getPayload().getDemandId()
          : null,
        e.getMessage(),
        e
      );

      ack.acknowledge();
    }
  }
}
