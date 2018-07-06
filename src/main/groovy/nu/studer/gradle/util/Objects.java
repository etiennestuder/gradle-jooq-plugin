/**
 Copyright 2014 Etienne Studer

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */
package nu.studer.gradle.util;

import groovy.lang.Closure;

/**
 * Utility class.
 */
public final class Objects {

    private Objects() {
    }

    /**
     * Applies the given closure to the given delegate.
     *
     * @param closure  the closure to apply
     * @param delegate the delegate that the closure is applied to
     */
    public static void applyClosureToDelegate(Closure closure, Object delegate) {
        Closure copy = (Closure) closure.clone();
        copy.setResolveStrategy(Closure.DELEGATE_FIRST);
        copy.setDelegate(delegate);
        if (copy.getMaximumNumberOfParameters() == 0) {
            copy.call();
        } else {
            copy.call(delegate);
        }
    }

}
