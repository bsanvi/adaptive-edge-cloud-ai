package com.edgecloud.edge.research;
import org.junit.jupiter.api.*;
import java.awt.image.BufferedImage;
import java.util.Base64;
import static org.junit.jupiter.api.Assertions.*;
import static com.edgecloud.edge.research.Research.*;
class ImageModelContractTest {
    static ImageModel model;
    @BeforeAll static void start(){model=new ImageModel();assertTrue(model.capability().ready(),model.capability().hardware().toString());}
    @AfterAll static void stop()throws Exception{model.close();}
    private ImageInput input(int pattern){
        var image=new BufferedImage(384,256,pattern==2?BufferedImage.TYPE_INT_ARGB:BufferedImage.TYPE_INT_RGB);
        for(int y=0;y<256;y++)for(int x=0;x<384;x++){
            int rgb=pattern==0?((x*255/383)<<16)|(y<<8)|((x+y)%256):pattern==1?((x/24+y/24)%2==0?0xffaa22:0x2244cc):((x*255/383)<<24)|0x55aa33;
            image.setRGB(x,y,rgb);
        }
        return new ImageInput("golden-"+pattern,Base64.getEncoder().encodeToString(model.png(image)),ImageModel.VERSION,Privacy.NORMAL);
    }
    @Test void goldenPatternsHaveEquivalentDirectAndHybridPredictions(){
        for(int i=0;i<3;i++){
            var original=input(i);var direct=model.predict(original);var prepared=model.prepare(original);
            var hybrid=model.postprocess(model.logits(new ImageInput(original.requestId(),prepared.imageBase64(),ImageModel.VERSION,Privacy.NORMAL),true));
            assertEquals(new int[]{326,904,653}[i],direct.classIndex());
            assertEquals(new double[]{.18257336208457883,.8038110654228273,.02998935097856817}[i],direct.confidence(),1e-5);
            assertEquals(direct.classIndex(),hybrid.classIndex());assertEquals(direct.confidence(),hybrid.confidence(),1e-5);
            assertEquals(direct.top5().stream().map(ClassScore::classIndex).toList(),hybrid.top5().stream().map(ClassScore::classIndex).toList());
            assertEquals(1000,model.logits(original,false).logits().length);assertEquals(5,direct.top5().size());
            assertTrue(direct.stages().inferenceMs()>0);assertTrue(prepared.preprocessingMs()>0);
        }
    }
    @Test void malformedAndWrongVersionInputsAreRejected(){
        assertThrows(IllegalArgumentException.class,()->model.predict(new ImageInput("x","invalid!",ImageModel.VERSION,Privacy.NORMAL)));
        assertThrows(org.springframework.web.server.ResponseStatusException.class,()->model.predict(new ImageInput("x",input(0).imageBase64(),"wrong",Privacy.NORMAL)));
        assertThrows(IllegalArgumentException.class,()->model.logits(input(0),true));
    }
}
