/*
 * Copyright 2013 Shazam Entertainment Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.github.karsaig.approvalcrest;

import static com.github.karsaig.approvalcrest.ResultComparison.containsComparableJson;
import static java.util.Collections.singletonList;

import java.util.List;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.ComparisonFailure;

import com.github.karsaig.approvalcrest.matcher.CustomisableMatcher;

import junit.framework.AssertionFailedError;

/**
 * Modified version of {@link org.hamcrest.MatcherAssert}. If the matcher doesn't match, uses
 * {@link ResultComparison#containsComparableJson(String, Description)} to determine if a {@link ComparisonFailure} should be
 * thrown. The exception is thrown instead of {@link AssertionError}, so that IDE like eclipse and IntelliJ can display a
 * pop-up window highlighting the String differences.
 */
public class MatcherAssert {
    /**
     * @param actual  the object that will be matched against the matcher
     * @param matcher defines the condition the object have to fulfill in order to match
     * @see org.hamcrest.MatcherAssert#assertThat(Object, Matcher)
     */
    public static <T> void assertThat(T actual, Matcher<? super T> matcher) {
        assertThat("", actual, matcher);
    }

    /**
     * Checks if the object matches the condition defined by the matcher provided.
     *
     * @param reason  describes the assertion
     * @param actual  the object that will be matched against the matcher
     * @param matcher defines the condition the object have to fulfill in order to match
     */
    public static <T> void assertThat(String reason, T actual, Matcher<? super T> matcher) {
        if (!matcher.matches(actual)) {
            Description description = new ComparisonDescription();
            description.appendText(reason)
                    .appendText("\nExpected: ")
                    .appendDescriptionOf(matcher)
                    .appendText("\n     but: ");
            matcher.describeMismatch(actual, description);

            containsComparableJson(reason, description);

            throw new AssertionError(description.toString());
        }
    }

    private static final List<Class<? extends Throwable>> BLACKLIST = singletonList(OutOfMemoryError.class);

    @SuppressWarnings({"ProhibitedExceptionCaught", "ThrowInsideCatchBlockWhichIgnoresCaughtException"})
    public static Throwable assertThrows(CustomisableMatcher<Throwable, ?> matcher, Executable executable) {
        return assertThrows(null, matcher, executable);
    }

    @SuppressWarnings({"ProhibitedExceptionCaught", "ThrowInsideCatchBlockWhichIgnoresCaughtException"})
    public static Throwable assertThrows(String reason, CustomisableMatcher<Throwable, ?> matcher, Executable executable) {
        try {
            executable.execute();
        } catch (Throwable throwable) {
            if (BLACKLIST.stream().anyMatch((exceptionType) -> exceptionType.isInstance(throwable))) {
                throw new RuntimeException(throwable);
            }
            assertThat(reason, throwable, matcher);
            return throwable;
        }
        throw new AssertionFailedError("Expected exception but no exception was thrown!");
    }
}
