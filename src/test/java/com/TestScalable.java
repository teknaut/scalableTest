package com;

import com.scalable.Target;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TestScalable {

    private Logger LOG = Logger.getLogger(TestScalable.class.getName());

    private StringBuilder html = new StringBuilder();

    @BeforeMethod
    public void setUp(){
        String line;
        try (InputStream inputStream =
                     TestScalable.class.
                             getClassLoader().
                             getResourceAsStream("files/test.html")){

            Assert.assertNotNull(inputStream);
            BufferedReader in           = new BufferedReader(new InputStreamReader(inputStream));
            while ((line = in.readLine()) != null) {
                html.append(line);
            }
        }
        catch(Exception ex){
            LOG.log(Level.WARNING, "UNABLE T0 READ TEST FILE", ex);
        }

        Assert.assertNotNull(html);
    }

    /**
     * I know this is no comprehensive test but demonstrates testing
     * one aspect of the process
     */
    @Test
    public void testScriptRegex(){
        Target target = new Target("test");
        Map<String, Integer> libraries = target.getScripts(html);
        Assert.assertEquals(libraries.size(), 2);
    }

}
