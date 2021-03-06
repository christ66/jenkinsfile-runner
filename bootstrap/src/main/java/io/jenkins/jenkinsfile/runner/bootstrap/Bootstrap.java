package io.jenkins.jenkinsfile.runner.bootstrap;

import java.io.Console;
import java.io.File;
import java.io.IOException;

/**
 * @author Kohsuke Kawaguchi
 */
public class Bootstrap {

    /**
     * This system property is set by the bootstrap script created by appassembler Maven plugin
     * to point to a local Maven repository.
     */
    public final File appRepo = new File(System.getProperty("app.repo"));

    /**
     * Exploded jenkins.war
     */
    public final File warDir;

    /**
     * Where to load plugins from?
     */
    public final File pluginsDir;

    /**
     * Checked out copy of the working space.
     */
    public final File wsDir;

    public Bootstrap(File warDir, File pluginsDir, File wsDir) {
        this.warDir = warDir;
        this.pluginsDir = pluginsDir;
        this.wsDir = wsDir;
    }

    public static void main(String[] args) throws Throwable {
        // break for attaching profiler
        if (Boolean.getBoolean("start.pause")) {
            System.console().readLine();
        }

        // TODO: support exploding war. See WebInfConfiguration.unpack()
        if (args.length<2) {
            System.err.println("Usage: jenkinsfilerunner <jenkins.war> <pluginsDir> <ws>");
            System.exit(1);
        }

        System.exit(new Bootstrap(new File(args[0]), new File(args[1]), new File(args[2])).run());
    }

    public int run() throws Throwable {
        ClassLoader jenkins = createJenkinsWarClassLoader();
        ClassLoader setup = createSetupClassLoader(jenkins);

        Thread.currentThread().setContextClassLoader(setup);    // or should this be 'jenkins'?

        Class<?> c = setup.loadClass("io.jenkins.jenkinsfile.runner.App");
        return ((IApp)c.newInstance()).run(this);
    }

    public ClassLoader createJenkinsWarClassLoader() throws IOException {
        return new ClassLoaderBuilder(new SideClassLoader(null))
                .collectJars(new File(warDir,"WEB-INF/lib"))
                // servlet API needs to be visible to jenkins.war
                .collectJars(new File(appRepo,"javax/servlet"))
                .make();
    }

    public ClassLoader createSetupClassLoader(ClassLoader jenkins) throws IOException {
        return new ClassLoaderBuilder(jenkins)
                .collectJars(appRepo)
                .make();
    }
}
