package com.adaptive.estimate.research;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static com.adaptive.estimate.research.Research.*;

class CalibratedEstimatorTest {
    @TempDir Path directory;
    private CalibratedEstimator estimator() throws Exception {return new CalibratedEstimator(directory.toString(),15,35,.5,500,500,500);}
    private EstimateInput input(String version, String source, boolean online) {
        var p=new Profile(10000,300,200,60000,"PNG","LOW","input handling","model",List.of(1,3,224,224),"pre",8000);
        var d=new Device(10d,20d,null,null,Instant.now(),source,"test","edge");
        var n=new Network(online,online?10d:null,online?20d:null,online?40d:null,Instant.now(),source,"test",1000);
        return new EstimateInput("test",p,d,n,true,true,"edge","cloud",14000,11000,1600,version);
    }
    @Test void learnsOnlyAfterMinimumCountAndCanReplayFrozenVersion() throws Exception {
        var estimator=estimator();var r=input(null,"LIVE_MEASUREMENT",true);
        double cold=estimator.estimate(r).routes().get(Route.EDGE).totalMs();
        for(int i=0;i<5;i++)estimator.observe(new Observation("obs"+i,r,Route.EDGE,new Stages(2,8,1),13,0,0,true,"LIVE_MEASUREMENT",Instant.now()));
        var warm=estimator.estimate(r).routes().get(Route.EDGE);
        assertEquals("CALIBRATED_EWMA",warm.source());assertEquals(13,warm.totalMs(),1e-8);assertNotEquals(cold,warm.totalMs());
        assertEquals(cold,estimator.estimate(input("assumed-image-v2","LIVE_MEASUREMENT",true)).routes().get(Route.EDGE).totalMs());
        assertEquals("ASSUMED_COEFFICIENT",estimator.estimate(input("ewma-v2-00000004","LIVE_MEASUREMENT",true)).routes().get(Route.EDGE).source());
        assertEquals("ewma-v2-00000005",estimator().snapshot().version());
        estimator.observe(new Observation("obs4",r,Route.EDGE,new Stages(2,8,1),13,0,0,true,"LIVE_MEASUREMENT",Instant.now()));
        assertEquals(5,estimator.snapshot().observationIds().size());
    }
    @Test void rejectsSimulationAndInvalidTiming() throws Exception {
        var estimator=estimator();
        assertThrows(IllegalArgumentException.class,()->estimator.observe(new Observation("x",input(null,"SIMULATED_CONDITIONS",true),Route.EDGE,new Stages(1,2,1),6,0,0,true,"LIVE_MEASUREMENT",Instant.now())));
        assertThrows(IllegalArgumentException.class,()->estimator.observe(new Observation("y",input(null,"LIVE_MEASUREMENT",true),Route.EDGE,new Stages(1,20,1),6,0,0,true,"LIVE_MEASUREMENT",Instant.now())));
        assertEquals("assumed-image-v2",estimator.snapshot().version());
    }
    @Test void offlineKeepsEdgeAndDirectionalTransferUsesCorrectUnits() throws Exception {
        var estimates=estimator().estimate(input(null,"LIVE_MEASUREMENT",false));
        assertTrue(estimates.routes().get(Route.EDGE).available());assertFalse(estimates.routes().get(Route.CLOUD).available());
        assertNull(estimates.routes().get(Route.CLOUD).totalMs());
        assertEquals(1210,CalibratedEstimator.transfer(1_000_000,1_000_000,new Network(true,10d,10d,20d,Instant.now(),"LIVE_MEASUREMENT","test",1)),1e-9);
    }
}
