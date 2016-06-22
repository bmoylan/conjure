/*
 * Copyright 2016 Palantir Technologies, Inc. All rights reserved.
 */

package com.palantir.conjure

import com.google.common.io.Resources
import java.nio.charset.Charset
import org.gradle.testkit.runner.TaskOutcome

public class ConjureJavaPluginTest extends GradleTestSpec {
    def setup() {
        buildFile << """
        plugins {
            id 'com.palantir.gradle-conjure-java'
        }

        repositories { jcenter() }

        dependencies {
            generatedCompile "javax.ws.rs:javax.ws.rs-api:2.0.1"
        }
        """
    }

    def 'compileConjure is up to date when no source files exist'() {
        when:
        def result = run("compileConjureJavaServer")

        then:
        result.task(":compileConjureJavaServer").outcome == TaskOutcome.UP_TO_DATE
    }

    def 'compiles all source files'() {
        when:
        createSourceFile("a.yml", readResource("test-service-a.yml"))
        createSourceFile("b.yml", readResource("test-service-b.yml"))
        def result = run("compileConjureJavaServer")

        then:
        result.task(":compileConjureJavaServer").outcome == TaskOutcome.SUCCESS
        compiledFile("test/a/api/TestServiceA.java").text.contains("public interface TestServiceA")
        compiledFile("test/b/api/TestServiceB.java").text.contains("public interface TestServiceB")
    }

    def 'can compile generated Java sourceSet'() {
        when:
        createSourceFile("a.yml", readResource("test-service-a.yml"))
        def result = run("compileConjureJavaServer", "compileGeneratedJava")

        then:
        result.task(":compileConjureJavaServer").outcome == TaskOutcome.SUCCESS
        result.task(":compileGeneratedJava").outcome == TaskOutcome.SUCCESS
    }

    def 'main sourceSet depends on generated source set'() {
        when:
        createSourceFile("a.yml", readResource("test-service-a.yml"))
        createFile("src/main/java/Foo.java").text = """
            import test.a.api.SimpleObject;
            public class Foo {
                SimpleObject var = new SimpleObject("foo");
            }
        """
        def result = run("compileJava")

        then:
        result.task(":compileConjureJavaServer").outcome == TaskOutcome.SUCCESS
        result.task(":compileGeneratedJava").outcome == TaskOutcome.SUCCESS
        result.task(":compileJava").outcome == TaskOutcome.SUCCESS
    }

    def createSourceFile(String fileName, String text) {
        createFile("src/main/conjure/" + fileName).text = text
    }

    def readResource(String name) {
        return Resources.asCharSource(Resources.getResource(name), Charset.defaultCharset()).read()
    }

    def compiledFile(String fileName) {
        return file("src/generated/java/" + fileName)
    }
}