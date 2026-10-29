package com.team.aecai.orchestrator.research;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** No real services or fake inference are used by these argument-validation tests. */
class ResearchScenarioValidationTest {
    private ResearchExperimentController.Scenario valid(){
        return new ResearchExperimentController.Scenario("scenario-test",30.0,40.0,80.0,
                20.0,50.0,2000.0,65536L,true,true,false,400.0);
    }
    @Test void acceptsBoundedSimulatedInputs(){ assertDoesNotThrow(()->ResearchExperimentController.validate(valid())); }
    @Test void rejectsPayloadBeyondEstimatorLimit(){
        var s=new ResearchExperimentController.Scenario("scenario-test",30.0,40.0,80.0,
                20.0,50.0,2000.0,1048577L,true,true,false,400.0);
        assertThrows(IllegalArgumentException.class,()->ResearchExperimentController.validate(s));
    }
    @Test void rejectsMissingPrivacyFlag(){
        var s=new ResearchExperimentController.Scenario("scenario-test",30.0,40.0,80.0,
                20.0,50.0,2000.0,65536L,true,true,null,400.0);
        assertThrows(IllegalArgumentException.class,()->ResearchExperimentController.validate(s));
    }
    @Test void rejectsNaNDeviceMeasurement(){
        var s=new ResearchExperimentController.Scenario("scenario-test",Double.NaN,40.0,80.0,
                20.0,50.0,2000.0,65536L,true,true,false,400.0);
        assertThrows(IllegalArgumentException.class,()->ResearchExperimentController.validate(s));
    }
    @Test void rejectsImpossibleNonpositiveDeadline(){
        var s=new ResearchExperimentController.Scenario("scenario-test",30.0,40.0,80.0,
                20.0,50.0,0.0,65536L,true,true,false,400.0);
        assertThrows(IllegalArgumentException.class,()->ResearchExperimentController.validate(s));
    }
    @Test void acceptsUnavailableCloudAsExplicitSimulation(){
        var s=new ResearchExperimentController.Scenario("scenario-test",30.0,40.0,80.0,
                300.0,0.0,2000.0,65536L,true,false,false,400.0);
        assertDoesNotThrow(()->ResearchExperimentController.validate(s));
    }
}
