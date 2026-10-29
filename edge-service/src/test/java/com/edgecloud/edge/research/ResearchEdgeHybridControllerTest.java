package com.edgecloud.edge.research;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ResearchEdgeHybridControllerTest {

    private MockMvc mockMvc;
    private ImageModel imageModel;

    @BeforeEach
    void setUp() {

        imageModel =
                mock(ImageModel.class);

        ResearchEdgeHybridController controller =
                new ResearchEdgeHybridController(
                        imageModel
                );

        mockMvc =
                MockMvcBuilders
                        .standaloneSetup(controller)
                        .build();
    }

    @Test
    void preprocessShouldReturnPreparedImage()
            throws Exception {

        Research.Prepared prepared =
                new Research.Prepared(
                        "HYBRID-PRE-TEST-01",
                        "squeezenet1.1-7-image-v2",
                        "rgb-stretch224-bilinear-imagenet-v1",
                        "prepared-base64-image",
                        15066,
                        5909,
                        2.5
                );

        when(
                imageModel.prepare(any())
        ).thenReturn(prepared);

        mockMvc.perform(
                        post(
                                "/api/v2/edge/hybrid/preprocess"
                        )
                                .contentType(
                                        "application/json"
                                )
                                .content("""
                                        {
                                          "requestId": "HYBRID-PRE-TEST-01",
                                          "imageBase64": "original-base64-image",
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
                                        "HYBRID-PRE-TEST-01"
                                )
                )
                .andExpect(
                        jsonPath("$.modelVersion")
                                .value(
                                        "squeezenet1.1-7-image-v2"
                                )
                )
                .andExpect(
                        jsonPath("$.preprocessingVersion")
                                .value(
                                        "rgb-stretch224-bilinear-imagenet-v1"
                                )
                )
                .andExpect(
                        jsonPath("$.imageBase64")
                                .value(
                                        "prepared-base64-image"
                                )
                )
                .andExpect(
                        jsonPath("$.originalBytes")
                                .value(15066)
                )
                .andExpect(
                        jsonPath("$.preparedBytes")
                                .value(5909)
                )
                .andExpect(
                        jsonPath("$.preprocessingMs")
                                .value(2.5)
                );

        verify(
                imageModel
        ).prepare(any());
    }
}
