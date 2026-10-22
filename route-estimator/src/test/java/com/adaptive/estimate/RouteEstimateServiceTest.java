package com.adaptive.estimate;

import org.junit.jupiter.api.Test;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;
import static com.adaptive.estimate.EstimateRequest.*;

class RouteEstimateServiceTest {
    final Instant now=Instant.parse("2026-09-27T08:00:00Z");
    final RouteEstimateService service=new RouteEstimateService(new Calibration(),Clock.fixed(now,ZoneOffset.UTC));
    EstimateRequest request(boolean connected,boolean edge,Instant sampledAt){return new EstimateRequest("r1",
        new Task("t1","SENSOR_ANOMALY",Complexity.LOW,1000000,100,100),
        edge?new Device(0.0,0.0,null,Source.MEASURED):null,
        new Network(connected,connected?8.0:null,connected?20.0:null,sampledAt,Source.MEASURED),edge,true,1000.0,500.0,500.0);}
    @Test void transferUnitsAndRttOnce(){
        assertEquals(1000,RouteEstimateService.transferMs(1000000,8));
        var cloud=service.estimate(request(true,true,now)).estimates().get("CLOUD");
        assertEquals(1025.1,cloud.estimatedLatencyMs(),1e-8);
        assertEquals(1.0001,cloud.communicationMb(),1e-9);
    }
    @Test void powerAndCarbonConversions(){
        var edge=service.estimate(request(true,true,now)).estimates().get("EDGE");
        assertEquals(0.15,edge.energyEstimateJ(),1e-10);
        assertEquals(0.15*500/3600000,edge.carbonEstimateG(),1e-12);
        var cloud=service.estimate(request(true,true,now)).estimates().get("CLOUD");
        assertEquals(0.175+0.50005,cloud.energyEstimateJ(),1e-10);
        assertEquals(0.50005/2,cloud.deviceEnergyEstimateJ(),1e-10);
    }
    @Test void disconnectedStillEstimatesEdge(){
        var estimates=service.estimate(request(false,true,now)).estimates();
        assertTrue(estimates.get("EDGE").estimateAvailable());
        assertEquals("NETWORK_UNAVAILABLE",estimates.get("CLOUD").reason());
        assertNull(estimates.get("HYBRID").energyEstimateJ());
    }
    @Test void edgeUnavailableStillEstimatesCloud(){
        var estimates=service.estimate(request(true,false,now)).estimates();
        assertTrue(estimates.get("CLOUD").estimateAvailable());
        assertFalse(estimates.get("EDGE").estimateAvailable());assertFalse(estimates.get("HYBRID").estimateAvailable());
    }
    @Test void staleAndFutureProbesAreUnavailable(){
        assertEquals("STALE_PROBE",service.estimate(request(true,true,now.minusSeconds(6))).estimates().get("CLOUD").reason());
        assertEquals("STALE_PROBE",service.estimate(request(true,true,now.plusSeconds(1))).estimates().get("CLOUD").reason());
        assertEquals("STALE_PROBE",service.estimate(request(true,true,Instant.MIN)).estimates().get("CLOUD").reason());
    }
    @Test void invalidArithmeticAndCalibrationAreRejected(){
        assertThrows(IllegalArgumentException.class,()->RouteEstimateService.transferMs(1,0));
        assertThrows(IllegalArgumentException.class,()->RouteEstimateService.transferMs(1,Double.NaN));
        assertThrows(IllegalArgumentException.class,()->RouteEstimateService.transferMs(1,Double.MIN_VALUE));
        var calibration=new Calibration();calibration.setEdgePowerW(-1);
        assertThrows(IllegalArgumentException.class,calibration::validate);
    }
    @Test void deadlineDoesNotCauseEstimatorToChooseOrRejectRoutes(){
        assertTrue(service.estimate(request(true,true,now)).estimates().get("CLOUD").estimateAvailable());
    }
}
