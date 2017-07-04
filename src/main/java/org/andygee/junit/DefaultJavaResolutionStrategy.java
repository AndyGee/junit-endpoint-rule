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

import org.junit.Assert;

import java.io.File;
import java.io.IOException;

/**
 * A default lookup strategy the uses JAVA_HOME to locate the Java executable
 */
public class DefaultJavaResolutionStrategy implements ResolutionStrategy {

    /**
     * See {@link ResolutionStrategy#getJavaExecutable()}
     */
    @Override
    public File getJavaExecutable() {

        //Locate java
        final String javaHome = System.getenv("JAVA_HOME");
        final File java;
        try {
            java = new File(javaHome + "/bin/java" + (System.getProperty("os.name").toLowerCase().contains("win") ? "" +
                    ".exe" : "")).getCanonicalFile();
        } catch (IOException e) {
            throw new AssertionError("Failed to determine " + "canonical path to java using JAVA_HOME: " + javaHome, e);
        }

        Assert.assertTrue("Ensure that JAVA_HOME points to " + "a " + "valid java installation", java.exists());

        return java;
    }
}
