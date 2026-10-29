package com.edgecloud.edge.research;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.edgecloud.edge.research.Research.ImageInput;
import static com.edgecloud.edge.research.Research.Prediction;

@RestController
@RequestMapping("/api/v2/edge")
public class ResearchEdgePredictionController {

    private final ImageModel imageModel;

    public ResearchEdgePredictionController(
            ImageModel imageModel
    ) {
        this.imageModel = imageModel;
    }

    @PostMapping("/predict")
    public Prediction predict(
            @RequestBody ImageInput request
    ) {
        return imageModel.predict(request);
    }
}
