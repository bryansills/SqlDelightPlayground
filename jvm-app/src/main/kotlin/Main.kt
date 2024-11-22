import kotlinx.coroutines.*
import app.cash.sqldelight.async.coroutines.awaitAsList
import ninja.bryansills.sqldelightplayground.DriverFactory
import ninja.bryansills.sqldelightplayground.Get_all_playlists_with_songs
import ninja.bryansills.sqldelightplayground.createDatabase
import ninja.bryansills.sqldelightplayground.preloadDatabase
import kotlin.system.exitProcess

fun main() {
    val mainScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    mainScope.launchBlocking {
        val database = createDatabase(DriverFactory())
        val queries = database.songQueries

        database.preloadDatabase()

        val results: List<Get_all_playlists_with_songs> = queries.get_all_playlists_with_songs().awaitAsList()
        val groupedPlaylists = results.groupBy { it.playlistName }
        groupedPlaylists.entries.forEach { entry ->
            println("playlist: ${entry.key} has ${entry.value.size} songs")
        }
    }
}

/**
 * Do some work and then kill the process. We don't want Android Studio to think that we are still
 * waiting for work to finish.
 */
private fun CoroutineScope.launchBlocking(block: suspend CoroutineScope.() -> Unit) {
    try {
        runBlocking(this.coroutineContext) {
            block()
            this.cancel("Time to die.")
        }
    } catch (ex: Exception) {
        if (ex !is CancellationException) {
            ex.printStackTrace()
        }
    }

    exitProcess(0)
}
