package com.team.aecai.orchestrator.research;

import com.team.aecai.orchestrator.integration.IntegrationTelemetry;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/** Server-Sent Events for real job transitions and real observed telemetry. */
@Component
@Profile("integration")
public class ResearchOperationsStream {
    private final CopyOnWriteArrayList<SseEmitter> jobListeners = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<SseEmitter> telemetryListeners = new CopyOnWriteArrayList<>();
    private final IntegrationTelemetry telemetry;

    public ResearchOperationsStream(IntegrationTelemetry telemetry){this.telemetry=telemetry;}

    public SseEmitter register(boolean metrics){
        CopyOnWriteArrayList<SseEmitter> target=metrics?telemetryListeners:jobListeners;
        if(target.size()>=25)throw new IllegalStateException("Too many streaming clients");
        SseEmitter emitter=new SseEmitter(70_000L);
        target.add(emitter);
        emitter.onCompletion(()->target.remove(emitter));
        emitter.onTimeout(()->{target.remove(emitter);emitter.complete();});
        emitter.onError(e->target.remove(emitter));
        send(target,emitter,"connected",Map.of("source","SERVER","at",Instant.now().toString()));
        if(metrics)send(target,emitter,"telemetry",telemetry.snapshot());
        return emitter;
    }
    public void publish(Map<String,Object> event){
        if(event==null)return;
        for(SseEmitter emitter:jobListeners)send(jobListeners,emitter,"job",event);
    }
    private void send(CopyOnWriteArrayList<SseEmitter> list,SseEmitter emitter,String type,Object payload){
        try{emitter.send(SseEmitter.event().name(type).data(payload,MediaType.APPLICATION_JSON));}
        catch(Exception e){list.remove(emitter);emitter.completeWithError(e);}
    }
    @Scheduled(fixedDelay=8000,initialDelay=4000)
    public void broadcastTelemetry(){
        if(telemetryListeners.isEmpty())return;
        Map<String,Object> snapshot=telemetry.snapshot();
        for(SseEmitter emitter:telemetryListeners)send(telemetryListeners,emitter,"telemetry",snapshot);
    }
    @Scheduled(fixedDelay=15000,initialDelay=15000)
    public void heartbeat(){
        Map<String,Object> beat=Map.of("at",Instant.now().toString());
        for(SseEmitter emitter:jobListeners)send(jobListeners,emitter,"heartbeat",beat);
        for(SseEmitter emitter:telemetryListeners)send(telemetryListeners,emitter,"heartbeat",beat);
    }
}
