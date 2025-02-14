package hudson.plugins.ansicolor;

import hudson.Functions;
import hudson.Launcher;
import hudson.console.ConsoleNote;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import java.io.IOException;
import java.io.StringWriter;
import java.util.logging.Level;
import static org.hamcrest.Matchers.*;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Assume;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.runners.model.Statement;
import org.jvnet.hudson.test.BuildWatcher;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.LoggerRule;
import org.jvnet.hudson.test.RestartableJenkinsRule;
import org.jvnet.hudson.test.TestBuilder;

public class AnsiColorBuildWrapperTest {

    public AnsiColorBuildWrapperTest() {
    }

    @Test
    public void testGetColorMapNameNull() {
        AnsiColorBuildWrapper instance = new AnsiColorBuildWrapper(null);
        assertEquals("xterm", instance.getColorMapName());
    }

    @Test
    public void testGetColorMapNameVga() {
        AnsiColorBuildWrapper instance = new AnsiColorBuildWrapper("vga");
        assertEquals("vga", instance.getColorMapName());
    }

    @Test
    public void testDecorateLogger() {
        AnsiColorBuildWrapper ansiColorBuildWrapper = new AnsiColorBuildWrapper(null);
        assertThat(ansiColorBuildWrapper, instanceOf(AnsiColorBuildWrapper.class));
    }

    @ClassRule
    public static BuildWatcher buildWatcher = new BuildWatcher();
    @Rule
    public RestartableJenkinsRule story = new RestartableJenkinsRule();
    @Rule
    public LoggerRule logging = new LoggerRule().recordPackage(ConsoleNote.class, Level.FINE).record(ColorConsoleAnnotator.class, Level.FINER);

    @Test
    public void maven() throws Exception {
        story.then(r -> {
            FreeStyleProject p = r.createFreeStyleProject();
            p.getBuildWrappersList().add(new AnsiColorBuildWrapper(null));
            p.getBuildersList().add(new TestBuilder() {
                @Override
                public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
                    // Like Maven 3.6.0 when using (MNG-6380) MAVEN_OPTS=-Djansi.force=true
                    listener.getLogger().println("[\u001B[1;34mINFO\u001B[m] Scanning for projects...");
                    listener.getLogger().println("[\u001B[1;34mINFO\u001B[m] ");
                    listener.getLogger().println("[\u001B[1;34mINFO\u001B[m] \u001B[1m--------------< \u001B[0;36morg.jenkins-ci.plugins:build-token-root\u001B[0;1m >---------------\u001B[m");
                    listener.getLogger().println("[\u001B[1;34mINFO\u001B[m] \u001B[1mBuilding Build Authorization Token Root Plugin 1.5-SNAPSHOT\u001B[m");
                    listener.getLogger().println("[\u001B[1;34mINFO\u001B[m] \u001B[1m--------------------------------[ hpi ]---------------------------------\u001B[m");
                    listener.getLogger().println("[\u001B[1;34mINFO\u001B[m] ");
                    listener.getLogger().println("[\u001B[1;34mINFO\u001B[m] \u001B[1m--- \u001B[0;32mmaven-clean-plugin:3.0.0:clean\u001B[m \u001B[1m(default-clean)\u001B[m @ \u001B[36mbuild-token-root\u001B[0;1m ---\u001B[m");
                    listener.getLogger().println("[\u001B[1;34mINFO\u001B[m] \u001B[1m------------------------------------------------------------------------\u001B[m");
                    listener.getLogger().println("[\u001B[1;34mINFO\u001B[m] \u001B[1;32mBUILD SUCCESS\u001B[m");
                    return true;
                }
            });
            FreeStyleBuild b = r.buildAndAssertSuccess(p);
            StringWriter writer = new StringWriter();
            b.getLogText().writeHtmlTo(0L, writer);
            String html = writer.toString();
            System.out.print(html);
            assertThat(html.replaceAll("<!--.+?-->", ""),
                allOf(
                    containsString("[<b><span style=\"color: #1E90FF;\">INFO</span></b>]"),
                    containsString("<b>--------------&lt; </b><span style=\"color: #00CDCD;\">org.jenkins-ci.plugins:build-token-root</span><b> &gt;---------------</b>")));
        });
    }

    @Test
    public void testMultilineEscapeSequence() throws Exception {
        story.then(r -> {
            FreeStyleProject p = r.createFreeStyleProject();
            p.getBuildWrappersList().add(new AnsiColorBuildWrapper(null));
            p.getBuildersList().add(new TestBuilder() {
                @Override
                public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
                    listener.getLogger().println("\u001B[1;34mThis text should be bold and blue");
                    listener.getLogger().println("Still bold and blue");
                    listener.getLogger().println("\u001B[mThis text should be normal");
                    return true;
                }
            });
            FreeStyleBuild b = r.buildAndAssertSuccess(p);
            StringWriter writer = new StringWriter();
            b.getLogText().writeHtmlTo(0L, writer);
            String html = writer.toString();
            System.out.print(html);
            assertThat(html.replaceAll("<!--.+?-->", ""),
                allOf(
                    containsString("<b><span style=\"color: #1E90FF;\">This text should be bold and blue\n</span></b>"),
                    containsString("<b><span style=\"color: #1E90FF;\">Still bold and blue\n</span></b>"),
                    not(containsString("\u001B[m"))));
        });
    }

    @Test
    public void testDefaultForegroundBackground() throws Exception {
        story.then(r -> {
            FreeStyleProject p = r.createFreeStyleProject();
            // The VGA ColorMap sets default foreground and background colors.
            p.getBuildWrappersList().add(new AnsiColorBuildWrapper("vga"));
            p.getBuildersList().add(new TestBuilder() {
                @Override
                public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
                    listener.getLogger().println("White on black");
                    listener.getLogger().println("\u001B[1;34mBold and blue on black");
                    listener.getLogger().println("Still bold and blue on black\u001B[mBack to white on black");
                    return true;
                }
            });
            FreeStyleBuild b = r.buildAndAssertSuccess(p);
            StringWriter writer = new StringWriter();
            b.getLogText().writeHtmlTo(0L, writer);
            String html = writer.toString();
            System.out.print(html);
            assertThat(html.replaceAll("<!--.+?-->", ""),
                allOf(
                    containsString("<div style=\"background-color: #000000;color: #AAAAAA;\">White on black\n</div>"),
                    containsString("<div style=\"background-color: #000000;color: #AAAAAA;\"><b><span style=\"color: #0000AA;\">Bold and blue on black\n</span></b></div>"),
                    containsString("<div style=\"background-color: #000000;color: #AAAAAA;\"><b><span style=\"color: #0000AA;\">Still bold and blue on black</span></b>Back to white on black\n</div>")));
        });
    }

    @Issue("JENKINS-54133")
    @Test
    public void testWorkflowWrap() throws Exception {
        story.addStep(new Statement() {

            @Override
            public void evaluate() throws Throwable {
                Assume.assumeTrue(!Functions.isWindows());
                story.j.createSlave();
                WorkflowJob p = story.j.jenkins.createProject(WorkflowJob.class, "p");
                p.setDefinition(new CpsFlowDefinition(
                        "node('!master') {\n"
                        + "  wrap([$class: 'AnsiColorBuildWrapper', 'colorMapName': 'XTerm']) {\n"
                        + "    sh(\"\"\"#!/bin/bash\n"
                        + "      printf 'The following word is supposed to be \\\\e[31mred\\\\e[0m\\\\n'\"\"\"\n"
                        + "    )\n"
                        + "  }\n"
                        + "}"
                ));
                story.j.assertBuildStatusSuccess(p.scheduleBuild2(0));
                StringWriter writer = new StringWriter();
                p.getLastBuild().getLogText().writeHtmlTo(0L, writer);
                String html = writer.toString();
                assertTrue("Failed to match color attribute in following HTML log output:\n" + html,
                    html.replaceAll("<!--.+?-->", "").matches("(?s).*<span style=\"color: #CD0000;\">red</span>.*"));
            }
        });
    }

    @Test
    public void testNonAscii() throws Exception {
        story.then(r -> {
            FreeStyleProject p = r.createFreeStyleProject();
            p.getBuildWrappersList().add(new AnsiColorBuildWrapper(null));
            p.getBuildersList().add(new TestBuilder() {
                @Override
                public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
                    listener.getLogger().println("\033[94;1m[ INFO ] Récupération du numéro de version de l'application\033[0m");
                    listener.getLogger().println("\033[94;1m[ INFO ] ビルドのコンソール出力を取得します。\033[0m");
                     // There are 3 smiley face emojis in this String
                    listener.getLogger().println("\033[94;1m[ INFO ] 😀😀\033[0m😀");
                    return true;
                }
            });
            FreeStyleBuild b = r.buildAndAssertSuccess(p);
            StringWriter writer = new StringWriter();
            b.getLogText().writeHtmlTo(0L, writer);
            String html = writer.toString();
            System.out.print(html);
            assertThat(html.replaceAll("<!--.+?-->", ""),
                allOf(
                    containsString("<span style=\"color: #4682B4;\"><b>[ INFO ] Récupération du numéro de version de l'application</b></span>"),
                    containsString("<span style=\"color: #4682B4;\"><b>[ INFO ] ビルドのコンソール出力を取得します。</b></span>"),
                    containsString("<span style=\"color: #4682B4;\"><b>[ INFO ] 😀😀</b></span>😀")));
        });
    }

    @Issue("JENKINS-55139")
    @Test
    public void testTerraform() throws Exception {
        story.then(r -> {
            FreeStyleProject p = r.createFreeStyleProject();
            p.getBuildWrappersList().add(new AnsiColorBuildWrapper(null));
            p.getBuildersList().add(new TestBuilder() {
                @Override
                public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
                    // Mimics terraform, c.f. `docker run --rm hashicorp/terraform plan 2>&1 | od -t a`
                    listener.getLogger().println("\033[31m");
                    listener.getLogger().println("\033[1m\033[31mError: \033[0m\033[0m\033[1mNo configuration files found!");
                    listener.getLogger().println("bold text blurb\033[0m");
                    listener.getLogger().println("\033[0m\033[0m\033[0m");
                    return true;
                }
            });
            FreeStyleBuild b = r.buildAndAssertSuccess(p);
            StringWriter writer = new StringWriter();
            b.getLogText().writeHtmlTo(0L, writer);
            String html = writer.toString();
            System.out.print(html);
            assertThat(html.replaceAll("<!--.+?-->", ""),
                allOf(
                    containsString("Error"),
                    containsString("No configuration files found!"),
                    not(containsString("\033[0m"))));
        });
    }

    @Issue("JENKINS-55139")
    @Test
    public void testRedundantResets() throws Exception {
        story.then(r -> {
            FreeStyleProject p = r.createFreeStyleProject();
            p.getBuildWrappersList().add(new AnsiColorBuildWrapper(null));
            p.getBuildersList().add(new TestBuilder() {
                @Override
                public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
                    listener.getLogger().println("[Foo] \033[0m[\033[0m\033[0minfo\033[0m] \033[0m\033[0m\033[32m- this text is green\033[0m\033[0m");
                    return true;
                }
            });
            FreeStyleBuild b = r.buildAndAssertSuccess(p);
            StringWriter writer = new StringWriter();
            b.getLogText().writeHtmlTo(0L, writer);
            String html = writer.toString();
            System.out.print(html);
            assertThat(html.replaceAll("<!--.+?-->", ""),
                allOf(
                    containsString("[Foo]"),
                    containsString("[info]"),
                    containsString("<span style=\"color: #00CD00;\">- this text is green</span>"),
                    not(containsString("\033[0m"))));
        });
    }
}
