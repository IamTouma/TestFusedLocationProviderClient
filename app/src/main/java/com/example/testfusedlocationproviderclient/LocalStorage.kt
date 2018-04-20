package com.example.testfusedlocationproviderclient

import android.content.Context
import java.io.*

class LocalStorage(val filePath: String) {

    fun Write(log: String) {
        try  {
            FileOutputStream(filePath, true).use {
                OutputStreamWriter(it, "UTF-8").use {
                    BufferedWriter(it).use {
                        it.write(log)
                        it.flush()
                    }
                }
            }
        }
        catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun Read(log: String): String {

        var buf = StringBuffer("")

        try  {
            FileInputStream(filePath).use {
                InputStreamReader(it, "UTF-8").use {
                    BufferedReader(it).use {
                        var lineBuffer: String = ""

                        do {
                            var lineBuffer: String? = it.readLine()
                            if (lineBuffer != null) {
                                buf.append(lineBuffer)
                                buf.append(System.getProperty("line.separator"))
                            }
                        } while (lineBuffer != null)
                    }
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return buf.toString()
    }
}
