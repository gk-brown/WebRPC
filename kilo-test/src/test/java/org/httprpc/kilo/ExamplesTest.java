/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.httprpc.kilo;

import org.httprpc.kilo.io.CSVDecoder;
import org.httprpc.kilo.io.CSVEncoder;
import org.httprpc.kilo.io.JSONDecoder;
import org.httprpc.kilo.io.JSONEncoder;
import org.httprpc.kilo.io.TemplateEncoder;
import org.httprpc.kilo.io.TextDecoder;
import org.httprpc.kilo.io.TextEncoder;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.httprpc.kilo.util.Collections.entry;
import static org.httprpc.kilo.util.Collections.listOf;
import static org.httprpc.kilo.util.Collections.mapOf;

public class ExamplesTest {
    @Test
    public void testJSONEncoder() throws IOException {
        Map<String, Object> map = mapOf(
            entry("vegetables", listOf(
                "carrots",
                "peas",
                "potatoes"
            )),
            entry("desserts", listOf(
                "cookies",
                "cake",
                "ice cream"
            ))
        );

        var jsonEncoder = new JSONEncoder();

        jsonEncoder.write(map, System.out);
    }

    @Test
    public void testJSONDecoder() throws IOException {
        try (var inputStream = getClass().getResourceAsStream("months.json")) {
            var jsonDecoder = new JSONDecoder();

            List<Map<String, Object>> months = jsonDecoder.read(inputStream);

            for (var month : months) {
                System.out.println(String.format("%s has %s days", month.get("name"), month.get("days")));
            }
        }
    }

    @Test
    public void testCSVEncoder() throws IOException {
        List<Map<String, Object>> months;
        try (var inputStream = getClass().getResourceAsStream("months.json")) {
            var jsonDecoder = new JSONDecoder();

            months = jsonDecoder.read(inputStream);

            var csvEncoder = new CSVEncoder(listOf("name", "days"));

            csvEncoder.write(months, System.out);
        }
    }

    @Test
    public void testCSVDecoder() throws IOException {
        try (var inputStream = getClass().getResourceAsStream("months.csv")) {
            var csvDecoder = new CSVDecoder();

            var months = csvDecoder.read(inputStream);

            for (var month : months) {
                System.out.println(String.format("%s has %s days", month.get("name"), month.get("days")));
            }
        }
    }

    @Test
    public void testTextEncoderAndDecoder() throws IOException {
        var file = File.createTempFile("kilo", ".txt");

        try {
            try (var outputStream = new FileOutputStream(file)) {
                var textEncoder = new TextEncoder();

                textEncoder.write("Hello, World!", outputStream);
            }

            String text;
            try (var inputStream = new FileInputStream(file)) {
                var textDecoder = new TextDecoder();

                text = textDecoder.read(inputStream);
            }

            System.out.println(text);
        } finally {
            file.delete();
        }
    }

    @Test
    public void testTemplateEncoder() throws IOException {
        Map<String, Object> map = mapOf(
            entry("a", "hello"),
            entry("b", 123),
            entry("c", true)
        );

        var templateEncoder = new TemplateEncoder(getClass().getResource("example.txt"));

        templateEncoder.write(map, System.out);
    }

    @Test
    public void testModifier() throws IOException {
        var templateEncoder = new TemplateEncoder(getClass().getResource("modifier.txt"));

        templateEncoder.getModifiers().put("uppercase", (value, argument, locale, timeZone) -> value.toString().toUpperCase(locale));

        templateEncoder.write(mapOf(
            entry("text", "hello")
        ), System.out);
    }
}
