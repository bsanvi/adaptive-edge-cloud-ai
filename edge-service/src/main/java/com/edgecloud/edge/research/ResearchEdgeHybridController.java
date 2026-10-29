package com.edgecloud.edge.research;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.edgecloud.edge.research.Research.ImageInput;
import static com.edgecloud.edge.research.Research.Prepared;

@RestController
@RequestMapping("/api/v2/edge/hybrid")
public class ResearchEdgeHybridController {

    private final ImageModel imageModel;

    public ResearchEdgeHybridController(
            ImageModel imageModel
    ) {
        this.imageModel = imageModel;
    }

    @PostMapping("/preprocess")
    public Prepared preprocess(
            @RequestBody ImageInput request
    ) {
        return imageModel.prepare(request);
    }
}
