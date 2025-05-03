import com.ancevt.d2d2.D2D2;
import com.ancevt.d2d2.debug.FpsMeter;
import com.ancevt.d2d2.debug.StarletSpace;
import com.ancevt.d2d2.event.SceneEvent;
import com.ancevt.d2d2.lifecycle.D2D2Application;
import com.ancevt.d2d2.scene.Scene;
import lombok.NonNull;

import java.util.Map;

public class Dev implements D2D2Application {

    public static void main(String[] args) {
        D2D2.init(Dev.class, args, null,
                Map.of(
                        "d2d2.engine", "com.ancevt.d2d2.engine.lwjgl.LwjglEngine"
                ));

    }


    @Override
    public void onCreate(@NonNull Scene scene) {
        StarletSpace.haveFun();
        scene.addChild(new FpsMeter());
    }
}
