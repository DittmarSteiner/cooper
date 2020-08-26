package com.github.dittmarsteiner.cooper;

import org.junit.jupiter.api.Test;

import static com.github.dittmarsteiner.cooper.Json.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class JsonTest {

    @Test
    void marshalToString() {
        var obj = new Object() {
            public String name = "name";
            public int count = 1;
        };

        var str = JSON.marshal(obj);
//        System.out.println(str);
        assertEquals("{\"name\":\"name\",\"count\":1}", str);
    }

    @Test
    void marshalPrettyToString() {
        var obj = new Object() {
            public String name = "name";
            public int count = 1;
        };

        var str = PRETTY.marshal(obj);
//        System.out.println(str);
        assertEquals(
                "{\n" +
                "  \"name\" : \"name\",\n" +
                "  \"count\" : 1\n" +
                "}",
                str);
    }
}