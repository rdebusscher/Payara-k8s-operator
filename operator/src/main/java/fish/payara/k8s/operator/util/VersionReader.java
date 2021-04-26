package fish.payara.k8s.operator.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.Properties;

public class VersionReader {
    private String releaseVersion;
    private String buildTime;

    /**
     * @param module Initial characters of the jar file with the manifest file of interest.
     */
    public VersionReader(String module) {
        readInfo(module);
    }

    private void readInfo(String module) {

        Properties properties = new Properties();
        try {
            URL manifestFile = findManifestFile(module);

            // Is the manifest file found.
            if (manifestFile != null) {
                InputStream resourceAsStream = manifestFile.openStream();
                properties.load(resourceAsStream);

                resourceAsStream.close();
            } else {
                LogHelper.log(String.format("Unable to find manifest file %s module", module));
            }

        } catch (IOException e) {
            LogHelper.log(String.format("Exception during loading of the %s MANIFEST.MF file : %s", module, e.getMessage()));
        }

        releaseVersion = properties.getProperty("Release-Version");
        buildTime = properties.getProperty("Build-Time");

    }

    private URL findManifestFile(String module) throws IOException {
        URL result = null;
        ClassLoader classLoader = this.getClass().getClassLoader();
        Enumeration<URL> systemResources = classLoader.getResources("META-INF/MANIFEST.MF");
        while (systemResources.hasMoreElements() && result == null) {
            URL url = systemResources.nextElement();
            if (url.toExternalForm().contains("/" + module)) {
                result = url;
            }
        }
        return result;
    }

    /**
     * Returns the <code>Release-Version</code> property of the manifest or null if manifest could not be found.
     *
     * @return Release-Version property value.
     */
    public String getReleaseVersion() {
        return releaseVersion;
    }

    /**
     * Returns the <code>buildTime</code> property of the manifest or null if manifest could not be found.
     *
     * @return buildTime property value
     */
    public String getBuildTime() {
        return buildTime;
    }

}
