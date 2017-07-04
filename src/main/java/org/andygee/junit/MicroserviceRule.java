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

import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.junit.Assert;
import org.junit.rules.ExternalResource;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

/**
 * The MicroserviceRule provides a JUnit rule primarily for use with {@link org.junit.ClassRule}
 */
public class MicroserviceRule extends ExternalResource {

    private final Logger log = Logger.getLogger(MicroserviceRule.class.getName());

    private final ReentrantLock lock = new ReentrantLock();
    private final CountDownLatch latch = new CountDownLatch(1);
    private final AtomicBoolean poll = new AtomicBoolean(true);
    private final AtomicReference<URL> url = new AtomicReference<>();
    private File file;
    private String[] args;
    private ResolutionStrategy strategy = new DefaultJavaResolutionStrategy();
    private long time = 30;
    private TimeUnit unit = TimeUnit.SECONDS;
    private int interval = 2000;

    /**
     * URL constructor
     *
     * @param url URL to test against
     */
    public MicroserviceRule(URL url) {
        this.url.set(url);
    }

    /**
     * String constructor
     *
     * @param url Valid URL String to test against
     */
    public MicroserviceRule(String url) {
        try {
            this.url.set(new URL(url));
        } catch (MalformedURLException e) {
            throw new RuntimeException("Invalid URL: " + url, e);
        }
    }

    /**
     * Provide the executable jar file
     *
     * @param file Accessible File location for the jar
     * @param args Optional String array of arguments
     * @return MicroserviceRule
     */
    public MicroserviceRule withExecutableJar(File file, String... args) {
        Assert.assertTrue("The file must exist and be readable: " + file, file.exists() && file.canRead());

        this.file = file;
        this.args = args;
        return this;
    }

    /**
     * Override the default Java binary resolution strategy, that uses JAVA_HOME.
     * <p>
     * See {@link DefaultJavaResolutionStrategy}
     *
     * @param strategy ResolutionStrategy implementation
     * @return MicroserviceRule
     */
    public MicroserviceRule withJavaResolutionStrategy(ResolutionStrategy strategy) {
        this.strategy = (null != strategy ? strategy : this.strategy);
        return this;
    }

    /**
     * Define the period to wait for the Microservice to start
     *
     * @param time int time value
     * @param unit TimeUnit value
     * @return MicroserviceRule
     */
    public MicroserviceRule withTimeout(int time, TimeUnit unit) {
        this.time = time;
        this.unit = unit;
        return this;
    }

    /**
     * Override the default polling interval of 2000ms
     *
     * @param interval int milliseconds between 100 and 5000
     * @return MicroserviceRule
     */
    public MicroserviceRule withPollingInterval(int interval) {
        this.interval = (interval >= 100 && interval <= 5000 ? interval : 2000);
        return this;
    }

    private Process process;

    @Override
    protected void before() throws Throwable {

        Assert.assertNotNull("The MicroserviceRule requires a valid jar file", this.file);
        Assert.assertNotNull("The MicroserviceRule requires a valid url", this.url.get());

        this.lock.lock();

        try {
            ArrayList<String> args = new ArrayList<>();
            args.add(this.strategy.getJavaExecutable().toString());
            args.add("-jar");
            args.add(this.file.toString());

            if (null != this.args) {
                args.addAll(Arrays.asList(this.args));
            }

            ProcessBuilder pb = new ProcessBuilder(args.toArray(new String[args.size()]));
            pb.directory(file.getParentFile());
            pb.inheritIO();
            process = pb.start();

            log.info("Started " + this.file);

            final Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    if (MicroserviceRule.this.connect(MicroserviceRule.this.url.get())) {
                        MicroserviceRule.this.latch.countDown();
                    }
                }
            }, "Microservice Connect thread :: " + this.url.get());

            t.start();

            if (!latch.await(this.time, this.unit)) {
                throw new RuntimeException("Failed to connect to server within timeout: " + this.url.get());
            }

        } finally {
            this.poll.set(false);
            this.lock.unlock();
        }
    }

    @Override
    protected void after() {

        this.lock.lock();

        try {
            if (null != process) {
                process.destroy();
                process = null;
            }
        } finally {
            this.lock.unlock();
        }
    }

    private boolean connect(final URL url) {

        do {
            try {
                Request request = new Request.Builder().url(url).build();

                if (new OkHttpClient().newCall(request).execute().isSuccessful()) {
                    return true;
                } else {
                    throw new Exception("Unexpected family");
                }
            } catch (Exception ignore) {

                if (poll.get()) {
                    try {
                        Thread.sleep(interval);
                    } catch (InterruptedException e) {
                        return false;
                    }
                }
            }
        } while (poll.get());

        return false;
    }

}
