/*
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
package nu.studer.gradle.jooq.util;

import groovy.lang.Closure;
import org.gradle.api.GradleException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Utility class.
 */
public final class Objects {

    /**
     * Applies the given closure to the given delegate.
     *
     * @param closure the closure to apply
     * @param delegate the delegate that the closure is applied to
     */
    public static void applyClosureToDelegate(Closure<?> closure, Object delegate) {
        Closure<?> copy = (Closure<?>) closure.clone();
        copy.setResolveStrategy(Closure.DELEGATE_FIRST);
        copy.setDelegate(delegate);
        if (copy.getMaximumNumberOfParameters() == 0) {
            copy.call();
        } else {
            copy.call(delegate);
        }
    }

    /**
     * Clones the given object via in-memory object serialization and deserialization.
     *
     * @param obj the object to clone
     * @param <T> the type of the object to clone
     * @return the cloned object
     */
    public static <T> T cloneObject(T obj) {
        ByteArrayOutputStream bas = new ByteArrayOutputStream();
        try (ObjectOutputStream os = new ObjectOutputStream(bas)) {
            os.writeObject(obj);
            os.flush();
        } catch (IOException e) {
            throw new GradleException("Cannot serialize object: " + obj.getClass(), e);
        }

        ByteArrayInputStream bis = new ByteArrayInputStream(bas.toByteArray());
        try (ObjectInputStream is = new ObjectInputStream(bis)) {
            @SuppressWarnings("unchecked")
            T clone = (T) is.readObject();
            return clone;
        } catch (IOException | ClassNotFoundException e) {
            throw new GradleException("Cannot deserialize object: " + obj.getClass(), e);
        }
    }

    private Objects() {
    }

}
