package org.andygee.junit;

import org.junit.ClassRule;
import org.junit.Test;

import java.io.File;
import java.util.concurrent.TimeUnit;

public class MicroserviceRuleTest {

    @ClassRule
    public static MicroserviceRule ms1 = new MicroserviceRule
            ("http://localhost:8080/endpoint1").withExecutableJar
            (new File
            ("ms1.jar"))
            .withJavaResolutionStrategy(new
                    DefaultJavaResolutionStrategy())
            .withTimeout(2, TimeUnit.SECONDS);

    @Test
    public void test(){

    }

}