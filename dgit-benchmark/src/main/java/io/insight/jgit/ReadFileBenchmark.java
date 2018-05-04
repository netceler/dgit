/*
 * Copyright (c) 2014, Oracle America, Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  * Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 *  * Neither the name of Oracle nor the names of its contributors may be used
 *    to endorse or promote products derived from this software without
 *    specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 */

package io.insight.jgit;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.openjdk.jmh.annotations.*;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 5, timeUnit = TimeUnit.SECONDS)
@Fork(1)
@State(Scope.Benchmark)
public class ReadFileBenchmark {

    @Param({"dgit",/*"cassandra",*/ "file",/* "cassandra", "postgres","mysql","mem",*/})
    String engine;

    @Param({"MEDIUM", /*"SMALL"*/})
    String repoName;

    private Ref head;
    private Repository repository;

    File getPath() {
        return new File("/tmp", repoName);
    }

    ArrayList<String> files = new ArrayList<>();
    private BenchmarkUtil util=new BenchmarkUtil();
    @Setup
    public void setup() throws IOException, GitAPIException, URISyntaxException {
        util.startDaemon();
        util.clone(getPath(), repoName);
        util.push(getPath(), engine, repoName);
        repository = getRepo();
        head = repository.exactRef("refs/heads/master");
        try (RevWalk walk = new RevWalk(repository)) {
            RevCommit commit = walk.parseCommit(head.getObjectId());
            RevTree tree = walk.parseTree(commit.getTree().getId());
            TreeWalk treeWalk = new TreeWalk(repository);
            treeWalk.addTree(tree);
            treeWalk.setRecursive(true);
            while (treeWalk.next()) {
                files.add(treeWalk.getPathString());
            }
            treeWalk.close();
        }
        System.gc();
    }

    @TearDown
    public void teardown() {
        util.stopDaemon();

    }

    private Random r=new Random();


    @Benchmark
    public String readFile() throws IOException, GitAPIException {
        try (Repository repository = getRepo();
             RevWalk walk = new RevWalk(repository)) {
            try {
                String file = files.get(r.nextInt(files.size()));
                RevCommit commit = walk.parseCommit(head.getObjectId());
                RevTree tree = walk.parseTree(commit.getTree().getId());
                TreeWalk treeWalk = TreeWalk.forPath(repository, file, tree);
                byte[] bytes = repository.open(treeWalk.getObjectId(0)).getBytes();
                String content = new String(bytes);
                walk.dispose();
                return content;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

    }

    private Repository getRepo() throws IOException, GitAPIException {
        if (engine.equals("mem")) return this.repository;

        this.repository = util.openRepo(engine ,repoName);
        return this.repository;
    }

    public static void main(String[] args) throws IOException, GitAPIException, URISyntaxException {
        ReadFileBenchmark p = new ReadFileBenchmark();
        p.engine = "dgit";
        p.repoName = "MEDIUM";
        p.setup();
        String content = p.readFile();
        System.out.println("content = " + content);
        System.out.println("content = " + p.readFile());
        p.teardown();
    }
}