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

package org.httprpc.kilo.test;

import org.httprpc.kilo.WebServiceProxy;
import org.httprpc.kilo.io.JSONDecoder;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URL;
import java.util.Map;

import static org.httprpc.kilo.util.Collections.entry;
import static org.httprpc.kilo.util.Collections.mapOf;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class DocumentationTest {
    private URL baseURL;

    public DocumentationTest() throws IOException {
        baseURL = new URL("http://localhost:8080/kilo-test/");
    }

    @Test
    public void testDocumentation() throws IOException {
        testDocumentation("math");
        testDocumentation("file-upload");
        testDocumentation("catalog");
    }

    private void testDocumentation(String name) throws IOException {
        Map<?, ?> expected;
        try (var inputStream = getClass().getResourceAsStream(String.format("%s.json", name))) {
            var jsonDecoder = new JSONDecoder();

            expected = (Map<?, ?>)jsonDecoder.read(inputStream);
        }

        var webServiceProxy = new WebServiceProxy("GET", baseURL, name);

        webServiceProxy.setHeaders(mapOf(
            entry("Accept", "application/json")
        ));

        webServiceProxy.setArguments(mapOf(
            entry("api", "json")
        ));

        webServiceProxy.setMonitorStream(System.out);

        var actual = webServiceProxy.invoke();

        assertEquals(expected, actual);
    }
}
