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

package org.httprpc;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import static org.httprpc.WebServiceProxy.mapOf;
import static org.httprpc.WebServiceProxy.entry;

/**
 * Test servlet.
 */
@WebServlet(urlPatterns={"/test/*"}, loadOnStartup=1)
@MultipartConfig
public class TestServlet extends HttpServlet {
    private static final long serialVersionUID = 0;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String value = request.getParameter("value");

        Object result;
        if (value == null) {
            result = mapOf(
                entry("string", request.getParameter("string")),
                entry("strings", Arrays.asList(request.getParameterValues("strings"))),
                entry("number", Integer.parseInt(request.getParameter("number"))),
                entry("flag", Boolean.parseBoolean(request.getParameter("flag")))
            );
        } else {
            String delay = request.getParameter("delay");

            try {
                Thread.sleep((delay == null) ? 0 : Integer.parseInt(delay));
            } catch (InterruptedException exception) {
                throw new ServletException(exception);
            }

            result = Integer.parseInt(value);
        }

        writeResult(result, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (request.getCharacterEncoding() == null) {
            request.setCharacterEncoding("UTF-8");
        }

        LinkedList<Map<String, ?>> attachmentInfo = new LinkedList<>();

        for (Part part : request.getParts()) {
            String submittedFileName = part.getSubmittedFileName();

            if (submittedFileName == null || submittedFileName.length() == 0) {
                continue;
            }

            if (part.getName().equals("attachments")) {
                long bytes = 0;
                long checksum = 0;

                try (InputStream inputStream = part.getInputStream()) {
                    int b;
                    while ((b = inputStream.read()) != -1) {
                        bytes++;
                        checksum += b;
                    }
                }

                attachmentInfo.add(mapOf(
                    entry("bytes", bytes),
                    entry("checksum", checksum))
                );
            }
        }

        writeResult(mapOf(
            entry("string", request.getParameter("string")),
            entry("strings", Arrays.asList(request.getParameterValues("strings"))),
            entry("number", Integer.parseInt(request.getParameter("number"))),
            entry("flag", Boolean.parseBoolean(request.getParameter("flag"))),
            entry("attachmentInfo", attachmentInfo)
        ), response);
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws IOException {
        writeResult(request.getParameter("text").equals("héllo") ? "göodbye" : null, response);
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws IOException {
        writeResult(Integer.parseInt(request.getParameter("id")) == 101, response);
    }

    private void writeResult(Object result, HttpServletResponse response) throws IOException {
        response.setContentType("application/json;charset=UTF-8");

        JSONEncoder encoder = new JSONEncoder();

        encoder.writeValue(result, response.getOutputStream());
    }
}
