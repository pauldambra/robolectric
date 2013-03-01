package org.robolectric;

import org.robolectric.annotation.Config;
import org.robolectric.res.AndroidSdkFinder;
import org.robolectric.res.ResourcePath;

import java.io.File;
import java.net.URL;

public class Configurer {
    public ResourcePath findSystemResourcePath(Config config) {
        return new AndroidSdkFinder().findSystemResourcePath();
    }

    public AndroidManifest createAppManifest(Config config) {
        File manifestFile = new File(config.manifest());
        if (!manifestFile.exists()) {
            URL resource = Configurer.class.getClassLoader().getResource(config.manifest());
            manifestFile = new File(resource.getFile());
        }
        return new AndroidManifest(manifestFile, new File(manifestFile.getParentFile(), "res"));
    }
}
