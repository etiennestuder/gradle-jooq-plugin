package nu.studer.gradle.util;

import org.junit.Test;

import java.awt.*;
import java.util.*;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class ObjectsTest {

    @Test
    public void recursive_hash_of_null_is_zero() {
        assertEquals(0, Objects.deepHashCode(null));
    }

    @Test
    @SuppressWarnings("UnnecessaryBoxing")
    public void recursive_hash_of_primitive_is_different_from_hash_of_primitive() {
        double pi = 3.14159;

        assertNotEquals(Objects.deepHashCode(pi), new Double(pi).hashCode());
        assertEquals(Objects.deepHashCode(pi), Objects.deepHashCode(new Double(3.14159)));
    }

    @Test
    @SuppressWarnings("RedundantStringConstructorCall")
    public void recursive_hash_of_string_is_different_from_hash_of_string() {
        String string = "hello";

        assertNotEquals(Objects.deepHashCode(string), string.hashCode());
        assertEquals(Objects.deepHashCode(string), Objects.deepHashCode(new String("hello")));
    }

    @Test
    public void multiple_booleans_with_same_values_on_same_fields() {
        Booleans booleans1 = new Booleans();
        booleans1.value1 = true;
        booleans1.value2 = false;
        booleans1.value3 = true;

        Booleans booleans2 = new Booleans();
        booleans2.value1 = true;
        booleans2.value2 = false;
        booleans2.value3 = true;

        assertEquals(Objects.deepHashCode(booleans1), Objects.deepHashCode(booleans2));
    }

    @Test
    public void multiple_booleans_with_same_values_on_different_fields() {
        Booleans booleans1 = new Booleans();
        booleans1.value1 = false;
        booleans1.value2 = true;
        booleans1.value3 = false;

        Booleans booleans2 = new Booleans();
        booleans2.value1 = true;
        booleans2.value2 = false;
        booleans2.value3 = false;

        assertNotEquals(Objects.deepHashCode(booleans1), Objects.deepHashCode(booleans2));
    }

    @Test
    public void multiple_booleans_with_different_values() {
        Booleans booleans1 = new Booleans();
        booleans1.value1 = false;
        booleans1.value2 = true;
        booleans1.value3 = false;

        Booleans booleans2 = new Booleans();
        booleans2.value1 = false;
        booleans2.value2 = true;
        booleans2.value3 = true;

        assertNotEquals(Objects.deepHashCode(booleans1), Objects.deepHashCode(booleans2));
    }

    @Test
    public void class_with_custom_hashCode_same_content() {
        Dimension d1 = new Dimension(3, 4);
        Dimension d2 = new Dimension(3, 4);

        assertEquals(Objects.deepHashCode(d1), Objects.deepHashCode(d2));
    }

    @Test
    public void class_with_custom_hashCode_different_content() {
        Dimension d1 = new Dimension(3, 4);
        Dimension d2 = new Dimension(4, 3);

        assertNotEquals(Objects.deepHashCode(d1), Objects.deepHashCode(d2));
    }

    @Test
    public void class_without_custom_hashCode_same_content() {
        StringBuilder sb1 = new StringBuilder("hello");
        StringBuilder sb2 = new StringBuilder("hello");

        assertEquals(Objects.deepHashCode(sb1), Objects.deepHashCode(sb2));
    }

    @Test
    public void class_without_custom_hashCode_different_content() {
        StringBuilder sb1 = new StringBuilder("hello");
        StringBuilder sb2 = new StringBuilder("welcome");

        assertNotEquals(Objects.deepHashCode(sb1), Objects.deepHashCode(sb2));
    }

    @Test
    public void different_classes_with_same_hashCode() {
        FixedHashA h1 = new FixedHashA();
        FixedHashB h2 = new FixedHashB();

        assertNotEquals(Objects.deepHashCode(h1), Objects.deepHashCode(h2));
    }

    @Test
    public void set_with_elements_without_hashCode_same_content() {
        Set<StringBuilder> set1 = new HashSet<StringBuilder>();
        set1.add(new StringBuilder("hello"));
        set1.add(new StringBuilder("welcome"));

        Set<StringBuilder> set2 = new HashSet<StringBuilder>();
        set2.add(new StringBuilder("welcome"));
        set2.add(new StringBuilder("hello"));

        assertEquals(Objects.deepHashCode(set1), Objects.deepHashCode(set2));
    }

    @Test
    public void set_with_elements_without_hashCode_different_content() {
        Set<StringBuilder> set1 = new HashSet<StringBuilder>();
        set1.add(new StringBuilder("hello"));
        set1.add(new StringBuilder("welcome"));

        Set<StringBuilder> set2 = new HashSet<StringBuilder>();
        set2.add(new StringBuilder("welcome"));
        set2.add(new StringBuilder("goodbye"));

        assertNotEquals(Objects.deepHashCode(set1), Objects.deepHashCode(set2));
    }

    @Test
    public void map_with_elements_without_hashCode_same_content() {
        Map<StringBuilder, StringTokenizer> map1 = new HashMap<StringBuilder, StringTokenizer>();
        map1.put(new StringBuilder("hello"), new StringTokenizer("bye"));
        map1.put(new StringBuilder("welcome"), new StringTokenizer("goodbye"));

        Map<StringBuilder, StringTokenizer> map2 = new HashMap<StringBuilder, StringTokenizer>();
        map2.put(new StringBuilder("hello"), new StringTokenizer("bye"));
        map2.put(new StringBuilder("welcome"), new StringTokenizer("goodbye"));

        assertEquals(Objects.deepHashCode(map1), Objects.deepHashCode(map2));
    }

    @Test
    public void map_with_elements_without_hashCode_different_content() {
        Map<StringBuilder, StringTokenizer> map1 = new HashMap<StringBuilder, StringTokenizer>();
        map1.put(new StringBuilder("hello"), new StringTokenizer("bye"));
        map1.put(new StringBuilder("welcome"), new StringTokenizer("goodbye"));

        Map<StringBuilder, StringTokenizer> map2 = new HashMap<StringBuilder, StringTokenizer>();
        map2.put(new StringBuilder("hello"), new StringTokenizer("goodbye"));
        map2.put(new StringBuilder("welcome"), new StringTokenizer("bye"));

        assertNotEquals(Objects.deepHashCode(map1), Objects.deepHashCode(map2));
    }

    @Test
    public void nested_objects_same_content() {
        Container c1 = new Container();
        c1.singleValue = "hello";

        List<Object> elements = new ArrayList<Object>();
        elements.add(new StringBuilder("hello"));
        elements.add(new StringBuilder("welcome"));
        elements.add(Arrays.asList(1, 2, 3, 4));
        elements.add(Collections.singletonMap(new Dimension(3, 4), new StringTokenizer("bye")));
        c1.elements = elements;

        Container c2 = new Container();
        c2.singleValue = "hello";

        elements = new ArrayList<Object>();
        elements.add(new StringBuilder("hello"));
        elements.add(new StringBuilder("welcome"));
        elements.add(Arrays.asList(1, 2, 3, 4));
        elements.add(Collections.singletonMap(new Dimension(3, 4), new StringTokenizer("bye")));
        c2.elements = elements;

        assertEquals(Objects.deepHashCode(c1), Objects.deepHashCode(c2));
    }

    @Test
    public void nested_objects_different_content() {
        Container c1 = new Container();
        c1.singleValue = "hello";

        List<Object> elements = new ArrayList<Object>();
        elements.add(new StringBuilder("hello"));
        elements.add(new StringBuilder("welcome"));
        elements.add(Arrays.asList(1, 2, 3, 4));
        elements.add(Collections.singletonMap(new Dimension(3, 4), new StringTokenizer("bye")));
        c1.elements = elements;

        Container c2 = new Container();
        c2.singleValue = "hello";

        elements = new ArrayList<Object>();
        elements.add(new StringBuilder("hello"));
        elements.add(new StringBuilder("welcome"));
        elements.add(Arrays.asList(1, 2, 3, 4));
        elements.add(Collections.singletonMap(new Dimension(3, 4), new StringTokenizer("goodbye")));
        c2.elements = elements;

        assertNotEquals(Objects.deepHashCode(c1), Objects.deepHashCode(c2));
    }

    private static final class Booleans {

        boolean value1;
        boolean value2;
        boolean value3;

    }

    private static final class FixedHashA {

        @Override
        public int hashCode() {
            return 7;
        }

    }

    private static final class FixedHashB {

        @Override
        public int hashCode() {
            return 7;
        }

    }

    @SuppressWarnings("UnusedDeclaration")
    private static final class Container {

        private Object singleValue;
        private Collection<?> elements;

    }

}
