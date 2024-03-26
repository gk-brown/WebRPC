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

package org.httprpc.kilo.test

import org.httprpc.kilo.WebServiceProxy
import org.httprpc.kilo.beans.BeanAdapter
import org.httprpc.kilo.io.JSONEncoder
import java.net.URL

fun main() {
    val baseURL = URL("https://jsonplaceholder.typicode.com/")

    val userServiceProxy = WebServiceProxy.of(UserServiceProxy::class.java, baseURL) { webServiceProxy ->
        webServiceProxy.monitorStream = System.out
    }

    val users = userServiceProxy.getUsers()

    println()

    for (user in users) {
        println("${user.name} (${user.username})")
        println("${user.email}")

        val address = user.address

        if (address != null) {
            println("${address.street}, ${address.suite}, ${address.city} ${address.zipCode}")

            val geolocation = address.geolocation

            if (geolocation != null) {
                println("${geolocation.latitude}, ${geolocation.longitude}")
            }
        }

        val company = user.company

        if (company != null) {
            println("${company.name} (\"${company.catchphrase}\")")
            println("${company.bs}")
        }

        println()
    }

    val jsonEncoder = JSONEncoder()

    jsonEncoder.write(BeanAdapter.adapt(users), System.out)

    println()
}
