package example.delsart.modalbottomsheet

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import work.delsart.modalbottomsheet.BottomSheet
import work.delsart.modalbottomsheet.LocalBottomSheetState
import work.delsart.modalbottomsheet.TAG
import work.delsart.modalbottomsheet.ui.theme.ModalBottomSheetTheme

class MainActivity : ComponentActivity() {

    private var sheetContentList = mutableListOf<@Composable () -> Unit>(
        { FullScreenSheetContent() },
        { SheetContent() }
    )
    var sheetContentState by mutableStateOf(0)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ModalBottomSheetTheme {
                val coroutineScope = rememberCoroutineScope()
                // bottomSheet
                BottomSheet(sheetContent = {
                    val bottomSheetState = LocalBottomSheetState.current

                    Row {


                        Button(onClick = {
                            coroutineScope.launch {
                                bottomSheetState.hide()
                            }
                        }) {
                            Text(text = "hide")
                        }



                        Button(onClick = {
                            sheetContentState = (sheetContentState + 1) % 2
                            Log.d(TAG, "onCreate: $sheetContentState")

                        }) {
                            Text(text = "switch")
                        }
                    }

                    sheetContentList[sheetContentState]()

                }) {
                    val bottomSheetState = LocalBottomSheetState.current

                    Button(onClick = {
                        coroutineScope.launch {
                            bottomSheetState.show()
                        }
                    }) {
                        Text(text = "show")
                    }
                }
            }
        }
    }

    @Composable
    fun FullScreenSheetContent() {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(100) {
                Text(text = "hello world")
            }

        }
    }

    @Composable
    fun SheetContent() {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(text = "hello world")
        }
    }


}


