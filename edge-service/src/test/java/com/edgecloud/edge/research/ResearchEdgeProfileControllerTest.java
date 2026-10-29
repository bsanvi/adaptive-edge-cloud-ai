package com.edgecloud.edge.research;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.awt.image.BufferedImage;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ResearchEdgeProfileControllerTest {

    private MockMvc mockMvc;
    private ImageModel imageModel;

    @BeforeEach
    void setUp() {

        imageModel =
                mock(ImageModel.class);

        ResearchEdgeProfileController controller =
                new ResearchEdgeProfileController(
                        imageModel
                );

        mockMvc =
                MockMvcBuilders
                        .standaloneSetup(controller)
                        .build();
    }

    @Test
    void profileShouldReturnMediumImageProfile()
            throws Exception {

        byte[] originalBytes =
                new byte[15066];

        BufferedImage originalImage =
                new BufferedImage(
                        800,
                        600,
                        BufferedImage.TYPE_INT_RGB
                );

        BufferedImage resizedImage =
                new BufferedImage(
                        224,
                        224,
                        BufferedImage.TYPE_INT_RGB
                );

        byte[] hybridBytes =
                new byte[5909];

        when(
                imageModel.validate(any())
        ).thenReturn(originalBytes);

        when(
                imageModel.decode(originalBytes)
        ).thenReturn(
                new ImageModel.Decoded(
                        originalImage,
                        "JPEG"
                )
        );

        when(
                imageModel.resize(originalImage)
        ).thenReturn(resizedImage);

        when(
                imageModel.png(resizedImage)
        ).thenReturn(hybridBytes);

        mockMvc.perform(
                        post("/api/v2/edge/profile")
                                .contentType(
                                        "application/json"
                                )
                                .content("""
                                        {
                                          "requestId": "PROFILE-TEST-01",
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
                        jsonPath(
                                "$.profile.fileSizeBytes"
                        ).value(15066)
                )
                .andExpect(
                        jsonPath(
                                "$.profile.width"
                        ).value(800)
                )
                .andExpect(
                        jsonPath(
                                "$.profile.height"
                        ).value(600)
                )
                .andExpect(
                        jsonPath(
                                "$.profile.pixelCount"
                        ).value(480000)
                )
                .andExpect(
                        jsonPath(
                                "$.profile.format"
                        ).value("JPEG")
                )
                .andExpect(
                        jsonPath(
                                "$.profile.complexity"
                        ).value("MEDIUM")
                )
                .andExpect(
                        jsonPath(
                                "$.profile.complexityMethod"
                        ).value("pixel-count-v2")
                )
                .andExpect(
                        jsonPath(
                                "$.profile.modelVersion"
                        ).value(
                                "squeezenet1.1-7-image-v2"
                        )
                )
                .andExpect(
                        jsonPath(
                                "$.profile.preprocessingVersion"
                        ).value(
                                "rgb-stretch224-bilinear-imagenet-v1"
                        )
                )
                .andExpect(
                        jsonPath(
                                "$.profile.hybridImageBytes"
                        ).value(5909)
                );
    }
}
