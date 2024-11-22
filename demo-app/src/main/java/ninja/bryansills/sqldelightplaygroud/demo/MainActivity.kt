package ninja.bryansills.sqldelightplaygroud.demo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ninja.bryansills.sqldelightplaygroud.demo.ui.theme.SqlDelightPlaygroundTheme
import ninja.bryansills.sqldelightplayground.Database
import ninja.bryansills.sqldelightplayground.DriverFactory
import ninja.bryansills.sqldelightplayground.createDatabase
import ninja.bryansills.sqldelightplayground.preloadDatabase

class MainActivity : ComponentActivity() {

    private lateinit var database: Database
    private var hasLoadedDatabase by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        database = createDatabase(DriverFactory(this.applicationContext))
        lifecycleScope.launch {
            database.preloadDatabase()
            hasLoadedDatabase = true
        }

        enableEdgeToEdge()
        setContent {
            SqlDelightPlaygroundTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    if (hasLoadedDatabase) {
                        val items by database
                            .songQueries
                            .get_all_songs()
                            .asFlow()
                            .mapToList(Dispatchers.IO)
                            .collectAsState(listOf())

                        LazyColumn(modifier = Modifier.padding(innerPadding)) {
                            items(items) { song ->
                                Column(modifier = Modifier.padding(4.dp)) {
                                    Text(text = "Title: ${song.name}")
                                    Text(text = "Played at: ${song.played_at}")
                                }
                            }
                        }
                    } else {
                        Column(modifier = Modifier.padding(innerPadding)) {
                            Text(text = "loading")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    SqlDelightPlaygroundTheme {
        Greeting("Android")
    }
}