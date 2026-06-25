package com.artcreator.application;

import com.artcreator.model.Parameters;
import java.awt.image.BufferedImage;

public interface TemplateGenerationAsync {
    void generateAsync(BufferedImage source, Parameters params, InnerGenerationCallback callback);
    void cancelPending();
}
