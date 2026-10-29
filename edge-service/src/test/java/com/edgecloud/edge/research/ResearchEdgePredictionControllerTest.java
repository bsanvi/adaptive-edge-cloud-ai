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

class ResearchEdgePredictionControllerTest {

    private MockMvc mockMvc;
    private ImageModel imageModel;

    @BeforeEach
    void setUp() {

        imageModel =
                mock(ImageModel.class);

        ResearchEdgePredictionController controller =
                new ResearchEdgePredictionController(
                        imageModel
                );

        mockMvc =
                MockMvcBuilders
                        .standaloneSetup(controller)
                        .build();
    }

    @Test
    void predictShouldReturnV2Prediction()
            throws Exception {

        Research.ClassScore first =
                new Research.ClassScore(
                        714,
                        "pick",
                        0.40
                );

        Research.ClassScore second =
                new Research.ClassScore(
                        713,
                        "parking meter",
                        0.20
                );

        Research.Stages stages =
                new Research.Stages(
                        2.5,
                        10.0,
                        0.5
                );

        Research.Prediction prediction =
                new Research.Prediction(
                        "PREDICT-TEST-01",
                        "squeezenet1.1-7-image-v2",
                        "model-sha",
                        "labels-sha",
                        714,
                        "pick",
                        0.40,
                        List.of(
                                first,
                                second
                        ),
                        stages
                );

        when(
                imageModel.predict(any())
        ).thenReturn(prediction);

        mockMvc.perform(
                        post("/api/v2/edge/predict")
                                .contentType(
                                        "application/json"
                                )
                                .content("""
                                        {
                                          "requestId": "PREDICT-TEST-01",
                                          "imageBase64": "placeholder",
                                          "modelVersion": "squeezenet1.1-7-image-v2",
                                          "privacy": "NORMAL"
                                        }
                                        """)
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.requestId")
                                .value(
                                        "PREDICT-TEST-01"
                                )
                )
                .andExpect(
                        jsonPath("$.modelVersion")
                                .value(
                                        "squeezenet1.1-7-image-v2"
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
                                .value(0.40)
                )
                .andExpect(
                        jsonPath(
                                "$.top5[0].classIndex"
                        ).value(714)
                )
                .andExpect(
                        jsonPath(
                                "$.top5[0].label"
                        ).value("pick")
                )
                .andExpect(
                        jsonPath(
                                "$.stages.preprocessingMs"
                        ).value(2.5)
                )
                .andExpect(
                        jsonPath(
                                "$.stages.inferenceMs"
                        ).value(10.0)
                )
                .andExpect(
                        jsonPath(
                                "$.stages.postprocessingMs"
                        ).value(0.5)
                );

        verify(
                imageModel
        ).predict(any());
    }
}
