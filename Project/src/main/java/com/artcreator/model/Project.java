package com.artcreator.model;

import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Holds the complete state of a single editing session: the original image,
 * the current parameter set and, optionally, the most recently generated
 * template.
 */
public final class Project {

    private File sourceFile;          // original image file on disk; may be null if not yet saved
    private File projectFile;         // .3dart file, if loaded/saved
    private BufferedImage original;   // unmodified source image
    private BufferedImage processed;  // result of preprocessing + quantization
    private Parameters parameters;    // current settings
    private Template   template;      // last generated template, or null
    private boolean dirty;            // unsaved changes?

    public Project() {
        this.parameters = new Parameters();
    }

    /* ---------------- accessors ---------------- */

    public File getSourceFile()              { return sourceFile; }
    public void setSourceFile(File f)        { this.sourceFile = f; markDirty(); }

    public File getProjectFile()             { return projectFile; }
    public void setProjectFile(File f)       { this.projectFile = f; }

    public BufferedImage getOriginal()       { return original; }
    public void setOriginal(BufferedImage v) { this.original = v; markDirty(); }

    public BufferedImage getProcessed()      { return processed; }
    public void setProcessed(BufferedImage v){ this.processed = v; }

    public Parameters getParameters()        { return parameters; }
    public void setParameters(Parameters p)  { this.parameters = p; markDirty(); }

    public Template getTemplate()            { return template; }
    public void setTemplate(Template t)      { this.template = t; }

    public boolean isDirty()                 { return dirty; }
    public void markClean()                  { this.dirty = false; }
    public void markDirty()                  { this.dirty = true; }

    public boolean hasImage()                { return original != null; }
    public boolean hasTemplate()             { return template != null; }
}
