package com.adaptive.estimate;

import org.springframework.stereotype.Service;
import java.time.*;
import java.util.*;

@Service
public class RouteEstimateService {
    public record Estimate(boolean estimateAvailable,String reason,Double estimatedLatencyMs,
        Double energyEstimateJ,Double deviceEnergyEstimateJ,Double cloudEnergyEstimateJ,Double transferEnergyEstimateJ,
        Double carbonEstimateG,Long uploadBytes,Long downloadBytes,Double communicationMb,Double resourcePressure,String source) {
        static Estimate unavailable(String reason) { return new Estimate(false,reason,null,null,null,null,null,null,null,null,null,null,"ESTIMATED"); }
    }
    public record Response(String requestId,String calibrationVersion,String calibrationSource,Map<String,Estimate> estimates) {}
    private final Calibration calibration;
    private final Clock clock;
    @org.springframework.beans.factory.annotation.Autowired
    public RouteEstimateService(Calibration calibration) { this(calibration,Clock.systemUTC()); }
    RouteEstimateService(Calibration calibration,Clock clock) { this.calibration=calibration; this.clock=clock; }
    public Response estimate(EstimateRequest r) {
        finiteNonnegative(r.edgeCarbonIntensityGPerKwh()); finiteNonnegative(r.cloudCarbonIntensityGPerKwh());
        if(r.remainingDeadlineMs()!=null) finiteNonnegative(r.remainingDeadlineMs());
        if(r.network().networkLatencyMs()!=null) finiteNonnegative(r.network().networkLatencyMs());
        if(r.network().bandwidthMbps()!=null && (!Double.isFinite(r.network().bandwidthMbps()) || r.network().bandwidthMbps()<=0))
            throw new IllegalArgumentException("Bandwidth must be positive and finite");
        int index=r.task().complexity().ordinal();
        Double cpu=r.device()==null?null:r.device().cpuPercent(), ram=r.device()==null?null:r.device().ramPercent();
        if (cpu!=null) percent(cpu); if(ram!=null) percent(ram);
        if(r.device()!=null && r.device().batteryPercent()!=null) percent(r.device().batteryPercent());
        boolean local=r.edgeCapable() && cpu!=null && ram!=null;
        double edgeMs=calibration.getEdgeMs()[index], cloudMs=calibration.getCloudMs()[index];
        // Explicit prototype CPU slowdown assumption. Replace only with versioned measured calibration.
        double slowdown=cpu==null?1:1+cpu/100;
        Map<String,Estimate> result=new LinkedHashMap<>();
        result.put("EDGE",local ? available(r,edgeMs*slowdown,edgeMs*slowdown,0,0,0,Math.min(1,Math.max(cpu,ram)/100+0.1))
            : Estimate.unavailable(r.edgeCapable()?"DEVICE_METRICS_UNAVAILABLE":"EDGE_UNAVAILABLE"));
        String reason=remoteReason(r);
        if(reason!=null) {
            result.put("CLOUD",Estimate.unavailable(reason)); result.put("HYBRID",Estimate.unavailable(reason));
        } else {
            long down=r.task().estimatedResultBytes();
            double cloudLatency=transferMs(r.task().dataSizeBytes()+down,r.network().bandwidthMbps())+r.network().networkLatencyMs()+cloudMs;
            result.put("CLOUD",available(r,cloudLatency,0,cloudMs,r.task().dataSizeBytes(),down,0.05));
            double localMs=calibration.getPreprocessMs()[index]*slowdown+calibration.getPostprocessMs();
            double hybridCloudMs=calibration.getHybridCloudMs()[index];
            double hybridLatency=localMs+transferMs(r.task().estimatedFeatureBytes()+down,r.network().bandwidthMbps())+r.network().networkLatencyMs()+hybridCloudMs;
            result.put("HYBRID",local?available(r,hybridLatency,localMs,hybridCloudMs,r.task().estimatedFeatureBytes(),down,
                Math.min(1,Math.max(cpu,ram)/100+0.05)):Estimate.unavailable(r.edgeCapable()?"DEVICE_METRICS_UNAVAILABLE":"EDGE_UNAVAILABLE"));
        }
        return new Response(r.requestId(),calibration.getCalibrationVersion(),"ASSUMED",result);
    }
    private String remoteReason(EstimateRequest r) {
        if(!r.cloudCapable())return "CLOUD_UNAVAILABLE";
        if(!r.network().connected())return "NETWORK_UNAVAILABLE";
        if(r.network().bandwidthMbps()==null || r.network().networkLatencyMs()==null || r.network().sampledAt()==null)return "NETWORK_METRICS_UNAVAILABLE";
        Duration age=Duration.between(r.network().sampledAt(),clock.instant());
        if(age.isNegative() || age.compareTo(Duration.ofMillis(calibration.getProbeMaxAgeMs()))>0)return "STALE_PROBE";
        return null;
    }
    private Estimate available(EstimateRequest r,double latency,double edgeMs,double cloudMs,long up,long down,double pressure) {
        double deviceCompute=edgeMs/1000*calibration.getEdgePowerW();
        double cloud=cloudMs/1000*calibration.getCloudPowerW();
        double mb=(up+down)/1e6, transfer=mb*calibration.getTransferJPerMb();
        // Transfer coefficient represents the whole link; half allocated to Laptop 1 battery.
        double carbon=(deviceCompute*r.edgeCarbonIntensityGPerKwh()+cloud*r.cloudCarbonIntensityGPerKwh()
            +transfer*calibration.getNetworkCarbonGPerKwh())/3_600_000;
        for(double value:new double[]{latency,deviceCompute,cloud,transfer,carbon}) finiteNonnegative(value);
        return new Estimate(true,null,latency,deviceCompute+cloud+transfer,deviceCompute+transfer/2,cloud,transfer,
            carbon,up,down,mb,pressure,"ESTIMATED");
    }
    public static double transferMs(long bytes,double bandwidthMbps) {
        if(bytes<0 || !Double.isFinite(bandwidthMbps) || bandwidthMbps<=0) throw new IllegalArgumentException("Invalid transfer inputs");
        double duration=1000*8.0*bytes/(bandwidthMbps*1e6);
        finiteNonnegative(duration); return duration;
    }
    private static void finiteNonnegative(double x){if(!Double.isFinite(x)||x<0)throw new IllegalArgumentException("Values must be finite and nonnegative");}
    private static void percent(double x){finiteNonnegative(x);if(x>100)throw new IllegalArgumentException("Percentage exceeds 100");}
}
