package com.artcreator.application;

import com.artcreator.template.TemplateGenerator.GenerationResult;

public interface InnerGenerationCallback {
    void onSuccess(GenerationResult result);
    void onFailure(Throwable cause);
}
