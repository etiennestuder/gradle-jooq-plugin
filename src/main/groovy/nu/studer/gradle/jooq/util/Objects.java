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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utility class.
 */
public final class Objects {

    private static final MessageDigest MESSAGE_DIGEST;

    static {
        try {
            MESSAGE_DIGEST = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

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

    /**
     * Calculates a hash of the given object from its serialized version.
     *
     * @param obj the object for which to calculate the hash
     * @return the hash
     */
    public static String deepHash(Object obj) {
        try {
            ByteArrayOutputStream bas = new ByteArrayOutputStream();
            try (ObjectOutputStream os = new ObjectOutputStream(bas)) {
                os.writeObject(obj);
                os.flush();
            } catch (IOException e) {
                throw new GradleException("Cannot serialize object: " + obj.getClass(), e);
            }

            StringBuilder hexString = new StringBuilder();
            byte[] encodedHash = MESSAGE_DIGEST.digest(bas.toByteArray());
            for (byte hash : encodedHash) {
                String hex = Integer.toHexString(0xff & hash);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (GradleException e) {
            throw new RuntimeException(e);
        }
    }

    private Objects() {
    }

}
