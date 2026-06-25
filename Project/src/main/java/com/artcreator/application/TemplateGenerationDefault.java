package com.artcreator.application;

import com.artcreator.model.Parameters;
import com.artcreator.template.TemplateGenerator;

import java.awt.image.BufferedImage;
import java.util.Objects;

public final class TemplateGenerationDefault implements TemplateGeneration {

    @Override
    public TemplateGenerator.GenerationResult createTemplate(BufferedImage og, Parameters params) {
        Objects.requireNonNull(og, "source image must not be null");
        Objects.requireNonNull(params, "parameters must not be null");
        return TemplateGenerator.generate(og, params);
    }
}
