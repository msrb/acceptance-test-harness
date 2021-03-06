/*
 * The MIT License
 *
 * Copyright (c) 2014 Red Hat, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.jenkinsci.test.acceptance.junit;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.jenkinsci.test.acceptance.guice.World;
import org.junit.Assume;
import org.junit.internal.AssumptionViolatedException;
import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

import com.google.inject.Inject;

/**
 * Test rule to filter tests to run.
 *
 * @author ogondza
 */
public class FilterRule implements MethodRule {

    @Override
    public Statement apply(final Statement base, final FrameworkMethod method, final Object target) {
        return new Statement() {

            @Inject(optional = true)
            Filter filter;

            @Override
            public void evaluate() throws Throwable {
                World.get().getInjector().injectMembers(this);
                if (filter != null) {
                    String reason = filter.whySkip(base, method, target);
                    // Fail assumption if there is some reason to skip
                    Assume.assumeTrue(reason, reason == null);
                }

                applyActivtionProperties(method, target);

                base.evaluate();
            }
        };
    }

    private static void applyActivtionProperties(final FrameworkMethod method, final Object target) {
        TestActivation caseActivation = target.getClass().getAnnotation(TestActivation.class);
        assumePropertyConfigured(caseActivation, target.getClass());
        TestActivation methodActivation = method.getAnnotation(TestActivation.class);
        assumePropertyConfigured(methodActivation, target.getClass());
    }

    private static void assumePropertyConfigured(TestActivation activation, Class<?> testClass) {
        if (activation == null) return; // No activation - always run

        String className = testClass.getSimpleName();

        for (String property: activation.value()) {
            String propertyName = className + "." + property;
            if (System.getProperty(propertyName) == null) {
                throw new AssumptionViolatedException("No propererty provided: " + propertyName);
            }
        }

        return; // All properties provided - run
    }

    public static abstract class Filter {
        /**
         * @return null if test should be run, the reason why not otherwise.
         */
        public abstract String whySkip(Statement base, FrameworkMethod method, Object target);

        public static <T extends Annotation> Set<T> getAnnotations(FrameworkMethod method, Object target, Class<T> type) {
            Set<T> annotations = new HashSet<T>();
            annotations.add(method.getAnnotation(type));
            annotations.add(target.getClass().getAnnotation(type));
            annotations.remove(null);
            return annotations;
        }

        public static Set<Annotation> getAnnotations(FrameworkMethod method, Object target) {
            Set<Annotation> annotations = new HashSet<Annotation>();
            annotations.addAll(Arrays.asList(method.getAnnotations()));
            annotations.addAll(Arrays.asList(target.getClass().getAnnotations()));
            return annotations;
        }
    }
}
