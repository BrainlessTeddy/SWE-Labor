package com.artcreator.application;

import com.artcreator.model.Parameters;
import com.artcreator.template.TemplateGenerator;
 
import java.awt.image.BufferedImage;

public interface TemplateGeneration {
    TemplateGenerator.GenerationResult createTemplate(BufferedImage og, Parameters params);
}
