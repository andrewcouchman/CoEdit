package com.andrewcouchman.coedit

import org.http4k.core.*
import org.http4k.core.Credentials
import org.http4k.filter.ServerFilters
import org.http4k.routing.bind
import org.http4k.routing.poly
import org.http4k.routing.routes
import org.http4k.routing.websocket.bind as wsBind
import org.http4k.routing.websockets
import org.http4k.server.Undertow
import org.http4k.server.asServer
import org.http4k.websocket.WsMessage
import org.http4k.websocket.WsResponse
import org.http4k.websocket.Websocket
import java.util.concurrent.CopyOnWriteArrayList

// Shared document state (naively maintained as a simple String)
@Volatile
var document: String = ""

// Thread-safe list to track active WebSocket connections
val activeConnections = CopyOnWriteArrayList<Websocket>()

fun main() {
    // Define an auth filter with a hardcoded username and password.
    val authFilter = ServerFilters.BasicAuth("editor", { credentials: Credentials ->
        credentials.user == "user" && credentials.password == "password"
    })

    // Define the WebSocket endpoint at "/ws"
    val ws = websockets(
        "/ws" wsBind { _: Request ->
            WsResponse { ws: Websocket ->
                println("client connected. sending: $document")
                ws.send(WsMessage(document))
                activeConnections.add(ws)

                ws.onMessage { msg ->
                    println("message received: $msg")
                    document = msg.bodyString()
                    activeConnections.filterNot { it == ws }.forEach { connection ->
                        connection.send(WsMessage(document))
                    }
                }

                ws.onClose {
                    activeConnections.remove(ws)
                }
            }
        }
    )

    // Define the HTTP endpoint serving the editor UI at "/editor"
    val http = routes(
        "/editor" bind { _: Request ->
            Response(Status.OK).body(
                """
                <!DOCTYPE html>
                <html>
                <head>
                  <meta charset="utf-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <title>Collaborative Editor</title>
                  <!-- Load Ace Editor from CDN -->
                  <script src="https://cdnjs.cloudflare.com/ajax/libs/ace/1.4.12/ace.js"></script>
                  <style>
                    html, body, #editor {
                      margin: 0;
                      padding: 0;
                      width: 100%;
                      height: 100%;
                    }
                  </style>
                </head>
                <body>
                  <div id="editor">Loading document...</div>
                  <script>
                    // Initialize Ace Editor.
                    var editor = ace.edit("editor");
                    var updating = false;
                    editor.setTheme("ace/theme/monokai");
                    editor.session.setMode("ace/mode/text");
                    
                    // Connect to the WebSocket endpoint.
                    var socket = new WebSocket("ws://" + window.location.host + "/ws");
                    socket.onopen = function() {
                      console.log("Connected to WebSocket server.");
                    };
                    socket.onmessage = function(event) {
                      // Update the editor content with the received document.
                      updating = true;
                      editor.setValue(event.data, -1);
                      updating = false;
                    };
                    // On every change, send the full document text.
                    editor.on('change', function() {
                      if (!updating) {
                        socket.send(editor.getValue());
                      }
                    });
                  </script>
                </body>
                </html>
                """.trimIndent()
            )
        }
    )

    // Combine the HTTP and WebSocket endpoints using poly.
    val app = poly(http, ws)

    // Wrap the entire app with the auth filter.
    val appWithAuth = authFilter.then(app)

    // Start the server on port 9111 using Undertow.
    val port = 9111
    appWithAuth.asServer(Undertow(port)).start()
    println("Server started at http://localhost:$port/editor")
}
