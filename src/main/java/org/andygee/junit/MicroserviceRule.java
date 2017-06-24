package org.andygee.junit;

import org.junit.rules.ExternalResource;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.locks.ReentrantLock;

public class MicroserviceRule extends ExternalResource {

    private final ReentrantLock lock = new ReentrantLock();
    private final URL url;
    private File file;
    private String[] args;
    private ResolutionStrategy strategy = new
            DefaultJavaResolutionStrategy();

    public MicroserviceRule(URL url) {
        this.url = url;
    }

    public MicroserviceRule(String url) {
        try {
            this.url = new URL(url);
        } catch (MalformedURLException e) {
            throw new RuntimeException("Invalid URL: " + url, e);
        }
    }

    MicroserviceRule withExecutableJar(File file, String... args) {
        this.file = file;
        this.args = args;
        return this;
    }

    MicroserviceRule withJavaResolutionStrategy(ResolutionStrategy
                                                        strategy) {
        this.strategy = (null != strategy ? strategy : this.strategy);
        return this;
    }

    private Process process;

    @Override
    protected void before() throws Throwable {

        if (null == this.file) {
            return;
        }

        this.lock.lock();

        try {
            ArrayList<String> args = new ArrayList<>();
            args.add(this.strategy.getJavaExecutable().toString());
            args.add("-jar");
            args.add(this.file.toString());

            if (null != this.args) {
                args.addAll(Arrays.asList(this.args));
            }


            ProcessBuilder pb = new ProcessBuilder(args.toArray(new
                    String[args.size()]));
            pb.directory(file.getParentFile());
            pb.inheritIO();
            process = pb.start();
        } finally {
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

    private boolean connect(final String url) {
        Client client = null;
        int err = 0;

        do {
            try {
                client = ClientBuilder.newClient();
                Response.Status.Family family = client.target(url)
                        .request(MediaType.APPLICATION_JSON_TYPE)
                        .get().getStatusInfo().getFamily();

                if (Response.Status.Family.SUCCESSFUL.equals
                        (family)) {
                    return true;
                } else {
                    throw new Exception("Unexpected family");
                }
            } catch (Exception ignore) {
                err++;
                System.out.println("Waited to connect: " + ignore
                        .getMessage());
                if (err < 10) {
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        return false;
                    }
                }
            } finally {
                if (client != null) {
                    client.close();
                }
            }
        } while (err < 10);

        return false;
    }


}
