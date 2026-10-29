package com.edgecloud.edge.research;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.edgecloud.edge.research.Research.Logits;
import static com.edgecloud.edge.research.Research.Prediction;

@RestController
@RequestMapping("/api/v2/edge/hybrid")
public class ResearchEdgeHybridPostprocessController {

    private final ImageModel imageModel;

    public ResearchEdgeHybridPostprocessController(
            ImageModel imageModel
    ) {
        this.imageModel = imageModel;
    }

    @PostMapping("/postprocess")
    public Prediction postprocess(
            @RequestBody Logits logits
    ) {
        return imageModel.postprocess(logits);
    }
}
