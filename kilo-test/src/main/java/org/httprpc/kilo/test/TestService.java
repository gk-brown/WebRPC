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

import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import org.httprpc.kilo.Description;
import org.httprpc.kilo.RequestMethod;
import org.httprpc.kilo.Required;
import org.httprpc.kilo.ResourcePath;
import org.httprpc.kilo.WebService;
import org.httprpc.kilo.beans.BeanAdapter;
import org.httprpc.kilo.beans.Key;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.URL;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Period;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

import static org.httprpc.kilo.util.Collections.entry;
import static org.httprpc.kilo.util.Collections.mapOf;

@WebServlet(urlPatterns = {"/test/*"}, loadOnStartup = 1)
@MultipartConfig
public class TestService extends WebService {
    public interface A {
        int getA();
    }

    public interface B {
        @Description("B's version of B")
        double getB();
    }

    public interface C extends A, B {
        @Override
        @Description("C's version of B")
        double getB();
        String getC();
    }

    public static class D {
        public int getD() {
            return 0;
        }
    }

    public static class E extends D {
        public double getE() {
            return 0;
        }
    }

    public static class TestList extends ArrayList<Integer> {
    }

    public static class TestMap extends HashMap<String, Double> {
    }

    public interface Response {
        @Required
        String getString();
        List<String> getStrings();
        int getNumber();
        boolean getFlag();
        DayOfWeek getDayOfWeek();
        Date getDate();
        List<Date> getDates();
        Instant getInstant();
        LocalDate getLocalDate();
        LocalTime getLocalTime();
        LocalDateTime getLocalDateTime();
        Duration getDuration();
        Period getPeriod();
        @Key("uuid")
        UUID getUUID();
        List<AttachmentInfo> getAttachmentInfo();
    }

    public interface AttachmentInfo {
        int getBytes();
        int getChecksum();
    }

    public interface Body {
        @Required
        String getString();
        List<String> getStrings();
        int getNumber();
        boolean getFlag();
    }

    private static class FibonacciSequence extends AbstractList<Number> {
        private int count;

        FibonacciSequence(int count) {
            this.count = count;
        }

        @Override
        public BigInteger get(int index) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int size() {
            return count;
        }

        @Override
        public Iterator<Number> iterator() {
            return new Iterator<>() {
                int i = 0;

                BigInteger a = BigInteger.valueOf(0);
                BigInteger b = BigInteger.valueOf(1);

                @Override
                public boolean hasNext() {
                    return i < count;
                }

                @Override
                public BigInteger next() {
                    if (!hasNext()) {
                        throw new NoSuchElementException();
                    }

                    BigInteger next;
                    if (i == 0) {
                        next = a;
                    } else {
                        if (i > 1) {
                            var c = a.add(b);

                            a = b;
                            b = c;
                        }

                        next = b;
                    }

                    i++;

                    return next;
                }
            };
        }
    }

    @RequestMethod("GET")
    public Map<String, Object> testGet(@Required String string, List<String> strings,
        int number, boolean flag, DayOfWeek dayOfWeek,
        Date date, List<Date> dates,
        Instant instant, LocalDate localDate, LocalTime localTime, LocalDateTime localDateTime,
        Duration duration, Period period,
        UUID uuid) {
        return mapOf(
            entry("string", string),
            entry("strings", strings),
            entry("number", number),
            entry("flag", flag),
            entry("dayOfWeek", dayOfWeek),
            entry("date", date),
            entry("dates", dates),
            entry("instant", instant),
            entry("localDate", localDate),
            entry("localTime", localTime),
            entry("localDateTime", localDateTime),
            entry("duration", duration),
            entry("period", period),
            entry("uuid", uuid)
        );
    }

    @RequestMethod("GET")
    @ResourcePath("a/?/b/?/c/?/d/?")
    public Map<String, Object> testKeys(
        @Description("First key.") int a,
        @Description("Second key.") String b,
        @Description("Third key.") int c,
        @Description("Fourth key.") String d
    ) {
        return mapOf(
            entry("a", a),
            entry("b", b),
            entry("c", c),
            entry("d", d)
        );
    }

    @RequestMethod("POST")
    @ResourcePath("foo/?/bar/?")
    public Map<String, Object> testParameters(int x, int y, int a, int b, List<Double> values) {
        return mapOf(
            entry("x", x),
            entry("y", y),
            entry("a", a),
            entry("b", b),
            entry("values", values)
        );
    }

    @RequestMethod("GET")
    @ResourcePath("fibonacci")
    public List<Number> testGetFibonacci(int count) {
        return new FibonacciSequence(count);
    }

    @RequestMethod("GET")
    @ResourcePath("c")
    public C testGetC() {
        return null;
    }

    @RequestMethod("GET")
    @ResourcePath("e")
    public E testGetE() {
        return null;
    }

    @RequestMethod("GET")
    @ResourcePath("list")
    public TestList testGetList() {
        return new TestList();
    }

    @RequestMethod("GET")
    @ResourcePath("map")
    public TestMap testGetMap() {
        return new TestMap();
    }

    @RequestMethod("POST")
    public Response testPost(@Required String string, List<String> strings,
        int number, boolean flag, DayOfWeek dayOfWeek,
        Date date, List<Date> dates,
        Instant instant, LocalDate localDate, LocalTime localTime, LocalDateTime localDateTime,
        Duration duration, Period period,
        UUID uuid, List<URL> attachments) throws IOException {
        List<Map<String, ?>> attachmentInfo = new LinkedList<>();

        for (var attachment : attachments) {
            long bytes = 0;
            long checksum = 0;

            try (var inputStream = attachment.openStream()) {
                int b;
                while ((b = inputStream.read()) != -1) {
                    bytes++;
                    checksum += b;
                }
            }

            attachmentInfo.add(mapOf(
                entry("bytes", bytes),
                entry("checksum", checksum)
            ));
        }

        return BeanAdapter.coerce(mapOf(
            entry("string", string),
            entry("strings", strings),
            entry("number", number),
            entry("flag", flag),
            entry("dayOfWeek", dayOfWeek),
            entry("date", date),
            entry("dates", dates),
            entry("instant", instant),
            entry("localDate", localDate),
            entry("localTime", localTime),
            entry("localDateTime", localDateTime),
            entry("duration", duration),
            entry("period", period),
            entry("uuid", uuid),
            entry("attachmentInfo", attachmentInfo)
        ), Response.class);
    }

    @RequestMethod("POST")
    @ResourcePath("list")
    public List<String> testPostList(List<String> list) {
        return list;
    }

    @RequestMethod("POST")
    @ResourcePath("map")
    public Map<String, Double> testPostMap(Map<String, Double> map) {
        return map;
    }

    @RequestMethod("POST")
    @ResourcePath("body")
    public Body testPostBody(Body body) {
        body.getString();

        return body;
    }

    @RequestMethod("POST")
    @ResourcePath("coordinates")
    public Coordinates testPostCoordinates(Coordinates coordinates) {
        return coordinates;
    }

    @RequestMethod("POST")
    @ResourcePath("image")
    public void testPostImage() throws IOException {
        echo();
    }

    @RequestMethod("PUT")
    public void testPut() throws IOException {
        echo();
    }

    @RequestMethod("PUT")
    @ResourcePath("?")
    public int testEmptyPut(int value) {
        return value;
    }

    private void echo() throws IOException {
        InputStream inputStream = getRequest().getInputStream();
        OutputStream outputStream = getResponse().getOutputStream();

        int b;
        while ((b = inputStream.read()) != -1) {
            outputStream.write(b);
        }

        outputStream.flush();
    }

    @RequestMethod("DELETE")
    @ResourcePath("?")
    public Integer testDelete(Integer id) {
        return id;
    }

    @RequestMethod("GET")
    @ResourcePath("headers")
    public Map<String, String> testHeaders() {
        var request = getRequest();

        return mapOf(
            entry("X-Header-A", request.getHeader("X-Header-A")),
            entry("X-Header-B", request.getHeader("X-Header-B"))
        );
    }

    @RequestMethod("GET")
    @ResourcePath("deprecated")
    @Deprecated
    public void testDeprecated() {
        // No-op
    }

    @RequestMethod("GET")
    @ResourcePath("error")
    public void testError() throws Exception {
        throw new Exception("Sample error message.");
    }

    @RequestMethod("GET")
    public int testTimeout(int value, int delay) throws InterruptedException {
        Thread.sleep(delay);

        return value;
    }

    @RequestMethod("GET")
    @ResourcePath("math/sum")
    public double getSum(double a, double b) {
        return getInstance(MathService.class).getSum(a, b);
    }

    @RequestMethod("GET")
    @ResourcePath("math/sum")
    public double getSum(List<Double> values) {
        return getInstance(MathService.class).getSum(values);
    }
}
