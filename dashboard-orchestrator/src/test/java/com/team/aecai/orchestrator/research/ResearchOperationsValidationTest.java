package com.team.aecai.orchestrator.research;

import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class ResearchOperationsValidationTest {
    private ResearchOperationsService.Submission valid(){return new ResearchOperationsService.Submission(
        "EXECUTION","trial-1",List.of(0.1,0.2,0.3,0.4,0.5,0.6,0.7,0.8),20000.0,true,"LOW",null,null,null);
    }
    @Test void validPrivateExecutionAccepted(){assertDoesNotThrow(()->ResearchOperationsService.validate(valid()));}
    @Test void shortSamplesRejected(){var s=new ResearchOperationsService.Submission("EXECUTION","trial-2",
        List.of(1.0,2.0),1000.0,true,"LOW",null,null,null);
        assertThrows(IllegalArgumentException.class,()->ResearchOperationsService.validate(s));}
    @Test void nonFiniteSamplesRejected(){var s=new ResearchOperationsService.Submission("EXECUTION","trial-3",
        List.of(1.0,2.0,3.0,4.0,5.0,6.0,7.0,Double.NaN),1000.0,true,"LOW",null,null,null);
        assertThrows(IllegalArgumentException.class,()->ResearchOperationsService.validate(s));}
    @Test void negativeDeadlineRejected(){var s=new ResearchOperationsService.Submission("EXECUTION","trial-4",
        List.of(1.0,2.0,3.0,4.0,5.0,6.0,7.0),-1.0,true,"LOW",null,null,null);
        assertThrows(IllegalArgumentException.class,()->ResearchOperationsService.validate(s));}
    @Test void scenarioRequiresPayload(){var s=new ResearchOperationsService.Submission("EXPERIMENT","trial-5",
        null,null,null,null,null,null,null);
        assertThrows(IllegalArgumentException.class,()->ResearchOperationsService.validate(s));}
    @Test void futureScheduleAccepted(){long now=System.currentTimeMillis();long due=ResearchOperationsService.scheduledTime(
        Instant.ofEpochMilli(now+120_000).toString(),now);assertEquals(now+120_000,due);}
    @Test void distantScheduleRejected(){long now=System.currentTimeMillis();assertThrows(IllegalArgumentException.class,
        ()->ResearchOperationsService.scheduledTime(Instant.ofEpochMilli(now+40L*24*3600*1000).toString(),now));}
    @Test void invalidDatasetIdsRejected(){assertFalse(ResearchDatasetAdminController.validId("../secret"));
        assertFalse(ResearchDatasetAdminController.validId("C:\\temp"));assertTrue(ResearchDatasetAdminController.validId("test_images-2026"));}
}
