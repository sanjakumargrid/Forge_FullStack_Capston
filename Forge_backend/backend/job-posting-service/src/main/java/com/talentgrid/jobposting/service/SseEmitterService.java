package com.talentgrid.jobposting.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Service
public class SseEmitterService {

    private final CopyOnWriteArrayList<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(0L); // no timeout — client reconnects automatically
        emitters.add(emitter);

        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> { emitters.remove(emitter); emitter.complete(); });
        emitter.onError(e -> emitters.remove(emitter));

        // Confirm connection to client
        try {
            emitter.send(SseEmitter.event().name("connected").data(Map.of("subscribers", emitters.size())));
        } catch (Exception e) {
            emitters.remove(emitter);
        }

        log.info("SSE client subscribed. Active connections: {}", emitters.size());
        return emitter;
    }

    public void broadcast(String eventName, Object payload) {
        List<SseEmitter> dead = new CopyOnWriteArrayList<>();

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(payload));
            } catch (Exception e) {
                dead.add(emitter);
            }
        }

        if (!dead.isEmpty()) {
            emitters.removeAll(dead);
            log.debug("Removed {} dead SSE connections", dead.size());
        }

        log.info("Broadcasted {} to {} clients", eventName, emitters.size());
    }
}
