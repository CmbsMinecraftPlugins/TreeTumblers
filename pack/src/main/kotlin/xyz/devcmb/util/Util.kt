package xyz.devcmb.util

import java.io.IOException
import java.net.URI
import java.net.URISyntaxException
import java.net.URL
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Paths

fun listResourcesRobust(resourcePath: String = ""): List<String> {
    val classLoader = Thread.currentThread().contextClassLoader ?: ResourcePath::class.java.classLoader
    val resourceUrl = classLoader.getResource(resourcePath) ?: return emptyList()

    return try {
        when (resourceUrl.protocol) {
            "file" -> listFilesFromFileSystem(resourceUrl.toURI())
            "jar" -> listFilesFromJar(resourceUrl)
            else -> emptyList()
        }
    } catch (e: URISyntaxException) {
        emptyList()
    } catch (e: IOException) {
        emptyList()
    }
}

private fun listFilesFromFileSystem(uri: URI): List<String> {
    val path = Paths.get(uri)
    return Files.list(path)
        .filter(Files::isRegularFile)
        .map { it.fileName.toString() }
        .toList()
}

private fun listFilesFromJar(url: URL): List<String> {
    val jarUri = url.toURI()
    val jarPath = jarUri.path.substringAfter("file:").substringBefore("!")
    val entries = FileSystems.newFileSystem(URI.create("jar:file:$jarPath"), emptyMap<String, String>())

    return Files.walk(entries.getPath(jarUri.path.substringAfter("!")))
        .filter(Files::isRegularFile)
        .map { it.fileName.toString() }
        .toList()
        .also { entries.close() }
}