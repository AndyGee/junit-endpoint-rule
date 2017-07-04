/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.andygee.junit;

import org.junit.ClassRule;
import org.junit.Test;

import java.io.File;
import java.net.URI;
import java.util.concurrent.TimeUnit;

/**
 * Simple WireMock tests
 */
public class MicroserviceRuleTest {

    @ClassRule
    public static MicroserviceRule ms1 = new MicroserviceRule("http://localhost:8080/endpoint1")
            .withExecutableJar(new File("ms1.jar"))
            .withJavaResolutionStrategy(new DefaultJavaResolutionStrategy())
            .withTimeout(2, TimeUnit.SECONDS)
            .withPollingInterval(500);

    @ClassRule
    public static MicroserviceRule ms2 = new MicroserviceRule(URI.create("http://localhost:8080/endpoint1"))
            .withExecutableJar(new File("ms1.jar"))
            .withJavaResolutionStrategy(new DefaultJavaResolutionStrategy())
            .withTimeout(2, TimeUnit.SECONDS)
            .withPollingInterval(500);

    @Test
    public void test() {

    }

}