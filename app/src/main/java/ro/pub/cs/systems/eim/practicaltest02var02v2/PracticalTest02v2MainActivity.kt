package ro.pub.cs.systems.eim.practicaltest02var02v2

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import ro.pub.cs.systems.eim.practicaltest02var02v2.R
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket
import java.net.URL

class PracticalTest02v2MainActivity : AppCompatActivity() {

    private var serverThread: ServerThread? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_practical_test02v2_main)

        val tvResult = findViewById<TextView>(R.id.tvResult)
        val etServerPort = findViewById<EditText>(R.id.etServerPort)
        val etClientAddress = findViewById<EditText>(R.id.etClientAddress)
        val etClientPort = findViewById<EditText>(R.id.etClientPort)
        val etQuery = findViewById<EditText>(R.id.etQuery)

        findViewById<Button>(R.id.btnStartServer).setOnClickListener {
            val portText = etServerPort.text.toString()

            if (portText.isNotEmpty()) {
                val port = portText.toInt()
                serverThread = ServerThread(port)
                serverThread?.start()

                Toast.makeText(this, "Server started on port $port", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Please enter a server port!", Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<Button>(R.id.btnGetInfo).setOnClickListener {
            val addr = etClientAddress.text.toString()
            val portText = etClientPort.text.toString()
            val query = etQuery.text.toString()

            if (addr.isEmpty() || portText.isEmpty() || query.isEmpty()) {
                Toast.makeText(this, "Fill all client fields!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val port = portText.toInt()

            Toast.makeText(this, "Sending '$query' to $addr:$port...", Toast.LENGTH_SHORT).show()

            Thread {
                try {
                    val socket = Socket(addr, port)
                    val writer = PrintWriter(socket.getOutputStream(), true)
                    val reader = BufferedReader(InputStreamReader(socket.getInputStream()))

                    writer.println(query)

                    val response = reader.readLine()

                    runOnUiThread { tvResult.text = response }

                    socket.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                    runOnUiThread { tvResult.text = "Connection Error: ${e.message}" }
                }
            }.start()
        }
    }

    class ServerThread(private val port: Int) : Thread() {
        private val cache = HashMap<String, String>()

        override fun run() {
            try {
                val serverSocket = ServerSocket(port)
                Log.d("Server", "Waiting for clients on port $port...")
                while (true) {
                    val socket = serverSocket.accept()
                    CommunicationThread(socket, cache).start()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    class CommunicationThread(private val socket: Socket, private val cache: HashMap<String, String>) : Thread() {
        override fun run() {
            try {
                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                val writer = PrintWriter(socket.getOutputStream(), true)

                val request = reader.readLine()

                if (request.isNullOrEmpty()) {
                    socket.close()
                    return
                }

                var resultMessage = ""

                try {
                    val parts = request.trim().split(",")

                    if (parts.size == 3) {
                        val operation = parts[0].lowercase().trim()

                        val op1Str = parts[1].trim()
                        val op2Str = parts[2].trim()

                        val op1 = op1Str.toLong()
                        val op2 = op2Str.toLong()

                        if (op1 > Int.MAX_VALUE || op1 < Int.MIN_VALUE ||
                            op2 > Int.MAX_VALUE || op2 < Int.MIN_VALUE) {
                            resultMessage = "overflow"
                        }

                        var resultLong: Long = 0
                        var isOperationValid = true

                        when (operation) {
                            "add", "+" -> resultLong = op1 + op2
                            "mul", "*" -> {
                                resultLong = op1 * op2
                                try {
                                    Thread.sleep(2000)
                                } catch (e: InterruptedException) {
                                    e.printStackTrace()
                                }
                            }
                        }

                        if (!isOperationValid) {
                            resultMessage = "Error: Invalid operation or division by zero"
                        } else {
                            if (resultLong > Int.MAX_VALUE || resultLong < Int.MIN_VALUE) {
                                resultMessage = "Overflow"
                            } else {
                                resultMessage = resultLong.toString()
                                cache[request] = resultMessage
                            }
                        }
                    } else {
                        resultMessage = "Error: Format invalid. Foloseste 'operatie,nr1,nr2'"
                    }

                } catch (e: NumberFormatException) {
                    resultMessage = "Error: Numerele sunt invalide (sau prea mari pt Long)!"
                } catch (e: Exception) {
                    resultMessage = "Server Error: ${e.message}"
                }

                Log.d("Server", "Calculated: $request = $resultMessage")
                writer.println(resultMessage)

                socket.close()

            } catch (e: Exception) { e.printStackTrace() }
        }
    }
}