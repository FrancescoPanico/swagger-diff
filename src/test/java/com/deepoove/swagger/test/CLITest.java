package com.deepoove.swagger.test;

import java.io.ByteArrayOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import com.deepoove.swagger.diff.SwaggerDiff;
import com.deepoove.swagger.diff.cli.CLI;
import com.deepoove.swagger.diff.output.HtmlRender;
import com.deepoove.swagger.diff.output.MarkdownRender;

public class CLITest {

    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();

    @Before
    public void setUpStreams() {
        System.setOut(new PrintStream(outContent));
    }

    @After
    public void restoreStreams() {
        System.setOut(System.out);
    }

    @Test
    public void testCLI() {
        CLI cli = new CLI();
        String[] argv = { "-v", "2.0", "-old", "http://petstore.swagger.io/v2/swagger.json",
                "--help" };
        JCommander commander = JCommander.newBuilder().addObject(cli).build();

        commander.setProgramName("java swagger-diff.jar");
        commander.usage();
        commander.parse(argv);
        Assert.assertEquals(cli.getVersion(), "2.0");
        Assert.assertEquals(cli.getOldSpec(), "http://petstore.swagger.io/v2/swagger.json");
    }

    @Test
    public void testRegex() {
        CLI cli = new CLI();
        String[] argv = { "--help", "-v", "2.0", "-output-mode", "markdown" };
        JCommander.newBuilder().addObject(cli).build().parse(argv);

        argv = new String[] { "--help", "-v", "1.0", "-output-mode", "html" };
        JCommander.newBuilder().addObject(cli).build().parse(argv);

        argv = new String[] { "--help", "-v", "1.1.0" };
        try {
            JCommander.newBuilder().addObject(cli).build().parse(argv);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            Assert.assertTrue(e instanceof ParameterException);
        }

        argv = new String[] { "--help", "-output-mode", "html5" };
        try {
            JCommander.newBuilder().addObject(cli).build().parse(argv);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            Assert.assertTrue(e instanceof ParameterException);
        }
    }

    @Test
    public void testHelp() {
        CLI cli = new CLI();
        String[] argv = { "--help" };
        JCommander jCommander = JCommander.newBuilder().addObject(cli).build();
        jCommander.parse(argv);
        cli.run(jCommander);
        Assert.assertTrue(outContent.toString().startsWith("Usage: java -jar swagger-diff.jar "));
    }
    
    @Test
    public void testVersion() {
        CLI cli = new CLI();
        String[] argv = { "--version" };
        JCommander jCommander = JCommander.newBuilder().addObject(cli).build();
        jCommander.parse(argv);
        cli.run(jCommander);
        Assert.assertEquals("1.2.2-oas3", outContent.toString().trim());
    }
    
    @Test
    public void testMain() {
        CLI cli = new CLI();
        String[] argv = { "-old", "petstore_v2_1.json", "-new", "petstore_v2_2.json" };
        JCommander jCommander = JCommander.newBuilder().addObject(cli).build();
        jCommander.parse(argv);
        cli.run(jCommander);
        //System.setOut(System.out); // ripristina per vedere
        //String out = outContent.toString();
        //System.err.println("=== OUTPUT ===\n" + out + "\n=== FINE ===");
        //Assert.assertTrue(out.startsWith("## Version 1.0.0 to 1.0.2"));
        Assert.assertTrue(outContent.toString().startsWith("## Version 1.0.0 to 1.0.2"));
    }

    @Test
    public void testResponseRenderMarkdown() {
        SwaggerDiff diff = SwaggerDiff.compareV3(
            "src/test/resources/petstore_v3_diff1.json",
            "src/test/resources/petstore_v3_diff2.json"
        );
        String md = new MarkdownRender().render(diff);

        Assert.assertTrue("manca sezione Responses", md.contains("Responses"));
        Assert.assertTrue("manca 201 aggiunto", md.contains("Insert response 201"));
        Assert.assertTrue("manca 404 rimosso", md.contains("Delete response 404"));
    }

    @Test
    public void testResponseRenderHtml() {
        SwaggerDiff diff = SwaggerDiff.compareV3(
            "src/test/resources/petstore_v3_diff1.json",
            "src/test/resources/petstore_v3_diff2.json"
        );
        String html = new HtmlRender("Changelog",
                "http://deepoove.com/swagger-diff/stylesheets/demo.css")
                .render(diff);

        // (opzionale) scrivi su file per ispezione visiva
        try (FileWriter fw = new FileWriter("testResponseRender.html")) {
            fw.write(html);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // sezione Responses presente
        Assert.assertTrue("manca header Responses", html.contains("<h3>Responses</h3>"));

        // status aggiunti (201 e 400 nei tuoi dati)
        Assert.assertTrue("manca 201 aggiunto", html.contains("Add response 201"));
        Assert.assertTrue("manca 400 aggiunto", html.contains("Add response 400"));

        // status rimosso (404)
        Assert.assertTrue("manca 404 rimosso",
                html.contains("Delete response") && html.contains("404"));

        // status 200 modificato (description cambiata ok -> update Success)
        Assert.assertTrue("manca riferimento al Response 200", html.contains("Response 200"));
    }

}
