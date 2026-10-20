package com.adaptive.cloud;

import org.junit.jupiter.api.*;
import org.springframework.core.io.DefaultResourceLoader;
import tools.jackson.databind.json.JsonMapper;
import static org.junit.jupiter.api.Assertions.*;

class InferenceTest {
    private final FeatureExtractor extractor=new FeatureExtractor();
    @Test void goldenFixturesAgreeForRawAndHybrid() throws Exception {
        var loader=new DefaultResourceLoader();
        var model=new ModelExecutor(loader,"classpath:model/reference.onnx","","reference-v1");
        try {
            assertTrue(model.ready());
            var mapper=JsonMapper.builder().build();
            try(var input=loader.getResource("classpath:fixtures/golden.json").getInputStream()) {
                for(var fixture:mapper.readTree(input)) {
                    double[] raw=mapper.treeToValue(fixture.get("samples"),double[].class);
                    float[] features=mapper.treeToValue(fixture.get("features"),float[].class);
                    float[] expected=mapper.treeToValue(fixture.get("probabilities"),float[].class);
                    assertArrayEquals(features,extractor.extract(raw),1e-6f,fixture.get("name").asString());
                    assertArrayEquals(expected,model.infer(features),1e-5f);
                    assertArrayEquals(model.infer(features),model.infer(extractor.extract(raw)),1e-5f);
                    assertEquals(fixture.get("prediction").asString(),model.infer(features)[1]>=0.5?"ANOMALY":"NORMAL");
                }
            }
        } finally {model.close();}
    }
    @Test void invalidSignalsAreRejected() {
        assertThrows(ApiFailure.class,()->extractor.extract(new double[7]));
        assertThrows(ApiFailure.class,()->extractor.extract(new double[4097]));
        double[] samples=new double[8]; samples[0]=Double.NaN;
        assertThrows(ApiFailure.class,()->extractor.extract(samples));
        samples[0]=1e7; assertThrows(ApiFailure.class,()->extractor.extract(samples));
    }
    @Test void zerosDoNotCountAsSignCrossings() {
        assertEquals(0,extractor.extract(new double[]{-1,0,1,0,-1,0,1,0})[6]);
        assertEquals(1,extractor.extract(new double[]{-1,1,-1,1,-1,1,-1,1})[6]);
    }
    @Test void incorrectChecksumIsNotReady() throws Exception {
        var model=new ModelExecutor(new DefaultResourceLoader(),"classpath:model/reference.onnx","bad","reference-v1");
        try { assertFalse(model.ready()); assertThrows(ApiFailure.class,()->model.infer(new float[8])); } finally {model.close();}
    }
}
