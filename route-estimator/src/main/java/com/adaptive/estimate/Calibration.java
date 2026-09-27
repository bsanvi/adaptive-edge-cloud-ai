package com.adaptive.estimate;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;

@Component
@ConfigurationProperties(prefix="estimate")
public class Calibration {
    private String calibrationVersion="prototype-v1";
    private double[] edgeMs={10,30,80}, cloudMs={5,15,40}, hybridCloudMs={4,12,32}, preprocessMs={1,3,8};
    private double postprocessMs=1, edgePowerW=15, cloudPowerW=35, transferJPerMb=0.5, networkCarbonGPerKwh=500;
    private long probeMaxAgeMs=5000;
    @PostConstruct public void validate() {
        if (calibrationVersion==null || calibrationVersion.isBlank() || probeMaxAgeMs<=0) throw new IllegalArgumentException("Invalid calibration version or probe age");
        for (double[] values:new double[][]{edgeMs,cloudMs,hybridCloudMs,preprocessMs}) {
            if (values==null || values.length!=3) throw new IllegalArgumentException("Three complexity timings required");
            for (double x:values) positive(x);
        }
        for(double x:new double[]{postprocessMs,edgePowerW,cloudPowerW,transferJPerMb,networkCarbonGPerKwh}) positive(x);
    }
    private void positive(double value) { if (!Double.isFinite(value) || value<=0) throw new IllegalArgumentException("Calibration values must be positive and finite"); }
    public String getCalibrationVersion(){return calibrationVersion;} public void setCalibrationVersion(String x){calibrationVersion=x;}
    public double[] getEdgeMs(){return edgeMs.clone();} public void setEdgeMs(double[] x){edgeMs=x.clone();}
    public double[] getCloudMs(){return cloudMs.clone();} public void setCloudMs(double[] x){cloudMs=x.clone();}
    public double[] getHybridCloudMs(){return hybridCloudMs.clone();} public void setHybridCloudMs(double[] x){hybridCloudMs=x.clone();}
    public double[] getPreprocessMs(){return preprocessMs.clone();} public void setPreprocessMs(double[] x){preprocessMs=x.clone();}
    public double getPostprocessMs(){return postprocessMs;} public void setPostprocessMs(double x){postprocessMs=x;}
    public double getEdgePowerW(){return edgePowerW;} public void setEdgePowerW(double x){edgePowerW=x;}
    public double getCloudPowerW(){return cloudPowerW;} public void setCloudPowerW(double x){cloudPowerW=x;}
    public double getTransferJPerMb(){return transferJPerMb;} public void setTransferJPerMb(double x){transferJPerMb=x;}
    public double getNetworkCarbonGPerKwh(){return networkCarbonGPerKwh;} public void setNetworkCarbonGPerKwh(double x){networkCarbonGPerKwh=x;}
    public long getProbeMaxAgeMs(){return probeMaxAgeMs;} public void setProbeMaxAgeMs(long x){probeMaxAgeMs=x;}
}
