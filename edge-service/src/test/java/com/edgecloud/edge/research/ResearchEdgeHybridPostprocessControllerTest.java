package com.edgecloud.edge.research;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ResearchEdgeHybridPostprocessControllerTest {

    private MockMvc mockMvc;
    private ImageModel imageModel;

    @BeforeEach
    void setUp() {

        imageModel =
                mock(ImageModel.class);

        ResearchEdgeHybridPostprocessController controller =
                new ResearchEdgeHybridPostprocessController(
                        imageModel
                );

        mockMvc =
                MockMvcBuilders
                        .standaloneSetup(controller)
                        .build();
    }

    @Test
    void postprocessShouldReturnFinalPrediction()
            throws Exception {

        Research.ClassScore first =
                new Research.ClassScore(
                        714,
                        "pick",
                        0.90
                );

        Research.Stages stages =
                new Research.Stages(
                        2.0,
                        9.0,
                        0.4,
                        0.0
                );

        Research.Prediction prediction =
                new Research.Prediction(
                        "HYBRID-POST-TEST-01",
                        "squeezenet1.1-7-image-v2",
                        "model-sha",
                        "labels-sha",
                        714,
                        "pick",
                        0.90,
                        List.of(first),
                        stages
                );

        when(
                imageModel.postprocess(any())
        ).thenReturn(prediction);

        mockMvc.perform(
                        post(
                                "/api/v2/edge/hybrid/postprocess"
                        )
                                .contentType(
                                        "application/json"
                                )
                                .content("""
                                        {
                                          "requestId": "HYBRID-POST-TEST-01",
                                          "modelVersion": "squeezenet1.1-7-image-v2",
                                          "modelSha256": "model-sha",
                                          "labelsSha256": "labels-sha",
                                          "logits": [0.1, 0.2, 0.3],
                                          "stages": {
                                            "preprocessingMs": 2.0,
                                            "inferenceMs": 9.0,
                                            "postprocessingMs": 0.0,
                                            "remotePreprocessingMs": 0.0
                                          }
                                        }
                                        """)
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.requestId")
                                .value(
                                        "HYBRID-POST-TEST-01"
                                )
                )
                .andExpect(
                        jsonPath("$.classIndex")
                                .value(714)
                )
                .andExpect(
                        jsonPath("$.label")
                                .value("pick")
                )
                .andExpect(
                        jsonPath("$.confidence")
                                .value(0.90)
                )
                .andExpect(
                        jsonPath("$.top5[0].classIndex")
                                .value(714)
                )
                .andExpect(
                        jsonPath("$.stages.inferenceMs")
                                .value(9.0)
                );

        verify(
                imageModel
        ).postprocess(any());
    }
}
